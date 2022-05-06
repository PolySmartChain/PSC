package org.tdf.sunflower.net

import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.store.JsonStore
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.facade.PeerServerListener
import org.tdf.sunflower.facade.PropertiesWrapper
import org.tdf.sunflower.proto.Message
import org.tdf.sunflower.types.PropertyReader
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import java.util.stream.Stream

@Slf4j(topic = "net")
class PeerServerImpl(// if non-database provided, use memory database
    val peerStore: JsonStore,
    consensusEngine: ConsensusEngine,
    properties: Properties
) : ChannelListener, PeerServer {
    private val plugins: MutableList<Plugin> = CopyOnWriteArrayList()
    private var config: PeerServerConfig

    val client: Client

    override lateinit var self: PeerImpl
        private set

    private val builder: MessageBuilder
    private val netLayer: NetLayer


    override val isFull: Boolean
        get() {
            return client.peersCache.isFull
        }


    override fun dial(peer: Peer, message: ByteArray) {
        builder.buildAnother(message, 1, peer)
            .forEach { client.dial(peer, it) }
    }

    override fun broadcast(message: ByteArray) {
        client.peersCache.channels
            .filter { ch: Channel -> ch.remote != null }
            .forEach { ch: Channel ->
                builder.buildAnother(message, config.maxTTL.toLong(), ch.remote!!)
                    .forEach { ch.write(it, client) }
            }
    }

    override val bootstraps: List<Peer>
        get() = client.peersCache.bootstraps.keys.toList()


    override val peers: List<Peer>
        get() = client.peersCache.peers

    override fun addListeners(vararg peerServerListeners: PeerServerListener) {
        for (listener in peerServerListeners) {
            plugins.add(PluginWrapper(listener))
        }
    }


    override fun start() {
        plugins.forEach { it.onStart(this) }
        netLayer.start()
        log.info(
            "peer server is listening on " +
                    self.encodeURI()
        )
        client.bootstrap(config.bootstraps)
        client.trust(config.trusted)

        // connect to stored peers when server restarts
        // TODO: discover by udp protocol
        val keys = peerStore.node.entries.toList()
        keys.forEach { k ->
            if ("self" == k.key)
                return
            val peer = PeerImpl.parse(k.value.asText())!!
            client.dial(peer.host, peer.port, builder.buildPing())
        }
    }

    init {
        try {
            config = PeerServerConfig(PropertyReader(PropertiesWrapper(properties)))
        } catch (e: Exception) {
            throw RuntimeException(
                "load properties failed"
            )
        }
        if (!config.discovery &&
            Stream.of(config.bootstraps, config.trusted)
                .filter { obj: List<URI?>? -> Objects.nonNull(obj) }
                .map { obj: List<URI?> -> obj.size }
                .reduce(0) { a: Int, b: Int -> Integer.sum(a, b) } == 0
        ) {
            log.warn(
                "cannot connect to any peer for the discovery " +
                        "is disabled and none bootstraps and trusted provided"
            )
        }
        try {
            // find valid private key from 1.properties 2.persist 3. generate
            val sk = config.privateKey.bytes
            self = PeerImpl.createSelf(config.address, sk)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("failed to load peer server invalid address " + config.address)
        }
        builder = MessageBuilder(self, config)
        val port = if (config.port == 0) self.port else config.port
        netLayer = if ("grpc" == self.protocol) {
            GRpcNetLayer(port, builder)
        } else {
            WebSocketNetLayer(port, builder)
        }
        client = Client(self, config, builder, netLayer)
        client.listener = this

        netLayer.handler = Consumer { it.addListeners(client, this) }

        // loading plugins
        plugins.add(MessageFilter(config, consensusEngine))
        plugins.add(MessageLogger())
        plugins.add(PeersManager(config))
    }

    override fun onConnect(remote: PeerImpl, channel: Channel) {
        log.info("new connection {} channel direction = {}", remote, channel.direction)
        for (plugin in plugins) {
            plugin.onNewPeer(remote, this)
        }
    }

    override fun onMessage(message: Message, channel: Channel, udp: Boolean) {
        val peer = channel.remote
        if (peer == null) {
            channel.close("failed to parse peer " + message.remotePeer)
            throw RuntimeException("failed to parse peer")
        }
        val context =
            ContextImpl(channel = channel, client = client, msg = message, builder = builder, remote = peer, udp = udp)

        for (plugin in plugins) {
            if (context.exited) break
            try {
                plugin.onMessage(context, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onError(throwable: Throwable, channel: Channel) {}
    override fun onClose(channel: Channel) {
        if (channel.remote != null) {
            for (plugin in plugins) {
                plugin.onDisconnect(channel.remote!!, this)
            }
        }
    }

    private fun resolveHost() {
        if (self.host != "localhost" && self.host != "127.0.0.1") {
            return
        }
        var externalIP: String? = null
        try {
            externalIP = Util.externalIp()
        } catch (ignored: Exception) {
            log.error("cannot get external ip, fall back to bind ip")
        }
        if (externalIP != null && Util.ping(externalIP, self.port)) {
            log.info("ping $externalIP success, set as your host")
            self = self.copy(host = externalIP)
            return
        }
        var bindIP: String? = null
        try {
            bindIP = Util.bindIp()
        } catch (e: Exception) {
            log.error("get bind ip failed")
        }
        if (bindIP != null) {
            self = self.copy(host = bindIP)
        }
    }

    override fun stop() {
        plugins.forEach { x: Plugin -> x.onStop(this) }
        client.peersCache
            .channels
            .forEach { x: Channel -> x.close("application will shutdown") }
        try {
            netLayer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        log.info("peer server closed")
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("p2p")
    }
}