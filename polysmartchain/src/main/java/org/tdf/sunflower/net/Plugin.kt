package org.tdf.sunflower.net

// Plugin for message handling
internal interface Plugin {
    fun onMessage(context: ContextImpl, server: PeerServerImpl)
    fun onStart(server: PeerServerImpl)
    fun onNewPeer(peer: PeerImpl, server: PeerServerImpl)
    fun onDisconnect(peer: PeerImpl, server: PeerServerImpl)
    fun onStop(server: PeerServerImpl)
}