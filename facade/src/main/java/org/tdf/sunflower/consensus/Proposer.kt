package org.tdf.sunflower.consensus

import org.tdf.common.util.HexBytes

data class Proposer(
    val address: HexBytes,
    val startTimeStamp: Long = 0,
    val endTimeStamp: Long = 0
)