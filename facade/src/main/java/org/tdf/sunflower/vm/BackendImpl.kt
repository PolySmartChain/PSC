package org.tdf.sunflower.vm

import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.types.Uint256
import org.tdf.common.util.Address
import org.tdf.common.util.HashUtil
import org.tdf.common.util.HexBytes
import org.tdf.common.util.sha3
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.state.Builtin
import org.tdf.sunflower.types.Header

// backend for mining
class BackendImpl(
    private val parent: Header?,
    override val parentBackend: BackendImpl? = null,
    private val trie: Trie<HexBytes, Account>,
    private val contractStorageTrie: Trie<HexBytes, HexBytes>,
    private val modifiedAccounts: MutableMap<HexBytes, Account> = mutableMapOf(),
    private val modifiedStorage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>> = mutableMapOf(),
    override val builtins: Map<HexBytes, Builtin> = mutableMapOf(),
    override val bios: Map<HexBytes, Builtin> = mutableMapOf(),
    override val staticCall: Boolean,
    // address -> code
    private val codeStore: Store<HexBytes, HexBytes>,

    // code hash -> code
    private val codeCache: MutableMap<HexBytes, HexBytes> = mutableMapOf(),
    override val height: Long = (parent?.height ?: -1) + 1,
    private val consensusCode: Map<HexBytes, HexBytes> = emptyMap(),
    override var rpcCall: Boolean = false
) : Backend {
    // get account without clone
    private fun lookup(address: HexBytes): Account {
        val a = modifiedAccounts[address]
        if (a != null)
            return a
        if (parentBackend != null) return parentBackend.lookup(address)
        val aInTrie = trie[address]
        return aInTrie ?: Account.empty()
    }

    override fun createChild(staticCall: Boolean): BackendImpl {
        return BackendImpl(
            parent,
            this,
            trie,
            contractStorageTrie,
            mutableMapOf(),
            mutableMapOf(),
            builtins,
            bios,
            staticCall,
            codeStore,
            codeCache,
        )
    }

    private fun mergeInternal(
        accounts: MutableMap<HexBytes, Account>,
        storage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>>,
    ) {
        parentBackend?.mergeInternal(accounts, storage)
        modifiedAccounts.forEach { accounts[it.key] = it.value }
        for ((key, value) in modifiedStorage) {
            storage.putIfAbsent(key, mutableMapOf())
            storage[key]!!.putAll(value)
        }
    }

    override val trieRoot: HexBytes
        get() = trie.rootHash
    override val root: Backend
        get() = parentBackend?.root ?: this

    override fun merge(): HexBytes {
        val accounts: MutableMap<HexBytes, Account> = mutableMapOf()
        val storage: MutableMap<HexBytes, MutableMap<HexBytes, HexBytes>> = mutableMapOf()
        mergeInternal(accounts, storage)

        val modified: MutableSet<HexBytes> = mutableSetOf()
        modified.addAll(accounts.keys)
        modified.addAll(storage.keys)

        val tmpTrie = trie.revert(trie.rootHash)
        for (addr in modified) {
            // some account has not touched, but storage or code modified
            var a = accounts[addr] ?: lookup(addr)
            val s = contractStorageTrie.revert(a.storageRoot)
            val map: Map<HexBytes, HexBytes> = storage.getOrDefault(addr, emptyMap())
            for ((key, value) in map) {
                if (value.size == 0) {
                    s.remove(key)
                } else {
                    s[key] = value
                }
            }
            a = a.copy(storageRoot = s.commit())

            if (!a.isEmpty)
                tmpTrie[addr] = a
            else
                tmpTrie.remove(addr)

            codeCache[a.contractHash]?.let { codeStore[a.contractHash] = it }
        }
        return tmpTrie.commit()
    }

    override fun close() {

    }

    override fun getContractHash(address: HexBytes): HexBytes {
        return lookup(address).contractHash
    }

    override val parentHash: HexBytes
        get() = parent!!.hash

    override fun getBalance(address: HexBytes): Uint256 {
        return lookup(address).balance
    }

    override fun setBalance(address: HexBytes, balance: Uint256) {
        if (getBalance(address) == balance)
            return
        if (staticCall)
            throw RuntimeException("cannot set balance in static call")
        val a = lookup(address).copy(balance = balance)
        modifiedAccounts[address] = a
    }

    override fun getNonce(address: HexBytes): Long {
        return lookup(address).nonce
    }

    override fun setNonce(address: HexBytes, nonce: Long) {
        if (staticCall)
            throw RuntimeException("cannot set balance in static call")
        if(address == AddrUtil.empty())
            return
        val a = lookup(address).copy(nonce = nonce)
        modifiedAccounts[address] = a
    }

    override fun dbSet(address: HexBytes, key: HexBytes, value: HexBytes) {
        if (staticCall)
            throw RuntimeException("cannot set balance in static call")
        if (key.size == 0) throw RuntimeException("invalid key length = 0")
        modifiedStorage.putIfAbsent(address, mutableMapOf())
        modifiedStorage[address]!![key] = value
    }

    override fun dbGet(address: HexBytes, key: HexBytes): HexBytes {
        if (key.size == 0) throw RuntimeException("invalid key length = 0")

        // if has modified
        if (modifiedStorage.containsKey(address)) {
            // if it is delete mark
            val value = modifiedStorage[address]!![key]
            if (value != null) {
                // delete mark
                return value
            }
        }
        if (parentBackend != null) {
            return parentBackend.dbGet(address, key)
        }
        val a = trie[address] ?: return HexBytes.empty()
        val v = contractStorageTrie.revert(a.storageRoot)[key]
        return v ?: HexBytes.empty()
    }

    override fun dbHas(address: HexBytes, key: HexBytes): Boolean {
        if (key.size == 0) throw RuntimeException("invalid key length = 0")
        val value = dbGet(address, key)
        return value.size != 0
    }

    override fun dbRemove(address: HexBytes, key: HexBytes) {
        if (staticCall)
            throw RuntimeException("cannot set balance in static call")
        if (key.size == 0) throw RuntimeException("invalid key length = 0")
        dbSet(address, key, HexBytes.empty())
    }

    private fun lookupCode(h: HexBytes): HexBytes {
        return (codeCache[h] ?: codeStore[h]) ?: HexBytes.empty()
    }

    override fun getCode(address: HexBytes): HexBytes {
        val code = consensusCode[address]
        if(code != null)
            return code
        val ad = replace[address]
        if (ad != null) {
            println("replace from $address to $ad")
            return getCode(ad)
        }
        val a = lookup(address)
        if (a.contractHash == HashUtil.EMPTY_DATA_HASH_HEX)
            return HexBytes.empty()
        return lookupCode(a.contractHash)
    }

    override fun setCode(address: HexBytes, code: HexBytes) {
        if(consensusCode.containsKey(address))
            throw RuntimeException("cannot set code of consensus")
        if (staticCall)
            throw RuntimeException("cannot set balance in static call")
        val h = code.sha3()
        val a = lookup(address).copy(contractHash = h)
        modifiedAccounts[address] = a
        codeCache[h] = code
    }

    companion object {
        val replace: MutableMap<Address, Address> = mutableMapOf()
    }
}