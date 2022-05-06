package org.tdf.sunflower.state

import lombok.SneakyThrows
import org.slf4j.LoggerFactory
import org.tdf.common.store.Store
import org.tdf.common.trie.ReadOnlyTrie
import org.tdf.common.trie.Trie
import org.tdf.common.util.HexBytes

abstract class AbstractStateTrie<ID, S> : StateTrie<ID, S> {
    abstract val db: Store<ByteArray, ByteArray>

    override fun get(rootHash: HexBytes, id: ID): S? {
        return getTrieForReadOnly(rootHash)[id]
    }

    override fun getTrie(rootHash: HexBytes): Trie<ID, S> {
        return trie.revert(rootHash)
    }

    @SneakyThrows
    protected fun getTrieForReadOnly(rootHash: HexBytes): Trie<ID, S> {
        return ReadOnlyTrie.of(getTrie(rootHash))
    }

    companion object {
        private val log = LoggerFactory.getLogger("trie")
    }
}