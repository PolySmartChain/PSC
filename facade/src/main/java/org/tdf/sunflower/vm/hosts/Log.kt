package org.tdf.sunflower.vm.hosts

import org.slf4j.LoggerFactory
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.types.FunctionType
import org.tdf.lotusvm.types.ValueType
import org.tdf.sunflower.vm.WBI.peek
import org.tdf.sunflower.vm.abi.WbiType

class Log : HostFunction("_log", FUNCTION_TYPE) {
    override fun execute(vararg args: Long): Long {
        log.info(
            peek(instance, args[0].toInt(), WbiType.STRING) as String
        )
        return 0
    }

    companion object {
        val FUNCTION_TYPE = FunctionType(listOf(ValueType.I64), emptyList())
        private val log = LoggerFactory.getLogger("vm")
    }
}