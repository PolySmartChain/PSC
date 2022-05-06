package org.tdf.sunflower.consensus.pos

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.cache.CacheBuilder
import lombok.extern.slf4j.Slf4j
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.tdf.common.types.Uint256
import org.tdf.common.util.*
import org.tdf.sunflower.facade.AbstractConsensusEngine
import org.tdf.sunflower.facade.PropertyLike
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.*
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil
import org.tdf.sunflower.vm.abi.Abi
import java.math.BigInteger

class PosConfig(val prop: PropertyLike) : ConsensusConfig(prop) {
    val code = prop.getProperty("code")!!
    val abi = prop.getProperty("abi")!!
}

@Slf4j(topic = "pos")
class PoS : AbstractConsensusEngine() {
    private lateinit var config: PosConfig
    private lateinit var posValidator: PoSValidator
    private lateinit var posMiner: PoSMiner
    private lateinit var genesis: Genesis

    var eraSize = 0
    var proposerMinStake = BigInteger.ZERO
    var maxMiners = 0

    lateinit var consensusAbi: Abi
    val consensusAddr = "pos".ascii().sha3().tail20().hex()
    val cache = CacheBuilder.newBuilder().maximumSize(128).build<HexBytes, List<Pair<HexBytes, BigInteger>>>()

    fun getDifficulty(rd: RepositoryReader, parent: Header): Uint256 {
        val w = accountTrie.createWrapper(rd, parent, consensusAbi, consensusAddr)
        val r = w.call(
            "target",
        )
        return (r[0] as BigInteger).u256()
    }

    private fun initMaxMiners(w: ContractWrapper): Int {
        val l = maxMiners
        if (l != 0)
            return l

        val r = w.call(
            "maxMiners",
        )[0] as BigInteger

        maxMiners = r.intValueExact()
        return r.intValueExact()
    }

    fun getProposer(rd: RepositoryReader, parent: Header, now: Long): Triple<HexBytes, Long, Long>? {
        val candidates = getCandidates(rd, parent)
        val w = accountTrie.createWrapper(rd, parent, consensusAbi, consensusAddr)

        val maxMiners = initMaxMiners(w)
        val end = Math.min(candidates.size, maxMiners)

        val r = w.call(
            "getProposer",
            parent.coinbase.bytes,
            parent.createdAt.toBigInteger(),
            candidates.map { it.first.bytes }.subList(0, end),
            now.toBigInteger()
        )
        if ((r[0] as ByteArray).hex() == AddrUtil.empty())
            return null
        var e = r[2] as BigInteger

        if (e > Long.MAX_VALUE.toBigInteger())
            e = Long.MAX_VALUE.toBigInteger()
        return Triple((r[0] as ByteArray).hex(), (r[1] as BigInteger).longValueExact(), e.longValueExact())
    }

    fun getCandidates(rd: RepositoryReader, parent: Header): List<Pair<HexBytes, BigInteger>> {
        if (eraSize == 0) {
            val w = accountTrie.createWrapper(rd, rd.genesis, consensusAbi, consensusAddr)
            val newEraSize = w.call("eraSize")[0] as BigInteger
            this.eraSize = newEraSize.intValueExact()
        }

        if (proposerMinStake == BigInteger.ZERO) {
            val w = accountTrie.createWrapper(rd, rd.genesis, consensusAbi, consensusAddr)
            this.proposerMinStake = w.call("proposerMinStake")[0] as BigInteger
        }

        val origin = parent.height / eraSize * eraSize
        val h = rd.getAncestor(parent.hash, origin)
        val trie = accountTrie.trie.revert(h.stateRoot)
        val a = trie[consensusAddr]!!

        val li = cache.get(a.storageRoot) {
            val w = accountTrie.createWrapper(rd, h, consensusAbi, consensusAddr)
            val n = (w.call("n")[0] as BigInteger).intValueExact()

            val votes = mutableMapOf<HexBytes, BigInteger>()

            var i = 0

            while (true) {
                val r = w.call("votes", i.toBigInteger())
                val to = r[1] as ByteArray
                val amount = r[2] as BigInteger
                val score = r[4] as BigInteger

                if (amount == BigInteger.ZERO)
                    break

                val v = votes[to.hex()] ?: BigInteger.ZERO
                votes[to.hex()] = v + score
                i++
            }

            var li = mutableListOf<Pair<HexBytes, BigInteger>>()
            for (k in 1..n) {
                val addr = w.call("candidates", k.toBigInteger())[0] as ByteArray
                val score = votes[addr.hex()] ?: BigInteger.ZERO
                val stake = w.call("balanceOf", addr)[0] as BigInteger
                if (stake >= proposerMinStake)
                    li.add(Pair(addr.hex(), score))
            }

            li.sortByDescending { it.second }
            log.info("new candidates list created at height {} hash {} list = {}", h.height, h.hash, li)

            if (li.isEmpty()) {
                val l = w.call("defaultMiners")[0] as Array<*>
                li = l.map { Pair((it as ByteArray).hex(), BigInteger.ZERO) }.toMutableList()
            }
            li
        }
        return li
    }


    val _builtins: MutableList<Builtin> = mutableListOf()

    override val builtins: List<Builtin>
        get() = _builtins

    override val alloc: Map<HexBytes, Account>
        get() = genesis.alloc

    override val code: Map<HexBytes, HexBytes> = mutableMapOf()

    override fun init(config: ConsensusConfig) {
        if(config.debug) {
            _builtins.add(LoggingContract())
        }

        this.config = PosConfig(config.properties)
        genesis = Genesis(config.genesisJson)
        genesisBlock = genesis.block
        posMiner = PoSMiner(accountTrie, eventBus, this.config, this)
        posMiner.repo = repo
        posMiner.transactionPool = transactionPool
        miner = posMiner
        posValidator = PoSValidator(accountTrie, this, this.config.chainId)
        validator = posValidator

        val m = code as MutableMap
        val createCode =
            MapperUtil.OBJECT_MAPPER.readValue(FileUtils.getInputStream(this.config.code), JsonNode::class.java)
        val bin = FileUtils.getInputStream(this.config.abi).readAllBytes()
        consensusAbi = Abi.fromJson(String(bin))
        val c = createCode["object"]
        m[consensusAddr] = c.textValue().hex()

    }

    override val name: String
        get() = "pos"

    companion object {
        val log = LoggerFactory.getLogger("pos")
    }
}