package org.tdf.sunflower.state

import org.tdf.common.util.*
import org.tdf.sunflower.consensus.Proposer
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.AddrUtil.empty
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.types.StorageWrapper
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.abi.Abi
import java.math.BigInteger
import java.util.*

/**
 * used for node join/exit
 */
class Authentication(
    private val nodes: Collection<HexBytes>,
    address: HexBytes,
    private val accounts: StateTrie<HexBytes, Account>,
    private val config: ConsensusConfig
) : AbstractBuiltin(address) {

    override val pure: Boolean
        get() = false
    override val abi: Abi
        get() = Authentication.abi

    override fun call(
        rd: RepositoryReader,
        backend: Backend,
        ctx: CallContext,
        callData: CallData,
        method: String,
        vararg args: Any
    ): List<*> {
        val wrapper = StorageWrapper(backend.getAsStore(address))
        val nodes = wrapper.getList(NODES_KEY, HexBytes::class.java, mutableListOf())!!
        val pending = StorageWrapper(PENDING_NODES_KEY, backend.getAsStore(address))
        return when (method) {
            "approved" -> listOf(nodes.map { it.bytes })
            "pending" -> {
                val dstBytes = args[0] as ByteArray
                val dst = HexBytes.fromBytes(dstBytes)
                val r = pending.getSet(dst, HexBytes::class.java, TreeSet())!!
                listOf(r.map { it.bytes })
            }
            "join" -> {
                val fromAddr = callData.caller
                if (nodes.contains(fromAddr)) throw RuntimeException("authentication contract error: $fromAddr has already in nodes")
                val s = pending.getSet(fromAddr, HexBytes::class.java, null)
                if (s != null) {
                    throw RuntimeException("authentication contract error: $fromAddr has already in pending")
                }
                pending.save(fromAddr, TreeSet<Any>())
                emptyList<Any>()
            }
            "approve" -> {
                val toApprove = (args[0] as ByteArray).hex()
                if (!nodes.contains(callData.caller)) {
                    throw RuntimeException("authentication contract error: cannot approve " + callData.caller + " is not in nodes list")
                }
                val approves = pending.getSet(toApprove, HexBytes::class.java, null)
                    ?: throw RuntimeException("authentication contract error: cannot approve $toApprove not in pending")
                if (approves.contains(callData.caller)) {
                    throw RuntimeException("authentication contract error: cannot approve $toApprove has approved")
                }
                approves.add(callData.caller)
                if (approves.size >= divideAndCeil(nodes.size * 2, 3)) {
                    pending.remove(toApprove)
                    nodes.add(toApprove)
                } else {
                    pending.save(toApprove, approves)
                }
                wrapper.save(NODES_KEY, nodes)
                emptyList<Any>()
            }
            "exit" -> {
                val fromAddr = callData.caller
                if (!nodes.contains(fromAddr)) throw RuntimeException("authentication contract error: $fromAddr not in nodes")
                if (nodes.size <= 1) throw RuntimeException("authentication contract error: cannot exit, at least one miner")
                nodes.remove(fromAddr)
                wrapper.save(NODES_KEY, nodes)
                emptyList<Any>()
            }
            "getProposer" -> {
                val parent = rd.getHeaderByHash(backend.parentHash) ?: throw RuntimeException("block not found")
                val x = args[0]
                when (x) {
                    is Array<*> -> {
                        x.forEach { println(it) }
                    }
                }
                val o =
                    getProposerInternal(
                        parent, (args[0] as BigInteger).toLong(),
                        nodes, config.blockInterval.toLong()
                    )
                        ?: emptyProposer
                listOf(
                    o.address.bytes,
                    o.startTimeStamp.bn(),
                    o.endTimeStamp.bn()
                )
            }
            else -> throw RuntimeException("method not found")
        }
    }

    fun getProposer(rd: RepositoryReader, parentHash: HexBytes, now: Long): Proposer {
        val parent = rd.getHeaderByHash(parentHash) ?: throw RuntimeException("header $parentHash not found")
        val li = view(rd, accounts.createBackend(parent, staticCall = true), "getProposer", BigInteger.valueOf(now))
        val address = li[0] as ByteArray
        val start = li[1] as BigInteger
        val end = li[2] as BigInteger
        return Proposer(
            HexBytes.fromBytes(address),
            start.toLong(),
            end.toLong()
        )
    }


    override val genesisStorage: Map<HexBytes, HexBytes>
        get() {
            val ret = HashMap<HexBytes, HexBytes>()
            ret[NODES_KEY] = nodes.rlp().hex()
            return ret
        }

    companion object {

        val emptyProposer: Proposer = Proposer(empty(), 0, 0)

        private fun getProposerInternal(
            parent: Header,
            currentEpochSeconds: Long,
            minerAddresses: List<HexBytes>,
            blockInterval: Long
        ): Proposer? {
            if (currentEpochSeconds - parent.createdAt < blockInterval) {
                return null
            }
            if (parent.height == 0L) {
                return Proposer(minerAddresses[0], 0, Long.MAX_VALUE)
            }
            val prev = parent.coinbase
            var prevIndex = minerAddresses.indexOf(prev)
            if (prevIndex < 0) prevIndex += minerAddresses.size
            val step = ((currentEpochSeconds - parent.createdAt)
                    / blockInterval)
            val currentIndex = ((prevIndex + step) % minerAddresses.size).toInt()
            val startTime = parent.createdAt + step * blockInterval
            val endTime = startTime + blockInterval
            return Proposer(
                minerAddresses[currentIndex],
                startTime,
                endTime
            )
        }

        private const val ABI_JSON =
            """
            [{"inputs":[{"internalType":"address","name":"dst","type":"address"}],"name":"approve","outputs":[],"stateMutability":"nonpayable","type":"function"},{"inputs":[],"name":"approved","outputs":[{"internalType":"address[]","name":"","type":"address[]"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"exit","outputs":[],"stateMutability":"nonpayable","type":"function"},{"inputs":[{"internalType":"uint256","name":"timestamp","type":"uint256"}],"name":"getProposer","outputs":[{"internalType":"address","name":"","type":"address"},{"internalType":"uint256","name":"start","type":"uint256"},{"internalType":"uint256","name":"end","type":"uint256"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"join","outputs":[],"stateMutability":"nonpayable","type":"function"},{"inputs":[{"internalType":"address","name":"dst","type":"address"}],"name":"pending","outputs":[{"internalType":"address[]","name":"","type":"address[]"}],"stateMutability":"view","type":"function"}]                
            """

        val abi: Abi = Abi.fromJson(ABI_JSON)

        val NODES_KEY = "nodes".ascii().hex()
        val PENDING_NODES_KEY = "pending".ascii().hex()

        fun divideAndCeil(a: Int, b: Int): Int {
            val ret = a / b
            return if (a % b != 0) ret + 1 else ret
        }
    }
}