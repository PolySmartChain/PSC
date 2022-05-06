package org.tdf.sunflower.net

import org.tdf.common.crypto.ECKey
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.types.PropertyReader
import java.net.URI

class PeerServerConfig(private val reader: PropertyReader) {
    companion object {
        const val PREFIX = ""
    }

    private fun PropertyReader.getAsPositive(key: String): Int {
        val raw = this.getAsInt(key)
        return raw.takeIf { it > 0 } ?: throw RuntimeException("invalid $key = $raw, should be positive")
    }

    val maxPeers: Int = reader.getAsPositive("max-peers")

    val maxTTL: Int = reader.getAsPositive("max-ttl")

    val discovery: Boolean = reader.getAsBool("enable-discovery")

    val address: URI = URI(reader.getAsNonNull("address"))

    val bootstraps: List<URI> = reader.getAsList("bootstraps").map { URI(it) }
    val trusted: List<URI> = reader.getAsList("trusted").map { URI(it) }
    val blocks: Set<HexBytes> = reader.getAsList("blocks").map { it.hex() }.toSet()

    val persist: Boolean = reader.getAsBool("persist")
    val discoverRate: Int = reader.getAsPositive("discover-rate")
    val maxPacketSize: Int = reader.getAsPositive("max-packet-size")
    val cacheExpiredAfter: Int = reader.getAsPositive("cache-expired-after")
    val privateKey: HexBytes = reader.getAsPrivate("private-key") ?: ECKey().privKeyBytes.hex()

    val port: Int = reader.getAsInt("port", 0)
}