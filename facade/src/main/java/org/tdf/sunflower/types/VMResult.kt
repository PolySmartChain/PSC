package org.tdf.sunflower.types

import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes

class VMResult(
    val gasUsed: Long = 0,
    val executionResult: HexBytes = HexBytes.empty(),
    val logs: List<LogInfo> = emptyList(),
    val fee: Uint256 = Uint256.ZERO
) {

    companion object {
        val EMPTY = VMResult()
    }
}