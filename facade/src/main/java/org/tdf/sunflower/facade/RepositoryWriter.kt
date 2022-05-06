package org.tdf.sunflower.facade

import org.tdf.sunflower.types.Block

interface RepositoryWriter : RepositoryReader {
    fun writeBlock(b: Block, infos: List<TransactionInfo>)
    fun saveGenesis(b: Block)
    fun deleteGT(height: Long)
    fun canonicalize()
}