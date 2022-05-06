package org.tdf.sunflower.facade

import java.io.Closeable
import java.util.concurrent.locks.Lock

class LockedRepositoryWriter(private val proxy: RepositoryWriter, val lock: Lock) : RepositoryWriter by proxy,
    Closeable {
    override fun close() {
        lock.unlock()
    }
}