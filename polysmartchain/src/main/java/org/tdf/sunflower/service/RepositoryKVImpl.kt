package org.tdf.sunflower.service

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import org.tdf.common.event.EventBus
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.Store
import org.tdf.common.store.StoreWrapper
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.events.NewBestBlock
import org.tdf.sunflower.facade.DatabaseStoreFactory
import org.tdf.sunflower.facade.TransactionInfo
import org.tdf.sunflower.facade.blockHash
import org.tdf.sunflower.facade.index
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.types.*
import java.util.*

class RepositoryKVImpl(
        bus: EventBus,
        factory: DatabaseStoreFactory,
        accountTrie: AccountTrie,
        override val genesisCfg: AbstractGenesis
) : AbstractRepository(
        bus, factory, accountTrie
) {

    // transaction hash -> transaction
    private val transactionsStore: Store<HexBytes, Transaction>

    // block hash -> header
    private val headerStore: Store<HexBytes, Header>

    // transactions root -> transaction hashes
    private val transactionsRoot: Store<HexBytes, Array<HexBytes>>

    // block height -> block hashes, the first hash is canonical
    private val heightIndex: Store<Long, Array<HexBytes>>

    // "best" -> best header, "prune" -> pruned header
    private val status: Store<String, Header>

    // transaction hash -> receipts
    private val transactionIndices: Store<HexBytes, Array<TransactionIndex>>

    private val cache: Cache<HexBytes, List<Pair<Long, ByteArray>>> =
            CacheBuilder.newBuilder().maximumSize(8).build()

    override fun saveGenesis(b: Block) {
        super.saveGenesis(b)
        if (status[BEST_HEADER] == null) {
            status[BEST_HEADER] = b.header
        }
    }

    override fun getBlockFromHeader(header: Header): Block {
        val txHashes = transactionsRoot[header.transactionsRoot]
                ?: throw RuntimeException("transactions of header $header not found")
        return Block(header, txHashes.map { transactionsStore[it]!! })
    }

    override fun containsHeader(hash: HexBytes): Boolean {
        return headerStore[hash] != null
    }

    override val bestHeader: Header
        get() = status[BEST_HEADER]!!

    override fun getHeaderByHash(hash: HexBytes): Header? {
        return headerStore[hash]
    }

    override fun getHeadersBetween(startHeight: Long, stopHeight: Long, limit: Int, descend: Boolean): List<Header> {
        val ret: MutableList<Header> = mutableListOf()
        val range = if (descend) {
            stopHeight downTo startHeight
        } else {
            startHeight..stopHeight
        }

        for (i in range) {
            val idx = heightIndex[i] ?: continue
            for (bytes in idx) {
                val h = headerStore[bytes]!!
                ret.add(h)
                if (ret.size == limit)
                    return ret
            }
        }
        return ret
    }

    private fun getCanonicalHashAt(height: Long): HexBytes? {
        return heightIndex[height]?.getOrNull(0)
    }

    private fun setCanonicalHashAt(height: Long, hash: HexBytes) {
        val set = heightIndex[height]?.toMutableSet() ?: mutableSetOf()
        set.remove(hash)
        heightIndex[height] = arrayOf(hash) + set.toTypedArray()
    }

    override fun getHeadersByHeight(height: Long): List<Header> {
        return (heightIndex[height] ?: emptyArray()).map { getHeaderByHash(it)!! }
    }

    override fun writeBlock(b: Block, infos: List<TransactionInfo>) {
        begin("writeBlock")
        writeBlockNoReset(b, infos)
        val best = bestBlock
        if (Block.BEST_COMPARATOR.compare(best, b) < 0) {
            status[BEST_HEADER] = b.header
            var hash = b.hash
            begin("reset canonical hash")
            while (true) {
                // reset canonical hash
                val o = headerStore[hash] ?: break
                val canonicalHash = getCanonicalHashAt(o.height)
                if (canonicalHash == hash && hash != b.hash)
                    break
                log.debug("reset {} from {} to hash {}", o.height, canonicalHash, hash)
                setCanonicalHashAt(o.height, hash)
                hash = o.hashPrev
            }
            end()
            begin("publish event")
            eventBus.publish(NewBestBlock(b))
            end()
        }
        end()
    }

    override fun deleteGT(height: Long) {
        val b = bestHeader
        val c = getCanonicalHeader(height)!!

        for (i in (c.height + 1)..b.height) {
            // get height indices
            heightIndex[i]?.forEach {
                // remove header
                headerStore.remove(it)
            }
            heightIndex.remove(i)
        }
        status[BEST_HEADER] = c
    }

    override fun canonicalize() {
        val best = bestBlock
        var hash = best.hash
        while (true) {
            // reset canonical hash
            val o = headerStore[hash]!!
            if (o.height == 0L) break
            setCanonicalHashAt(o.height, hash)
            heightIndex[o.height]?.filter { it != hash }?.forEach {
                headerStore.remove(it)
            }
            heightIndex[o.height] = arrayOf(hash)
            hash = o.hashPrev
        }
    }

    override fun containsTransaction(hash: HexBytes): Boolean {
        return transactionsStore[hash] != null
    }

    private fun isCanonical(h: Header): Boolean {
        val hash = getCanonicalHashAt(h.height)
        return hash?.equals(h.hash) ?: false
    }

    private fun isCanonical(hash: HexBytes): Boolean {
        val h = headerStore[hash] ?: return false
        return isCanonical(h)
    }

    override fun writeGenesis(genesis: Block) {
        writeBlockNoReset(genesis, emptyList())
        setCanonicalHashAt(0L, genesis.hash)
    }

    private fun writeBlockNoReset(block: Block, infos: List<TransactionInfo>) {
        begin("writeBlockNoReset")
        // ensure the state root exists
        begin("accountTrie.trieStore")
        val v = accountTrie.trieStore[block.stateRoot.bytes]
        if (block.stateRoot != accountTrie.trie.nullHash
                && (v == null || v.isEmpty())
        ) {
            throw RuntimeException("unexpected error: account trie " + block.stateRoot + " not synced")
        }
        end()
        // if the block has written before
        if (containsHeader(block.hash)) return
        // write header into store
        begin("set header store")
        headerStore[block.hash] = block.header
        end()

        begin("save transaction and infos")
        // save transaction and transaction infos
        for (i in block.body.indices) {
            val t = block.body[i]
            // save transaction
            transactionsStore[t.hash] = t

            // skip coinbase transaction
            if (i == 0)
                continue

            val info = infos[i]
            val found = transactionIndices[t.hash]

            val founds: MutableList<TransactionIndex> = found?.toMutableList() ?: mutableListOf()
            if (founds.none
                    { it.blockHash == info.blockHash }
            ) {
                founds.add(info.index)
            }
            transactionIndices[t.hash] = founds.toTypedArray()
        }
        end()

        begin("save tx roots")
        // save transaction root -> tx hashes
        val txHashes: Array<HexBytes> = block.body.map { it.hash }.toTypedArray()
        transactionsRoot[block.transactionsRoot] = txHashes
        end()

        begin("save header index")
        // save header index
        val headerHashes: MutableList<HexBytes> = heightIndex[block.height]?.toMutableList() ?: mutableListOf()
        headerHashes.remove(block.hash)
        headerHashes.add(block.hash)
        heightIndex[block.height] = headerHashes.toTypedArray()
        end()

        log.info("write block at height " + block.height + " " + block.header.hash + " to database success")
        end()
    }

    private val li: Stack<Pair<String, Long>> = Stack()

    private fun begin(msg: String) {
        if(!log.isDebugEnabled) return
        li.push(Pair(msg, System.currentTimeMillis()))
    }

    private fun end() {
        if(!log.isDebugEnabled) return
        val x = li.pop()
        log.debug("${x.first} consume ${(System.currentTimeMillis() - x.second) / 1000.0} second")
    }

    override fun getCanonicalHeader(height: Long): Header? {
        val v = getCanonicalHashAt(height)
        return v?.let { getHeaderByHash(it) }
    }

    override fun createBlockHashMap(hash: HexBytes): List<Pair<Long, ByteArray>> {
        val m = cache.asMap()[hash]
        if (m != null)
            return m

        // lookup by parent
        val h = getHeaderByHash(hash)!!
        val prev = cache.asMap()[h.hashPrev]

        if (prev != null) {
            // parent found, return self + parent
            // ensure size <= 255
            val np = prev.subList(if (prev.size < 255) 0 else prev.size - 255, prev.size).toMutableList()
            // add to last
            np.add(Pair(h.height, h.hash.bytes))
            cache.asMap()[hash] = np
            return np
        }

        val np = mutableListOf<Pair<Long, ByteArray>>()
        var now = h

        while (np.size <= 256) {
            np.add(0, Pair(now.height, now.hash.bytes))

            if (now.height == 0L)
                break
            now = getHeaderByHash(now.hashPrev)!!
        }

        cache.asMap()[hash] = np
        return np
    }

    override fun getTransactionInfo(hash: HexBytes): TransactionInfo? {
        val infos = transactionIndices[hash] ?: emptyArray()
        val i = infos.firstOrNull { isCanonical(it.blockHash) } ?: return null
        return TransactionInfo(i, transactionsStore[hash]!!)
    }

    companion object {
        private const val BEST_HEADER = "best"
        private val log = LoggerFactory.getLogger("db")
    }

    init {
        transactionsStore = StoreWrapper(
                factory.create('b', "transaction"),
                Codecs.hex,
                Transaction.Companion
        )

        headerStore = StoreWrapper(
                factory.create('h', "block header"),
                Codecs.hex,
                Header.Companion
        )
        transactionsRoot = StoreWrapper(
                factory.create('t', "transaction root"),
                Codecs.hex,
                Codecs.rlp(Array<HexBytes>::class.java)
        )
        heightIndex = StoreWrapper(
                factory.create('i', "height index"),
                Codecs.rlp(Long::class.java),
                Codecs.rlp(Array<HexBytes>::class.java)
        )
        status = StoreWrapper(
                factory.create('s', "status"),
                Codecs.string,
                Header.Companion
        )
        transactionIndices = StoreWrapper(
                factory.create('f', "transaction index"),
                Codecs.hex,
                Codecs.rlp(Array<TransactionIndex>::class.java)
        )
    }
}