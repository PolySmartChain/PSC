package org.tdf.sunflower.types

import org.tdf.common.types.Uint256
import org.tdf.common.util.Address
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.facade.PropertyLike
import java.math.BigInteger

class PropertyReader(val properties: PropertyLike) {
    fun getAsLowerCased(property: String): String {
        val s = properties.getProperty(property)
        return (s ?: "").trim { it <= ' ' }.lowercase()
    }

    /**
     * get property as non null
     */
    fun getAsNonNull(property: String): String {
        val s = properties.getProperty(property)
        return (s ?: "").trim { it <= ' ' }
    }

    fun getAsBool(property: String): Boolean {
        return "true" == getAsLowerCased(property)
    }

    fun getAsInt(property: String): Int {
        val s = properties.getProperty(property)
            ?: throw RuntimeException("read property $property failed, property not found")
        return s.trim { it <= ' ' }.toInt()
    }

    fun getAsU256(property: String, defaultValue: Uint256): Uint256 {
        var s = properties.getProperty(property)
        if (s == null || s.trim { it <= ' ' }.isEmpty()) {
            return defaultValue
        }
        s = s.trim { it <= ' ' }.lowercase()
        val b = if (s.startsWith("0x")) BigInteger(s.substring(2), 16) else BigInteger(s)
        return Uint256.of(b)
    }

    fun getAsInt(property: String, defaultValue: Int): Int {
        val s = properties.getProperty(property)
        return s?.trim { it <= ' ' }?.takeIf { it.isNotEmpty() }?.toInt() ?: defaultValue;
    }

    fun getAsPrivate(property: String): HexBytes? {
        val s = properties.getProperty(property)
        val k = s?.trim { it <= ' ' }?.hex()
        if (k != null && k.size != 32) throw RuntimeException("invalid private key size: $k")
        return k
    }

    fun getAsLong(property: String, defaultValue: Long): Long {
        val s = properties.getProperty(property)
        if (s == null || s.trim { it <= ' ' }.isEmpty())
            return defaultValue
        return s.trim { it <= ' ' }.toLong()
    }

    fun getAsLong(property: String): Long {
        val s = properties.getProperty(property)
        if (s == null || s.trim { it <= ' ' }
                .isEmpty()) throw RuntimeException("read property $property failed, property not found")
        return s.trim { it <= ' ' }.toLong()
    }

    fun getAsAddress(property: String): Address? {
        val s = properties.getProperty(property)
        val addr = s?.lowercase()?.hex()
        if (addr != null && addr.size != Transaction.ADDRESS_LENGTH)
            throw RuntimeException("invalid address $addr length is ${addr.size}")
        return addr
    }

    fun getAsList(prefix: String): List<String> {
        val r = mutableListOf<String>()
        var i = 0
        while (true) {
            val v = properties.getProperty("$prefix.$i") ?: break
            r.add(v)
            i++
        }
        return r
    }
}