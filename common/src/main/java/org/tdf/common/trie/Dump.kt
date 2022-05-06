package org.tdf.common.trie

import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex

internal class Dump : ScannerAction {
    internal val pairs: MutableMap<HexBytes, HexBytes> = HashMap()

    override fun scan(path: TrieKey, node: Node): Boolean {
        if (node.hash != null) {
            pairs[node.hash.hex()] = node.rlp.hex()
        }
        return true
    }
}