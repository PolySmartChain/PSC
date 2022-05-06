package org.tdf.evm

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert
import org.spongycastle.util.encoders.Hex
import org.tdf.evm.SlotUtils.SLOT_SIZE
import org.tdf.evm.StackImpl.Companion.P_MAX
import org.tdf.evm.StackImpl.Companion.P_SIGNED_MAX
import java.io.InputStream
import java.math.BigInteger
import kotlin.experimental.and

interface TestOperator {
    fun skip(args: Array<BigInteger>): Boolean
    fun expect(args: Array<BigInteger>): BigInteger
    fun actual(args: Array<BigInteger>): BigInteger
}

class TestOpImpl(
    private val expect: (Array<BigInteger>) -> BigInteger,
    private val actual: (Array<BigInteger>) -> BigInteger,
    private val skip: (Array<BigInteger>) -> Boolean = { _ -> false },
) : TestOperator {
    override fun skip(args: Array<BigInteger>): Boolean {
        return skip.invoke(args)
    }

    override fun expect(args: Array<BigInteger>): BigInteger {
        return expect.invoke(args)
    }

    override fun actual(args: Array<BigInteger>): BigInteger {
        return actual.invoke(args)
    }
}

data class EvmSpec(
    @JsonProperty("X") val x: String = "",
    @JsonProperty("Y") val y: String = "",
    @JsonProperty("Expected") val expected: String = ""
) {
    val xData: IntArray
    val yData: IntArray

    init {
        xData = IntArray(SLOT_SIZE)
        yData = IntArray(SLOT_SIZE)
        val xBytes = Hex.decode(x)
        val yBytes = Hex.decode(y)
        SlotUtils.decodeBE(xBytes, 0, xData, 0)
        SlotUtils.decodeBE(yBytes, 0, yData, 0)
    }
}

object TestUtil {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()


    fun testSpec(fileName: String, op: (EvmSpec) -> String) {
        val bytes = readClassPathFile(fileName)
        val cases = objectMapper.readValue(bytes, Array<EvmSpec>::class.java)

        for (c in cases) {
            val actual: String
            try {
                actual = op.invoke(c)
            } catch (e: Exception) {
                println("exception!! case = $c")
                throw e
            }
            if (c.expected != actual) {
                println("case = $c")
                throw RuntimeException("expected ${c.expected} while $actual returned")
            }
        }
    }

    fun readClassPathFile(name: String): ByteArray {
        val stream: InputStream = TestUtil::class.java.classLoader.getResource(name)!!.openStream()
        val all = ByteArray(stream.available())
        if (stream.read(all) != all.size) throw RuntimeException("read bytes from stream failed")
        return all
    }

    private const val LOOPS = 100

    fun testSinglePair(op: TestOperator, args: Array<BigInteger>) {
        if (op.skip(args))
            return
        val expected = op.expect(args)
        val actual = op.actual(args)

        if (actual != expected) {
            println("test failed: args = ${args.contentToString()}")
        }
        Assert.assertEquals("", expected.toString(16), actual.toString(16))
    }

    private fun IntArray.last(): Boolean {
        for (i in 0 until this.size) {
            if (this[i] < 32)
                return false
        }
        return true
    }

    fun IntArray.next() {
        var carry = 1
        for (i in 0 until this.size) {
            if (this[i] == 32 && carry == 1) {
                this[i] = 0
                carry = 1
            } else {
                this[i] += carry
                carry = 0
            }
        }
    }


    fun randomTest(op: TestOperator, signed: Boolean = false, loops: Int = LOOPS, argLen: Int = 2) {
        val sz = IntArray(argLen)
        val nums = arrayOfNulls<BigInteger>(argLen)
        val bufs = arrayOfNulls<ByteArray>(argLen)

        while (true) {
            // initialize buffers
            for (i in 0 until argLen) {
                bufs[i] = ByteArray(sz[i])
            }

            for (i in 0 until loops) {
                bufs.requireNoNulls().forEachIndexed { idx, it ->
                    U256Tests.SR.nextBytes(it)
                    if (it.size == 32 && signed) {
                        it[0] = it[0] and 0x7f
                    }

                    if (it.size >= 24 && i == 3)
                        it.fill(0, 4, it.size)

                    val neg = if (signed) {
                        U256Tests.SR.nextBoolean()
                    } else {
                        false
                    }

                    if (idx == 1 && i == 2) {
                        nums[idx] = (nums[idx - 1]!! + nums[idx - 1]!!) and (if (signed) {
                            P_SIGNED_MAX
                        } else {
                            P_MAX
                        })
                        return@forEachIndexed
                    }

                    nums[idx] = BigInteger(
                        if (neg) {
                            -1
                        } else {
                            1
                        }, it
                    )
                    return@forEachIndexed
                }


                try {
                    testSinglePair(op, nums.requireNoNulls())
                } catch (e: Exception) {
                    println("exception found args = ${nums.contentToString()}")
                    throw e
                }
            }
            if (sz.last())
                break
            sz.next()
        }
    }
}