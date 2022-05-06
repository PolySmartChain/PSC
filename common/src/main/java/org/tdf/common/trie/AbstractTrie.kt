package org.tdf.common.trie

import org.tdf.common.serialize.Codec
import org.tdf.common.store.Store
import java.util.function.BiFunction
import java.util.function.Function

abstract class AbstractTrie<K, V> : Trie<K, V> {
    abstract val kCodec: Codec<K>
    abstract val vCodec: Codec<V>
    abstract override val store: Store<ByteArray, ByteArray>

    abstract fun getFromBytes(key: ByteArray): V?

    abstract fun putBytes(key: ByteArray, value: ByteArray)
    abstract fun removeBytes(key: ByteArray)
    override fun set(k: K, v: V) {
        putBytes(kCodec.encoder.apply(k), vCodec.encoder.apply(v))
    }

    override fun get(k: K): V? {
        val data = kCodec.encoder.apply(k)
        return getFromBytes(data)
    }

    override fun remove(k: K) {
        val data = kCodec.encoder.apply(k)
        removeBytes(data)
    }

    override fun traverse(traverser: BiFunction<in K, in V, Boolean>) {
        traverseInternal { k: ByteArray, v: ByteArray ->
            traverser.apply(
                kCodec.decoder.apply(k), vCodec.decoder.apply(v)
            )
        }
    }

    override fun traverseValue(traverser: Function<in V, Boolean>) {
        traverseInternal { _: ByteArray, v: ByteArray ->
            traverser.apply(
                vCodec.decoder.apply(v)
            )
        }
    }

    abstract fun traverseInternal(traverser: BiFunction<ByteArray, ByteArray, Boolean>)
}