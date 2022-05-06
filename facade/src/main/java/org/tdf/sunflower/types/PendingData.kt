package org.tdf.sunflower.types

import org.tdf.sunflower.vm.Backend

data class PendingData(
    val pending: List<Transaction>,
    val receipts: List<TransactionReceipt>,
    val current: Backend?,
)