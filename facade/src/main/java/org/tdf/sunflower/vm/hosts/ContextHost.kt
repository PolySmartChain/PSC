package org.tdf.sunflower.vm.hosts

import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.types.FunctionType
import org.tdf.lotusvm.types.ValueType
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.WBI.malloc
import org.tdf.sunflower.vm.WBI.mallocAddress

class ContextHost(
    private val callData: CallData
) : HostFunction("_context", FUNCTION_TYPE) {
    override fun execute(vararg args: Long): Long {
        return when (val type = args[0]) {
            ContextType.THIS_ADDRESS -> {
                mallocAddress(instance, callData.to).toLong()
            }
            ContextType.MSG_SENDER -> {
                mallocAddress(instance, callData.caller).toLong()
            }
            ContextType.MSG_VALUE -> {
                malloc(instance, callData.value).toLong()
            }
            else -> throw RuntimeException("unexpected type $type")
        }
    }

    object ContextType {
        const val THIS_ADDRESS: Long = 0x644836c2 // keccak256(this)
        const val MSG_SENDER: Long = 0xb2f2618c // keccak256(msg.sender)
        const val MSG_VALUE: Long = 0x6db8129b  // keccak256(msg.value)
    }

    companion object {
        val FUNCTION_TYPE = FunctionType(listOf(ValueType.I64, ValueType.I64), listOf(ValueType.I64))
    }
}