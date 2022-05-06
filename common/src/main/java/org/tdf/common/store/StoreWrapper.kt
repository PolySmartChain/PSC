package org.tdf.common.store

import org.tdf.common.serialize.Codec

/**
 * delegate `Store<ByteArray, ByteArray>` as `Store<K, V>`
 *
 * @param <K> type of key
 * @param <V> type of value
</V></K> */
class StoreWrapper<K, V>(
    val store: Store<ByteArray, ByteArray>,
    private val keyCodec: Codec<K>,
    private val valueCodec: Codec<V>
) : Store<K, V> {
    override fun get(k: K): V? {
        val v = store[keyCodec.encoder.apply(k)]
        return if (v == null || v.isEmpty()) null else valueCodec.decoder.apply(v)
    }

    override fun set(k: K, v: V) {
        store[keyCodec.encoder.apply(k)] = valueCodec.encoder.apply(v)
    }

    override fun remove(k: K) {
        store.remove(keyCodec.encoder.apply(k))
    }

    override fun flush() {}
}