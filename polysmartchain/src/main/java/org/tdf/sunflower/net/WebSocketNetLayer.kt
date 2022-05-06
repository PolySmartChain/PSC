package org.tdf.sunflower.net

import com.google.protobuf.InvalidProtocolBufferException
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.LoggerFactory
import org.tdf.sunflower.ApplicationConstants
import org.tdf.sunflower.proto.Message
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

private class WSClient(host: String, port: Int, builder: MessageBuilder) : WebSocketClient(
    URI("ws", "", host, port, "", "", "")
) {
    val channel: ProtoChannel = ProtoChannel(builder, WebSocketClientChannelOut(this))
    override fun onOpen(ignored: ServerHandshake) {}
    override fun onMessage(bytes: ByteBuffer) {
        try {
            channel.message(Message.parseFrom(bytes))
        } catch (e: InvalidProtocolBufferException) {
            channel.error(e)
        }
    }

    override fun onMessage(message: String) {}
    override fun onClose(code: Int, reason: String, remote: Boolean) {
        channel.close("websocket connection closed by remote")
    }

    override fun onError(ex: Exception) {
        channel.error(ex)
    }
}


internal class WebSocketChannelOut(private val conn: WebSocket) : ChannelOut {
    override val direction: Int
        get() = 1

    override fun write(message: Message) {
        conn.send(message.toByteArray())
    }

    override fun close() {
        try {
            conn.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

internal class WebSocketClientChannelOut(private val client: WebSocketClient) : ChannelOut {
    override val direction: Int
        get() = 0

    override fun write(message: Message) {
        client.send(message.toByteArray())
    }

    override fun close() {
        client.close()
    }
}

// net layer implemented by websocket
class WebSocketNetLayer internal constructor(port: Int, private val builder: MessageBuilder) :
    WebSocketServer(InetSocketAddress(port)), NetLayer {
    private val channels: MutableMap<WebSocket, Channel> = ConcurrentHashMap()
    override var handler: Consumer<Channel> = Consumer { }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val ch = ProtoChannel(builder, WebSocketChannelOut(conn))
        handler.accept(ch)
        channels[conn] = ch
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val ch = channels[conn] ?: return
        ch.close("websocket connection closed by remote")
        channels.remove(conn)
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
        val ch = channels[conn] ?: return
        try {
            val msg = Message.parseFrom(message)
            ch.message(msg)
        } catch (e: InvalidProtocolBufferException) {
            onError(conn, e)
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {}
    override fun onError(conn: WebSocket, ex: Exception) {
        val ch = channels[conn] ?: return
        ch.error(ex)
    }

    override fun onStart() {
        log.info("websocket server is served on port {}", port)
    }

    override fun createChannel(host: String, port: Int, vararg listeners: ChannelListener): Channel? {
        return try {
            val client = WSClient(host, port, builder)
            client.channel.addListeners(*listeners)
            if (client.connectBlocking(5, TimeUnit.SECONDS)) {
                client.channel
            } else null
        } catch (e: Exception) {
            log.info("failed to connect to {} {}", host, port)
            if(log.isDebugEnabled) {
                log.error("DEBUG", e)
            }
            null
        }
    }

    override fun close() {
        try {
            stop(ApplicationConstants.MAX_SHUTDOWN_WAITING.toInt() * 1000)
            log.info("websocket server closed normally")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


    companion object {
        val log = LoggerFactory.getLogger("net")
    }
}