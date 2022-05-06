package org.tdf.sunflower.net

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import org.tdf.common.util.*
import org.tdf.sunflower.facade.ConsensusEngine
import org.tdf.sunflower.proto.Code
import org.tdf.sunflower.proto.Message
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

private class MultiParts(val total: Int, val writeAt: Long) {
    val multiParts: Array<Message?> = arrayOfNulls(total)

    fun size(): Int {
        return multiParts.filterNotNull().count()
    }

    fun merge(): Message {
        val byteArraySize = multiParts.sumOf { it?.body?.size() ?: 0 }
        val total = ByteArray(byteArraySize)
        var current = 0
        for (part in multiParts) {
            val p = part!!.body.toByteArray()
            System.arraycopy(p, 0, total, current, p.size)
            current += p.size
        }
        if (total.sha3().hex() != multiParts[0]!!.signature.toByteArray().hex()) {
            throw RuntimeException("merge failed")
        }
        return Message.parseFrom(total)
    }
}

/**
 * message filter
 */
class MessageFilter internal constructor(private val config: PeerServerConfig, consensusEngine: ConsensusEngine) :
    Plugin {
    private val scheduler = FixedDelayScheduler("msg-filter", config.cacheExpiredAfter.toLong())

    private val consensusEngine: ConsensusEngine
    private val cache: Cache<HexBytes, Boolean> = CacheBuilder.newBuilder()
        .maximumSize((config.maxPeers * 8).toLong()).build()
    private val multiPartCache: MutableMap<HexBytes, MultiParts> = HashMap()
    private val multiPartCacheLock: Lock = LogLock(ReentrantLock(), "p2p-mp")
    override fun onMessage(context: ContextImpl, server: PeerServerImpl) {
        // cache multi part message

        if (context.msg.code == Code.MULTI_PART) {
            multiPartCacheLock.lock()
            val now = System.currentTimeMillis() / 1000
            val key = context.msg.signature.toByteArray().hex()
            try {
                val messages = multiPartCache.getOrDefault(
                    key,
                    MultiParts(
                        context.msg.ttl.toInt(),
                        now
                    )
                )
                messages.multiParts[context.msg.nonce.toInt()] = context.msg
                multiPartCache[key] = messages
                if (messages.size() == messages.total) {
                    multiPartCache.remove(key)
                    server.onMessage(messages.merge(), context.channel)
                }
            } finally {
                multiPartCacheLock.unlock()
            }
            return
        }

        // filter invalid signatures

        // reject blocked peer
        if (config.blocks.contains(context.remote.id) || server.client.peersCache.hasBlocked(context.remote)) {
            log.error("the peer " + context.remote + " has been blocked")
            context.disconnect()
            return
        }
        // filter message from your self
        if (context.remote == server.self) {
            log.error("message received from yourself")
            context.exit()
            return
        }
        // filter message which ttl < 0
        if (context.msg.ttl < 0) {
            log.error("receive message ttl less than 0")
            context.exit()
            return
        }

        val hash = Util.getRawForSign(context.msg).sha3().hex()
        // filter message had been received
        if (cache.asMap().containsKey(hash)) {
            context.exit()
        }
        cache.put(hash, true)
    }

    override fun onStart(server: PeerServerImpl) {}
    override fun onNewPeer(peer: PeerImpl, server: PeerServerImpl) {}
    override fun onDisconnect(peer: PeerImpl, server: PeerServerImpl) {}
    override fun onStop(server: PeerServerImpl) {}


    companion object {
        private val log = LoggerFactory.getLogger("net")
    }

    init {
        scheduler.delay {
            multiPartCacheLock.lock()
            val now = System.currentTimeMillis() / 1000
            try {
                multiPartCache.entries.removeIf { (_, value) -> now - value.writeAt > config.cacheExpiredAfter }
            } finally {
                multiPartCacheLock.unlock()
            }
        }
        this.consensusEngine = consensusEngine
    }
}