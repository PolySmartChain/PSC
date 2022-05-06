package org.tdf.evm

import org.tdf.evm.SlotUtils.SLOT_SIZE
import java.math.BigInteger

class U256Register {
    companion object {
        const val MAX_POW = 256
        val _2_256 = BigInteger.valueOf(2).pow(MAX_POW)
    }

    // slot0 and slot1 is used for store operand
    private val slot0 = IntArray(SLOT_SIZE)
    private val slot1 = IntArray(SLOT_SIZE)

    // variable length slot, used for multiply
    private val varSlot = IntArray(SLOT_SIZE * 2)
    private val divisorSlot = IntArray(SLOT_SIZE * 2)

    private val remSlot = IntArray(SLOT_SIZE * 2)
    private val rem = MutableBigInteger(remSlot)

    fun add(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        slot1.copyFrom(right)
        slot0.add(slot1)
        return slot0.toBigInt()
    }

    fun sub(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        slot1.copyFrom(right)
        SlotUtils.sub(slot0, 0, slot1, 0, slot1, 0);
        return slot1.toBigInt()
    }

    fun mul(left: BigInteger, right: BigInteger): BigInteger {
        if (left == BigInteger.ZERO || right == BigInteger.ZERO)
            return BigInteger.ZERO
        slot0.copyFrom(left)
        slot1.copyFrom(right)
        varSlot.reset()

        val m0 = MutableBigInteger(slot0)
        m0.normalize()

        val m1 = MutableBigInteger(slot1)
        m1.normalize()

        val m2 = MutableBigInteger(varSlot)
        m0.multiply(m1, m2)

        return m2.toBigInt().mod(_2_256)
    }

    fun div(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        val m = MutableBigInteger(slot0)
        m.normalize()

        slot1.copyFrom(right)
        val m1 = MutableBigInteger(slot1)
        m1.normalize()


        varSlot.reset()
        val quo = MutableBigInteger(varSlot)
        quo.reset()

        rem.clear()
        divisorSlot.reset()

        m.divideKnuth(m1, quo, rem, divisorSlot, false)
        return quo.toBigInt()
    }

    fun mod(left: BigInteger, right: BigInteger): BigInteger {
        slot0.copyFrom(left)
        val m = MutableBigInteger(slot0)
        m.normalize()

        slot1.copyFrom(right)
        val m1 = MutableBigInteger(slot1)
        m1.normalize()


        varSlot.reset()
        val quo = MutableBigInteger(varSlot)
        quo.reset()

        rem.clear()
        divisorSlot.reset()

        m.divideKnuth(m1, quo, rem, divisorSlot, true)
        return rem.toBigInt()
    }

}