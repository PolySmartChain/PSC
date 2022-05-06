package org.tdf.evm

import org.tdf.evm.SlotUtils.SLOT_BYTE_ARRAY_SIZE
import java.math.BigInteger
import java.util.*

fun IntArray.reset() {
    Arrays.fill(this, 0)
}

fun IntArray.copyFrom(bytes: ByteArray) {
    SlotUtils.copyFrom(this, 0, bytes)
}

fun IntArray.copyFrom(n: BigInteger) {
    SlotUtils.copyFrom(this, 0, n)
}

fun IntArray.toByteArray(): ByteArray {
    val r = ByteArray(SLOT_BYTE_ARRAY_SIZE)
    SlotUtils.encodeBE(this, 0, r, 0)
    return r
}

fun IntArray.toBigInt(): BigInteger {
    return SlotUtils.toBigInt(this, 0)
}

fun IntArray.add(slot: IntArray) {
    SlotUtils.add(this, 0, slot, 0, this, 0)
}
