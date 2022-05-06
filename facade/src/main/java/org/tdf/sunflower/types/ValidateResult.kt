package org.tdf.sunflower.types


open class ValidateResult(val success: Boolean, val reason: String) {
    companion object {
        private val SUCCESS = ValidateResult(true, "")

        @JvmStatic
        fun success(): ValidateResult {
            return SUCCESS
        }

        @JvmStatic
        fun fault(reason: String): ValidateResult {
            return ValidateResult(false, reason)
        }
    }
}