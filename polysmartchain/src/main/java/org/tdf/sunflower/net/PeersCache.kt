package org.tdf.sunflower.net

import org.tdf.common.util.HexBytes
import org.tdf.sunflower.types.Transaction
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// helper to avoid generic array
// kademlia k-bucket
class Bucket : ConcurrentHashMap<HexBytes, PeerChannel>()

// helper for store peer and channel in a single value in Bucket
class PeerChannel(
    val peer: PeerImpl,
    val channel: Channel,
)

// peers cache for peer searching/discovery
class PeersCache(
    private val self: PeerImpl,
    private val config: PeerServerConfig
) {
    private val buckets = arrayOfNulls<Bucket>(Transaction.ADDRESS_LENGTH * 8)
    val bootstraps: MutableMap<PeerImpl, Boolean> = ConcurrentHashMap()
    private val blocked: MutableMap<PeerImpl, Boolean> = ConcurrentHashMap()
    val trusted: MutableMap<PeerImpl, Boolean> = ConcurrentHashMap()

    fun size(): Int {
        return buckets.filterNotNull().sumOf { it.size }
    }

    operator fun contains(peer: PeerImpl): Boolean {
        val idx = self.subTree(peer)
        return buckets[idx]?.containsKey(peer.id) ?: false
    }

    fun keep(peer: PeerImpl, channel: Channel) {
        if (peer == self) {
            return
        }
        if (blocked.containsKey(peer)) return
        val idx = self.subTree(peer)
        if (buckets[idx] == null) {
            buckets[idx] = Bucket()
        }

        // if the peer already had been put
        val o: PeerImpl? = buckets[idx]?.let { it[peer.id]?.peer }

        // increase its score
        if (o != null) {
            o.score = o.score + PEER_SCORE
            return
        }

        peer.score = PEER_SCORE.toLong()
        val newPeerChannel = PeerChannel(peer, channel)
        if (size() < config.maxPeers) {
            buckets[idx]!![peer.id] = newPeerChannel
            return
        }

        // when neighbours is full, check whether some neighbours could be removed
        // 1. the bucket of the new neighbour is empty
        if (buckets[idx]!!.size > 0) {
            channel.close("neighbours is full")
            return
        }

        // 2. exists some bucket which contains more than one peer
        // find the maximum bucket
        val bucket: Bucket? = buckets
            .filterNotNull()
            .maxByOrNull { it.size }

        // if the maximum bucket contains less or equals to one element
        // the buckets is ideal, no need to evict
        if (bucket == null || bucket.size <= 1) {
            channel.close("neighbours is full")
            return
        }

        // the conditions above are both filled
        // evict one and add new peer
        bucket.keys
            .firstOrNull()
            ?.let {
                remove(it, "the new node $peer has more priority than $it")
            }
        buckets[idx]!![peer.id] = newPeerChannel
    }

    // remove the peer and close the channel
    fun remove(peerID: HexBytes, reason: String) {
        val idx = self.subTree(peerID.bytes)
        if (buckets[idx] == null) {
            return
        }
        val peerChannel = buckets[idx]!![peerID]
        buckets[idx]!!.remove(peerID)
        if (peerChannel == null) return
        peerChannel.channel.close(reason)
    }

    // get limit peers randomly
    fun getPeers(limit: Int): List<PeerImpl> {
        val res = peers.toMutableList()
        val rand = Random()
        while (res.size > 0 && res.size > limit) {
            val idx = Math.abs(rand.nextInt()) % res.size
            res.removeAt(idx)
        }
        return res
    }

    val peers: List<PeerImpl>
        get() {
            return buckets.filterNotNull()
                .flatMap { it.values }
                .map { it.peer }
        }

    fun block(peer: PeerImpl) {
        // trusted peer will not be blocked
        if (trusted.containsKey(peer)) return
        // if peer discovery is disabled, bootstrap peer are treat as trusted peer
        if (!config.discovery && bootstraps.containsKey(peer)) return
        // if the peer had been blocked before,
        // reset the score of this peer as EVIL_SCORE
        if (blocked.containsKey(peer)) {
            blocked.keys
                .filter { it == peer }
                .forEach { it.score = EVIL_SCORE.toLong() }
            return
        }
        // remove the peer and disconnect to it
        remove(peer.id, "block the peer $peer")
        peer.score = EVIL_SCORE.toLong()
        blocked[peer] = true
    }

    // decrease score of peer
    fun half(peer: PeerImpl) {
        val idx = self.subTree(peer)
        if (buckets[idx] == null) return

        buckets[idx]!![peer.id]
            ?.peer?.takeIf { p ->
                p.score -= if (p.score < 8) p.score else 8
                p.score /= 2
                p.score == 0L
            }?.let { remove(it.id, " the score of $it is 0") }
    }

    // decrease score of all peer
    fun half() {
        val toRemoves: List<PeerImpl> = buckets
            .filterNotNull()
            .flatMap { it.values }
            .filter { b: PeerChannel ->
                val p = b.peer
                p.score -= if (p.score < 8) p.score else 8
                p.score /= 2
                p.score == 0L || b.channel.closed
            }.map { it.peer }

        toRemoves.forEach { x: PeerImpl -> remove(x.id, " the score of $x is 0") }

        val toRestores = blocked.keys
            .filter { p: PeerImpl ->
                p.score /= 2
                p.score == 0L
            }

        toRestores.forEach { p: PeerImpl -> blocked.remove(p) }
    }

    val isFull: Boolean
        get() = size() >= config.maxPeers

    /**
     * get all connected channels
     */
    val channels: List<Channel>
        get() = buckets
            .filterNotNull()
            .flatMap { it.values }
            .map { it.channel }
            .filter { it.isAlive }

    // get channel of the peer
    fun getChannel(peer: PeerImpl): Channel? {
        return getChannel(peer.id)
    }

    // get channel by peer id
    fun getChannel(id: HexBytes): Channel? {
        val idx = self.subTree(id.bytes)
        return buckets[idx]?.get(id)?.channel?.takeIf { it.isAlive }
    }

    fun hasBlocked(peer: PeerImpl): Boolean {
        return blocked.containsKey(peer)
    }

    companion object {
        private const val PEER_SCORE = 32
        private const val EVIL_SCORE = Int.MIN_VALUE
    }
}