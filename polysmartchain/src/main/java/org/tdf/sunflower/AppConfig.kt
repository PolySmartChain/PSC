package org.tdf.sunflower

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.core.env.Environment
import org.tdf.common.crypto.ECKey
import org.tdf.common.types.Constants
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.types.PropertyReader
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil

class AppConfig(private val properties: PropertyLike) {
    constructor(env: Environment) : this(EnvWrapper(env))

    private val reader = PropertyReader(properties)

    val myKey = ECKey()

    val eip8: Boolean
        get() = true

    val peerConnectionTimeout: Int
        get() = 2000
    val peerChannelReadTimeout: Long
        get() = 30L
    val nodeId: ByteArray
        get() = myKey.nodeId
    val maxActivePeers: Int
        get() = 16

    fun peerCapabilities(): List<String> {
        return listOf("eth")
    }

    val defaultP2PVersion: Int
        get() = 5
    val listenPort: Int
        get() = 8545
    val rlpxMaxFrameSize: Int
        get() = 32768
    val p2pPingInterval: Int
        get() = 5

    data class EnvWrapper(val env: Environment) : PropertyLike {
        override fun getProperty(key: String): String? {
            return env.getProperty(key)
        }
    }

    val p2pTransactionCacheSize: Int = reader.getAsInt("sunflower.cache.p2p.transaction")
    val p2pProposalCacheSize: Int = reader.getAsInt("sunflower.cache.p2p.proposal")

    val genesisJson: JsonNode by lazy {
        val objectMapper = MapperUtil.OBJECT_MAPPER
            .enable(JsonParser.Feature.ALLOW_COMMENTS)

        val genesis = properties.getProperty("sunflower.consensus.genesis")

        if (genesis.isNullOrBlank())
            throw RuntimeException("genesis not set")

        val `in` = FileUtils.getInputStream(
            genesis
        )

        return@lazy objectMapper.readValue(`in`, JsonNode::class.java)
    }

    val replace: Map<HexBytes, HexBytes> by lazy {
        val n = genesisJson
        val alloc = n["replace"] ?: return@lazy emptyMap()
        val r: MutableMap<HexBytes, HexBytes> = mutableMapOf()
        val it = alloc.fieldNames()
        while (it.hasNext()) {
            val k = it.next()
            val v = alloc[k].asText()
            r[k.hex()] = v.hex()
        }
        return@lazy r
    }

    val vmGasPrice: Uint256 = reader.getAsU256("sunflower.vm.gas-price", Uint256.ZERO)
    val vmLogs: String = (reader.properties.getProperty("sunflower.vm.logs") ?: "").trim()
    val isTrieSecure: Boolean = reader.getAsBool("sunflower.trie.secure")
    val chainId: Int get() = genesisJson["chainId"]?.asInt() ?: Constants.DEFAULT_CHAIN_ID
    val rpcTimeOut: Int get() = reader.getAsInt("sunflower.rpc.timeout", Int.MAX_VALUE)

    companion object {
        fun get(): AppConfig {
            return INSTANCE
        }

        lateinit var INSTANCE: AppConfig
    }
}