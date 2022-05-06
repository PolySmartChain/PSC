package org.tdf.sunflower.sync

import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.util.HexBytes

@RlpProps("bestBlockHeight", "bestBlockHash", "genesisBlockHash", "prunedHeight", "prunedHash")
data class Status @RlpCreator constructor(
    val bestBlockHeight: Long,
    val bestBlockHash: HexBytes,
    val genesisBlockHash: HexBytes,
    val prunedHeight: Long,
    val prunedHash: HexBytes,
)