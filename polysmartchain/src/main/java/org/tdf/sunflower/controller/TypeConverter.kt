package org.tdf.sunflower.controller

import org.spongycastle.util.encoders.Hex
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import java.math.BigInteger

@JvmInline
internal value class JsonHex(private val s: String) {
    override fun toString(): String {
        return s
    }

    val bytes: ByteArray get() = ByteUtil.hexStringToBytes(s)

    val isHash: Boolean get() = s.startsWith("0x") && s.length == 32 * 2 + 2

    val long: Long
        get() {
            if (!s.startsWith("0x")) throw RuntimeException("Incorrect hex syntax")
            val x = s.substring(2)
            return x.toLong(16)
        }

    val int: Int
        get() {
            if (!s.startsWith("0x")) throw RuntimeException("Incorrect hex syntax")
            val x = s.substring(2)
            return x.toInt(16)
        }

    val u256: Uint256
        get() {
            return Uint256.of(ByteUtil.hexStringToBytes(s))
        }

    val hex: HexBytes
        get() {
            return HexBytes.fromBytes(ByteUtil.hexStringToBytes(s))
        }
}

internal val String.jsonHex: JsonHex
    get() {
        return JsonHex(this)
    }

internal fun String?.bytes(): ByteArray {
    return ByteUtil.hexStringToBytes(this)
}

internal val ByteArray.jsonHex: String get() = "0x" + Hex.toHexString(this)

internal val ByteArray?.jsonHexNum: String
    get() {
        if (this == null) {
            return "0x0"
        }
        val hex = Hex.toHexString(this)
        return "0x" + (if (hex.isEmpty()) "0" else hex)
    }

internal val Int.jsonHex: String get() = "0x" + java.lang.Long.toHexString(this.toLong())

internal val Long.jsonHex: String get() = "0x" + java.lang.Long.toHexString(this)

internal val Uint256.jsonHex: String get() = "0x" + this.value.toString(16)

internal val HexBytes.jsonHex: String get() = this.bytes.jsonHex

internal val HexBytes.jsonHexNum: String get() = this.bytes.jsonHexNum

internal val BigInteger.jsonHex: String get() = "0x" + this.toString(16)

object TypeConverter {
    fun toJsonHex(x: Long?): String? {
        return if (x == null) null else "0x" + java.lang.Long.toHexString(x)
    }

    @JvmStatic
    fun toJsonHex(n: BigInteger): String {
        return "0x" + n.toString(16)
    }
}