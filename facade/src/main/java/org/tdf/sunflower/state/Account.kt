package org.tdf.sunflower.state

import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.types.Uint256
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes

@RlpProps("nonce", "balance", "storageRoot", "contractHash")
data class Account @RlpCreator constructor(
    val nonce: Long = 0L,
    val balance: Uint256 = Uint256.ZERO,
    val storageRoot: HexBytes = HashUtil.EMPTY_TRIE_HASH_HEX,
    val contractHash: HexBytes = HashUtil.EMPTY_DATA_HASH_HEX
) {

    val isEmpty: Boolean
        get() = nonce == 0L && balance.isZero && contractHash == HashUtil.EMPTY_DATA_HASH_HEX && storageRoot == HashUtil.EMPTY_TRIE_HASH_HEX

    companion object {
        private val EMPTY_ACCOUNT = Account()

        fun empty(): Account {
            return EMPTY_ACCOUNT
        }
    }
}