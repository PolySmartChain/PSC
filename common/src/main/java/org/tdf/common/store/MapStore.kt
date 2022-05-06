package org.tdf.common.store

/**
 * delegate Map as Store
 *
 * @param <K> key type
</K> */
open class MapStore<K, V>(val cache: MutableMap<K, V>) : BatchStore<K, V>, IterableStore<K, V> {

    constructor() : this(mutableMapOf())

    override fun get(k: K): V? {
        return cache[k]
    }

    override fun set(k: K, v: V) {
        cache[k] = v
    }

    override fun putAll(rows: Collection<Map.Entry<K, V>>) {
        rows.forEach { cache[it.key] = it.value }
    }

    override fun remove(k: K) {
        cache.remove(k)
    }

    override fun flush() {}
    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return cache.entries.iterator()
    }
}