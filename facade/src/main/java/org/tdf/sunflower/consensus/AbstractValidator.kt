package org.tdf.sunflower.consensus

import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.common.util.min
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.facade.TransactionInfo
import org.tdf.sunflower.facade.Validator
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.state.AddrUtil
import org.tdf.sunflower.state.StateTrie
import org.tdf.sunflower.types.*
import org.tdf.sunflower.types.BlockValidateResult.Companion.fault
import org.tdf.sunflower.types.BlockValidateResult.Companion.success
import org.tdf.sunflower.vm.CallContext
import org.tdf.sunflower.vm.CallData
import org.tdf.sunflower.vm.VMExecutor

abstract class AbstractValidator(protected val accountTrie: StateTrie<HexBytes, Account>) : Validator {
    abstract val chainId: Int

    protected fun commonValidate(rd: RepositoryReader, block: Block, parent: Block): BlockValidateResult {
        block.validate().let {
            if (!it.success)
                return fault(it.reason)
        }

        // a block should contains exactly one coin base transaction
        if (block.body.isEmpty()) return fault("missing block body")

        if (block.gasLimit != rd.genesis.gasLimit)
            return fault("invalid block gas limit")

        var isCoinbase = true
        for (t in block.body) {
            // validate transaction signature
            try {
                t.validate()
            } catch (e: Exception) {
                return fault(e.message!!)
            }
            if (!isCoinbase && !t.verifySig)
                return fault("verify signature failed")

            if (!isCoinbase && t.chainId != chainId)
                return fault("invalid chainId ${t.chainId}")

            isCoinbase = false
        }
        if (!parent.isParentOf(block) || parent.height + 1 != block.height) {
            return fault("dependency is not parent of block")
        }
        if (parent.createdAt >= block.createdAt) {
            return fault(String.format("invalid timestamp %d at block height %d", block.createdAt, block.height))
        }
        if (Transaction.calcTxTrie(block.body) != block.transactionsRoot) {
            return fault("transactions root not match")
        }
        var totalFee = Uint256.ZERO
        val results: MutableMap<HexBytes, VMResult> = mutableMapOf()
        val currentBackend = accountTrie.createBackend(parent = parent.header)
        var currentGas: Long = 0
        val receipts: MutableList<TransactionReceipt> = mutableListOf()
        try {
            val coinbase = block.body[0]
            for (tx in block.body.subList(1, block.body.size)) {
                if (currentGas > block.gasLimit) return fault("block gas overflow")
                var r: VMResult
                val executor = VMExecutor.create(
                    rd,
                    currentBackend,
                    CallContext.fromTx(tx, timestamp = block.createdAt, coinbase = block.coinbase, blockHashMap = rd.createBlockHashMap(block.hashPrev).toMap()),
                    CallData.fromTx(tx),
                    (block.gasLimit - currentGas).min(tx.gasLimit)
                )
                r = executor.execute()
                results[tx.hash] = r
                totalFee += r.fee
                currentGas += r.gasUsed
                val receipt = TransactionReceipt(
                    cumulativeGas = currentGas,
                    logInfoList = r.logs,
                    gasUsed = r.gasUsed,
                    result = r.executionResult
                )
                receipts.add(receipt)
            }

            val executor =
                VMExecutor.create(
                    rd,
                    currentBackend,
                    CallContext.fromTx(coinbase, chainId, block.createdAt, block.coinbase, rd.createBlockHashMap(block.hashPrev).toMap()),
                    CallData.fromTx(coinbase),
                    coinbase.gasLimit
                )


            // assert fee
            if (totalFee != coinbase.value) {
                return fault("total fee $totalFee != coinbase value ${coinbase.value}")
            }

            currentBackend.addBalance(AddrUtil.empty(), totalFee)
            val r = executor.execute()

            currentGas += r.gasUsed

            val receipt = TransactionReceipt(
                cumulativeGas = currentGas,
                logInfoList = r.logs,
                gasUsed = r.gasUsed,
                result = r.executionResult
            )

            receipts.add(0, receipt)
            val rootHash = currentBackend.merge()
            if (rootHash != block.stateRoot) {
                return fault("state trie root not match")
            }

            // validate receipt trie root
            if (TransactionReceipt.calcTrie(receipts) != block.receiptsRoot) {
                return fault("receipts trie root not match")
            }

            // validate bloom
            if (TransactionReceipt.bloomOf(receipts).data.hex() != block.logsBloom)
                return fault("logs bloom not match")

        } catch (e: Exception) {
            e.printStackTrace()
            return fault("contract evaluation failed or " + e.message)
        }
        val indices: MutableList<TransactionInfo> = mutableListOf()
        for (i in receipts.indices) {
            indices.add(
                TransactionInfo(TransactionIndex(receipts[i], block.hash, i), block.body[i])
            )
        }
        return success(currentGas, totalFee, results, indices)
    }
}