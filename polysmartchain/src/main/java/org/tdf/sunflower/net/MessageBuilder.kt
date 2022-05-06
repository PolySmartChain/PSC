package org.tdf.sunflower.net

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.sha3
import org.tdf.sunflower.proto.*
import org.tdf.sunflower.proto.Nothing
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

class MessageBuilder(val self: PeerImpl, private val config: PeerServerConfig) {
    private val nonce = AtomicLong()
    fun buildNothing(): Message {
        return buildMessage(Code.NOTHING, 1, Nothing.newBuilder().build().toByteArray())
    }

    fun buildPing(): Message {
        return buildMessage(Code.PING, 1, Ping.newBuilder().build().toByteArray())
    }

    fun buildPong(): Message {
        return buildMessage(Code.PONG, 1, Pong.newBuilder().build().toByteArray())
    }

    fun buildLookup(): Message {
        return buildMessage(Code.LOOK_UP, 1, Lookup.newBuilder().build().toByteArray())
    }

    fun buildDisconnect(reason: String): Message {
        return buildMessage(Code.DISCONNECT, 1, Disconnect.newBuilder().setReason(reason).build().toByteArray())
    }

    fun buildPeers(peers: Collection<Peer>): Message {
        return buildMessage(
            Code.PEERS, 1, Peers
                .newBuilder().addAllPeers(
                    peers.map { it.encodeURI() }
                )
                .build().toByteArray()
        )
    }

    fun buildAnother(body: ByteArray, ttl: Long, remote: Peer): List<Message> {
        val buildResult = buildMessage(Code.ANOTHER, ttl, body)

        // if size of packet < 2m
        if (buildResult.serializedSize <= config.maxPacketSize) {
            return listOf(buildResult)
        }

        // set ttl = total size of build result
        // set nonce = sequence number
        val serialized = buildResult.toByteArray()

        var remained = serialized.size
        var current = 0
        val multiParts: MutableList<Message> = mutableListOf()
        val builder = Message.newBuilder()
        val hash = serialized.sha3()
        var i = 0
        val total = serialized.size / config.maxPacketSize +
                if (serialized.size % config.maxPacketSize == 0) 0 else 1

        val packSize = 1024 * 1024
        while (remained > 0) {
            val size = min(remained, packSize)
            val bodyBytes = ByteArray(size)
            System.arraycopy(serialized, current, bodyBytes, 0, size)
            builder.body = ByteString.copyFrom(bodyBytes)
            builder.nonce = i.toLong()
            builder.ttl = total.toLong()
            builder.code = Code.MULTI_PART
            builder.signature = ByteString.copyFrom(hash)
            multiParts.add(builder.build())
            builder.clear()
            i++
            current += size
            remained -= size
        }
        return multiParts
    }

    fun buildRelay(message: Message): Message {
        val builder = Message.newBuilder().mergeFrom(message)
            .setCreatedAt(
                Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000).build()
            )
            .setRemotePeer(self.encodeURI())
            .setNonce(nonce.incrementAndGet())
            .setTtl(message.ttl - 1)
        val sig = ByteUtil.EMPTY_BYTE_ARRAY
        return builder.setSignature(ByteString.copyFrom(sig)).build()
    }

    fun buildMessage(code: Code, ttl: Long, msg: ByteArray): Message {
        val builder = Message.newBuilder()
            .setCode(code)
            .setCreatedAt(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
            .setRemotePeer(self.encodeURI())
            .setTtl(ttl)
            .setNonce(nonce.incrementAndGet())
            .setBody(ByteString.copyFrom(msg))

        return builder.setSignature(ByteString.copyFrom(ByteUtil.EMPTY_BYTE_ARRAY)).build()
    }
}