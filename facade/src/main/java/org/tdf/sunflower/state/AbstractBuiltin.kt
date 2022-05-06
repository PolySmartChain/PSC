package org.tdf.sunflower.state

import org.tdf.common.util.FastByteComparisons
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.common.util.selector
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi


abstract class AbstractBuiltin protected constructor(
    override val address: HexBytes,
) : Builtin {

    private fun getSelector(data: HexBytes): ByteArray {
        return data.bytes.selector()
    }

    override fun call(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData): ByteArray {
        val func = getFunction(callData.data)
        val inputs: Array<Any> = func.decode(callData.data.bytes).map { it as Any }.toTypedArray()
        val results = call(rd, backend, ctx, callData, func.name, *inputs)
        return Abi.Entry.Param.encodeList(func.outputs, *results.toTypedArray())
    }

    private fun getFunction(data: HexBytes): Abi.Function {
        val selector = getSelector(data)
        return abi.findFunction { FastByteComparisons.equal(it.encodeSignature(), selector) }!!
    }

    private fun getFunction(method: String): Abi.Function {
        return abi.findFunction { it.name == method }!!
    }

    override fun view(rd: RepositoryReader, backend: Backend, method: String, vararg args: Any): List<*> {
        val func = getFunction(method)
        val encoded = func.encode(*args)
        val callData = CallData(data = encoded.hex())
        return call(
            rd,
            backend,
            CallContext(),
            callData,
            method,
            *args
        )
    }
}