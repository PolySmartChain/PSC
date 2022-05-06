package org.tdf.common

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.RlpBuffer
import com.github.salpadding.rlpstream.RlpWritable
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.math.BigInteger

@RunWith(JUnit4::class) // set env WRAPPER=true to test wrapper method
class ChainCacheTest {
    @Test
    fun test() {
        val b = Rlp.encode(Bn(BigInteger.ONE))

    }
}

@JvmInline
internal value class Bn(val data: BigInteger): RlpWritable{
    operator fun plus(other: BigInteger): Bn {
        return Bn(data + other)
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeBigInt(data)
    }
}