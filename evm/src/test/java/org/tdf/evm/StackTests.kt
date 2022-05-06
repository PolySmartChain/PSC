package org.tdf.evm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.evm.StackImpl.Companion.P_2_256
import org.tdf.evm.StackImpl.Companion.P_MAX
import java.math.BigInteger

class StackBinaryOp(
    private val expect: (BigInteger, BigInteger) -> BigInteger,
    private val actual: (Stack) -> BigInteger,
    private val skip: (BigInteger, BigInteger) -> Boolean = { _, _ -> false }
) : TestOperator {
    private val stack = StackImpl()

    override fun skip(args: Array<BigInteger>): Boolean {
        return skip.invoke(args[0], args[1])
    }

    override fun expect(args: Array<BigInteger>): BigInteger {
        return expect.invoke(args[0], args[1])
    }

    override fun actual(args: Array<BigInteger>): BigInteger {
        stack.push(args[1])
        stack.push(args[0])
        return actual.invoke(stack)
    }
}


val stackAdd = StackBinaryOp(
    { l, r -> (l + r) % P_2_256 },
    fun(stack: Stack): BigInteger {
        stack.add()
        return stack.popBigInt()
    }
)

val stackSub = StackBinaryOp(
    { l, r -> (l - r).and(P_MAX) },
    fun(stack: Stack): BigInteger {
        stack.sub()
        return stack.popBigInt()
    }
)

val stackMul = StackBinaryOp(
    { l, r -> (l * r) % P_2_256 },
    fun(stack: Stack): BigInteger {
        stack.mul()
        return stack.popBigInt()
    }
)

val stackDiv = StackBinaryOp(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l / r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.div()
        return stack.popBigInt()
    }
)

val stackMod = StackBinaryOp(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l % r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.mod()
        return stack.popBigInt()
    }
)

val stackSDiv = StackBinaryOp(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l / r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.signedDiv()
        return stack.popBigInt(true)
    }
)

val stackSMod = StackBinaryOp(
    { l, r ->
        if (r == BigInteger.ZERO) {
            BigInteger.ZERO
        } else {
            l % r
        }
    },
    fun(stack: Stack): BigInteger {
        stack.signedMod()
        return stack.popBigInt(true)
    }
)

val stackLt = StackBinaryOp(
    { l, r ->
        if (l < r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.lt()
        return stack.popBigInt()
    }
)

val stackSLt = StackBinaryOp(
    { l, r ->
        if (l < r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.slt()
        return stack.popBigInt()
    }
)

val stackGt = StackBinaryOp(
    { l, r ->
        if (l > r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.gt()
        return stack.popBigInt()
    }
)

val stackSGt = StackBinaryOp(
    { l, r ->
        if (l > r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.sgt()
        return stack.popBigInt()
    }
)

val stackAnd = StackBinaryOp(
    { l, r ->
        l.and(r)
    },
    fun(stack: Stack): BigInteger {
        stack.and()
        return stack.popBigInt()
    }
)

val stackOr = StackBinaryOp(
    { l, r ->
        l.or(r)
    },
    fun(stack: Stack): BigInteger {
        stack.or()
        return stack.popBigInt()
    }
)

val stackXor = StackBinaryOp(
    { l, r ->
        l.xor(r)
    },
    fun(stack: Stack): BigInteger {
        stack.xor()
        return stack.popBigInt()
    }
)

val stackEq = StackBinaryOp(
    { l, r ->
        if (l == r) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.eq()
        return stack.popBigInt()
    }
)

val stackIsZero = StackBinaryOp(
    { l, _ ->
        if (l == BigInteger.ZERO) {
            BigInteger.ONE
        } else {
            BigInteger.ZERO
        }
    },
    fun(stack: Stack): BigInteger {
        stack.isZero()
        return stack.popBigInt()
    }
)

val stackExp = StackBinaryOp(
    { l, r ->
        l.modPow(r, P_2_256)
    },
    fun(stack: Stack): BigInteger {
        stack.exp()
        return stack.popBigInt()
    }
)

val stackDup = StackBinaryOp(
    { _, r -> r },
    fun(stack: Stack): BigInteger {
        stack.dup(2)
        return stack.popBigInt()
    }
)

class SingleOp(private val stack: Stack) : TestOperator {
    override fun skip(args: Array<BigInteger>): Boolean {
        return false
    }

    override fun expect(args: Array<BigInteger>): BigInteger {
        return args[0]
    }

    override fun actual(args: Array<BigInteger>): BigInteger {
        stack.push(args[0])
        return stack.popBigInt(true)
    }

}


@RunWith(JUnit4::class)
class StackTests {
    @Test
    fun testRandomAdd() {
        TestUtil.randomTest(stackAdd)
    }

    @Test
    fun testRandomSub() {
        TestUtil.randomTest(stackSub)
    }

    @Test
    fun testRandomMul() {
        TestUtil.randomTest(stackMul)
    }

    @Test
    fun testRandomDiv() {
        TestUtil.randomTest(stackDiv)
    }

    @Test
    fun testRandomMod() {
        TestUtil.randomTest(stackMod)
    }

    @Test
    fun testRandomSignedDiv() {
        TestUtil.randomTest(stackSDiv, signed = true)
    }


    @Test
    fun testRandomSignedMod() {
        TestUtil.randomTest(stackSMod, signed = true)
    }

    @Test
    fun testRandomLt() {
        TestUtil.randomTest(stackLt)
    }


    @Test
    fun testRandomSLt() {
        TestUtil.randomTest(stackSLt, signed = true)
    }

    @Test
    fun testRandomGt() {
        TestUtil.randomTest(stackGt)
    }


    @Test
    fun testRandomSGt() {
        TestUtil.randomTest(stackSGt, signed = true)
    }

    @Test
    fun testRandomAnd() {
        TestUtil.randomTest(stackAnd)
    }

    @Test
    fun testRandomOr() {
        TestUtil.randomTest(stackOr)
    }

    @Test
    fun testRandomXor() {
        TestUtil.randomTest(stackXor)
    }

    @Test
    fun testRandomEq() {
        TestUtil.randomTest(stackEq)
    }

    @Test
    fun testRandomIsZero() {
        TestUtil.randomTest(stackIsZero)
    }

    @Test
    fun testRandomExp() {
        TestUtil.randomTest(stackExp, loops = 10)
    }

    @Test
    fun testRandomDup() {
        TestUtil.randomTest(stackDup, loops = 10)
    }

    @Test
    fun testSingleOp() {
        val op = SingleOp(StackImpl())
        TestUtil.randomTest(op, signed = true, argLen = 1)
    }

    @Test
    fun testAddMod() {
        val stack = StackImpl()
        val stackAddMod = TestOpImpl(
            { if (it[2] == BigInteger.ZERO) BigInteger.ZERO else (it[0] + it[1]) % it[2] },
            fun(args: Array<BigInteger>): BigInteger {
                stack.push(args[2])
                stack.push(args[1])
                stack.push(args[0])
                stack.addMod()
                return stack.popBigInt()
            }
        )

        TestUtil.randomTest(stackAddMod, argLen = 3)
    }

    @Test
    fun testMulMod() {
        val stack = StackImpl()
        val stackAddMod = TestOpImpl(
            { if (it[2] == BigInteger.ZERO) BigInteger.ZERO else (it[0] * it[1]) % it[2] },
            fun(args: Array<BigInteger>): BigInteger {
                stack.push(args[2])
                stack.push(args[1])
                stack.push(args[0])
                stack.mulMod()
                return stack.popBigInt()
            }
        )

        TestUtil.randomTest(stackAddMod, argLen = 3)
    }

    @Test
    fun testPushInt() {
        val stack = StackImpl()
        val stackPushInt = TestOpImpl(
            { it[0].and(BigInteger.valueOf(0xffffffffL)) },
            fun(args: Array<BigInteger>): BigInteger {
                stack.pushInt(
                    args[0]
                        .and(BigInteger.valueOf(0xffffffffL))
                        .longValueExact().toInt()
                )
                return stack.popBigInt()
            }
        )

        TestUtil.randomTest(stackPushInt, argLen = 1)
    }
}