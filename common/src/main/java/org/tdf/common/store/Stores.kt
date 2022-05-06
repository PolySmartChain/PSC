package org.tdf.common.store

import org.tdf.common.util.ByteArrayMap
import java.util.function.Predicate

/**
 * @param <K> key type
 * @param <V> value type
 * @author zhuyingjie
</V></K> */
interface BatchStore<K, V> : Store<K, V> {
    /**
     * Copies all of the mappings from rows to this store.
     * This operation should be atomic when writes to disk.
     * If null or trap value entry exists in rows, the entry will be removed.
     *
     * @param rows row to put into this store
     */
    fun putAll(rows: Collection<Map.Entry<K, V>>)
}

open class ByteArrayMapStore<V>(cache: MutableMap<ByteArray, V>) : MapStore<ByteArray, V>(cache) {
    constructor() : this(ByteArrayMap<V>())
}

/**
 * no delete store will store deleted key-value pair to @see deleted
 * when compact method called, clean the key-pari in @see deleted
 */
open class NoDeleteStore<K, V>(
    private val delegate: Store<K, V>,
    private val isNull: Predicate<V?>
) : Store<K, V> by delegate {

    override fun set(k: K, v: V) {
        if (isNull.test(v)) return
        delegate[k] = v
    }

    override fun remove(k: K) {}
}

class NoDoubleDeleteStore<K, V>(private val delegate: Store<K, V>, private val isNull: Predicate<V?>) :
    Store<K, V> by delegate {
    override fun remove(k: K) {
        val v = get(k)
        if (isNull.test(v)) throw RuntimeException("trying to delete a non-exists key")
        delegate.remove(k)
    }
}

interface IterableStore<K, V> : Store<K, V>, Iterable<Map.Entry<K, V>>


class MemoryDatabaseStore : ByteArrayMapStore<ByteArray>(), DatabaseStore {
    override fun init(settings: DBSettings) {}
    override val alive: Boolean
        get() {
            return true
        }

    override fun close() {}
    override fun clear() {
        cache.clear()
    }
}

interface ReadonlyStore<K, V> : Store<K, V> {
    companion object {
        fun <K, V> of(delegate: Store<K, V>): Store<K, V> {
            return if (delegate is ReadonlyStore) delegate else ReadOnlyStoreImpl(delegate)
        }
    }
}

internal class ReadOnlyStoreImpl<K, V>(private val delegate: Store<K, V>) : Store<K, V> by delegate,
    ReadonlyStore<K, V> {
    override fun set(k: K, v: V) {
        throw UnsupportedOperationException(READ_ONLY_TIP)
    }

    override fun remove(k: K) {
        throw UnsupportedOperationException(READ_ONLY_TIP)
    }

    override fun flush() {
        throw UnsupportedOperationException(READ_ONLY_TIP)
    }

    companion object {
        private const val READ_ONLY_TIP = "the store is read only"
    }
}