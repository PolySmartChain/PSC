package org.tdf.sunflower.vm.hosts

import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.lotusvm.Module
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.types.FunctionType
import org.tdf.lotusvm.types.ValueType
import org.tdf.sunflower.vm.ModuleValidator
import org.tdf.sunflower.vm.VMExecutor
import org.tdf.sunflower.vm.WBI
import org.tdf.sunflower.vm.abi.WbiType

class Reflect(private val executor: VMExecutor) : HostFunction("_reflect", FUNCTION_TYPE) {
    override fun execute(vararg args: Long): Long {

        when (val t = args[0]) {
            CALL -> {
                throw UnsupportedOperationException()
            }
            CREATE -> {
                throw UnsupportedOperationException()
            }
            UPDATE -> {
                val code = WBI.peek(instance, args[3].toInt(), WbiType.BYTES) as HexBytes
                // validate code
                Module.create(code.bytes).use {
                    ModuleValidator.validate(it, true)
                    // drop init code
                    executor.backend.setCode(executor.callData.to, WBI.dropInit(code.bytes).hex())
                    return 0
                }
            }
            else -> throw RuntimeException("reflect failed: unexpected $t")
        }

    }


    companion object {
        const val CALL: Long = 0x431a75a0L // keccak(call)
        const val CREATE: Long = 0x94a69ce1L // keccak(create)
        const val UPDATE: Long = 0x5ef8d21b // keccak(update)
        val FUNCTION_TYPE = FunctionType( // offset, length, offset
            listOf(
                ValueType.I64, ValueType.I64,
                ValueType.I64, ValueType.I64,
                ValueType.I64, ValueType.I64
            ), listOf(ValueType.I64)
        )
    }
}