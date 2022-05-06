package org.tdf.evm

import org.tdf.evm.SlotUtils.SLOT_BYTE_ARRAY_SIZE
import org.tdf.evm.SlotUtils.SLOT_SIZE

val slot = IntArray(SLOT_SIZE)
val zeros = IntArray(SLOT_SIZE * 2)
val bytes = ByteArray(SLOT_BYTE_ARRAY_SIZE)

fun main() {
    val start = System.currentTimeMillis()
    val loops = 1000000

    for (i in 0 until loops) {
        SlotUtils.encodeBE(slot, 0, bytes, 0)
    }

    val end = System.currentTimeMillis()

    println("ops = ${loops.toDouble() / (end - start) * 1000}")
}