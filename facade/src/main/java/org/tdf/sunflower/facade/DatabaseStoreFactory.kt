package org.tdf.sunflower.facade

import org.tdf.common.store.Store

interface DatabaseStoreFactory {
    val directory: String
    fun create(prefix: Char, comment: String = ""): Store<ByteArray, ByteArray>
    fun cleanup() {}
    val name: String?
}