package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "sunflower.transaction-pool")
@Component
data class TransactionPoolConfig(
    var expiredIn: Long = 0,
    var lockTimeout: Long = 0,
)