package org.tdf.sunflower.facade

import org.tdf.sunflower.net.Context
import org.tdf.sunflower.net.Peer
import org.tdf.sunflower.net.PeerServer

interface PeerServerListener {
    // triggered when new message received
    fun onMessage(context: Context, server: PeerServer)

    // triggered when server starts, you could run scheduled task here
    // you could set the PeerServer as your member here
    fun onStart(server: PeerServer)

    // triggered when a new peer connected
    fun onNewPeer(peer: Peer, server: PeerServer)

    // triggered when a peer disconnected
    fun onDisconnect(peer: Peer, server: PeerServer)

    companion object {
        val NONE: PeerServerListener = object : PeerServerListener {
            override fun onMessage(context: Context, server: PeerServer) {}
            override fun onStart(server: PeerServer) {}
            override fun onNewPeer(peer: Peer, server: PeerServer) {}
            override fun onDisconnect(peer: Peer, server: PeerServer) {}
        }
    }
}