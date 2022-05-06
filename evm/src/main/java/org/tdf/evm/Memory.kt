package org.tdf.evm

import org.tdf.evm.SlotUtils.SLOT_BYTE_ARRAY_SIZE

interface Memory {
    operator fun get(idx: Int): Byte {
        return data[idx]
    }

    operator fun set(idx: Int, v: Byte) {
        data[idx] = v
    }

    val size: Int
        get() = data.size

    /**
     * resize memory, returns the actual extended bytes
     * memory will extended words (32byte)
     */
    fun resize(size: Int): Int

    val data: ByteArray
    val limit: Int


    fun resize(off: Long, len: Long)

    fun resizeAndCopy(off: Long, len: Long): ByteArray


    fun write(off: Int, buf: ByteArray, bufOff: Int = 0, bufSize: Int = buf.size) {
        if (off < 0 || off + bufSize > size)
            throw RuntimeException("memory access overflow")

        for (i in 0 until bufSize) {
            this[off + i] = buf[bufOff + i]
        }
    }

    fun copy(off: Int, limit: Int): ByteArray {
        if (off < 0 || limit < 0)
            throw RuntimeException("memory access overflow")
        if (limit < off)
            throw RuntimeException("memory: limit < off")
        val r = ByteArray(limit - off)
        read(off, r)
        return r
    }

    fun read(off: Int, buf: ByteArray, bufOff: Int = 0, bufSize: Int = buf.size) {
        if (off < 0 || off + bufSize > size)
            throw RuntimeException("memory access overflow")
        for (i in 0 until bufSize) {
            buf[bufOff + i] = this[off + i]
        }
    }
}

class MemoryImpl(override val limit: Int = Int.MAX_VALUE) : Memory {
    private fun toResize(off: Long, len: Long): Int {
        val i = off + len
        if (off < 0 || len < 0 || i < 0)
            throw RuntimeException("memory access overflow off = $off len = $len i = $i")
        if (i > Int.MAX_VALUE)
            throw RuntimeException("memory access overflow")
        return i.toInt()
    }

    override fun resize(off: Long, len: Long) {
        resize(toResize(off, len))
    }

    override fun resizeAndCopy(off: Long, len: Long): ByteArray {
        resize(toResize(off, len))
        val r = ByteArray(len.toInt())
        read(off.toInt(), r)
        return r
    }

    override fun resize(size: Int): Int {
        if (size < 0 || size > limit)
            throw RuntimeException("memory size exceeds limit")
        if (size > data.size) {
            val tmpSize = if (size % SLOT_BYTE_ARRAY_SIZE == 0) {
                size
            } else {
                size - (size % SLOT_BYTE_ARRAY_SIZE) + SLOT_BYTE_ARRAY_SIZE
            }
            val r = tmpSize - data.size
            val tmp = ByteArray(tmpSize)
            System.arraycopy(data, 0, tmp, 0, data.size)
            this.data = tmp
            return r
        }
        return 0
    }

    override var data: ByteArray = ByteArray(0)
        private set
}