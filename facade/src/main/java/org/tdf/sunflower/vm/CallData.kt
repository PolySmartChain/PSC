package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.Address
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.Transaction

data class CallContext(
    val origin: HexBytes = AddrUtil.empty(),
    val txHash: HexBytes = HashUtil.EMPTY_DATA_HASH_HEX,
    val txNonce: Long = 0,
    val gasPrice: Uint256 = Uint256.ZERO,
    val chainId: Int = 0,
    val timestamp: Long = 0,
    val coinbase: HexBytes = AddrUtil.empty(),
    val blockHashMap: Map<Long, ByteArray> = emptyMap()
) {
    companion object {
        @JvmStatic
        fun fromTx(
            tx: Transaction, chainId: Int = tx.chainId ?: 0, timestamp: Long = 0, coinbase: HexBytes = AddrUtil.empty(), blockHashMap: Map<Long, ByteArray> = emptyMap()): CallContext {
            return CallContext(
                tx.sender,
                tx.hash,
                tx.nonce,
                tx.gasPrice,
                chainId,
                timestamp,
                coinbase,
                blockHashMap
            )
        }
    }
}

data class CallData(
    val caller: Address = AddrUtil.empty(),
    val value: Uint256 = Uint256.ZERO,
    val to: Address = AddrUtil.empty(),
    val callType: CallType = CallType.CALL,
    val data: HexBytes = HexBytes.empty(),
    // for delegate call, address is the delegate address
    val delegate: Address = AddrUtil.empty()
) {

    fun clone(): CallData {
        return this.copy()
    }

    companion object {
        @JvmStatic
        fun empty(): CallData {
            return CallData()
        }

        @JvmStatic
        fun fromTx(tx: Transaction, callType: CallType? = null): CallData {
            val t = callType ?: if (tx.to.isEmpty()) CallType.CREATE else CallType.CALL
            return CallData(
                tx.sender,
                tx.value,
                tx.to,
                t,
                tx.data,
            )
        }
    }
}