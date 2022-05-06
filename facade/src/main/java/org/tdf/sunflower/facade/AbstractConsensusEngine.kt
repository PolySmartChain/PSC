package org.tdf.sunflower.facade

import org.tdf.common.event.EventBus
import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.Builtin
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.types.HeaderImpl
import org.tdf.sunflower.util.FileUtils

abstract class AbstractConsensusEngine : ConsensusEngine {
    // contract storage trie, injected before init() called
    lateinit var contractStorageTrie: Trie<HexBytes, HexBytes>

    // a map between hash and wasm byte code, injected before init() called
    lateinit var contractCodeStore: Store<HexBytes, HexBytes>

    // sub class should set miner explicitly when init() called
    override var miner: Miner = Miner.NONE

    // sub class should set validator explicitly when init() called
    override var validator: Validator = Validator.NONE

    // account stateTrie will be injected before init() called
    lateinit var accountTrie: StateTrie<HexBytes, Account>

    // sub class should set genesis block explicitly when init() called
    override var genesisBlock: Block = Block(HeaderImpl())

    // event bus will be injected before init() called
    lateinit var eventBus: EventBus

    // transaction pool will be injected before init() called
    lateinit var transactionPool: TransactionPool

    // sunflowerRepository will be injected before init() called
    lateinit var repo: RepositoryService

    // sub class should set peer server listener explicitly when init() called
    override var peerServerListener = PeerServerListener.NONE

    open val alloc: Map<HexBytes, Account>
        get() = emptyMap()

    open val builtins: List<Builtin>
        get() = emptyList()
    open val bios: List<Builtin>
        get() = emptyList()

    open val code: Map<HexBytes, HexBytes> get() = emptyMap()

    companion object {
        @JvmField
        val NONE: AbstractConsensusEngine = object : AbstractConsensusEngine() {
            override fun init(config: ConsensusConfig) {
            }

            override val name: String
                get() {
                    return "none"
                }
        }
    }
}