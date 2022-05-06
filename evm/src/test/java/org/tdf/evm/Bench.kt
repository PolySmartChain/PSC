package org.tdf.evm

import org.tdf.evm.SlotUtils.SLOT_BYTE_ARRAY_SIZE
import org.tdf.evm.SlotUtils.SLOT_SIZE
import java.math.BigInteger
import java.security.SecureRandom

val slot0 = IntArray(SLOT_SIZE)
val m0 = MutableBigInteger(slot0)

val slot1 = IntArray(SLOT_SIZE)
val m1 = MutableBigInteger(slot1)

val slot2 = IntArray(SLOT_SIZE)


val tmp = ByteArray(SLOT_BYTE_ARRAY_SIZE)
val mulTmp = IntArray(SLOT_SIZE * 2)
val tmpMutBigInt = MutableBigInteger(mulTmp)

val sr = SecureRandom()

fun interface Operator {
    fun mul(left: ByteArray, right: ByteArray)
}

val bigIntOp = Operator { left, right ->
    val l = BigInteger(1, left)
    val r = BigInteger(1, right)
    l * r
}

val slotOp = Operator { left, right ->
    slot0.copyFrom(left)
    slot1.copyFrom(right)
    m0.setValue(slot0, SLOT_SIZE)
    m0.normalize()
    m1.setValue(slot1, SLOT_SIZE)
    m1.normalize()

    tmpMutBigInt.clear()
    m0.multiply(m1, tmpMutBigInt)

}

fun main() {
    val left = ByteArray(SLOT_BYTE_ARRAY_SIZE)
    val right = ByteArray(SLOT_BYTE_ARRAY_SIZE)
    val out = ByteArray(SLOT_BYTE_ARRAY_SIZE)

    val count = 10000000

    val start = System.currentTimeMillis()

    val op = if (System.getenv("op") == "bigint") {
        bigIntOp
    } else {
        slotOp
    }

    for (i in 0 until count) {
        sr.nextBytes(left)
        sr.nextBytes(right)
        op.mul(left, right)
    }

    val end = System.currentTimeMillis()

    println("ops = ${count * 1.0 / (end - start) * 1000}")
}