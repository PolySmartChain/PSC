package org.tdf.sunflower.net

import org.tdf.sunflower.facade.PeerServerListener
import org.tdf.sunflower.proto.Code

// wrap listener as plugin
class PluginWrapper internal constructor(private val listener: PeerServerListener) : Plugin {
    override fun onMessage(context: ContextImpl, server: PeerServerImpl) {
        if (context.msg.code == Code.ANOTHER) {
            listener.onMessage(context, server)
        }
    }

    override fun onStart(server: PeerServerImpl) {
        listener.onStart(server)
    }

    override fun onNewPeer(peer: PeerImpl, server: PeerServerImpl) {
        listener.onNewPeer(peer, server)
    }

    override fun onDisconnect(peer: PeerImpl, server: PeerServerImpl) {
        listener.onDisconnect(peer, server)
    }

    override fun onStop(server: PeerServerImpl) {}
}