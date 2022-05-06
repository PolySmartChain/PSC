package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.tdf.sunflower.facade.PropertiesWrapper
import org.tdf.sunflower.types.PropertyReader
import java.util.*

object ApplicationConstants {
    const val CONSENSUS_POA = "poa"
    const val CONSENSUS_NONE = "none"
    const val CONSENSUS_POW = "pow"
    const val CONSENSUS_POS = "pos"
    const val CONSENSUS_VRF = "vrf"
    const val MAX_SHUTDOWN_WAITING: Long = 5
}


@ConfigurationProperties(prefix = "sunflower.consensus")
@Component
object ConsensusProperties : Properties()

@ConfigurationProperties(prefix = "sunflower.database")
@Component
class DatabaseConfigProperties : Properties()

@Component
class DatabaseConfig(properties: DatabaseConfigProperties) {
    val rd = PropertyReader(PropertiesWrapper(properties))
    val name: String = rd.getAsLowerCased("name")
    val maxOpenFiles: Int = rd.getAsInt("max-open-files")
    val directory: String = rd.getAsNonNull("directory")
    val reset: Boolean = rd.getAsBool("reset")
    val blockStore: String = rd.getAsNonNull("block-store")
    val buffer: Int = rd.getAsInt("buffer")
    val canonicalize: Boolean = rd.getAsBool("canonicalize")
    val deleteGT: Long = rd.getAsLong("delete-gt", -1L)
}

@ConfigurationProperties(prefix = "sunflower")
@Component
class GlobalConfig : HashMap<String, Any>()


@ConfigurationProperties(prefix = "sunflower.p2p")
@Component
class PeerServerProperties : Properties()