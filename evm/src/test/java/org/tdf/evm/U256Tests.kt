package org.tdf.evm


import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.evm.SlotUtils.SLOT_BYTE_ARRAY_SIZE
import org.tdf.evm.SlotUtils.SLOT_SIZE
import org.tdf.evm.StackImpl.Companion.P_2_256
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

class RegTestOperator(
    private val expect: (BigInteger, BigInteger) -> BigInteger,
    private val actual: (U256Register, BigInteger, BigInteger) -> BigInteger,
    private val skip: (BigInteger, BigInteger) -> Boolean = { _, _ -> false }
) : TestOperator {
    private val reg = U256Register()

    override fun skip(args: Array<BigInteger>): Boolean {
        return skip.invoke(args[0], args[1])
    }

    override fun expect(args: Array<BigInteger>): BigInteger {
        return expect.invoke(args[0], args[1])
    }

    override fun actual(args: Array<BigInteger>): BigInteger {
        return actual.invoke(reg, args[0], args[1])
    }
}

val regAdd = RegTestOperator(
    { l, r -> (l + r) % P_2_256 },
    { reg, l, r -> reg.add(l, r) }
)

val regSub = RegTestOperator(
    { l, r -> (l - r).and(StackImpl.P_MAX) },
    { reg, l, r -> reg.sub(l, r) }
)

val regMul = RegTestOperator(
    { l, r -> (l * r).and(StackImpl.P_MAX) },
    { reg, l, r -> reg.mul(l, r) }
)

val regDiv = RegTestOperator(
    { l, r -> (l / r).and(StackImpl.P_MAX) },
    { reg, l, r -> reg.div(l, r) },
    { _, r -> r == BigInteger.ZERO }
)

val regMod = RegTestOperator(
    { l, r -> l % r },
    { reg, l, r -> reg.mod(l, r) },
    { _, r -> r == BigInteger.ZERO }
)

@RunWith(JUnit4::class)
class U256Tests {
    companion object {
        const val MAX_POW = 256
        val _2_256 = BigInteger.valueOf(2).pow(MAX_POW)
        val MAX_U256 = BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16)
        val SR = SecureRandom()
    }

    @Test
    fun testLoadSlot() {
        for (i in 0 until 1000) {
            val left = ByteArray(SLOT_BYTE_ARRAY_SIZE)
            SR.nextBytes(left)

            val slot = IntArray(SLOT_SIZE)
            slot.copyFrom(left)
            val actual = slot.toByteArray()
            assertArrayEquals(left, actual)
        }
    }

    @Test
    fun testLoadSlot1() {
        for (i in 0 until 1000) {
            val left = ByteArray(16)
            SR.nextBytes(left)

            val slot = IntArray(SLOT_SIZE)
            slot.copyFrom(left)
            val actual = Arrays.copyOfRange(
                slot.toByteArray(),
                SLOT_BYTE_ARRAY_SIZE - 16,
                SLOT_BYTE_ARRAY_SIZE
            )
            assertArrayEquals(left, actual)
        }
    }


    @Test
    fun testLoadSlot2() {
        for (i in 0 until 1000) {
            val left = ByteArray(16)
            SR.nextBytes(left)
            val l = BigInteger(1, left)
            val slot = IntArray(SLOT_SIZE)

            slot.copyFrom(left)
            val r = slot.toBigInt()

            assertEquals(l, r)
        }
    }


    @Test
    fun testAdd() {
        val reg = U256Register()

        val l = BigInteger.valueOf(Long.MAX_VALUE)
        val r = BigInteger.valueOf(Long.MAX_VALUE)

        val expected = l + r

        val actual = reg.add(l, r).mod(_2_256)

        assertEquals(expected.toString(16), actual.toString(16))

    }

    @Test
    fun testRandomAdd() {
        TestUtil.randomTest(regAdd)
    }

    @Test
    fun testRandomSub() {
        TestUtil.randomTest(regSub)
    }

    @Test
    fun testMul() {
        val reg = U256Register()
        val l = BigInteger.valueOf(Long.MAX_VALUE)
        val r = BigInteger.valueOf(Long.MAX_VALUE)

        val expected = (l * r).mod(_2_256)
        val actual = reg.mul(l, r)

        assertEquals(expected.toString(16), actual.toString(16))

    }

    @Test
    fun testRandomMul() {
        TestUtil.randomTest(regMul)
    }


    @Test
    fun test2() {
        val a = BigInteger("FFFFFFFFFFFFFFFF", 16)
        val slot = IntArray(SLOT_SIZE)
        slot.copyFrom(a.toByteArray())
        println(Arrays.toString(slot))
        val r = U256Register()
        println(Arrays.toString(slot))
    }

    @Test
    fun testRandomDiv() {
        TestUtil.randomTest(regDiv)
    }


    @Test
    fun testRandomMod() {
        TestUtil.randomTest(regMod)
    }

    @Test
    fun testFailed() {
        val lefts = longArrayOf(252584348, 1811528554, 3654679626)
        val rights = longArrayOf(3676406621, 2111400643, 1969366594)

        for (i in 0 until lefts.size) {
            val l = BigInteger.valueOf(lefts[i])
            val r = BigInteger.valueOf(rights[i])
            val reg = U256Register()
            val actual = reg.mod(l, r)
            assertEquals(l % r, actual)
        }
    }
}