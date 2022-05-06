package org.tdf.common.types

import org.tdf.common.util.HexBytes

interface Hashed {
    val hash: HexBytes
}