package org.tdf.sunflower.consensus.pos

import org.tdf.common.util.HexBytes
import org.tdf.common.util.HexBytes.Companion.fromBytes
import org.tdf.sunflower.consensus.AbstractValidator
import org.tdf.sunflower.consensus.pow.PoW
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.ValidateResult
import org.tdf.sunflower.types.ValidateResult.Companion.fault

class PoSValidator(accountTrie: StateTrie<HexBytes, Account>, private val pos: PoS, override val chainId: Int) :
    AbstractValidator(accountTrie) {
    override fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult {
        val res = super.commonValidate(rd, block, dependency)
        if (!res.success) return res

        val p = pos.getProposer(rd, dependency, block.createdAt)
        val eq = p?.first == block.coinbase

        if (!eq) {
            return fault("invalid proposer " + block.body[0].sender)
        }

        // validate pow
        val diff = pos.getDifficulty(rd, dependency)


        return if (PoW.compare(PoW.getPoWHash(block), diff.byte32) > 0) fault(
            String.format(
                "nbits validate failed hash = %s, nbits = %s",
                fromBytes(PoW.getPoWHash(block)),
                fromBytes(diff.byte32)
            )
        ) else res
    }
}