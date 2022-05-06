package org.tdf.sunflower.vm.hosts

import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.types.FunctionType
import org.tdf.lotusvm.types.ValueType
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.WBI.peek
import org.tdf.sunflower.vm.abi.WbiType

class Transfer(private val backend: Backend, private val contractAddress: HexBytes) :
    HostFunction("_transfer", FUNCTION_TYPE) {
    override fun execute(vararg args: Long): Long {
        if (args[0] != 0L) {
            throw RuntimeException("unexpected")
        }
        val toAddr = HexBytes.fromBytes(peek(instance, args[1].toInt(), WbiType.ADDRESS) as ByteArray)
        val amount = peek(instance, args[2].toInt(), WbiType.UINT_256) as Uint256
        backend.subBalance(contractAddress, amount)
        backend.addBalance(toAddr, amount)
        return 0
    }

    companion object {
        val FUNCTION_TYPE = FunctionType(
            listOf(ValueType.I64, ValueType.I64, ValueType.I64), emptyList()
        )
    }
}