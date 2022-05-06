package org.tdf.common.types

import org.tdf.common.util.HexBytes

interface Chained : Hashed {
    val hashPrev: HexBytes
    fun isParentOf(another: Chained): Boolean {
        return another.isChildOf(this)
    }

    fun isChildOf(another: Chained): Boolean {
        return isChildOf(another.hash)
    }

    fun isChildOf(hash: HexBytes): Boolean {
        return hashPrev == hash
    }
}