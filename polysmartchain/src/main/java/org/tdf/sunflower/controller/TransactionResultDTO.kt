package org.tdf.sunflower.controller


import org.tdf.sunflower.types.*

data class TransactionResultDTO(
    val hash: String,
    val nonce: String,
    val blockHash: String?,
    val blockNumber: String?,
    val transactionIndex: String?,
    val from: String,
    val to: String?,
    val gas: String,
    val gasPrice: String,
    val value: String,
    val input: String?,
    val v: String?,
    val r: String?,
    var s: String?,
) {

    companion object {
        fun create(h: Header, index: Int, tx: Transaction): TransactionResultDTO {
            return TransactionResultDTO(
                tx.hash.jsonHex,
                tx.nonce.jsonHex,
                h.hash.jsonHex,
                h.height.jsonHex,
                index.jsonHex,
                tx.sender.jsonHex,
                tx.to.takeIf { !it.isEmpty() }?.jsonHex,
                tx.gasLimit.jsonHex,
                tx.gasPrice.jsonHex,
                tx.value.jsonHex,
                tx.data.jsonHex,
                tx.vrs?.v?.jsonHex,
                tx.vrs?.r?.jsonHex,
                tx.vrs?.s?.jsonHex
            )
        }
    }
}