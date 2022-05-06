package org.tdf.common.store

/**
 * abstract storage of key-value mappings
 * the key and value are not allowed to be null
 *
 * @param <K> key
 * @param <V> value
 * @author zhuyingjie
</V></K> */
interface Store<K, V> {
    /**
     * Gets a value by its key, return null or default value if key not found
     *
     * @param k key to query
     * @return value or
     */
    operator fun get(k: K): V?

    /**
     * puts key-value pair into store, remove key in the store if v is empty byte array
     *
     * @param k key of key-value mapping
     * @param v value of key-value mapping
     */
    operator fun set(k: K, v: V)

    /**
     * Deletes the key-value mapping from the source
     *
     * @param k key of key-value mapping
     */
    fun remove(k: K)

    /**
     * If this source has underlying level source then all
     * changes collected in this source are flushed into the
     * underlying source.
     * The implementation may do 'cascading' flush, i.e. call
     * Source didn't change
     */
    fun flush()
}