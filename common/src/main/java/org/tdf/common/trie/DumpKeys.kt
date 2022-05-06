package org.tdf.common.trie

import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex

internal class DumpKeys : ScannerAction {
    internal val keys: MutableSet<HexBytes> = HashSet()

    override fun scan(path: TrieKey, node: Node): Boolean {
        if (node.hash != null) {
            keys.add(node.hash.hex())
        }
        return true
    }
}