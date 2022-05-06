package org.tdf.sunflower

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.facade.PropertiesWrapper
import org.tdf.sunflower.types.PropertyReader
import java.util.*

@ConfigurationProperties(prefix = "sunflower.sync")
@Component
class SyncConfigProperties : Properties()

@Component
class SyncConfig(properties: SyncConfigProperties) {
    val rd = PropertyReader(PropertiesWrapper(properties))
    val heartRate: Long = rd.getAsLong("heart-rate")
    val blockWriteRate: Long = rd.getAsLong("block-write-rate")
    val maxPendingBlocks: Long = rd.getAsLong("max-pending-blocks")
    val maxBlocksTransfer: Int = rd.getAsInt("max-blocks-transfer")
    val pruneHash: HexBytes? = rd.getAsNonNull("prune-hash").takeIf { it.isNotEmpty() }?.hex()
    val fastSyncHeight: Long = rd.getAsLong("fast-sync-height", 0)
    val fastSyncHash: HexBytes? = rd.getAsNonNull("fast-sync-hash").takeIf { it.isNotEmpty() }?.hex()
    val maxAccountsTransfer: Int = rd.getAsInt("max-accounts-transfer")
    val maxHistoryBlocks: Int = rd.getAsInt("max-history-blocks")
    val statusLimit: Long = rd.getAsLong("rate-limits.status", 0)
    val blocksLimit: Long = rd.getAsLong("rate-limits.get-blocks", 0)
}