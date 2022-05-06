package org.tdf.sunflower.state

import org.tdf.common.crypto.ECKey
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.types.Transaction

object AddrUtil {
    val EMPTY: HexBytes = ByteArray(Transaction.ADDRESS_LENGTH).hex()

    @JvmStatic
    fun empty(): HexBytes {
        return EMPTY
    }

    @JvmStatic
    fun of(hex: String): HexBytes {
        val ret = hex.hex()
        if (ret.size == Transaction.ADDRESS_LENGTH) return ret
        throw RuntimeException("invalid hex, not an address")
    }

    @JvmStatic
    fun fromPrivate(privateK: ByteArray): HexBytes {
        return ECKey.fromPrivate(privateK).address.hex()
    }
}