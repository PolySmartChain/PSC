package org.tdf.common.store

data class DBSettings(
    val maxOpenFiles: Int = 32,
    val maxThreads: Int = 1
) {
    companion object {
        @JvmField
        val DEFAULT = DBSettings()
    }
}