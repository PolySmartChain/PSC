package org.tdf.common.types

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.github.salpadding.rlpstream.RlpBuffer
import com.github.salpadding.rlpstream.RlpWritable
import com.github.salpadding.rlpstream.StreamId
import com.github.salpadding.rlpstream.annotation.RlpCreator
import org.tdf.common.types.Constants.WORD_SIZE
import org.tdf.common.types.Uint256.Uint256Deserializer
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.IntSerializer
import org.tdf.common.util.bytes
import org.tdf.common.util.bytes32
import java.math.BigInteger
import kotlin.math.sign

@JsonDeserialize(using = Uint256Deserializer::class)
@JsonSerialize(using = IntSerializer::class)
class Uint256 private constructor(val value: BigInteger) : Number(), RlpWritable {
    override fun toString(): String {
        return value.toString()
    }

    override fun writeToBuf(buf: RlpBuffer): Int {
        return buf.writeBigInt(value)
    }


    override fun toInt(): Int {
        return value.intValueExact()
    }

    override fun toLong(): Long {
        return value.longValueExact()
    }

    override fun toShort(): Short {
        return value.shortValueExact()
    }

    override fun toFloat(): Float {
        return value.toFloat()
    }

    override fun toDouble(): Double {
        return value.toDouble()
    }

    override fun toByte(): Byte {
        return value.byteValueExact()
    }

    override fun toChar(): Char {
        return value.toChar()
    }

    /**
     * Returns instance data
     * Actually copy of internal byte array is provided
     * in order to protect Uint256 immutability
     *
     * @return instance data
     */
    val byte32: ByteArray by lazy {
        value.bytes32()
    }


    val bytes: ByteArray by lazy {
        value.bytes()
    }

    val isZero: Boolean
        get() = if (this === ZERO) true else this.compareTo(ZERO) == 0


    fun uncheckedPlus(word: Uint256): Uint256 {
        val result = value.add(word.value)
        return Uint256(result.and(MAX_VALUE))
    }

    operator fun plus(word: Uint256): Uint256 {
        val ret = uncheckedPlus(word)
        if (ret < this || ret < word) throw RuntimeException("unexpected exception: math overflow")
        return ret
    }

    operator fun div(word: Uint256): Uint256 {
        if (word.isZero) {
            throw RuntimeException("divided by zero")
        }
        return uncheckedDiv(word)
    }

    fun uncheckedDiv(word: Uint256): Uint256 {
        if (word.isZero) {
            return ZERO
        }
        val result = value.divide(word.value)
        return Uint256(result.and(MAX_VALUE))
    }

    fun uncheckedMinus(word: Uint256): Uint256 {
        val result = value.subtract(word.value)
        return Uint256(result.and(MAX_VALUE))
    }

    operator fun minus(word: Uint256): Uint256 {
        if (this < word) throw RuntimeException("math overflow")
        return uncheckedMinus(word)
    }

    fun uncheckedRem(word: Uint256): Uint256 {
        if (word.isZero) {
            return ZERO
        }
        val result = value.mod(word.value)
        return Uint256(result.and(MAX_VALUE))
    }

    operator fun rem(word: Uint256): Uint256 {
        if (word.isZero) {
            throw RuntimeException("divided by zero")
        }
        return uncheckedRem(word)
    }


    fun uncheckedTimes(word: Uint256): Uint256 {
        val result = value.multiply(word.value)
        return Uint256(result.and(MAX_VALUE))
    }

    operator fun times(word: Uint256): Uint256 {
        if (this.compareTo(ZERO) == 0) {
            return ZERO
        }
        val ret = uncheckedTimes(word)
        if (ret.div(this).compareTo(word) != 0) throw RuntimeException("SafeMath: multiplication overflow ")
        return ret
    }

    operator fun compareTo(o: Uint256?): Int {
        if (o == null)
            throw NullPointerException()
        val result = value.compareTo(o.value)
        // Convert result into -1, 0 or 1 as is the convention
        return sign(result.toFloat()).toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val uint256 = other as Uint256
        return this.compareTo(uint256) == 0
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }


    class Uint256Deserializer : StdDeserializer<Uint256>(Uint256::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Uint256 {
            val node = p.codec.readTree<JsonNode>(p)
            if (node.isNull) return ZERO
            val encoded = node.asText()
            if (encoded == null || encoded == "") {
                return ZERO
            }
            return if (encoded.startsWith("0x")) {
                of(encoded.substring(2), 16)
            } else of(encoded, 10)
        }
    }

    companion object {
        const val MAX_POW = 256
        val _2_256 = BigInteger.valueOf(2).pow(MAX_POW)
        val MAX_VALUE = _2_256.subtract(BigInteger.ONE)

        @JvmField
        val ZERO = Uint256(BigInteger.ZERO)
        val ONE = Uint256(BigInteger.ONE)

        fun of(pattern: String, radix: Int): Uint256 {
            val i = BigInteger(pattern, radix)
            if (i < BigInteger.ZERO || i > MAX_VALUE) throw RuntimeException(
                "$pattern overflow"
            )
            return Uint256(i)
        }

        @JvmStatic
        @RlpCreator
        fun create(bin: ByteArray, streamId: Long): Uint256 {
            return Uint256(StreamId.asBigInteger(bin, streamId))
        }

        @JvmStatic
        fun of(v: BigInteger): Uint256 {
            if (v < BigInteger.ZERO)
                throw RuntimeException("uint256 overflow")
            return Uint256(v)
        }

        @JvmStatic
        fun of(d: ByteArray?): Uint256 {
            val data = d ?: return ZERO
            if (data.isEmpty())
                return ZERO
            if (data.size > WORD_SIZE)
                throw RuntimeException(
                    String.format(
                        "Data word can't exceed 32 bytes: 0x%s",
                        ByteUtil.toHexString(data)
                    )
                )
            return Uint256(BigInteger(1, data))
        }

        @JvmStatic
        fun of(num: Int): Uint256 {
            return of(num.toLong())
        }

        @JvmStatic
        fun of(num: Long): Uint256 {
            require(num >= 0) { "num should be non-negative" }
            return Uint256(BigInteger.valueOf(num))
        }

        @JvmStatic
        fun of(num: String): Uint256 {
            if(num.startsWith("0x"))
                return Uint256(BigInteger(num.substring(2), 16))
            return Uint256(BigInteger(num))
        }
    }
}