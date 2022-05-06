package org.tdf.sunflower.net

import com.github.salpadding.rlpstream.Rlp
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.net.PeerImpl.Companion.parse
import org.tdf.sunflower.proto.Message
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

interface ChannelOut {
    fun write(message: Message)
    fun close()
    val direction: Int
}

interface UdpCtx {
    val lastUdp: Map<HexBytes, Pair<InetAddress, Int>>
    val socket: DatagramSocket
}

class GrpcChannel(messageBuilder: MessageBuilder, out: ChannelOut) : ProtoChannel(messageBuilder, out),
        StreamObserver<Message> {
    override fun onNext(value: Message) {
        message(value)
    }

    override fun onError(t: Throwable) {
        error(t)
    }

    override fun onCompleted() {
        close("closed by remote")
    }
}

// communicating channel with peer
open class ProtoChannel(private val messageBuilder: MessageBuilder, private val out: ChannelOut) : Channel {
    @Volatile
    override var closed = false
        protected set

    override var remote: PeerImpl? = null

    @Volatile
    private var pinged = false
    override var listeners: MutableList<ChannelListener> = CopyOnWriteArrayList()

    private var lastActiveTcp = 0L

    override fun message(message: Message, udp: Boolean) {
        if (!udp) lastActiveTcp = System.currentTimeMillis()
        if (closed) return
        handlePing(message)

        listeners.forEach {
            if (closed) return
            it.onMessage(message, this, udp)
        }
    }

    private fun handlePing(message: Message) {
        if (pinged) return
        remote = parse(message.remotePeer)
        if (remote == null) {
            close("invalid peer " + message.remotePeer)
            return
        }

        pinged = true
        listeners.forEach {
            if (closed) return
            it.onConnect(remote!!, this)
        }
    }

    override fun error(throwable: Throwable) {
        if (closed) return
        for (listener in listeners) {
            if (closed) return
            listener.onError(throwable, this)
        }
    }

    override fun close(reason: String) {
        if (closed) return
        closed = true
        if (reason.isNotEmpty()) {
            out.write(messageBuilder.buildDisconnect(reason))
            log.error("close channel to $remote reason is $reason")
        }
        listeners.forEach { it.onClose(this) }
        listeners.clear()

        try {
            out.close()
        } catch (ignore: Exception) {
        }
    }

    override fun write(message: Message, ctx: UdpCtx?) {
        val now = System.currentTimeMillis()

        val p = remote
        // try to keep tcp active
        if (ctx == null || p == null || now - lastActiveTcp > TCP_DELAY || message.serializedSize > UDP_SIZE) {
            writeTCP(message)
            return
        }

        if (direction == 0) {
            writeUDP(ctx.socket, InetAddress.getByName(p.host), p.port, message)
            return
        }

        val net = ctx.lastUdp[p.id]
        if (net == null) {
            writeTCP(message)
            return
        }

        writeUDP(ctx.socket, net.first, net.second, message)
    }

    private fun writeUDP(socket: DatagramSocket, address: InetAddress, port: Int, message: Message) {
        log.debug("write udp message to peer {} , code = {} ch = {} inet = {} port = {} data = {} ", message.remotePeer, message.code, remote, address, port, message)
        val p = UdpPacket(2, 0, "", HexBytes.fromBytes(message.toByteArray()))
        val bin = Rlp.encode(p)
        val packet = DatagramPacket(bin, bin.size, address, port)
        socket.send(packet)
    }

    private fun writeTCP(message: Message) {
        lastActiveTcp = System.currentTimeMillis()
        if (closed) {
            log.error("the channel is closed")
            return
        }
        try {
            log.debug("write tcp message to peer {} , code = {}", message.remotePeer, message.code)
            out.write(message)
        } catch (e: Throwable) {
            e.printStackTrace()
            log.error(e.message)
            error(e)
        }
    }

    override fun addListeners(vararg listeners: ChannelListener) {
        this.listeners.addAll(listeners)
    }

    override val direction: Int
        get() = out.direction


    companion object {
        private val log = LoggerFactory.getLogger("net")
        const val TCP_DELAY = 15000
        const val UDP_DELAY = 15000
        const val UDP_SIZE = 256
    }
}