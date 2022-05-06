package org.tdf.common.util

import com.github.salpadding.rlpstream.RlpList
import com.github.salpadding.rlpstream.StreamId

object RLPUtil {
    @JvmStatic
    fun decodePartial(bin: ByteArray, offset: Int): RlpList {
        val streamId = StreamId.decodeElement(bin, offset, bin.size, false)
        return StreamId.asList(bin, streamId)
    }
}