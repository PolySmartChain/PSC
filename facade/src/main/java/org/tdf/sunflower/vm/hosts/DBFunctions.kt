package org.tdf.sunflower.vm.hosts

import org.tdf.common.util.HexBytes
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.types.FunctionType
import org.tdf.lotusvm.types.ValueType
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.WBI.mallocBytes
import org.tdf.sunflower.vm.WBI.peek
import org.tdf.sunflower.vm.abi.WbiType

class DBFunctions(val backend: Backend, private val address: HexBytes) : HostFunction("_db", FUNCTION_TYPE) {
    private fun getKey(vararg longs: Long): HexBytes {
        return peek(instance, longs[1].toInt(), WbiType.BYTES) as HexBytes
    }

    private fun getValue(vararg longs: Long): HexBytes {
        return peek(instance, longs[2].toInt(), WbiType.BYTES) as HexBytes
    }

    override fun execute(vararg args: Long): Long {
        return when (Type.values()[args[0].toInt()]) {
            Type.SET -> {
                val key = getKey(*args)
                val value = getValue(*args)
                backend.dbSet(address, key, value)
                0
            }
            Type.GET -> {
                val key = getKey(*args)
                val value = backend.dbGet(address, key)
                mallocBytes(instance, value).toLong()
            }
            Type.HAS -> {
                val key = getKey(*args)
                if (backend.dbHas(address, key)) 1 else 0
            }
            Type.REMOVE -> {
                val key = getKey(*args)
                backend.dbRemove(address, key)
                0
            }
        }
    }

    internal enum class Type {
        SET, GET, REMOVE, HAS
    }

    companion object {
        val FUNCTION_TYPE = FunctionType(listOf(ValueType.I64, ValueType.I64, ValueType.I64), listOf(ValueType.I64))
    }
}