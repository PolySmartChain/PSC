package org.tdf.sunflower.net

import org.tdf.sunflower.facade.PeerServerListener

interface PeerServer {
    // dial a peer with a message
    fun dial(peer: Peer, message: ByteArray)

    // broadcast a message to all the peers
    fun broadcast(message: ByteArray)

    // get all peers had been connected
    val peers: List<Peer>

    // get all bootstraps
    val bootstraps: List<Peer>
    val isFull: Boolean
    fun addListeners(vararg peerServerListeners: PeerServerListener)
    fun start()
    fun stop()
    val self: Peer

    companion object {
        @JvmField
        val NONE: PeerServer = object : PeerServer {
            override fun dial(peer: Peer, message: ByteArray) {}
            override fun broadcast(message: ByteArray) {}
            override val peers: List<Peer>
                get() = emptyList()
            override val bootstraps: List<Peer>
                get() = emptyList()

            override fun addListeners(vararg peerServerListeners: PeerServerListener) {}
            override fun start() {}
            override fun stop() {}
            override val self: Peer
                get() = Peer.NONE
            override val isFull: Boolean
                get() = false
        }
    }
}