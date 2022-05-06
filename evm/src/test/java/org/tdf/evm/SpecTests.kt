package org.tdf.evm

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.spongycastle.util.encoders.Hex
import org.tdf.evm.SlotUtils.SLOT_BYTE_ARRAY_SIZE
import org.tdf.evm.SlotUtils.SLOT_SIZE

fun Stack.popHex(): String {
    val ints = IntArray(SLOT_SIZE)
    val bytes = ByteArray(SLOT_BYTE_ARRAY_SIZE)
    this.pop(ints)
    SlotUtils.encodeBE(ints, 0, bytes, 0)
    return Hex.toHexString(bytes)
}

@RunWith(JUnit4::class)
class SpecTests {
    @Test
    fun testSignExt() {
        val stack = StackImpl()

        TestUtil.testSpec("testdata/testcases_signext.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.signExtend()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testAdd() {
        val stack = StackImpl()

        TestUtil.testSpec("testdata/testcases_add.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData, 0)
                stack.push(c.yData, 0)
                stack.add()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testAnd() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_and.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.and()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testByte() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_byte.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.byte()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testDiv() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_div.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.div()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testEq() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_eq.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.eq()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testExp() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_exp.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.exp()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testGt() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_gt.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.gt()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testLt() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_lt.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.lt()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testMod() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_mod.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.mod()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testMul() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_mul.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.mul()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testOr() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_or.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.or()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testSdiv() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_sdiv.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.signedDiv()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testSGt() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_sgt.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.sgt()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testSlt() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_slt.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.slt()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testSMod() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_smod.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.signedMod()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testSub() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_sub.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.sub()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testXor() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_xor.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.xor()
                return stack.popHex()
            }
        )
    }


    @Test
    fun testShl() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_shl.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.shl()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testShr() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_shr.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.shr()
                return stack.popHex()
            }
        )
    }

    @Test
    fun testSar() {
        val stack = StackImpl()
        TestUtil.testSpec("testdata/testcases_sar.json",
            fun(c: EvmSpec): String {
                stack.push(c.xData)
                stack.push(c.yData)
                stack.sar()
                return stack.popHex()
            }
        )
    }
}