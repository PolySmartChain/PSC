package org.tdf.sunflower.controller

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.controller.JsonRpcUtil.toCallData
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.LogInfo
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData

interface JsonRpc {
    fun web3_clientVersion(): String

    fun web3_sha3(data: String): String
    fun net_version(): String
    fun eth_chainId(): String
    fun net_peerCount(): String
    fun net_listening(): Boolean
    fun eth_protocolVersion(): String
    fun eth_syncing(): Boolean
    fun eth_coinbase(): String
    fun eth_mining(): Boolean
    fun eth_hashrate(): String
    fun eth_gasPrice(): String
    fun eth_accounts(): Array<String>
    fun eth_blockNumber(): String

    fun eth_getBalance(address: String, block: String): String

    fun eth_getStorageAt(address: String, storageIdx: String, blockId: String): String

    fun eth_getTransactionCount(address: String, blockId: String): String

    fun eth_getBlockTransactionCountByHash(blockHash: String): String?

    fun eth_getBlockTransactionCountByNumber(bnOrId: String): String?

    fun eth_getUncleCountByBlockHash(blockHash: String): String

    fun eth_getUncleCountByBlockNumber(bnOrId: String): String

    fun eth_getCode(addr: String, bnOrId: String): String

    fun eth_sign(addr: String, data: String): String

    fun eth_sendTransaction(transactionArgs: CallArguments): String

    fun eth_sendRawTransaction(rawData: String): String

    fun eth_call(args: CallArguments, bnOrId: String): String

    fun eth_estimateGas(args: CallArguments): String

    fun eth_getBlockByHash(blockHash: String, fullTx: Boolean?): BlockResult?

    fun eth_getBlockByNumber(bnOrId: String, fullTx: Boolean?): BlockResult?


    fun eth_getTransactionByHash(transactionHash: String): TransactionResultDTO?


    fun eth_getTransactionByBlockHashAndIndex(blockHash: String, index: String): TransactionResultDTO?


    fun eth_getTransactionByBlockNumberAndIndex(bnOrId: String, index: String): TransactionResultDTO?


    fun eth_getTransactionReceipt(transactionHash: String): TransactionReceiptDTO?


    fun eth_getUncleByBlockHashAndIndex(blockHash: String?, uncleIdx: String?): BlockResult?


    fun eth_getUncleByBlockNumberAndIndex(blockId: String?, uncleIdx: String?): BlockResult?


    fun eth_newFilter(fr: FilterRequest?): String?
    fun eth_newBlockFilter(): String?
    fun eth_newPendingTransactionFilter(): String?
    fun eth_uninstallFilter(id: String?): Boolean
    fun eth_getFilterChanges(id: String?): Array<Any?>?
    fun eth_getFilterLogs(id: String?): Array<Any?>?


    fun eth_getLogs(fr: FilterRequest): List<Any>

    //    String eth_resend();
    //    String eth_pendingTransactions();
    fun eth_getWork(): List<Any?>?

    //    String eth_newFilter(String fromBlock, String toBlock, String address, String[] topics) throws Exception;

    fun eth_submitWork(nonce: String?, header: String?, digest: String?): Boolean
    fun eth_submitHashrate(hashrate: String?, id: String?): Boolean
    fun personal_signAndSendTransaction(tx: CallArguments?, password: String?): String?

    data class CallArguments(
        val from: String? = null,
        val to: String? = null,
        val gas: String? = null,
        val gasPrice: String? = null,
        val value: String? = null,
        val data // compiledCode
        : String? = null,
        val nonce: String? = null,
    ) {
        @JsonIgnore
        fun toCallContext(
            nonce: Long?,
            chainId: Int,
            timestamp: Long = System.currentTimeMillis() / 1000,
            coinbase: HexBytes = AddrUtil.empty(),
            blockHashMap: Map<Long, ByteArray> = emptyMap()
        ): CallContext {
            return JsonRpcUtil.toCallContext(this, nonce, chainId, timestamp, coinbase, blockHashMap)
        }

        @JsonIgnore
        fun toCallData(): CallData {
            return toCallData(this)
        }
    }

    data class BlockResult(
        val number // QUANTITY - the block number. null when its pending block.
        : String? = null,

        val hash // DATA, 32 Bytes - hash of the block. null when its pending block.
        : String? = null,

        val parentHash // DATA, 32 Bytes - hash of the parent block.
        : String? = null,

        val nonce // DATA, 8 Bytes - hash of the generated proof-of-work. null when its pending block.
        : String? = null,

        val sha3Uncles // DATA, 32 Bytes - SHA3 of the uncles data in the block.
        : String? = null,

        val logsBloom // DATA, 256 Bytes - the bloom filter for the logs of the block. null when its pending block.
        : String? = null,

        val transactionsRoot // DATA, 32 Bytes - the root of the transaction trie of the block.
        : String? = null,

        val stateRoot // DATA, 32 Bytes - the root of the final state trie of the block.
        : String? = null,
        val receiptsRoot // DATA, 32 Bytes - the root of the receipts trie of the block.
        : String? = null,
        val miner // DATA, 20 Bytes - the address of the beneficiary to whom the mining rewards were given.
        : String? = null,
        val difficulty // QUANTITY - integer of the difficulty for this block.
        : String? = null,
        val totalDifficulty // QUANTITY - integer of the total difficulty of the chain until this block.
        : String? = null,
        val extraData // DATA - the "extra data" field of this block
        : String? = null,
        val size //QUANTITY - integer the size of this block in bytes.
        : String? = null,
        val gasLimit //: QUANTITY - the maximum gas allowed in this block.
        : String? = null,
        val gasUsed // QUANTITY - the total used gas by all transactions in this block.
        : String? = null,
        val timestamp //: QUANTITY - the unix timestamp for when the block was collated.
        : String? = null,
        val transactions //: Array - Array of transaction objects, or 32 Bytes transaction hashes depending on the last given parameter.
        : List<Any>? = null,
        val uncles //: Array - Array of uncle hashes.
        : List<String>? = null,

        var mixHash: String? = null,
    )


    data class FilterRequest(
        val fromBlock: String? = null,
        val toBlock: String? = null,
        val address: JsonNode? = null,
        val topics: List<JsonNode?>? = null,
        val blockHash // EIP-234: makes fromBlock = toBlock = blockHash
        : String? = null,
    )

    data class LogFilterElement(
        val logIndex: String? = null,
        val transactionIndex: String? = null,
        val transactionHash: String? = null,
        val blockHash: String? = null,
        val blockNumber: String? = null,
        val address: String? = null,
        val data: String? = null,
        val topics: List<String>? = null
    ) {
        companion object {
            fun create(info: LogInfo, b: Block?, txIndex: Int?, tx: Transaction, logIdx: Int): LogFilterElement {
                return LogFilterElement(
                    logIdx.jsonHex,
                    txIndex?.jsonHex,
                    tx.hash.jsonHex,
                    b?.hash?.jsonHex,
                    b?.height?.jsonHex,
                    tx.to.takeIf { !it.isEmpty() }?.jsonHex ?: tx.contractAddress?.jsonHex,
                    info.data.jsonHex,
                    info.topics.map { it.jsonHex }
                )
            }
        }
    }
}