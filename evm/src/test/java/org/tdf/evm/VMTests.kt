package org.tdf.evm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.spongycastle.util.encoders.Hex
import org.tdf.common.util.*
import org.tdf.evm.address
import org.tdf.sunflower.vm.abi.Abi
import java.io.PrintStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicInteger

val ZERO_ADDRESS = ByteArray(20)

val sha3 = Digest { src: ByteArray, srcPos: Int, srcLen: Int,
                    dst: ByteArray, dstPos: Int ->
    HashUtil.sha3(src, srcPos, srcLen, dst, dstPos)
}

internal fun ByteArray.address(): ByteArray {
    return this.sliceArray((this.size - 20) until this.size)
}

class MemAccount(
    var nonce: Long = 0,
    var balance: BigInteger = BigInteger.ZERO,
    val storage: MutableMap<HexBytes, HexBytes> = mutableMapOf(),
    var code: ByteArray = ByteUtil.EMPTY_BYTE_ARRAY
)


class MockEvmHost : EvmHost {
    override val digest: Digest = sha3
    private val accounts: MutableMap<HexBytes, MemAccount> = mutableMapOf()
    private val cnt = AtomicInteger(0)

    private fun getLogFile(): PrintStream {
        val name = String.format("%04d.log", cnt.incrementAndGet())
        return PrintStream(
            Files.newOutputStream(
                Paths.get(System.getProperty("user.dir"), "logs", name),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        )
    }

    override fun getBalance(address: ByteArray): BigInteger {
        return accounts[HexBytes.fromBytes(address)]?.balance ?: BigInteger.ZERO
    }

    override fun getStorage(address: ByteArray, key: ByteArray): ByteArray {
        return accounts[HexBytes.fromBytes(address)]
            ?.storage?.let {
                it[HexBytes.fromBytes(key)]
            }?.bytes ?: HexBytes.EMPTY_BYTES
    }

    override fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray) {
        val account = accounts[HexBytes.fromBytes(address)] ?: MemAccount()
        account.storage[HexBytes.fromBytes(key)] = HexBytes.fromBytes(value)
        accounts[HexBytes.fromBytes(address)] = account
    }

    override fun getCode(addr: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getCodeSize(addr: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun call(
        caller: ByteArray,
        receipt: ByteArray,
        input: ByteArray,
        // when static call = true, value is always zero
        value: BigInteger,
        staticCall: Boolean,
    ): ByteArray {
        val contract = getOrCreate(receipt)
        if (contract.code.isEmpty())
            throw RuntimeException("not a contract account")

        val callData = EvmCallData(caller, receipt, value, input, contract.code)

        val interpreter = Interpreter(
            this,
            EvmContext(),
            callData,
            getLogFile()
        )

        return interpreter.execute()
    }

    override fun delegate(
        originCaller: ByteArray,
        originContract: ByteArray,
        delegateAddr: ByteArray,
        input: ByteArray
    ): ByteArray {
        TODO("Not yet implemented")
    }

    private fun getOrCreate(address: ByteArray): MemAccount {
        val r = accounts[HexBytes.fromBytes(address)]
        if (r != null)
            return r

        val a = MemAccount()
        accounts[HexBytes.fromBytes(address)] = a
        return a
    }

    override fun drop(address: ByteArray) {
    }

    override fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray, salt: ByteArray?): ByteArray {
        val cal = getOrCreate(caller)
        val nonce = cal.nonce
        cal.nonce++
        val newAddr = HashUtil.calcNewAddr(caller, ByteUtil.longToBytesNoLeadZeroes(nonce))

        val interpreter = Interpreter(
            this,
            EvmContext(),
            EvmCallData(caller, newAddr, value, emptyByteArray, createCode),
            getLogFile()
        )

        val con = getOrCreate(newAddr)
        con.code = interpreter.execute()
        return newAddr
    }



    override fun log(contract: ByteArray, data: ByteArray, topics: List<ByteArray>) {
    }
}

@RunWith(JUnit4::class)
class VMTests {

    @Test
    fun test0() {
        val code = intArrayOf(
            OpCodes.PUSH1, 10, OpCodes.PUSH1, 0, OpCodes.MSTORE, OpCodes.PUSH1, 32, OpCodes.PUSH1, 0, OpCodes.RETURN
        ).map { it.toByte() }.toByteArray()

        val ctx = EvmContext()
        val data = EvmCallData(code = code)
        val mock = MockEvmHost()
        val executor = Interpreter(mock, ctx, data)
        assert(BigInteger(1, executor.execute()).intValueExact() == 10)
    }

    @Test
    fun testOwnerContract() {
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        val codeJson = TestUtil.readClassPathFile("contracts/owner.json")
        val node = objectMapper.readValue(codeJson, JsonNode::class.java)

        val code = Hex.decode(node.get("object").asText())
        val owner = Hex.decode("8bb3194c582a9f70bdc079e4c20cde4a7fc3c807")
        val mock = MockEvmHost()

        val con = mock.create(owner, BigInteger.ZERO, code)

        val r = mock.call(ZERO_ADDRESS, con, Hex.decode("893d20e8"))

        assertEquals(Hex.toHexString(owner), Hex.toHexString(r.address()))
    }


    @Test
    fun testHashContract() {
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        val codeJson = TestUtil.readClassPathFile("contracts/Hasher.json")
        val node = objectMapper.readValue(codeJson, JsonNode::class.java)
        val abi = Abi.fromJson(node["abi"].toString())
        val code = node.get("bytecode").asText().hex()
        val owner = "8bb3194c582a9f70bdc079e4c20cde4a7fc3c807".hex()
        val mock = MockEvmHost()

        val con = mock.create(owner.bytes, BigInteger.ZERO, code.bytes)
        val input = abi.findFunction { it.name == "MiMCSponge" }.encode(
            "21663839004416932945382355908790599225266501822907911457504978515578255421292".bn(),
            "0".bn()
        )
        val r = mock.call(ZERO_ADDRESS, con, input, BigInteger.ZERO, true)
        println(r.hex())
    }
}