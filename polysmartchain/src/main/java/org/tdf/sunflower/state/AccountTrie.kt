package org.tdf.sunflower.state

import org.slf4j.LoggerFactory
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.NoDeleteStore
import org.tdf.common.store.Store
import org.tdf.common.trie.SecureTrie
import org.tdf.common.trie.Trie
import org.tdf.common.trie.TrieImpl
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.Start
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.vm.*
import org.tdf.sunflower.vm.hosts.Limit


class AccountTrie(
    override val db: Store<ByteArray, ByteArray>,
    private val contractCodeStore: Store<HexBytes, HexBytes>,
    val contractStorageTrie: Trie<HexBytes, HexBytes>,
    secure: Boolean
) : AbstractStateTrie<HexBytes, Account>() {
    override val trie: Trie<HexBytes, Account>

    lateinit var bios: Map<HexBytes, Builtin>
    lateinit var builtins: Map<HexBytes, Builtin>

    override val trieStore: Store<ByteArray, ByteArray>

    override fun createBackend(
        parent: Header,
        staticCall: Boolean,
        root: HexBytes,
    ): Backend {
        return BackendImpl(
            parent = parent,
            trie = trie.revert(root),
            contractStorageTrie = contractStorageTrie,
            builtins = builtins,
            bios = bios,
            staticCall = staticCall,
            codeStore = contractCodeStore,
        )
    }

    init {
        trieStore = NoDeleteStore(db) { it == null || it.isEmpty() }
        var trie: Trie<HexBytes, Account> = TrieImpl(
            trieStore,
            Codecs.rlp(HexBytes::class.java),
            Codecs.rlp(Account::class.java),
        )
        if (secure)
            trie = SecureTrie(trie)
        this.trie = trie
    }

    override fun init(
        alloc: Map<HexBytes, Account>,
        bios: List<Builtin>,
        builtins: List<Builtin>,
        consensusCode: Map<HexBytes, HexBytes>,
        rd: RepositoryReader
    ): HexBytes {
        this.builtins = builtins.associateBy { it.address }
        this.bios = bios.associateBy { it.address }

        val genesisStates: MutableMap<HexBytes, Account> = alloc.toMutableMap()

        for (c in (bios + builtins)) {
            if(c.pure) continue
            val address = c.address

            val trie = contractStorageTrie.revert()
            for ((key, value) in c.genesisStorage) {
                trie[key] = value
            }
            val root = trie.commit()
            trie.flush()
            val a = Account(storageRoot = root)
            genesisStates[address] = a
        }


        // sync to genesis
        val tmp = trie.revert()
        genesisStates.forEach { tmp[it.key] = it.value }
        tmp.commit()

        val backend = BackendImpl(
            parent = null,
            trie = tmp,
            contractStorageTrie = contractStorageTrie,
            builtins = this.builtins,
            bios = this.bios,
            staticCall = false,
            codeStore = contractCodeStore,
        )

        // create consensus code
        consensusCode.forEach { (t, u) ->
            log.info("deploy consensus code at address {}", t)
            val cd = CallData(to = t, callType = CallType.CREATE, data = u)
            val ex = VMExecutor(rd, backend, CallContext(), cd, Limit(VMExecutor.GAS_UNLIMITED), mutableListOf())
            ex.execute()
        }

        log.info("genesis states = {}", Start.MAPPER.writeValueAsString(genesisStates))
        val r = backend.merge()
        log.info("genesis state root = $r")
        return r
    }


    companion object {
        private val log = LoggerFactory.getLogger("trie")
    }
}