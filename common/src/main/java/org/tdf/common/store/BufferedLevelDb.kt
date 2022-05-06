package org.tdf.common.store

import org.iq80.leveldb.DBFactory
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class BufferedLevelDb(
    factory: DBFactory, // parent directory
    directory: String,
    private val cacheSize: Long,
) : LevelDb(factory, directory) {
    private val cacheMtx = ReentrantLock()
    private var cacheCap: Long = 0
    private val cache: MutableMap<HexBytes, Optional<HexBytes>> = mutableMapOf()

    override fun get(k: ByteArray): ByteArray? {
        cacheMtx.withLock {
            val v = cache[k.hex()] ?: return super.get(k)
            if (v.isPresent)
                return v.get().bytes
            return null
        }
    }

    override fun set(k: ByteArray, v: ByteArray) {
       cacheMtx.withLock {
           cacheCap += k.size + v.size
           cache[k.hex()] = Optional.of(v.hex())

           if (cacheCap >= cacheSize) {
               flushCache()
               cacheCap = 0
           }
       }
    }

    override fun remove(k: ByteArray) {
        cacheMtx.withLock {
            cacheCap += k.size
            cache[k.hex()] = Optional.empty()

            if (cacheCap >= cacheSize) {
                flushCache()
                cacheCap = 0
            }
        }
    }

    private fun flushCache() {
        println("buffered leveldb flush cap = $cacheCap")
        db.createWriteBatch().use { batch ->
            cache.forEach {
                if (!it.value.isPresent) {
                    batch.delete(it.key.bytes)
                } else {
                    batch.put(it.key.bytes, it.value.get().bytes)
                }
            }
            db.write(batch)
        }
        cache.clear()
    }

    override fun flush() {
        flushCache()
        super.flush()
    }
}