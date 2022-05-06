package org.tdf.sunflower.consensus

import org.tdf.common.types.Uint256

interface EconomicModel {
    fun rewardAt(height: Long): Uint256
}