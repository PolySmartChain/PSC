package org.tdf.common.store

import org.iq80.leveldb.*
import org.slf4j.LoggerFactory
import org.tdf.common.util.ByteUtil
import org.tdf.common.util.HexBytes
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

open class LevelDb(
    private val factory: DBFactory, // parent directory
    private val directory: String
) : DatabaseStore {
    protected lateinit var db: DB

    private lateinit var dbSettings: DBSettings

    final override var alive = false
        private set

    private val resetDbLock: ReadWriteLock = ReentrantReadWriteLock()

    override fun init(settings: DBSettings) {
        dbSettings = settings
        resetDbLock.writeLock().withLock {
            log.debug("~> LevelDbDataSource.init(): $directory")
            if (alive) return
            val options = Options()
            options.createIfMissing(true)
            options.compressionType(CompressionType.NONE)
            options.blockSize(10 * 1024 * 1024)
            options.writeBufferSize(10 * 1024 * 1024)
            options.cacheSize(0)
            options.paranoidChecks(true)
            options.verifyChecksums(true)
            options.maxOpenFiles(settings.maxOpenFiles)
            try {
                log.debug("Opening database")
                val dbPath = path
                log.debug("Initializing new or existing database: '{}'", directory)
                db = try {
                    factory.open(dbPath.toFile(), options)
                } catch (e: IOException) {
                    // database could be corrupted
                    // exception in std out may look:
                    // org.fusesource.leveldbjni.internal.NativeDB$DBException: Corruption: 16 missing files; e.g.: /Users/stan/ethereumj/database-test/block/000026.ldb
                    // org.fusesource.leveldbjni.internal.NativeDB$DBException: Corruption: checksum mismatch
                    if (e.message?.contains("Corruption:") == true) {
                        log.warn("Problem initializing database.", e)
                        log.info("LevelDB database must be corrupted. Trying to repair. Could take some time.")
                        factory.repair(dbPath.toFile(), options)
                        log.info("Repair finished. Opening database again.")
                        factory.open(dbPath.toFile(), options)
                    } else {
                        // must be db lock
                        // org.fusesource.leveldbjni.internal.NativeDB$DBException: IO error: lock /Users/stan/ethereumj/database-test/state/LOCK: Resource temporarily unavailable
                        throw e
                    }
                }
                alive = true
            } catch (ioe: IOException) {
                log.error(ioe.message, ioe)
                throw RuntimeException("Can't initialize database", ioe)
            }
            log.debug("<~ LevelDbDataSource.init(): $directory")
        }
    }

    override fun close() {
        resetDbLock.writeLock().withLock {
            if (!alive) return
            try {
                log.debug("Close db: {}", directory)
                db.close()
                alive = false
            } catch (e: IOException) {
                log.error("Failed to find the db file on the close: {} ", directory)
            }
        }
    }

    fun destroyDB() {
        resetDbLock.writeLock().withLock {
            val options = Options()
            try {
                factory.destroy(path.toFile(), options)
            } catch (e: IOException) {
                log.error(e.message, e)
            }
        }
    }

    fun reset() {
        close()
        FileUtil.recursiveDelete(path.toString())
        init(dbSettings)
    }

    override fun putAll(rows: Collection<Map.Entry<ByteArray, ByteArray>>) {
        resetDbLock.readLock().withLock {
            if (log.isTraceEnabled) log.trace("~> LevelDbDataSource.updateBatch(): " + directory + ", " + rows.size)
            try {
                updateBatchInternal(rows)
                if (log.isTraceEnabled) log.trace("<~ LevelDbDataSource.updateBatch(): " + directory + ", " + rows.size)
            } catch (e: Exception) {
                log.error("Error, retrying one more time...", e)
                // try one more time
                try {
                    updateBatchInternal(rows)
                    if (log.isTraceEnabled) log.trace("<~ LevelDbDataSource.updateBatch(): " + directory + ", " + rows.size)
                } catch (e1: Exception) {
                    log.error("Error", e)
                    throw RuntimeException(e)
                }
            }
        }
    }

    private fun updateBatchInternal(rows: Collection<Map.Entry<ByteArray, ByteArray>>) {
        db.createWriteBatch().use { batch ->
            for ((key, value) in rows) {
                if (value.isEmpty()) {
                    batch.delete(key)
                } else {
                    batch.put(key, value)
                }
            }
            db.write(batch)
        }
    }

    private fun ByteArray.hex(): String {
        return HexBytes.encode(this)
    }

    override fun get(k: ByteArray): ByteArray? {
        resetDbLock.readLock().withLock {
            if (log.isTraceEnabled)
                log.trace("~> LevelDbDataSource.get(): " + directory + ", key: " + k.hex())
            return try {
                val ret = db[k]
                if (log.isTraceEnabled)
                    log.trace(
                        "<~ LevelDbDataSource.get(): " + directory + ", key: " + k.hex() + ", " + (ret?.size ?: "null")
                    )
                ret ?: ByteUtil.EMPTY_BYTE_ARRAY
            } catch (e: DBException) {
                log.warn("Exception. Retrying again...", e)
                val ret = db[k]
                if (log.isTraceEnabled)
                    log.trace(
                        "<~ LevelDbDataSource.get(): " + directory + ", key: " + k.hex() + ", " + (ret?.size ?: "null")
                    )
                ret ?: ByteUtil.EMPTY_BYTE_ARRAY
            }
        }
    }

    override fun set(k: ByteArray, v: ByteArray) {
        resetDbLock.readLock().withLock {
            if (log.isTraceEnabled) log.trace(
                "~> LevelDbDataSource.put(): " + directory + ", key: " + k.hex()
            )
            if (v.isEmpty()) {
                db.delete(k)
            } else {
                db.put(k, v)
            }
            if (log.isTraceEnabled) log.trace(
                "<~ LevelDbDataSource.put(): " + directory + ", key: " + k.hex()
            )
        }
    }

    override fun remove(k: ByteArray) {
        resetDbLock.readLock().withLock {
            if (log.isTraceEnabled) log.trace(
                "~> LevelDbDataSource.delete(): " + directory + ", key: " + k.hex()
            )
            db.delete(k)
            if (log.isTraceEnabled) log.trace(
                "<~ LevelDbDataSource.delete(): " + directory + ", key: " + k.hex()
            )
        }
    }

    override fun clear() {
        reset()
    }

    private val path: Path
        get() = Paths.get(directory)

    override fun flush() {}

    companion object {
        private val log = LoggerFactory.getLogger("leveldb")
    }
}