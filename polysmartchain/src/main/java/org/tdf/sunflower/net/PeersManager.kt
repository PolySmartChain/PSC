package org.tdf.sunflower.net

import com.fasterxml.jackson.databind.node.TextNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.util.FixedDelayScheduler
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Disconnect
import org.tdf.sunflower.proto.Peers
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PeersManager internal constructor(private val config: PeerServerConfig) : Plugin {
    private val pending: MutableMap<PeerImpl, Boolean> = ConcurrentHashMap()
    private lateinit var server: PeerServerImpl

    override fun onMessage(context: ContextImpl, server: PeerServerImpl) {
        val client = server.client
        val cache = client.peersCache
        val builder = client.builder
        context.keep()
        when (context.msg.code) {
            Code.PING -> {
                context.channel.write(builder.buildPong(), client)
                return
            }
            Code.LOOK_UP -> {
                context.channel.write(
                    builder.buildPeers(server.peers), client
                )
                return
            }
            Code.PEERS -> {
                if (!config.discovery) return
                Peers.parseFrom(context.msg.body).peersList
                    .mapNotNull { PeerImpl.parse(it) }
                    .filter { !cache.contains(it) && it != server.self && it.protocol == server.self.protocol }
                    .forEach { pending[it] = true }
                return
            }
            Code.DISCONNECT -> {
                val reason = Disconnect.parseFrom(context.msg.body).reason
                if (reason != null && reason.isNotEmpty()) log.error("disconnect from peer " + context.remote + " reason is " + reason)
                context.channel.close()
                return
            }
            else -> return
        }
    }

    override fun onStart(server: PeerServerImpl) {
        this.server = server
        val client = server.client
        val cache = client.peersCache
        val builder = client.builder

        val pingTicker = FixedDelayScheduler("PeersManager-Ping", config.discoverRate.toLong())
        // keep self alive
        pingTicker.delay {
            try {
                client.broadcast(builder.buildPing())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val loopTicker = FixedDelayScheduler("PeersManager-Discover", config.discoverRate.toLong())
        loopTicker.delay {
            try {
                loopPeers(server, client, cache, builder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loopPeers(server: PeerServerImpl, client: Client, cache: PeersCache, builder: MessageBuilder) {
        // persist peers
        server.peerStore.clear()
        server.peerStore.putAll(
            client.peersCache.peers.map { AbstractMap.SimpleEntry(it.id.hex, TextNode(it.encodeURI())) }
        )
        server.peerStore.flush()

        lookup()
        cache.half()
        if (!config.discovery)
            return

        pending.keys
            .filter { !cache.contains(it) }
            .take(config.maxPeers)
            .forEach {
                log.info("try to connect to peer $it")
                client.dial(it, builder.buildPing())
            }
        pending.clear()
    }

    private fun lookup() {
        val client = server.client
        val cache = client.peersCache
        val builder = client.builder

        if (!config.discovery) {
            // keep channel to bootstraps and trusted alive
            val keys = cache.bootstraps.keys + cache.trusted.keys
            for (key in keys) {
                if (cache.contains(key))
                    continue
                server.client.dial(key, builder.buildPing())
            }
            return
        }

        // query for neighbours when neighbours are not empty
        if (cache.size() > 0) {
            client.broadcast(builder.buildLookup())
            cache.trusted.keys.forEach { client.dial(it, builder.buildPing()) }
            return
        }

        // query for peers from bootstraps and trusted when neighbours are empty
        arrayOf(config.bootstraps, config.trusted)
            .flatMap { it }
            .forEach { client.dial(it.host, it.port, builder.buildLookup()) }
    }

    override fun onNewPeer(peer: PeerImpl, server: PeerServerImpl) {}
    override fun onDisconnect(peer: PeerImpl, server: PeerServerImpl) {}

    override fun onStop(server: PeerServerImpl) {
        server.peerStore.flush()
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }
}