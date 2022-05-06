package org.tdf.common.util

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.types.Constants.ADDRESS_SIZE
import org.tdf.common.types.Constants.WORD_SIZE
import org.tdf.common.types.Uint256
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.*

fun Int.min(other: Int): Int {
    return if(this < other) { this } else { other }
}

fun Long.min(other: Long): Long {
    return if(this < other) { this } else { other }
}

class Permutation<T> (private val col: List<List<T>>) {
    init {
        if(col.any { it.isEmpty() })
            throw RuntimeException("invalid permutation: empty list found")
    }

    private val indices = IntArray(col.size)
    private val max = col.map { it.size - 1 }.toIntArray()
    private var eof = false


    fun next(): List<T>? {
        if(eof)
            return null
        eof = indices.contentEquals(max)
        val r = mutableListOf<T>()

        for(i in indices.indices) {
            r.add(col[i][indices[i]])
        }
        inc()
        return r
    }

    private fun inc() {
        var carry = 1
        for(i in indices.indices) {
            if(indices[i] == max[i] && carry == 1) {
                indices[i] = 0
                carry = 1
            } else {
                indices[i] += carry
                carry = 0
            }
        }
    }
}

/**
 * abi encoded selector part
 */
fun ByteArray.selector(): ByteArray {
    return sliceArray(0 until 4)
}

fun Long.bn(): BigInteger {
    return BigInteger.valueOf(this)
}

/**
 * abi encoded without selector
 */
fun ByteArray.unselect(): ByteArray {
    return sliceArray(4 until size)
}

/**
 * decode from rlp
 */
fun <T> HexBytes.decode(clazz: Class<T>): T {
    return Rlp.decode(this.bytes, clazz)
}

fun HexBytes.h256(): H256 {
    if (this.size != WORD_SIZE)
        throw RuntimeException("invalid word size")
    return this
}

/**
 * decode from rlp
 */
fun <T> ByteArray.decode(clazz: Class<T>): T {
    return Rlp.decode(this, clazz)
}

fun ByteArray.hex(): HexBytes {
    return HexBytes.fromBytes(this)
}

fun ByteArray.sha3(): ByteArray {
    return HashUtil.sha3(this)
}

fun ByteArray.tail20(): ByteArray {
    return this.copyOfRange(this.size - 20, this.size)
}

fun HexBytes.sha3(): HexBytes {
    return HexBytes.fromBytes(HashUtil.sha3(this.bytes))
}

fun Any?.rlp(): ByteArray {
    return Rlp.encode(this)
}


fun String.hex(): HexBytes {
    return HexBytes.fromHex(this)
}

fun String.bn(): BigInteger {
    if(this.startsWith("0x"))
        return BigInteger(this, 16)
    return BigInteger(this)
}

fun String.u256(): Uint256 {
    if(this.startsWith("0x"))
        return Uint256.of(this.hex().bytes)
    return Uint256.of(BigInteger(this))
}

fun String.h256(): HexBytes {
    val r = this.hex()
    if(r.size != WORD_SIZE)
        throw RuntimeException("invalid hash $this size = ${r.size}")
    return r
}

fun String.address(): Address {
    val r = this.hex()
    if(r.size != ADDRESS_SIZE)
        throw RuntimeException("invalid address $this size = ${r.size}")
    return r
}

fun String.ascii(): ByteArray {
    return this.toByteArray(StandardCharsets.US_ASCII)
}

fun Long.bytes(): ByteArray {
    return ByteUtil.longToBytesNoLeadZeroes(this)
}

fun Long.bytes8(): ByteArray {
    return BigEndian.encodeInt64(this)
}

fun ByteArray.u256(): Uint256 {
    return Uint256.of(this)
}

fun ByteArray.long(): Long {
    return BigEndian.decodeInt64(this, 0)
}

fun ByteArray.bn(): BigInteger {
    return BigInteger(1, this)
}

fun Long.u256(): Uint256 {
    return Uint256.of(this)
}

fun BigInteger.u256(): Uint256 {
    return Uint256.of(this)
}

fun BigInteger.bytes(): ByteArray {
    return BigIntegers.asUnsignedByteArray(this)
}

fun BigInteger.bytes32(): ByteArray {
    return BigIntegers.asUnsignedByteArray(WORD_SIZE, this)
}