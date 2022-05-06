package org.tdf.common.store

import com.github.salpadding.rlpstream.Rlp
import org.tdf.common.serialize.Codec
import org.tdf.common.util.HexBytes
import org.tdf.common.util.decode
import org.tdf.common.util.hex
import java.util.*

class PrefixStore<K, V>(
    private val contractStorage: Store<HexBytes, HexBytes>,
    private val prefix: HexBytes,
    private val kCodec: Codec<K>,
    private val vCodec: Codec<V>
) : IterableStore<K, V> {
    override fun get(k: K): V? {
        val encoded = kCodec.encoder.apply(k)
        val withPrefix = verifyAndPrefix(encoded)
        val v = contractStorage[withPrefix]
        return if (v == null || v.isEmpty()) null else vCodec.decoder.apply(v.bytes)
    }

    private fun verifyAndPrefix(key: ByteArray): HexBytes {
        if (key.isEmpty()) throw RuntimeException("invalid key, length = 0")
        return (prefix.bytes + key).hex()
    }

    override fun set(k: K, v: V) {
        val encoded = kCodec.encoder.apply(k)
        val withPrefix = verifyAndPrefix(encoded)
        addKey(encoded)
        contractStorage[withPrefix] = vCodec.encoder.apply(v).hex()
    }

    private fun keySet(): TreeSet<HexBytes> {
        val keys = contractStorage[prefix]
        return if (keys == null || keys.size == 0) TreeSet() else TreeSet(
            keys.decode(
                Array<HexBytes>::class.java
            ).toList()
        )
    }

    private fun addKey(key: ByteArray) {
        val keySet = keySet()
        keySet.add(key.hex())
        contractStorage[prefix] = Rlp.encode(keySet.toTypedArray()).hex()
    }

    private fun removeKey(key: ByteArray) {
        val keySet = keySet()
        keySet.remove(key.hex())
        contractStorage[prefix] = Rlp.encode(keySet.toTypedArray()).hex()
    }

    override fun remove(k: K) {
        val encoded = kCodec.encoder.apply(k)
        val withPrefix = verifyAndPrefix(encoded)
        removeKey(encoded)
        contractStorage.remove(withPrefix)
    }

    override fun flush() {
        contractStorage.flush()
    }

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        val map: MutableMap<K, V> = mutableMapOf()
        for (key in keySet()) {
            map[kCodec.decoder.apply(key.bytes)] = vCodec.decoder.apply(
                contractStorage[verifyAndPrefix(key.bytes)]!!.bytes
            )
        }
        return map.entries.iterator()
    }
}