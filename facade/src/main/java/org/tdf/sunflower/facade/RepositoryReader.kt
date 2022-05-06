package org.tdf.sunflower.facade

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.*
import java.io.Closeable

typealias TransactionInfo = Pair<TransactionIndex, Transaction>

val TransactionInfo.index: TransactionIndex get() = first
val TransactionInfo.tx: Transaction get() = second
val TransactionInfo.receipt get() = first.receipt
val TransactionInfo.blockHash: HexBytes get() = first.blockHash
val TransactionInfo.i: Int get() = first.i

interface RepositoryReader : Closeable {
    val genesis: Block
    val genesisCfg: AbstractGenesis
    fun containsHeader(hash: HexBytes): Boolean
    val bestHeader: Header
    val bestBlock: Block
    fun getHeaderByHash(hash: HexBytes): Header?
    fun getBlockByHash(hash: HexBytes): Block?
    fun getHeadersByHeight(height: Long): List<Header>
    fun getBlocksByHeight(height: Long): List<Block>
    fun getCanonicalBlock(height: Long): Block?
    fun getCanonicalHeader(height: Long): Header?
    fun createBlockHashMap(hash: HexBytes): List<Pair<Long, ByteArray>>



    fun getAncestor(now: HexBytes, ancestor: Long): Header{
        var c = getHeaderByHash(now)!!
        require(c.height >= ancestor)

        while (c.height != ancestor)
            c = getHeaderByHash(c.hashPrev)!!
        return c
    }

    fun getHeadersBetween(
        startHeight: Long,
        stopHeight: Long,
        limit: Int = Int.MAX_VALUE,
        descend: Boolean = false
    ): List<Header>

    fun getBlocksBetween(
        startHeight: Long,
        stopHeight: Long,
        limit: Int = Int.MAX_VALUE,
        descend: Boolean = false
    ): List<Block>

    // get transaction info with transaction included
    fun getTransactionInfo(hash: HexBytes): TransactionInfo?

    fun getTransaction(hash: HexBytes): Transaction? {
        return getTransactionInfo(hash)?.tx
    }

    // is transaction exists
    fun containsTransaction(hash: HexBytes): Boolean

    override fun close() {

    }
}

