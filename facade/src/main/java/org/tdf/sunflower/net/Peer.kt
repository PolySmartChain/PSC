package org.tdf.sunflower.net

import org.tdf.common.util.HexBytes

// Peer is a p2p node could be connected
interface Peer {
    // get the host name of remote peer
    val host: String

    // get the server port of remote
    val port: Int

    // the id is typically an ecc public key
    val id: HexBytes

    // encode the remote peer as uri
    fun encodeURI(): String
    val protocol: String

    companion object {
        val NONE: Peer = object : Peer {
            override val host: String
                get() = ""
            override val port: Int
                get() = 0
            override val id: HexBytes
                get() = HexBytes.empty()

            override fun encodeURI(): String {
                return ""
            }

            override val protocol: String
                get() = ""
        }
    }
}