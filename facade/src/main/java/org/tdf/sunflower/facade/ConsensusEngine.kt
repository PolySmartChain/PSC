package org.tdf.sunflower.facade

import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.ConsensusConfig

interface ConsensusEngine {
    // the below getters shouldn't return null after initialized
    val miner: Miner
    val validator: Validator
    val genesisBlock: Block
    val peerServerListener: PeerServerListener

    // inject configurations, throw exception if configuration is invalid
    fun init(config: ConsensusConfig)
    val name: String
}