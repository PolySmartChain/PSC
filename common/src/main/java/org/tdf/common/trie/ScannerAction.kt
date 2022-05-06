package org.tdf.common.trie

internal fun interface ScannerAction {
    fun scan(path: TrieKey, node: Node): Boolean
}