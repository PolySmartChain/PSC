package org.tdf.sunflower.consensus.poa

import org.tdf.common.types.Uint256
import org.tdf.sunflower.consensus.EconomicModel

class EmptyModel : EconomicModel {
    override fun rewardAt(height: Long): Uint256 {
        return Uint256.ZERO
    }
}