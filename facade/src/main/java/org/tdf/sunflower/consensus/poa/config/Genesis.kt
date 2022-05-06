package org.tdf.sunflower.consensus.poa.config

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.HeaderImpl

class Genesis(parsed: JsonNode) : AbstractGenesis(parsed) {
    private fun getAddressList(field: String): List<HexBytes> {
        return getArray(field)
            .map { it["addr"].asText().hex() }
    }

    val miners: List<HexBytes>
        get() = getAddressList("miners")

    override val block: Block
        @JsonIgnore
        get() {
            val h = HeaderImpl(gasLimit = gasLimit, createdAt = timestamp)
            return Block(h)
        }
}