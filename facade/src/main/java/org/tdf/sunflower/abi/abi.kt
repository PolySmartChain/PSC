package org.tdf.sunflower.abi

import org.tdf.common.util.*
import java.math.BigInteger

internal fun String.selector(): Int {
    return BigEndian.decodeInt32(this.ascii().sha3(), 0)
}

sealed interface Union {
    fun asBn(): BigInteger {
        throw UnsupportedOperationException()
    }

    fun asAddress(): Address {
        throw UnsupportedOperationException()
    }

    fun asBytes(): HexBytes {
        throw UnsupportedOperationException()
    }

    fun asArray(): Array<Union> {
        throw UnsupportedOperationException()
    }

    companion object {
        fun bn(b: BigInteger) = Bn(b)
        fun address(addr: Address) = Addr(addr)
        fun bytes(bytes: HexBytes) = Bytes(bytes)
        fun array(a: Array<Union>) = Arr(a)
    }

    @JvmInline
    value class Bn(val b: BigInteger) : Union {
        override fun asBn(): BigInteger {
            return b
        }
    }

    @JvmInline
    value class Addr(val a: Address) : Union {
        override fun asAddress(): Address {
            return a
        }
    }

    @JvmInline
    value class Bytes(val b: HexBytes) : Union {
        override fun asBytes(): HexBytes {
            return b
        }
    }

    @JvmInline
    value class Arr(val a: Array<Union>) : Union {
        override fun asArray(): Array<Union> {
            return a
        }
    }
}


sealed class SolidityType {
    abstract val name: String
}

