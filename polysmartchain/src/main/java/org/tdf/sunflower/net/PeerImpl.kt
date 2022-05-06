package org.tdf.sunflower.net

import com.fasterxml.jackson.annotation.JsonIgnore
import lombok.SneakyThrows
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.Transaction
import java.net.URI


data class PeerImpl(
    @JsonIgnore private val privateKey: ByteArray? = null,
    override val protocol: String = "",
    override val host: String = "",
    override val port: Int = 0,
    override val id: HexBytes = AddrUtil.empty(),
) : Peer, Comparable<PeerImpl> {
    var score: Long = 0


    override fun toString(): String {
        return String.format("%s://%s@%s:%d", protocol, id, host, port)
    }

    override fun encodeURI(): String {
        return toString()
    }

    fun distance(that: PeerImpl): Int {
        return Util.distance(id.bytes, that.id.bytes)
    }

    fun subTree(thatID: ByteArray): Int {
        return Util.subTree(id.bytes, thatID)
    }

    // subtree is less than PUBLIC_KEY_SIZE * 8
    fun subTree(that: Peer): Int {
        return subTree(that.id.bytes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Peer) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun compareTo(other: PeerImpl): Int {
        return id.compareTo(other.id)
    }

    companion object {
        @JvmStatic
        fun parse(url: String): PeerImpl? {
            return try {
                parseInternal(url)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun URI.protocol(): String {
            return this.scheme ?: throw RuntimeException("missing protocol name $this, e.g. enode://192.168.1.1:7010")
        }

        private fun URI.port(): Int {
            return this.port.takeIf { it > 0 } ?: throw RuntimeException("missing port number $this")
        }

        // parse peer from uri like protocol://id@host:port
        // the id should be an ec public key
        @SneakyThrows
        private fun parseInternal(url: String): PeerImpl {
            val u = URI(url.trim { it <= ' ' })
            val protocol = u.protocol()
            val port = u.port()

            val host = u.host
            if (u.rawUserInfo == null || u.rawUserInfo.isEmpty()) throw RuntimeException("parse peer failed: missing public key")
            val id = u.rawUserInfo.hex()
            if (id.size != Transaction.ADDRESS_LENGTH) {
                throw RuntimeException("peer " + url + " address should be " + Transaction.ADDRESS_LENGTH)
            }
            return PeerImpl(null, protocol, host, port, id)
        }

        // create self as peer from input
        // if private key is missing, generate key automatically
        // create self as peer from input
        // if private key is missing, generate key automatically
        fun createSelf(u: URI, privateKey: ByteArray = ECKey().privKeyBytes): PeerImpl {
            if (u.rawUserInfo != null && u.rawUserInfo.isNotEmpty()) {
                throw RuntimeException(u.userInfo + " should be empty")
            }
            val protocol = u.protocol()
            val port = u.port()
            val host = if (u.host == null || u.host.trim { it <= ' ' } == "") "localhost" else u.host
            val id = AddrUtil.fromPrivate(privateKey)
            return PeerImpl(privateKey, protocol, host, port, id)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val p2 = createSelf(URI("enode://localhost"))
            println(p2)
        }
    }
}