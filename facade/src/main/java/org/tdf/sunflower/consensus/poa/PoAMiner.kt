package org.tdf.sunflower.consensus.poa

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.sunflower.consensus.AbstractMiner
import org.tdf.sunflower.consensus.poa.config.PoAConfig
import org.tdf.sunflower.events.NewBlockMined
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.RepositoryService
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.HeaderImpl
import org.tdf.sunflower.types.Transaction
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

class PoAMiner(private val poa: PoA) :
    AbstractMiner(poa.accountTrie, poa.eventBus, poa.config, poa.transactionPool) {

    override val chainId: Int
        get() = poa.config.chainId

    private val config: PoAConfig
        get() = poa.config

    val repo: RepositoryService
        get() = poa.repo

    private val executor = FixedDelayScheduler(
        "PoAMiner",
        Math.max(1, poa.config.blockInterval / 2).toLong()
    )

    override fun createHeader(parent: Block, createdAt: Long): Header {
        return HeaderImpl(
            height = parent.height + 1,
            createdAt = createdAt,
            coinbase = config.coinbase ?: throw RuntimeException("poa miner: private key not set"),
            hashPrev = parent.hash,
            gasLimit = parent.gasLimit,
        )
    }

    override fun finalizeBlock(f: Any, parent: Block, block: Block): CompletableFuture<Block> {
        val key =
            ECKey.fromPrivate(config.privateKey?.bytes ?: throw RuntimeException("poa miner: private key not set"))
        val signed = PoAUtils.sign(key, block)
        return CompletableFuture.completedFuture(Block(signed, block.body))
    }

    @Synchronized
    override fun start() {
        executor.delay {
            try {
                this.tryMine()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    override fun stop() {
        executor.shutdownNow()
    }

    private fun tryMine() {
        if (!config.enableMining) {
            return
        }
        repo.writer.use {
            val best = it.bestBlock
            val now = OffsetDateTime.now().toEpochSecond()

            val p = poa.minerContract.getProposer(
                it,
                best.hash,
                now
            )
            if (p.address != config.coinbase) return
            log.debug("try to mining at height " + (best.height + 1))
//            val b = createBlock(it, it.bestBlock, now)
//            if (b.block != null) {
//                log.info("mining success block: {}", b.block.header)
//                it.writeBlock(b.block, b.indices)
//                eventBus.publish(NewBlockMined(b.block, b.indices))
//            }
        }
    }

    override fun createCoinBase(height: Long): Transaction {
        return Transaction(
            nonce = height,
            value = poa.model.rewardAt(height),
            to = config.coinbase ?: throw RuntimeException("poa miner: private key not set"),
        )
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("miner")
    }
}