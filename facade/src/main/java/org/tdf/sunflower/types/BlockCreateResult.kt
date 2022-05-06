package org.tdf.sunflower.types

import org.tdf.sunflower.facade.TransactionInfo

data class BlockCreateResult(val block: Block?, val indices: List<TransactionInfo>) {
    val isEmpty: Boolean
        get() = this === EMPTY || block == null

    companion object {
        val EMPTY = BlockCreateResult(null, emptyList())

        @JvmStatic
        fun empty(): BlockCreateResult {
            return EMPTY
        }
    }
}