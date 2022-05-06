package org.tdf.sunflower.pool

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.event.EventBus
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.common.util.HexBytes
import org.tdf.common.util.LogLock
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.TransactionPoolConfig
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.events.NewTransactionsCollected
import org.tdf.sunflower.facade.*
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.VMExecutor
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.math.min

@Component
class TransactionPoolImpl(
    private val eventBus: EventBus,
    private val config: TransactionPoolConfig,
    private val repo: RepositoryService,
    private val trie: StateTrie<HexBytes, Account>,
    private val appCfg: AppConfig,
    private val conCfg: ConsensusConfig
) : TransactionPool {
    companion object {
        val log: Logger = LoggerFactory.getLogger("txPool")
    }

    // waiting, sorted by gasPrice
    private val waiting: TreeSet<WaitingInfo> = TreeSet()

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = LogLock(lock.readLock(), "tp-r")
    private val writeLock = LogLock(lock.writeLock(), "tp-w")

    // dropped transactions
    val cache: Cache<HexBytes, Dropped> =
        CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build()

    override val dropped: MutableMap<HexBytes, Dropped> get() = cache.asMap()

    // pending records
    // waiting -> pending -> included
    private var pendingRec: PendingRec = PendingRec()

    private val clearScheduler = FixedDelayScheduler("txPool-clear", config.expiredIn)

    private lateinit var gasLimit: BigInteger


    fun init() {
        repo.reader.use {
            pendingRec.reset(it.bestHeader)
            gasLimit = it.genesis.gasLimit.toBigInteger()
        }
    }


    /**
     * clear waiting transactions
     */
    private fun clear() {
        val now = System.currentTimeMillis()
        if (!writeLock.tryLock(config.lockTimeout, TimeUnit.SECONDS)) {
            return
        }
        try {
            waiting.removeIf {
                val remove = (now - it.receivedAt) / 1000 > config.expiredIn
                if (remove) {
                    dropped[it.tx.hash] = Dropped(it.tx, "transaction timeout")
                }
                remove
            }
        } finally {
            writeLock.unlock()
        }
    }


    override fun collect(
        rd: RepositoryReader,
        transactions: Collection<Transaction>,
        source: String
    ): Map<HexBytes, String> {
        val errors: MutableMap<HexBytes, String> = mutableMapOf()

        writeLock.withLock {
            val newCollected: MutableList<Transaction> = mutableListOf()
            for (tx in transactions) {
                log.debug(
                    "new tx {} received from {} rpc sender = {}, nonce = {}",
                    tx.hash,
                    source,
                    tx.sender,
                    tx.nonce
                )
                if (tx.gasPrice < appCfg.vmGasPrice) {
                    errors[tx.hash] = "transaction pool: gas price of tx less than vm gas price ${appCfg.vmGasPrice}"
                    continue
                }

                if (rd.containsTransaction(tx.hash))
                    continue

                val drop = dropped[tx.hash]
                if (drop != null) {
                    errors[tx.hash] = drop.err
                    continue
                }

                val info = WaitingInfo(System.currentTimeMillis(), tx)
                if (pendingRec.pending.contains(info) || waiting.contains(info)) {
                    continue
                }

                try {
                    tx.validate()
                } catch (e: Exception) {
                    errors[tx.hash] = e.message ?: "validate transaction failed"
                    continue
                }
                if (tx.vrs == null || !tx.verifySig) {
                    errors[tx.hash] = "validate signature failed"
                    continue
                }

                if (tx.chainId != appCfg.chainId) {
                    errors[tx.hash] = "invalid chainId ${tx.chainId}"
                    continue
                }

                log.debug("add tx {} from {} to waiting sender = {} nonce = {}", tx.hash, source, tx.sender, tx.nonce)
                waiting.add(info)
                newCollected.add(tx)
            }

            pendingRec.execute(rd, waiting.iterator())

            transactions.forEach { t ->
                this.dropped[t.hash]?.let { errors[t.hash] = it.second }
            }
            if (newCollected.isNotEmpty())
                eventBus.publish(NewTransactionsCollected(newCollected))
            return errors
        }
    }

    internal data class PendingRec(
        var backend: Backend? = null,
        var gas: Long = 0,
        var timestamp: Long = 0,
        val pending: MutableList<WaitingInfo> = mutableListOf(),
        val receipts: MutableList<TransactionReceipt> = mutableListOf()
    )

    private fun PendingRec.clear() {
        backend = null
        pending.clear()
        receipts.clear()
        timestamp = 0
        gas = 0
    }

    private fun PendingRec.rollback(best: Header, timestamp: Long? = null) {
        val ts = timestamp ?: if (best.height == 0L) {
            System.currentTimeMillis() / 1000
        } else {
            best.createdAt + conCfg.blockInterval
        }
        waiting.addAll(pending)
        pending.clear()
        receipts.clear()
        this.timestamp = ts
        gas = 0
        backend = trie.createBackend(best)
    }

    // reset pending record to a header
    private fun PendingRec.reset(best: Header) {
        pending.clear()
        receipts.clear()
        gas = 0
        backend = trie.createBackend(best)
        timestamp = if (best.height == 0L) {
            System.currentTimeMillis() / 1000
        } else {
            best.createdAt + conCfg.blockInterval
        }
    }

    // remove from waiting if success, keep unchanged if nonce too big, moved to drop if nonce too small or failed
    private fun PendingRec.execute(rd: RepositoryReader, waiting: MutableIterator<WaitingInfo>) {
        var backend = this.backend ?: return

        while (waiting.hasNext()) {
            val n = waiting.next()
            val t = n.tx
            val prevNonce = backend.getNonce(t.sender)

            if (t.nonce < prevNonce) {
                waiting.remove()
                dropped[t.hash] = Pair(t, "nonce is too small")
                continue
            }

            if (t.nonce != prevNonce) {
                continue
            }

            val blockGasLimit = gasLimit.longValueExact()

            try {
                val child = backend.createChild()
                val ctx = CallContext.fromTx(
                    tx = t,
                    timestamp = this.timestamp,
                    coinbase = conCfg.coinbase ?: AddrUtil.empty(),
                    blockHashMap = rd.createBlockHashMap(child.parentHash).toMap()
                )
                val callData = CallData.fromTx(t)
                val vmExecutor = VMExecutor.create(
                    rd,
                    child, ctx, callData,
                    min(t.gasLimit, blockGasLimit)
                )

                val res = vmExecutor.execute()

                if (res.gasUsed > blockGasLimit) {
                    waiting.remove()
                    dropped[t.hash] = Pair(t, "gas overflows block gas limit")
                    continue
                }

                // execute successfully
                // skip high gas transaction
                if (gas + res.gasUsed > blockGasLimit) {
                    continue
                }

                val receipt = TransactionReceipt(
                    cumulativeGas = gas + res.gasUsed,
                    logInfoList = res.logs,
                    gasUsed = res.gasUsed,
                    result = res.executionResult
                )

                this.pending.add(n)
                this.receipts.add(receipt)
                backend = child
                this.backend = backend
                this.gas += res.gasUsed
                log.debug(
                    "add tx {} to pending, ready to pack, sender = {}, nonce = {} remaining gas = {}",
                    n.tx.hash,
                    n.tx.sender,
                    n.tx.nonce,
                    blockGasLimit - gas
                )
                waiting.remove()
            } catch (e: Exception) {
                e.printStackTrace()
                log.info("dropped tx ${n.tx.hash}, execute failed, gas limit = ${n.tx.gasLimit}")
                dropped[t.hash] = Pair(t, e.message ?: "execute tx error")
                waiting.remove()
            }
        }
    }

    override fun pop(rd: RepositoryReader, parentHeader: Header, timestamp: Long): PendingData {
        writeLock.withLock {
            log.debug("pendings = {}, waitings = {}", this.pendingRec.pending.size, this.waiting.size)
            if (parentHeader.hash != this.pendingRec.backend?.parentHash || timestamp != pendingRec.timestamp) {
                pendingRec.rollback(parentHeader, timestamp)
                pendingRec.execute(rd, waiting.iterator())
            }

            val pending = pendingRec.pending.toList().map { it.tx }

            val receipts = pendingRec.receipts.toList()
            val cur = pendingRec.backend
            pendingRec.clear()
            log.debug("pop {} transactions to miner", pending.size)
            return PendingData(pending, receipts, cur)
        }
    }

    override fun reset(parent: Header) {
        writeLock.withLock {
            pendingRec.reset(parent)
        }
    }

    override fun current(): Backend? {
        readLock.withLock {
            return pendingRec.backend?.createChild()
        }
    }

    private fun onNewBestBlock(event: NewBestBlock) {
        if (!writeLock.tryLock()) {
            return
        }
        try {
            if (event.block.hash == this.pendingRec.backend?.parentHash) return
            pendingRec.rollback(event.block)
        } finally {
            writeLock.unlock()
        }
    }

    internal class WaitingInfo(val receivedAt: Long, val tx: Transaction) : Comparable<WaitingInfo> {
        override fun compareTo(other: WaitingInfo): Int {
            if (tx.hash == other.tx.hash)
                return 0
            var cmp = -tx.gasPrice.compareTo(other.tx.gasPrice)
            if (cmp != 0) return cmp
            cmp = tx.sender.compareTo(other.tx.sender)
            if (cmp != 0) return cmp
            cmp = tx.nonce.compareTo(other.tx.nonce)
            if (cmp != 0) return cmp
            return tx.hash.compareTo(other.tx.hash)
        }
    }

    init {
        eventBus.subscribe(NewBestBlock::class.java) { onNewBestBlock(it) }
        clearScheduler.delay {
            try {
                clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}