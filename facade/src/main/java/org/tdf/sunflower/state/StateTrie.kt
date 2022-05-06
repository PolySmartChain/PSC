package org.tdf.sunflower.state

import org.tdf.common.store.Store
import org.tdf.common.trie.Trie
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.types.Header
import org.tdf.sunflower.vm.*
import org.tdf.sunflower.vm.abi.Abi

/**
 * state storage
 *
 * @param <ID> identifier of state
 * @param <S>  state
</S></ID> */
interface StateTrie<ID, S> {
    // get an optional state at a root hash
    fun get(rootHash: HexBytes, id: ID): S?

    // init genesis states
    fun init(
        alloc: Map<HexBytes, Account>,
        bios: List<Builtin>,
        builtins: List<Builtin>,
        consensusCode: Map<HexBytes, HexBytes>,
        rd: RepositoryReader
    ): HexBytes

    val trie: Trie<ID, S>

    fun getTrie(rootHash: HexBytes): Trie<ID, S>

    val trieStore: Store<ByteArray, ByteArray>

    fun createBackend(
        parent: Header,
        staticCall: Boolean = false,
        root: HexBytes = parent.stateRoot,
    ): Backend

    fun createWrapper(
        rd: RepositoryReader,
        parent: Header,
        abi: Abi,
        addr: HexBytes,
        root: HexBytes = parent.stateRoot,
    ): ContractWrapper {
        return ContractWrapperImpl(addr, parent, abi, rd, this as StateTrie<HexBytes, Account>)
    }
}


interface ContractWrapper {
    fun call(method: String, vararg args: Any): List<*>
}

internal class ContractWrapperImpl(
    val addr: HexBytes,
    val parent: Header,
    val abi: Abi,
    val rd: RepositoryReader,
    val trie: StateTrie<HexBytes, Account>
) : ContractWrapper {
    override fun call(method: String, vararg args: Any): List<*> {
        val f = abi.filter { it.name == method && it is Abi.Function }
        require(f.size == 1) {
            "${f.size} functions found of name $method"
        }

        val encoded = f[0].encodeSignature() + Abi.Entry.Param.encodeList(f[0].inputs, *args)
        val backend = trie.createBackend(parent, true)
        val cd = CallData(to = addr, callType = CallType.CALL, data = encoded.hex())
        val ex = VMExecutor.create(rd, backend, CallContext(), cd, VMExecutor.GAS_UNLIMITED)
        val r = ex.execute()
        return Abi.Entry.Param.decodeList(f[0].outputs, r.executionResult.bytes)
    }

}