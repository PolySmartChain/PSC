package org.tdf.common.trie

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.serialize.Codec
import org.tdf.common.store.ByteArrayMapStore
import org.tdf.common.store.ReadonlyStore
import org.tdf.common.store.Store
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import java.util.function.BiFunction

// enhanced radix tree
class TrieImpl<K, V>(
    override val store: Store<ByteArray, ByteArray> = ByteArrayMapStore(),
    override val kCodec: Codec<K> = Codec.identity(),
    override val vCodec: Codec<V> = Codec.identity(),
    private var root: Node? = null
) : AbstractTrie<K, V>() {

    override fun getFromBytes(key: ByteArray): V? {
        require(key.isNotEmpty()) { "key cannot be null" }
        val v = root?.get(TrieKey.fromNormal(key))
        return if (v == null || v.isEmpty()) null else vCodec.decoder.apply(v)
    }

    override fun putBytes(key: ByteArray, value: ByteArray) {
        require(key.isNotEmpty()) { "key cannot be null" }
        if (value.isEmpty()) {
            removeBytes(key)
            return
        }
        if (root == null) {
            root = Node.newLeaf(TrieKey.fromNormal(key), value)
            return
        }
        root!!.insert(TrieKey.fromNormal(key), value, store)
    }

    override fun removeBytes(key: ByteArray) {
        require(key.isNotEmpty()) { "key cannot be null" }
        val r = root ?: return
        root = r.delete(TrieKey.fromNormal(key), store)
    }

    fun clear() {
        root = null
    }

    override fun commit(): HexBytes {
        val r = root ?: return nullHash
        if (!r.isDirty) return r.hash.hex()
        val hash = Rlp.decodeBytes(r.commit(store, true))
        if (r.isDirty || r.hash == null)
            throw RuntimeException("unexpected error: still dirty after commit")
        return hash.hex()
    }

    override fun flush() {
        store.flush()
    }

    override fun revert(rootHash: HexBytes, store: Store<ByteArray, ByteArray>): TrieImpl<K, V> {
        if (rootHash == nullHash) return TrieImpl(
            store,
            kCodec, vCodec, null
        )
        store[rootHash.bytes]?.takeIf { it.isNotEmpty() }
            ?: throw RuntimeException("rollback failed, root hash not exists")
        return TrieImpl(
            store, kCodec, vCodec,
            Node.fromRootHash(rootHash.bytes, ReadonlyStore.of(store))
        )
    }

    private fun traverseTrie(action: ScannerAction) {
        val r = root ?: return
        r.traverse(TrieKey.EMPTY, action)
    }

    override fun dumpKeys(): Set<HexBytes> {
        if (isDirty) throw UnsupportedOperationException()
        val dump = DumpKeys()
        traverseTrie(dump)
        return dump.keys
    }

    override fun dump(): Map<HexBytes, HexBytes> {
        if (isDirty) throw UnsupportedOperationException()
        val dump = Dump()
        traverseTrie(dump)
        return dump.pairs
    }

    override val rootHash: HexBytes
        get() {
            val r = root ?: return nullHash
            if (r.isDirty || r.hash == null)
                throw RuntimeException("the trie is dirty or root hash is null")
            return r.hash.hex()
        }
    override val isDirty: Boolean
        get() = root?.isDirty == true


    override fun traverseInternal(traverser: BiFunction<ByteArray, ByteArray, Boolean>) {
        traverseTrie { k: TrieKey, n: Node ->
            if (n.type != Node.Type.EXTENSION && n.value != null) {
                return@traverseTrie traverser.apply(k.toNormal(), n.value)
            }
            true
        }
    }
}