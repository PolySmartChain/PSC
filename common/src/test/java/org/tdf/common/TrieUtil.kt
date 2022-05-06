package org.tdf.common

import org.tdf.common.serialize.Codec
import org.tdf.common.store.ByteArrayMapStore
import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.trie.TrieImpl

object TrieUtil {
    @JvmStatic
    fun <K, V> newInstance(
        store: Store<ByteArray, ByteArray>,
        keyCodec: Codec<K>,
        valueCodec: Codec<V>
    ): TrieImpl<K, V> {
        return TrieImpl(
            store,
            keyCodec,
            valueCodec,
            null
        )
    }

    class Builder<K, V> {
        private var store: Store<ByteArray, ByteArray>? = null
        private var keyCodec: Codec<K>? = null
        private var valueCodec: Codec<V>? = null

        fun store(store: Store<ByteArray, ByteArray>): Builder<K, V> {
            this.store = store
            return this
        }

        fun keyCodec(keyCodec: Codec<K>): Builder<K, V> {
            this.keyCodec = keyCodec
            return this
        }

        fun valueCodec(valueCodec: Codec<V>): Builder<K, V> {
            this.valueCodec = valueCodec
            return this
        }

        fun build(): Trie<K, V> {
            return newInstance(store!!, keyCodec!!, valueCodec!!)
        }
    }

    @JvmStatic
    fun <K, V> builder(): Builder<K, V> {
        return Builder()
    }

    @JvmStatic
    val default: Trie<ByteArray, ByteArray>?
        get() = builder<ByteArray, ByteArray>()
            .store(ByteArrayMapStore())
            .keyCodec(Codec.identity())
            .valueCodec(Codec.identity())
            .build()
}