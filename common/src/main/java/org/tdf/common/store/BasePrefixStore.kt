package org.tdf.common.store

class BasePrefixStore(
    private val wrapped: Store<ByteArray, ByteArray>,
    val prefix: ByteArray
) : Store<ByteArray, ByteArray> {
    override fun get(k: ByteArray): ByteArray? {
        return wrapped[prefix + k]
    }

    override fun set(k: ByteArray, v: ByteArray) {
        wrapped[prefix + k] = v
    }

    override fun remove(k: ByteArray) {
        wrapped.remove(prefix + k)
    }

    override fun flush() {
        wrapped.flush()
    }
}