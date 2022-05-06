package org.tdf.sunflower.controller

import org.tdf.common.types.Uint256
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.CallType


internal object JsonRpcUtil {
    private fun String?.notEmptyOrNull(): String? {
        if (this == null || this.trim().isEmpty())
            return null
        return this.trim()
    }

    private fun String?.number(): Long {
        return this.notEmptyOrNull()?.jsonHex?.long ?: 0L
    }

    private fun String?.address(): HexBytes {
        return this.notEmptyOrNull()?.jsonHex?.hex ?: AddrUtil.empty()
    }

    private fun String?.u256(): Uint256 {
        return this.notEmptyOrNull()?.jsonHex?.u256 ?: Uint256.ZERO
    }

    private fun String?.hex(): HexBytes {
        return this.notEmptyOrNull()?.jsonHex?.hex ?: HexBytes.empty()
    }

    fun toCallContext(
        args: JsonRpc.CallArguments,
        nonce: Long?,
        chainId: Int,
        timestamp: Long,
        coinbase: HexBytes,
        blockHashMap: Map<Long, ByteArray> = emptyMap()
    ): CallContext {
        return CallContext(
            args.from.address(),
            HashUtil.EMPTY_DATA_HASH_HEX,
            nonce ?: args.nonce.number(),
            args.gasPrice.u256(),
            chainId,
            timestamp,
            coinbase,
            blockHashMap
        )
    }


    fun toCallData(args: JsonRpc.CallArguments): CallData {
        return CallData(
            args.from.address(),
            args.value.u256(),
            args.to.address(),
            if ((args.to ?: "").trim().isEmpty()) {
                CallType.CREATE
            } else {
                CallType.CALL
            },
            args.data.hex(),
        )
    }
}