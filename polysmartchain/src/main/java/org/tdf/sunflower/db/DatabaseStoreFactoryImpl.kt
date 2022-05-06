package org.tdf.sunflower.db

import org.fusesource.leveldbjni.JniDBFactory
import org.iq80.leveldb.impl.Iq80DBFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.store.*
import org.tdf.sunflower.DatabaseConfig
import org.tdf.sunflower.facade.DatabaseStoreFactory

@Component
class DatabaseStoreFactoryImpl(private val config: DatabaseConfig) : DatabaseStoreFactory {
    private val created: MutableSet<Byte> = mutableSetOf()
    private val base: DatabaseStore
    override val directory: String
        get() = config.directory

    override fun create(prefix: Char, comment: String): Store<ByteArray, ByteArray> {
        log.info("create storage with prefix {} {} directory = {}", prefix, comment, config.directory)
        val b = prefix.code.toByte()
        if (created.contains(b))
            throw RuntimeException("this prefix $prefix had been used")
        created.add(b)

        return BasePrefixStore(base, byteArrayOf(b))
    }

    override val name: String
        get() = config.name

    override fun cleanup() {
        base.flush()
    }

    companion object {
        private val log = LoggerFactory.getLogger("db")
    }

    init {
        log.info("buffer = ${config.buffer}")
        log.info("load database at directory {} type = {}", config.directory, config.name)
        when (config.name.trim { it <= ' ' }.lowercase()) {
            "leveldb-jni", "leveldb" -> base = LevelDb(JniDBFactory.factory, config.directory)
            "leveldb-iq80" -> base = LevelDb(Iq80DBFactory.factory, config.directory)
            "memory" -> base = MemoryDatabaseStore()
            else -> {
                base = BufferedLevelDb(JniDBFactory.factory, config.directory, config.buffer.toLong())
                log.warn("Data source is not supported, default is leveldb")
            }
        }
        base.init(
            DBSettings(
                config.maxOpenFiles, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
            )
        )
        if (config.reset) {
            base.clear()
        }
    }
}