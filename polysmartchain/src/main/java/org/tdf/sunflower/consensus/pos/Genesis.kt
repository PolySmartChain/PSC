package org.tdf.sunflower.consensus.pos

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import org.tdf.sunflower.types.AbstractGenesis
import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.HeaderImpl

class Genesis(parsed: JsonNode) : AbstractGenesis(parsed) {
    override val block: Block
        @JsonIgnore
        get() {
            val h = HeaderImpl(gasLimit = gasLimit, createdAt = timestamp)
            return Block(h)
        }
}