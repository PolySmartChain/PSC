package org.tdf.sunflower.types

import com.fasterxml.jackson.databind.JsonNode
import org.tdf.common.types.Uint256
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.state.Account
import java.math.BigInteger

internal fun String.bn(): BigInteger {
    return if (this.startsWith("0x")) BigInteger(this.substring(2), 16) else BigInteger(this)
}

abstract class AbstractGenesis(protected var parsed: JsonNode) {
    abstract val block: Block

    val mstore8Block: Long?
        get() = parsed["config"]?.get("mstore8Block")?.asLong()

    val timestamp: Long
        get() = parsed["timestamp"]?.asLong() ?: 0L

    val parentHash: HexBytes
        get() = parsed["gasLimit"]?.asText()?.hex() ?: ByteUtil.ZEROS_32

    val gasLimit: Long
        get() = parsed["gasLimit"]?.asLong() ?: DEFAULT_BLOCK_GAS_LIMIT

    val alloc: Map<HexBytes, Account>
        get() {
            val alloc = parsed["alloc"] ?: return emptyMap()
            val r: MutableMap<HexBytes, Account> = HashMap()
            val it = alloc.fieldNames()
            while (it.hasNext()) {
                val k = it.next()
                val v = alloc[k].asText()
                val balance = Uint256.of(v.bn())
                r[k.hex()] = Account(balance = balance)
            }
            return r
        }

    protected fun getArray(field: String): List<JsonNode> {
        val n = parsed[field]
        if (n == null || n.isNull) return emptyList()
        val li: MutableList<JsonNode> = ArrayList()
        for (i in 0 until n.size()) {
            li.add(n[i])
        }
        return li
    }

    companion object {
        const val DEFAULT_BLOCK_GAS_LIMIT: Long = 60000000
    }
}