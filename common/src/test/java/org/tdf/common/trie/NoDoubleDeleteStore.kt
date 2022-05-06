package org.tdf.common.trie

import org.spongycastle.util.encoders.Hex
import org.tdf.common.store.ByteArrayMapStore

class NoDoubleDeleteStore : ByteArrayMapStore<ByteArray?>() {
    override fun remove(k: ByteArray) {
        val v = get(k)
        if (v == null || v.isEmpty()) throw RuntimeException("key to delete not found")
        super.remove(k)
    }

    override fun toString(): String {
        val buffer = StringBuffer()
        cache.forEach { (k: ByteArray?, v: ByteArray?) ->
            buffer.append(Hex.toHexString(k))
            buffer.append(" = ")
            buffer.append(Hex.toHexString(v))
            buffer.append("\n")
        }
        return buffer.toString()
    }
}