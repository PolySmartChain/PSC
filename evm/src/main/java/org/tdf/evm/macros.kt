package org.tdf.evm

import java.math.BigInteger
import java.nio.charset.StandardCharsets

internal fun String.ascii(): ByteArray {
    return this.toByteArray(StandardCharsets.US_ASCII)
}

internal fun ByteArray.int(off: Int, len: Int): Int {
    return BigInteger(this.sliceArray(off until off + len)).intValueExact()
}

