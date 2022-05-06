package org.tdf.sunflower.net

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Message
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

@RlpProps("code", "n", "uri", "data")
data class UdpPacket @RlpCreator constructor(val code: Int, val n: Int, val uri: String, val data: HexBytes)

class Client(
        val self: PeerImpl,
        val config: PeerServerConfig,
        val builder: MessageBuilder,
        private val netLayer: NetLayer,
) : ChannelListener, AutoCloseable, UdpCtx {
    private val buf: ByteArray = ByteArray(256)
    private val tailBuf: ByteArray = ByteArray(512)
    private val lastUdpCache: Cache<HexBytes, Pair<InetAddress, Int>> = CacheBuilder.newBuilder().expireAfterWrite(ProtoChannel.UDP_DELAY * 1L, TimeUnit.MILLISECONDS).build<HexBytes, Pair<InetAddress, Int>>()

    override val socket = DatagramSocket(self.port)

    val ex = Executors.newSingleThreadExecutor()
    val n = AtomicInteger()

    @Volatile
    private var udpListening: Boolean = true

    val peersCache: PeersCache = PeersCache(self, config)

    // listener for channel event
    var listener = ChannelListener.NONE

    fun broadcast(message: Message) {
        peersCache.channels.forEach { it.write(message, this) }
    }

    fun dial(peer: Peer, message: Message) {
        getChannel(peer) { it.write(message, this) }
    }

    fun dial(host: String, port: Int, message: Message) {
        getChannel(host, port) { it.write(message, this) }
    }

    fun bootstrap(uris: Collection<URI>) {
        for (uri in uris) {
            log.info("bootstrap peer {}", uri.toString())
            pingUdp(uri.host, uri.port, { peersCache.bootstraps[it] = true }, {})
        }
    }

    fun trust(trusted: Collection<URI>) {
        for (uri in trusted) {
            log.info("bootstrap trusted peer {}", uri.toString())
            pingUdp(uri.host, uri.port, { peersCache.trusted[it] = true }, {})
        }
    }


    // try to get channel from cache, if channel not exists in cache,
    // create from net layer
    private fun getChannel(peer: Peer, handle: Consumer<Channel>) {
        // cannot create channel connect to your self
        if (peer == self) return
        val ch = peersCache
                .getChannel(peer.id)
        if (ch != null) {
            handle.accept(ch)
            return
        }
        pingUdp(peer.host, peer.port, {}, handle)
    }

    // try to get channel from cache, if channel not exists in cache,
    // create from net layer
    private fun getChannel(host: String, port: Int, handle: Consumer<Channel>) {
        val ch = peersCache.channels
                .firstOrNull {
                    it.remote?.host == host && it.remote?.port == port
                }

        if (ch != null) {
            handle.accept(ch)
            return
        }

        pingUdp(host, port, {}, handle)
    }

    override fun onConnect(remote: PeerImpl, channel: Channel) {
        if (!config.discovery &&
                !peersCache.bootstraps.containsKey(remote) &&
                !peersCache.trusted.containsKey(remote)
        ) {
            channel.close("discovery is not enabled accept bootstraps and trusted only")
            return
        }
        addPeer(remote, channel)
    }

    private fun addPeer(remote: PeerImpl, channel: Channel) {
        if (remote == self) {
            channel.close("close channel connect to self")
            return
        }
        val o = peersCache.getChannel(remote)
        if (o != null && o.isAlive) {
            return
        }
        peersCache.keep(remote, channel)
    }

    override fun onMessage(message: Message, channel: Channel, udp: Boolean) {}
    override fun onError(throwable: Throwable, channel: Channel) {
        throwable.printStackTrace()
        channel.remote
                ?.let {
                    peersCache.half(it)
                    log.error("error found decrease the score of peer " + it + " " + throwable.message)
                }
    }

    override fun onClose(channel: Channel) {
        channel.remote?.let {
            peersCache.remove(it.id, " channel closed")
        }
    }

    fun relay(message: Message, receivedFrom: PeerImpl) {
        peersCache.channels
                .filter { it.remote != null && it.remote != receivedFrom }
                .forEach {
                    if (message.code == Code.ANOTHER) {
                        val msg = message.body.toByteArray()
                        it.write(builder.buildMessage(Code.ANOTHER, message.ttl - 1, msg), this)
                        return@forEach
                    }
                    it.write(builder.buildRelay(message), this)
                }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger("net")
    }

    private val maxHandlers = 1024
    val handlers: Array<Triple<Int, Consumer<PeerImpl>, Consumer<Channel>>?> = arrayOfNulls(maxHandlers)

    // ping and get channel
    private fun pingUdp(host: String, port: Int, handle: Consumer<PeerImpl>, chHandle: Consumer<Channel>) {
        CompletableFuture.runAsync {
            if (cache.asMap().containsKey("${host}:${port}")) {
                return@runAsync
            }
            cache.asMap()["${host}:${port}"] = 1

            val a = InetAddress.getByName(host)
            val nonce = n.incrementAndGet()
            handlers[nonce % maxHandlers] = Triple(nonce, handle, chHandle)
            val b = UdpPacket(0, nonce, self.encodeURI(), HexBytes.empty())
            val buf = Rlp.encode(b)
            val p = DatagramPacket(buf, buf.size, a, port)
            log.debug("send packet {} to {} {}", b, p.address, p.port)
            socket.send(p)
        }
    }

    // debounce connection
    private val cache: Cache<String, Int> = CacheBuilder.newBuilder().expireAfterWrite(100, TimeUnit.MILLISECONDS).build<String, Int>()

    private fun handleUdp() {
        while (udpListening) {
            try {
                val req = DatagramPacket(buf, buf.size)
                socket.receive(req)

                val address: InetAddress = req.address
                val port: Int = req.port

                System.arraycopy(buf, 0, tailBuf, tailBuf.size - req.length, req.length)
                val p = Rlp.decode(tailBuf, tailBuf.size - req.length, UdpPacket::class.java)
                log.debug("receive {} from {} {}", p, req.address, req.port)

                if (p.code == 0) {
                    val pong = UdpPacket(1, p.n, self.encodeURI(), HexBytes.empty())
                    val e = Rlp.encode(pong)
                    val resp = DatagramPacket(e, e.size, address, port)
                    log.debug("send {} to {} {}", pong, address, port)
                    socket.send(resp)
                    continue
                }

                if (p.code == 1) {
                    val peer = PeerImpl.parse(p.uri) ?: continue
                    lastUdpCache.asMap()[peer.id] = Pair(req.address, req.port)

                    val h = handlers[p.n % maxHandlers] ?: continue
                    if (h.first != p.n) continue

                    handlers[p.n % maxHandlers] = null

                    if (peer == self) continue
                    val ch = peersCache
                            .getChannel(peer.id)

                    if (ch != null) {
                        h.second.accept(peer)
                        continue
                    }


                    val ft = CompletableFuture.supplyAsync {
                        netLayer.createChannel(peer.host, peer.port, this, listener)
                    }

                    ft.thenAccept {
                        if (it != null) {
                            h.second.accept(peer)
                            h.third.accept(it)
                        }
                    }
                }

                if (p.code == 2) {
                    val msg = Message.parseFrom(p.data.bytes)
                    val peer = PeerImpl.parse(msg.remotePeer) ?: continue
                    lastUdpCache.asMap()[peer.id] = Pair(req.address, req.port)
                    val ch = peersCache.getChannel(peer.id) ?: continue
                    CompletableFuture.runAsync {
                        (ch as ProtoChannel).message(msg, true)
                    }
                }
                continue
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        ex.submit {
            this.handleUdp()
        }
    }

    override fun close() {
        udpListening = false
        socket.close()
        ex.shutdown()
    }

    override val lastUdp: Map<HexBytes, Pair<InetAddress, Int>>
        get() = lastUdpCache.asMap()
}