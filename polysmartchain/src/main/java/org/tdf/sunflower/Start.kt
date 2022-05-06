package org.tdf.sunflower

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.googlecode.jsonrpc4j.spring.JsonServiceExporter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils
import org.tdf.common.event.EventBus
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.JsonStore
import org.tdf.common.store.NoDeleteStore
import org.tdf.common.store.Store
import org.tdf.common.store.StoreWrapper
import org.tdf.common.trie.SecureTrie
import org.tdf.common.trie.Trie
import org.tdf.common.trie.TrieImpl
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.consensus.poa.PoA
import org.tdf.sunflower.consensus.pos.Genesis
import org.tdf.sunflower.consensus.pos.PoS
import org.tdf.sunflower.consensus.pow.PoW
import org.tdf.sunflower.controller.JsonRpc
import org.tdf.sunflower.facade.*
import org.tdf.sunflower.net.PeerServer
import org.tdf.sunflower.net.PeerServerImpl
import org.tdf.sunflower.pool.TransactionPoolImpl
import org.tdf.sunflower.service.RepositoryKVImpl
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.sunflower.types.ConsensusConfig
import org.tdf.sunflower.types.PropertyReader
import org.tdf.sunflower.util.FileUtils
import org.tdf.sunflower.util.MapperUtil
import org.tdf.sunflower.vm.BackendImpl
import org.tdf.sunflower.vm.VMExecutor
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.*

@SpringBootApplication // use SPRING_CONFIG_LOCATION environment to locate spring config
// for example: SPRING_CONFIG_LOCATION=classpath:\application.yml,some-path\custom-config.yml
open class Start {
    @Bean
    open fun genesis(cfg: ConsensusConfig): AbstractGenesis {
        return when (cfg.name) {
            "pos" -> Genesis(cfg.genesisJson)
            else -> throw RuntimeException("require pos")
        }
    }

    @Bean
    open fun sunflowerRepository(
            cfg: DatabaseConfig,
            bus: EventBus,
            factory: DatabaseStoreFactory,
            accountTrie: AccountTrie,
            genesis: AbstractGenesis
    ): RepositoryServiceImpl {
        when (val type = cfg.blockStore) {
            "rdbms" -> {
                throw UnsupportedOperationException()
            }
            "kv" -> {
                val kv = RepositoryServiceImpl(RepositoryKVImpl(bus, factory, accountTrie, genesis))
                if (cfg.canonicalize) {
                    kv.writer.use {
                        val t0 = System.currentTimeMillis()
                        it.canonicalize()
                        val t1 = System.currentTimeMillis()
                        log.info("canonicalize use ${(t0 - t1) / 1000.0} ms")
                    }
                }
                if(cfg.deleteGT >= 0) {
                    kv.writer.use {
                        it.deleteGT(cfg.deleteGT)
                    }
                }
                return kv
            }
            else -> throw RuntimeException("unknown block store type: $type")
        }
    }

    @Bean
    open fun objectMapper(): ObjectMapper {
        return MAPPER
    }

    @Bean
    open fun miner(
            engine: ConsensusEngine,  // this dependency asserts the peer server had been initialized
            peerServer: PeerServer,
            cfg: ConsensusConfig
    ): Miner {
        val miner = engine.miner
        if (cfg.enableMining) {
            log.info("start miner")
            miner.start()
        } else {
            log.info("mining is not enabled")
        }
        return miner
    }

    @Bean
    open fun consensusConfig(properties: ConsensusProperties): ConsensusConfig {
        return ConsensusConfig(properties)
    }

    @Bean
    open fun appConfig(): AppConfig {
        return AppConfig.get()
    }

    @Bean
    open fun accountTrie(
            databaseStoreFactory: DatabaseStoreFactory,
            @Qualifier("contractStorageTrie") contractStorageTrie: Trie<HexBytes, HexBytes>,
            @Qualifier("contractCodeStore") contractCodeStore: Store<HexBytes, HexBytes>,
            c: AppConfig
    ): AccountTrie {
        return AccountTrie(
                databaseStoreFactory.create('a', "account trie"),
                contractCodeStore,
                contractStorageTrie,
                c.isTrieSecure
        )
    }

    private fun <T> Class<T>.create(): T {
        return this.getDeclaredConstructor().newInstance()
    }

    @Bean
    open fun consensusEngine(
            cfg: ConsensusConfig,
            repoSrv: RepositoryServiceImpl,
            transactionPool: TransactionPoolImpl,
            databaseStoreFactory: DatabaseStoreFactory,
            eventBus: EventBus,
            syncConfig: SyncConfig,
            accountTrie: AccountTrie,
            context: ApplicationContext,
            @Qualifier("contractStorageTrie") contractStorageTrie: Trie<HexBytes?, HexBytes?>?,
            @Qualifier("contractCodeStore") contractCodeStore: Store<HexBytes?, HexBytes?>?
    ): ConsensusEngine {

        log.info("load consensus engine name = {}", cfg.name)
        val engine: ConsensusEngine = when (cfg.name) {
            ApplicationConstants.CONSENSUS_NONE -> {
                log.warn("none consensus engine selected, please ensure you are in test mode")
                AbstractConsensusEngine.NONE
            }
            ApplicationConstants.CONSENSUS_POA ->                 // use poa as default consensus
                // another engine: pow, pos, pow+pos, vrf
                PoA()
            ApplicationConstants.CONSENSUS_VRF -> throw UnsupportedOperationException()
            ApplicationConstants.CONSENSUS_POS -> PoS()
            else -> try {
                customClassLoader.loadClass(cfg.name).create() as ConsensusEngine
            } catch (e: Exception) {
                e.printStackTrace()
                log.error(
                        "none available consensus configured by sunflower.consensus.name=" + cfg.name +
                                " please provide available consensus engine"
                )
                log.error("roll back to poa consensus")
                PoA()
            }
        }

        log.info("inject dependencies into consensus engine")
        inject(context, engine as AbstractConsensusEngine)

        log.info("initialize consensus engine")
        engine.init(cfg)

        log.info("consensus engine created chain id = {}", cfg.chainId)

        val nonNullFields = arrayOf("Miner", "Validator", "AccountTrie", "GenesisBlock", "PeerServerListener")

        for (field in nonNullFields) {
            val method = "get$field"
            if (engine.javaClass.getMethod(method).invoke(engine) == null) {
                throw RuntimeException("field $field not injected after init")
            }
        }

        val root =
                repoSrv.reader.use {
                    accountTrie.init(engine.alloc, engine.bios, engine.builtins, engine.code, it)
                }
        // init accountTrie and genesis block
        val g = engine.genesisBlock
        repoSrv.writer.use { it.saveGenesis(g.copy(header = g.header.impl.copy(stateRoot = root))) }
        transactionPool.init()
        return engine
    }

    // create peer server from properties
    @Bean
    open fun peerServer(
            properties: PeerServerProperties,
            engine: ConsensusEngine,
            factory: DatabaseStoreFactory
    ): PeerServer {
        val rd = PropertyReader(PropertiesWrapper(properties))
        if (rd.getAsLowerCased("name") == "none") {
            return PeerServer.NONE
        }

        val persist = rd.getAsBool("persist")
        val store = if (persist && "memory" != factory.name) JsonStore(
                Paths.get(factory.directory, "peers.json").toString(), MAPPER
        ) else JsonStore("\$memory", MAPPER)

        val peerServer: PeerServer = PeerServerImpl(store, engine, properties)
        peerServer.addListeners(engine.peerServerListener)
        peerServer.start()
        return peerServer
    }

    @Bean
    open fun eventBus(): EventBus {
        return EventBus(
                ThreadFactoryBuilder().setNameFormat("EventBus-%d").build()
        )
    }

    // storage root of contract store
    @Bean
    open fun contractStorageTrie(factory: DatabaseStoreFactory, c: AppConfig): Trie<HexBytes, HexBytes> {
        val ret = TrieImpl(
                NoDeleteStore(
                        factory.create('o', "contract storage trie")
                ) { it == null || it.isEmpty() },
                Codecs.hex,
                Codecs.hex
        )
        return if (c.isTrieSecure) SecureTrie(ret) else ret
    }

    // contract hash code -> contract binary
    @Bean
    open fun contractCodeStore(factory: DatabaseStoreFactory): Store<HexBytes, HexBytes> {
        return StoreWrapper(
                factory.create('c', "contract code hash -> contract code"),
                Codecs.hex,
                Codecs.hex
        )
    }

    // inject application context into consensus engine
    private fun inject(
            context: ApplicationContext,
            engine: AbstractConsensusEngine
    ) {
        engine.eventBus = context.getBean(EventBus::class.java)
        engine.transactionPool = context.getBean(TransactionPool::class.java)
        engine.repo = context.getBean(RepositoryService::class.java)
        engine.contractStorageTrie =
                context.getBean("contractStorageTrie", Trie::class.java) as Trie<HexBytes, HexBytes>
        engine.contractCodeStore = context.getBean("contractCodeStore", Store::class.java) as Store<HexBytes, HexBytes>
        engine.accountTrie = context.getBean(AccountTrie::class.java)
    }

    @Bean(name = ["/"])
    open fun jsonServiceExporter(jsonRpc: JsonRpc): JsonServiceExporter {
        val exporter = JsonServiceExporter()
        exporter.service = jsonRpc
        exporter.serviceInterface = JsonRpc::class.java
        return exporter
    }


    companion object {
        @JvmField
        val MAPPER = MapperUtil.OBJECT_MAPPER
        private val log = LoggerFactory.getLogger("init")
        var customClassLoader: ClassLoader = ClassUtils.getDefaultClassLoader()!!
            private set

        private fun loadDefaultConfig(): Properties {
            val r: Resource = ClassPathResource("default.config.properties")
            val p = Properties()
            p.load(r.inputStream)
            return p
        }

        private fun loadConstants(env: Environment) {
            AppConfig.INSTANCE = AppConfig(env)
            BackendImpl.replace.putAll(AppConfig.INSTANCE.replace)
        }

        private fun loadLibs(path: String) {
            val f = File(path)
            if (!f.exists()) throw RuntimeException("load libs $path failed")
            var urls: MutableList<URL> = mutableListOf()
            if (f.isDirectory) {
                val files = f.listFiles() ?: return
                for (file in files) {
                    if (!file.name.endsWith(".jar")) continue
                    urls.add(file.toURI().toURL())
                }
            } else {
                urls = mutableListOf(f.toURI().toURL())
            }
            customClassLoader = URLClassLoader(urls.toTypedArray(), ClassUtils.getDefaultClassLoader())
        }

        @JvmStatic
        fun main(args: Array<String>) {
            log.debug("class path = {}", System.getProperty("java.class.path"))
            // inject class loader
            FileUtils.setClassLoader(ClassUtils.getDefaultClassLoader())
            val app = SpringApplication(Start::class.java)

            app.setDefaultProperties(loadDefaultConfig())
            log.info("default properties = {}", loadDefaultConfig())

            app.addInitializers({
                loadConstants(it.environment)
                if (AppConfig.get().vmLogs.isNotEmpty()) {
                    val f = File(AppConfig.get().vmLogs)
                    if (!f.exists()) {
                        f.mkdir()
                    }
                    log.info("enable vm logs, path = {}", f.path)
                    VMExecutor.enableDebug(f.path)
                }
                val path = it.environment.getProperty("sunflower.libs") ?: return@addInitializers
                loadLibs(path)
            })
            app.run(*args)
        }
    }
}