package org.tdf.common.util

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.github.salpadding.rlpstream.RlpBuffer
import com.github.salpadding.rlpstream.RlpWritable
import com.github.salpadding.rlpstream.StreamId
import com.github.salpadding.rlpstream.annotation.RlpCreator
import org.spongycastle.util.encoders.Hex
import org.tdf.common.util.HexBytesUtil.HexBytesDeserializer
import org.tdf.common.util.HexBytesUtil.HexBytesSerializer

typealias Address = HexBytes
typealias H256 = HexBytes
typealias H2048 = HexBytes

/**
 * hex bytes helper for json marshal/unmarshal
 * non-null immutable wrapper fo byte[] inspired by ByteArrayWrapper
 * non-null
 *
 *
 * HexBytes bytes = mapper.readValue("ffff", HexBytes.class);
 * String json = mapper.writeValueAsString(new HexBytes(new byte[32]));
 */
@JsonDeserialize(using = HexBytesDeserializer::class)
@JsonSerialize(using = HexBytesSerializer::class)
class HexBytes private constructor(val bytes: ByteArray) : Comparable<HexBytes>, RlpWritable {
    private val hashCode: Int = bytes.contentHashCode()

    operator fun get(index: Int): Int {
        return java.lang.Byte.toUnsignedInt(bytes[index])
    }

    val size: Int = bytes.size

    fun isEmpty(): Boolean = bytes.isEmpty()

    override fun toString(): String {
        return hex
    }

    val hex: String by lazy {
        Hex.toHexString(bytes)
    }


    operator fun plus(other: HexBytes): HexBytes {
        return HexBytes(
            ByteUtil.merge(bytes, other.bytes)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val hexBytes = other as HexBytes
        return FastByteComparisons.equal(bytes, hexBytes.bytes)
    }

    override fun compareTo(other: HexBytes): Int {
        return FastByteComparisons.compareTo(
            bytes, 0, bytes.size,
            other.bytes, 0, other.bytes.size
        )
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun writeToBuf(rlpBuffer: RlpBuffer): Int {
        return rlpBuffer.writeBytes(bytes)
    }

    companion object {
        val EMPTY_BYTES = ByteArray(0)

        // singleton zero value of HexBytes
        @JvmField
        val EMPTY = HexBytes(EMPTY_BYTES)


        @RlpCreator
        @JvmStatic
        fun create(bin: ByteArray, streamId: Long): HexBytes {
            return fromBytes(StreamId.asBytes(bin, streamId))
        }

        @JvmStatic
        fun encode(bytes: ByteArray): String {
            return Hex.toHexString(bytes)
        }

        @JvmStatic
        fun decode(hex: String): ByteArray {
            return Hex.decode(if (hex.startsWith("0x")) hex.substring(2) else hex)
        }

        @JvmStatic
        fun fromHex(hex: String): HexBytes {
            return fromBytes(decode(hex))
        }

        @JvmStatic
        fun fromBytes(bytes: ByteArray?): HexBytes {
            return if (bytes == null || bytes.isEmpty()) EMPTY else HexBytes(bytes)
        }

        @JvmStatic
        fun empty(): HexBytes {
            return EMPTY
        }
    }

}