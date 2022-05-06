package org.tdf.sunflower.sync

import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps

@RlpProps("startHeight", "stopHeight", "descend", "limit")
data class GetBlocks @RlpCreator constructor(
    val startHeight: Long,
    val stopHeight: Long,
    val descend: Boolean,
    val limit: Int
) {
    fun clip(): GetBlocks {
        var startHeight = this.startHeight
        var stopHeight = this.stopHeight
        if (stopHeight - startHeight < limit) return this.copy()
        if (descend) {
            startHeight = this.stopHeight - limit
        } else {
            stopHeight = this.startHeight + limit
        }
        return GetBlocks(startHeight, stopHeight, descend, limit)
    }
}