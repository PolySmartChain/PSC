package org.tdf.common.trie


import org.tdf.common.store.Store
import org.tdf.common.util.HexBytes
import org.tdf.common.util.sha3
import java.util.function.BiFunction

/**
 * https://medium.com/codechain/secure-tree-why-state-tries-key-is-256-bits-1276beb68485
 *
 *
 * That is, if there are new nodes added into, a modification of, or an attempt to read these two tries,
 * there must be a disk IO, and if possible, must pass by the least amount of nodes possible until reaching the leaf node.
 * For this reason, MPT allowed for the compressing of the 1-child branch node with the extension node.
 * However, only branch nodes with a single child can be compressed into an extension node.
 * Thus, if an attacker can maliciously create a branch node with two children, he/she can attack at a low cost.
 * Thus, the secure tree uses a keccak-256 hash value as its key, and prevents an attacker from creating a node at a location that he/she desires.
 *
 * @param <V> value type
</V> */
class SecureTrie<K, V>(delegate: Trie<K, V>) : Trie<K, V> by delegate {
    private val delegate: AbstractTrie<K, V>;

    init {
        when (delegate) {
            is SecureTrie<K, V> -> throw RuntimeException("already a secure trie")
            is AbstractTrie<K, V> -> this.delegate = delegate
            else -> throw RuntimeException("unsupported type ${delegate.javaClass}")
        }
    }

    private fun K.bytes(): ByteArray {
        return delegate.kCodec.encoder.apply(this).sha3()
    }


    override fun get(k: K): V? {
        return delegate.getFromBytes(k.bytes())
    }

    override fun set(k: K, v: V) {
        delegate.putBytes(k.bytes(), delegate.vCodec.encoder.apply(v))
    }

    override fun remove(k: K) {
        delegate.removeBytes(k.bytes())
    }

    override fun revert(rootHash: HexBytes, store: Store<ByteArray, ByteArray>): Trie<K, V> {
        return SecureTrie(delegate.revert(rootHash, store))
    }

    override fun traverse(traverser: BiFunction<in K, in V, Boolean>) {
        throw UnsupportedOperationException("not supported in secure trie")
    }
}