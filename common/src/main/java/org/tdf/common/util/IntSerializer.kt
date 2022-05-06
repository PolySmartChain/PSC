package org.tdf.common.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.tdf.common.types.Uint256
import java.math.BigInteger

class IntSerializer : StdSerializer<Number>(Number::class.java) {
    override fun serialize(value: Number, gen: JsonGenerator, provider: SerializerProvider) {
        val v: BigInteger = when (value) {
            is Uint256 -> value.value
            is Long -> BigInteger.valueOf(value)
            is Byte -> return gen.writeNumber(value.toInt())
            is Short -> return gen.writeNumber(value)
            is Int -> return gen.writeNumber(value)
            else -> throw RuntimeException("unsupported type ${value.javaClass}")
        }

        if (v > MAX_SAFE_INTEGER || v < MIN_SAFE_INTEGER) {
            gen.writeString(v.toString(10))
        } else {
            gen.writeNumber(v.longValueExact())
        }
    }

    companion object {
        private val MAX_SAFE_INTEGER = BigInteger.valueOf(9007199254740991L)
        private val MIN_SAFE_INTEGER = BigInteger.valueOf(-9007199254740991L)
    }
}