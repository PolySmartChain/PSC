package org.tdf.sunflower.state

import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi
import java.math.BigInteger


interface Builtin {
    val codeSize: Int get() = 1
    val address: HexBytes
    val pure: Boolean
    val genesisStorage: Map<HexBytes, HexBytes>
        get() = emptyMap()

    /**
     * call builtin contract by abi encoded parameters
     */
    fun call(rd: RepositoryReader, backend: Backend, ctx: CallContext, callData: CallData): ByteArray {
        return ByteUtil.EMPTY_BYTE_ARRAY
    }

    /**
     * call builtin contract by var args
     */
    fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*>

    val abi: Abi

    /**
     * static call
     */
    fun view(rd: RepositoryReader, backend: Backend, method: String, vararg args: Any): List<*> {
        return emptyList<Any>()
    }
}


class LoggingContract : AbstractBuiltin(Constants.LOGGING_CONTRACT_ADDR) {
    override val pure: Boolean
        get() = true

    override fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        val a = args[0]
        when (a) {
            is String -> println(a)
            is ByteArray -> println(a.hex())
            is BigInteger -> println(a)
        }
        return emptyList<Any>()
    }

    override val abi: Abi = ABI


    companion object {
        const val abiJson = """
[
    {
        "inputs": [
            {
                "name": "arg",
                "type": "string"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    },
    {
        "inputs": [
            {
                "name": "arg",
                "type": "address"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    },
    {
        "inputs": [
            {
                "name": "arg",
                "type": "uint256"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    },
    {
        "inputs": [
            {
                "name": "arg",
                "type": "bytes32"
            }
        ],
        "name": "log",
        "outputs": [],
        "stateMutability": "pure",
        "type": "function"
    }               
]
        """
        val ABI = Abi.fromJson(abiJson)
    }
}