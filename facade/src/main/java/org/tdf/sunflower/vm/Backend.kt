package org.tdf.sunflower.vm

import org.tdf.common.store.Store
import org.tdf.common.types.Uint256
import org.tdf.common.util.Address
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.state.Builtin
import org.tdf.sunflower.state.Precompiled

interface Backend : AutoCloseable {
    fun subBalance(addr: HexBytes, amount: Uint256) {
        if (getBalance(addr) < amount) throw RuntimeException(
            String.format(
                "balance of account %s less than %s",
                addr,
                amount.value.toString(10)
            )
        )
        setBalance(addr, getBalance(addr) - amount)
    }

    fun addBalance(addr: HexBytes, amount: Uint256) {
        setBalance(addr, getBalance(addr) + amount)
    }

    fun getContractHash(address: HexBytes): HexBytes
    val height: Long
    val root: Backend
    val trieRoot: HexBytes
    val parentHash: HexBytes
    fun getBalance(address: HexBytes): Uint256

    fun getCodeSize(address: Address): Int {
        val b = (bios[address] ?: builtins[address]) ?: return getCode(address).size
        return b.codeSize
    }

    // only used for bios contract
    fun setBalance(address: HexBytes, balance: Uint256)
    fun getNonce(address: HexBytes): Long
    fun setNonce(address: HexBytes, nonce: Long)
    fun getInitialGas(create: Boolean, data: ByteArray): Long {
        var gas = if (create) {
            GasConfig.TRANSACTION_CREATE_CONTRACT
        } else {
            GasConfig.TRANSACTION
        }
        val zero: Byte = 0
        var i = 0;
        while (i < data.size) {
            gas += if (data[i] == zero) {
                GasConfig.TX_ZERO_DATA
            } else {
                GasConfig.TX_NO_ZERO_DATA
            }
            i++
        }
        if (gas < 0)
            throw RuntimeException("gas overflow")
        return gas
    }

    val builtins: Map<HexBytes, Builtin>
    val bios: Map<HexBytes, Builtin>
    fun dbSet(address: HexBytes, key: HexBytes, value: HexBytes)

    // return empty byte array if key not found
    fun dbGet(address: HexBytes, key: HexBytes): HexBytes
    fun dbHas(address: HexBytes, key: HexBytes): Boolean

    fun dbRemove(address: HexBytes, key: HexBytes)
    fun getCode(address: HexBytes): HexBytes
    fun setCode(address: HexBytes, code: HexBytes)
    val staticCall: Boolean
    val rpcCall: Boolean
    val parentBackend: Backend?
    fun createChild(staticCall: Boolean = false): Backend

    // merge modifications, return the new state root
    fun merge(): HexBytes


    fun getAsStore(address: HexBytes): Store<HexBytes, HexBytes> {
        return object : Store<HexBytes, HexBytes> {
            override operator fun get(k: HexBytes): HexBytes {
                return dbGet(address, k)
            }

            override fun set(k: HexBytes, v: HexBytes) {
                dbSet(address, k, v)
            }

            override fun remove(k: HexBytes) {
                dbRemove(address, k)
            }

            override fun flush() {
                throw RuntimeException("not implemented")
            }
        }
    }
}