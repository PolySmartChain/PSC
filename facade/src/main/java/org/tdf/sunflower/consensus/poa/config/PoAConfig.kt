package org.tdf.sunflower.consensus.poa.config

import org.tdf.common.crypto.ECKey
import org.tdf.common.util.Address
import org.tdf.common.util.hex
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.types.ConsensusConfig

class PoAConfig(properties: PropertyLike) : ConsensusConfig(properties) {
    override val coinbase: Address?
        get() {
            val key = privateKey?.bytes?.let { ECKey.fromPrivate(it) }
            return key?.address?.hex()
        }

    companion object {
        @JvmStatic
        fun from(c: ConsensusConfig): PoAConfig {
            return PoAConfig(c.properties)
        }
    }
}