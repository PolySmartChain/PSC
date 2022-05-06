package org.tdf.sunflower.controller

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.tdf.common.util.*
import org.tdf.sunflower.AppConfig
import org.tdf.sunflower.Start
import org.tdf.sunflower.controller.JsonRpc.BlockResult
import org.tdf.sunflower.controller.JsonRpc.CallArguments
import org.tdf.sunflower.facade.*
import org.tdf.sunflower.state.AccountTrie
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.*
import org.tdf.sunflower.vm.Backend
import org.tdf.sunflower.vm.BackendImpl
import org.tdf.sunflower.vm.NonContractException
import org.tdf.sunflower.vm.VMExecutor
import java.math.BigInteger

@Service
class JsonRpcImpl(
    private val accountTrie: AccountTrie,
    private val repo: RepositoryService,
    private val pool: TransactionPool,
    private val cfg: AppConfig,
    private val conCfg: ConsensusConfig,
) : JsonRpc {

    private val gasLimit: Long by lazy {
        this.repo.reader.use {
            it.genesis.gasLimit
        }
    }

    private fun getByJsonBlockId(id: String): Block? {
        return repo.reader.use {
            when (id.trim().lowercase()) {
                "earliest" -> it.genesis
                "latest" -> it.bestBlock
                "pending" -> null
                else -> {
                    val h = id.jsonHex
                    if (h.isHash) {
                        it.getBlockByHash(h.hex)
                    } else {
                        it.getCanonicalBlock(h.long)
                    }
                }
            }
        }
    }

    override fun web3_clientVersion(): String {
        return "tdos"
    }

    override fun web3_sha3(data: String): String {
        return data.jsonHex.bytes.sha3().jsonHex
    }

    override fun net_version(): String {
        return cfg.chainId.toString()
    }

    override fun eth_chainId(): String {
        return cfg.chainId.jsonHex
    }

    override fun net_peerCount(): String {
        return (0).jsonHex
    }

    override fun net_listening(): Boolean {
        return true
    }

    override fun eth_protocolVersion(): String {
        return "66"
    }

    override fun eth_syncing(): Boolean {
        return false
    }

    override fun eth_coinbase(): String {
        return AddrUtil.empty().jsonHex
    }

    override fun eth_mining(): Boolean {
        return false
    }

    override fun eth_hashrate(): String {
        return BigInteger.ZERO.jsonHex
    }

    override fun eth_gasPrice(): String {
        return cfg.vmGasPrice.jsonHex
    }

    override fun eth_accounts(): Array<String> {
        return emptyArray()
    }

    override fun eth_blockNumber(): String {
        return repo.reader.use { it.bestHeader.height.jsonHex }
    }

    private fun getBackendByBlockId(blockId: String, staticCall: Boolean): Backend {
        val header: Header
        repo.reader.use { rd ->
            when (blockId.trim()) {
                "latest" -> {
                    header = rd.bestHeader
                }
                "pending" -> {
                    val e = pool.current()
                    if (e == null) {
                        header = rd.bestHeader
                    } else {
                        return e.createChild(staticCall)
                    }
                }
                "earliest" -> header = rd.genesis.header
                else -> {
                    header = rd.getCanonicalHeader(blockId.jsonHex.long)!!
                }
            }
            return accountTrie.createBackend(parent = header, staticCall = staticCall)
        }
    }

    override fun eth_getBalance(address: String, block: String): String {
        getBackendByBlockId(block, true).use {
            val balance = it.getBalance(address.jsonHex.hex)
            return balance.value.jsonHex
        }
    }


    override fun eth_getStorageAt(address: String, storageIdx: String, blockId: String): String {
        return getBackendByBlockId(blockId, true).use {
            it.dbGet(address.jsonHex.hex, storageIdx.jsonHex.hex).jsonHex
        }
    }


    override fun eth_getTransactionCount(address: String, blockId: String): String {
        getBackendByBlockId(blockId, true).use { backend ->
            return backend.getNonce(address.jsonHex.hex).jsonHex
        }
    }

    override fun eth_getBlockTransactionCountByHash(blockHash: String): String? {
        repo.reader.use {
            return it.getBlockByHash(blockHash.jsonHex.hex)?.body?.size?.jsonHex
        }
    }


    override fun eth_getBlockTransactionCountByNumber(bnOrId: String): String? {
        repo.reader.use {
            return it.getCanonicalBlock(bnOrId.jsonHex.long)?.body?.size?.jsonHex
        }
    }

    override fun eth_getUncleCountByBlockHash(blockHash: String): String {
        return (0).jsonHex
    }

    override fun eth_getUncleCountByBlockNumber(bnOrId: String): String {
        return (0).jsonHex
    }

    override fun eth_getCode(addr: String, bnOrId: String): String {
        getBackendByBlockId(bnOrId, true).use { backend ->
            val code = backend.getCode(addr.jsonHex.hex)
            return code.jsonHex
        }
    }

    override fun eth_sign(addr: String, data: String): String {
        throw UnsupportedOperationException()
    }

    override fun eth_sendTransaction(transactionArgs: CallArguments): String {
        // for security issues, eth_sendTransaction is not disabled
        throw UnsupportedOperationException()
    }

    override fun eth_sendRawTransaction(rawData: String): String {
        val tx = Transaction.create(rawData.jsonHex.bytes)
        val errors = repo.reader.use { pool.collect(it, tx, "rpc") }
        if (errors[tx.hash] != null)
            throw RuntimeException("error transaction ${errors[tx.hash]}")
        return tx.hash.jsonHex
    }

    override fun eth_call(args: CallArguments, bnOrId: String): String {
        val start = System.currentTimeMillis()
        log.debug("eth_call start")
        try {
            getBackendByBlockId(bnOrId, false).use { backend ->
                (backend as BackendImpl).rpcCall = true
                repo.reader.use {
                    val cd = args.toCallData()
                    val executor = VMExecutor.create(
                        it,
                        backend,
                        args.toCallContext(
                            backend.getNonce(cd.caller),
                            cfg.chainId,
                            coinbase = conCfg.coinbase ?: AddrUtil.empty()
                        ),
                        cd,
                        gasLimit
                    )
                    try {
                        return executor.execute().executionResult.jsonHex
                    } catch (e: NonContractException) {
                        return "0x"
                    }
                }
            }
        } finally {
            log.debug("eth_call end")
            log.debug("eth call use " + (System.currentTimeMillis() - start) + " ms")
        }
    }

    override fun eth_estimateGas(args: CallArguments): String {
        getBackendByBlockId("latest", false).use { backend ->
            repo.reader.use { rd ->
                val callData = args.toCallData()
                val executor = VMExecutor.create(
                    rd,
                    backend,
                    args.toCallContext(
                        backend.getNonce(callData.caller),
                        cfg.chainId,
                        coinbase = conCfg.coinbase ?: AddrUtil.empty(),
                        blockHashMap = rd.createBlockHashMap(backend.parentHash).toMap()
                    ),
                    callData,
                    gasLimit
                )
                return executor.execute().gasUsed.jsonHex
            }
        }
    }

    private fun Block.result(fullTx: Boolean?): BlockResult {
        return getBlockResult(this, fullTx ?: false)
    }

    override fun eth_getBlockByHash(blockHash: String, fullTx: Boolean?): BlockResult? {
        return getByJsonBlockId(blockHash)?.result(fullTx)
    }

    override fun eth_getBlockByNumber(bnOrId: String, fullTx: Boolean?): BlockResult? {
        return getByJsonBlockId(bnOrId)?.result(fullTx)
    }


    override fun eth_getTransactionByHash(transactionHash: String): TransactionResultDTO? {
        val hash = transactionHash.jsonHex.hex
        repo.reader.use { rd ->
            val info = rd.getTransactionInfo(hash) ?: return null
            val h = rd.getHeaderByHash(info.blockHash)!!
            return TransactionResultDTO.create(h, info.i, info.tx)
        }
    }

    override fun eth_getTransactionByBlockHashAndIndex(blockHash: String, index: String): TransactionResultDTO? {
        return getTxByIndex(blockHash, index, true)
    }

    private fun getTxByIndex(bnOrId: String, index: String, hash: Boolean = false): TransactionResultDTO? {
        val json = bnOrId.jsonHex
        repo.reader.use { rd ->
            val b = if (hash) rd.getBlockByHash(json.hex) else rd.getCanonicalBlock(json.long)
            val tx = b?.body?.getOrNull(index.jsonHex.int)
            val info = tx?.hash?.let { rd.getTransactionInfo(it) }
            if (b == null || tx == null || info == null)
                return null
            return TransactionResultDTO.create(b.header, info.i, info.tx)
        }
    }

    override fun eth_getTransactionByBlockNumberAndIndex(bnOrId: String, index: String): TransactionResultDTO? {
        return getTxByIndex(bnOrId, index)
    }

    override fun eth_getTransactionReceipt(transactionHash: String): TransactionReceiptDTO? {
        val hash = transactionHash.jsonHex.hex

        val p = pool.dropped[hash]

        if (p != null) {
            pool.dropped.remove(hash)
            log.info("tx dropped info = {}", p)
            return null
        }

        repo.reader.use { rd ->
            val info = rd.getTransactionInfo(hash) ?: return null
            val b = rd.getBlockByHash(info.blockHash) ?: return null
            return TransactionReceiptDTO.create(b, info)
        }
    }

    override fun eth_getUncleByBlockHashAndIndex(blockHash: String?, uncleIdx: String?): BlockResult? {
        return null
    }

    override fun eth_getUncleByBlockNumberAndIndex(blockId: String?, uncleIdx: String?): BlockResult? {
        return null
    }

    override fun eth_newFilter(fr: JsonRpc.FilterRequest?): String? {
        return null
    }

    override fun eth_newBlockFilter(): String? {
        return null
    }

    override fun eth_newPendingTransactionFilter(): String? {
        return null
    }

    override fun eth_uninstallFilter(id: String?): Boolean {
        return false
    }

    override fun eth_getFilterChanges(id: String?): Array<Any?>? {
        return arrayOfNulls(0)
    }

    override fun eth_getFilterLogs(id: String?): Array<Any?>? {
        return arrayOfNulls(0)
    }

    fun JsonNode.addresses(): List<Address> {
        if (isArray)
            return Start.MAPPER.convertValue(this, Array<String>::class.java).map { it.address() }
        require(!isObject) { "invalid address format: $this" }
        return listOf(asText().address())
    }

    fun JsonNode.h256s(): List<H256> {
        return Start.MAPPER.convertValue(this, Array<String>::class.java).map { it.h256() }
    }

    override fun eth_getLogs(fr: JsonRpc.FilterRequest): List<Any> {
        return getLogsInternal(fr)
    }

    private fun getLogsInternal(fr: JsonRpc.FilterRequest): List<Any> {
        val addrs: List<Address>? = fr.address?.addresses()

        val topics: MutableList<List<H256>> = mutableListOf()

        fr.topics?.forEach {
            if (it == null || it.isNull) {
                return@forEach
            }
            if (it.isArray) {
                topics.add(it.h256s())
                return@forEach
            }
            if (it.isObject)
                throw RuntimeException("invalid topic $it")
            topics.add(listOf(it.asText().h256()))
        }

        val f = LogFilterV2(addrs, topics)
        val blockFrom: Block?
        val blockTo: Block?
        val logs: MutableList<JsonRpc.LogFilterElement> = mutableListOf()

        repo.reader.use { rd ->
            if (fr.blockHash != null) {
                val b =
                    rd.getBlockByHash(fr.blockHash.h256()) ?: throw RuntimeException("block ${fr.blockHash} not found")
                blockFrom = b
                val canonical = rd.getCanonicalHeader(b.height)!!
                if (b.hash != canonical.hash) {
                    throw RuntimeException("block ${fr.blockHash} is not canonical")
                }
                blockTo = blockFrom
            } else {
                blockFrom = fr.fromBlock?.let { getByJsonBlockId(it) }
                blockTo = fr.toBlock?.let { getByJsonBlockId(it) }
            }

            if (blockFrom != null) {
                val bTo = blockTo ?: rd.bestBlock

                for (i in blockFrom.height..bTo.height) {
                    val b = rd.getCanonicalBlock(i) ?: throw RuntimeException("header at $i not found")
                    f.onBlock(rd, b) { info, bk, txIdx, tx, logIdx ->
                        logs.add(JsonRpc.LogFilterElement.create(info, bk, txIdx, tx, logIdx))
                    }
                }
            }
        }
        return logs
    }

    override fun eth_getWork(): List<Any?>? {
        return null
    }

    override fun eth_submitWork(nonce: String?, header: String?, digest: String?): Boolean {
        return false
    }

    override fun eth_submitHashrate(hashrate: String?, id: String?): Boolean {
        return false
    }

    override fun personal_signAndSendTransaction(tx: CallArguments?, password: String?): String? {
        return null
    }

    private fun getBlockResult(block: Block, fullTx: Boolean): BlockResult {
        // TODO: estimate size
        val txs: MutableList<Any> = ArrayList()
        if (fullTx) {
            for (i in block.body.indices) {
                txs.add(TransactionResultDTO.create(block.header, i, block.body[i]))
            }
        } else {
            for (tx in block.body) {
                txs.add(tx.hash.jsonHex)
            }
        }

        return BlockResult(
            block.height.jsonHex, block.hash.jsonHex, block.hashPrev.jsonHex,
            block.nonce.jsonHex, block.unclesHash.jsonHex, block.logsBloom.jsonHex,
            block.transactionsRoot.jsonHex, block.stateRoot.jsonHex, block.receiptsRoot.jsonHex,
            block.coinbase.jsonHex, block.difficulty.jsonHex, BigInteger.ZERO.jsonHex,
            block.extraData.jsonHex, null, block.gasLimit.jsonHex,
            block.gasUsed.jsonHex, block.createdAt.jsonHex, txs, emptyList(),
            block.mixHash.jsonHex
        )
    }

    companion object {
        val log = LoggerFactory.getLogger("jsonrpc")
    }
}