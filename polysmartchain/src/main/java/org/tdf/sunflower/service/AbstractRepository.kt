package org.tdf.sunflower.service

import org.tdf.common.event.EventBus
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.facade.DatabaseStoreFactory
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.RepositoryWriter
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.Header

abstract class AbstractRepository
    (
    protected val eventBus: EventBus,
    protected val factory: DatabaseStoreFactory,
    protected val accountTrie: AccountTrie,
) : RepositoryReader, RepositoryWriter {


    var genesisCache: Block? = null

    override val genesis: Block
        get() = genesisCache!!

    protected abstract fun writeGenesis(genesis: Block)

    override fun saveGenesis(b: Block) {
        genesisCache = b
        val o = getBlocksByHeight(0)
        if (o.isEmpty()) {
            writeGenesis(genesisCache!!)
            return
        }
        if (o.size > 1 || o.stream().anyMatch { x: Block -> x.hash != b.hash }) {
            throw RuntimeException("genesis in db not equals to genesis in configuration")
        }
    }

    protected abstract fun getBlockFromHeader(header: Header): Block

    private fun getBlocksFromHeaders(headers: Collection<Header>): List<Block> {
        return headers.map { getBlockFromHeader(it) }
    }

    override fun getCanonicalBlock(height: Long): Block? {
        return getCanonicalHeader(height)?.let { getBlockFromHeader(it) }
    }

    override val bestBlock: Block
        get() = getBlockFromHeader(bestHeader)


    override fun getBlocksBetween(startHeight: Long, stopHeight: Long, limit: Int, descend: Boolean): List<Block> {
        return getBlocksFromHeaders(getHeadersBetween(startHeight, stopHeight, limit, descend))
    }

    override fun getBlocksByHeight(height: Long): List<Block> {
        return getBlocksFromHeaders(getHeadersByHeight(height))
    }

    override fun getBlockByHash(hash: HexBytes): Block? {
        return getHeaderByHash(hash)?.let { getBlockFromHeader(it) }
    }
}