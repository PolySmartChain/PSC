package org.tdf.sunflower.facade

import org.tdf.sunflower.types.Block
import org.tdf.sunflower.types.ValidateResult
import org.tdf.sunflower.types.ValidateResult.Companion.success

/**
 * validate blocks
 */
interface BlockValidator {
    /**
     * validate the block
     *
     * @param block      the block to validate
     * @param dependency parent block
     * @return validate result
     */
    fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult
}


interface Validator : BlockValidator {
    companion object {
        val NONE: Validator = object : Validator {
            override fun validate(rd: RepositoryReader, block: Block, dependency: Block): ValidateResult {
                return success()
            }
        }
    }
}