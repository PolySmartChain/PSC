package org.tdf.sunflower.vm

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import org.tdf.common.types.Uint256
import org.tdf.common.util.*
import org.tdf.evm.EvmCallData
import org.tdf.evm.EvmContext
import org.tdf.evm.Interpreter
import org.tdf.lotusvm.Module.Companion.create
import org.tdf.lotusvm.ModuleInstance
import org.tdf.lotusvm.runtime.ResourceFactory.createMemory
import org.tdf.lotusvm.runtime.ResourceFactory.createStack
import org.tdf.sunflower.facade.RepositoryReader
import org.tdf.sunflower.state.Constants
import org.tdf.sunflower.state.LoggingContract
import org.tdf.sunflower.state.Precompiled
import org.tdf.sunflower.types.LogInfo
import org.tdf.sunflower.types.VMResult
import org.tdf.sunflower.vm.ModuleValidator.validate
import org.tdf.sunflower.vm.WBI.InjectResult
import org.tdf.sunflower.vm.WBI.dropInit
import org.tdf.sunflower.vm.WBI.extractInitData
import org.tdf.sunflower.vm.WBI.inject
import org.tdf.sunflower.vm.WBI.peek
import org.tdf.sunflower.vm.abi.Abi
import org.tdf.sunflower.vm.abi.WbiType
import org.tdf.sunflower.vm.hosts.*
import java.io.PrintStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class NonContractException(msg: String) : RuntimeException(msg)

data class VMExecutor(
        val rd: RepositoryReader,
        val backend: Backend,
        val ctx: CallContext,
        val callData: CallData,
        val limit: Limit,
        val logs: MutableList<LogInfo>,
        val depth: Int = 0,
) {
    fun fork(): VMExecutor {
        if (depth + 1 == MAX_CALL_DEPTH) throw RuntimeException("vm call depth overflow")
        return copy(depth = depth + 1)
    }

    fun execute(): VMResult {
        val n = backend.getNonce(ctx.origin)

        // no needs to increase nonce when static call
        if (!backend.staticCall && callData.callType === CallType.CALL) {
            backend.setNonce(ctx.origin, n + 1)
            // contract deploy nonce will increase in executeInternal
        }

        // 2. set initial gas by payload size
        limit.initialGas = backend.getInitialGas(callData.callType == CallType.CREATE, callData.data.bytes)

        val result = executeInternal()

        // 3. calculate fee and
        val fee = limit.totalGas.u256() * ctx.gasPrice
        backend.subBalance(ctx.origin, fee)

        return VMResult(
                limit.totalGas,
                result.hex(),
                logs,
                fee
        )
    }

    private fun isWasm(bytes: ByteArray): Boolean {
        return bytes.size >= WASM_MAGIC.size
                &&
                Arrays.equals(bytes, 0, WASM_MAGIC.size, WASM_MAGIC, 0, WASM_MAGIC.size)
    }


    fun executeInternal(): ByteArray {
        if (Precompiled.PRECOMPILED.containsKey(callData.to)) {
            val p = Precompiled.PRECOMPILED[callData.to] ?: throw UnsupportedOperationException()
            return p.execute(callData.data.bytes)
        }

        // is prebuilt
        if (backend.builtins.containsKey(callData.to)) {
            backend.addBalance(callData.to, callData.value)
            backend.subBalance(callData.caller, callData.value)
            val updater = backend.builtins[callData.to]!!
            return updater.call(rd, backend, ctx, callData)
        }
        var code: ByteArray
        // contract constructor/call arguments
        var data: ByteArray
        // if call context is evm, else web assembly
        val isWasm: Boolean
        val receiver = callData.to
        val create = callData.callType == CallType.CREATE

        if (create && backend.getCodeSize(callData.to) != 0)
            throw RuntimeException("contract collide")

        when (callData.callType) {
            CallType.CREATE -> {
                isWasm = isWasm(callData.data.bytes)

                // increase sender nonce
                val n = backend.getNonce(callData.caller)
                if (isWasm) {
                    require(WASM_ENABLED)
                    create(callData.data.bytes).use {
                        // validate module
                        validate(it, false)
                        code = dropInit(callData.data.bytes)
                        data = extractInitData(it)
                        backend.setCode(receiver, code.hex())
                    }
                } else {
                    code = callData.data.bytes
                    data = ByteUtil.EMPTY_BYTE_ARRAY
                }

                // increase nonce here to avoid conflicts
                backend.setNonce(callData.caller, n + 1)
            }
            CallType.CALL, CallType.DELEGATE -> {
                // if is debug

                val codeAddr = if (callData.callType == CallType.DELEGATE) {
                    callData.delegate
                } else {
                    receiver
                }

                val hash = backend.getContractHash(codeAddr)
                // this is a transfer transaction
                code = if (hash == HashUtil.EMPTY_DATA_HASH_HEX) {
                    HexBytes.EMPTY_BYTES
                } else {
                    CACHE[hash, { backend.getCode(codeAddr).bytes }]
                }
                data = callData.data.bytes
                isWasm = isWasm(code)
                if (isWasm)
                    require(WASM_ENABLED)
            }
        }
        // call a non-contract account
        if (code.isEmpty() && !callData.data.isEmpty()) {
            if (backend.rpcCall)
                return ByteUtil.EMPTY_BYTE_ARRAY
            throw NonContractException("call receiver ${callData.to} not a contract call data = ${callData.data}")
        }
        backend.addBalance(receiver, callData.value)
        backend.subBalance(callData.caller, callData.value)
        if (code.isEmpty()) return ByteUtil.EMPTY_BYTE_ARRAY
        if (isWasm) return executeWasm(create, code, data) else return executeEvm(create, code, data)
    }

    private fun executeEvm(create: Boolean, code: ByteArray, input: ByteArray): ByteArray {
        val evmCallData = EvmCallData(
                callData.caller.bytes,
                callData.to.bytes,
                callData.value.value,
                input,
                code
        )

        val ctx = EvmContext(
                origin = ctx.origin.bytes,
                number = backend.height,
                chainId = ctx.chainId,
                gasPrice = ctx.gasPrice.value,
                timestamp = ctx.timestamp,
                coinbase = ctx.coinbase.bytes,
                blockHashMap = ctx.blockHashMap.toMap(),
                mstore8Block = rd.genesisCfg.mstore8Block
        )

        val host = EvmHostImpl(this)
        val interpreter =
                Interpreter(host, ctx, evmCallData, printStream(callData.to), limit, EVM_MAX_STACK_SIZE, EVM_MAX_MEMORY_SIZE)

        interpreter.id = COUNTER.get()
        val ret = interpreter.execute()
        if (create) {
            backend.setCode(callData.to, ret.hex())
        }
        return if (create) ByteUtil.EMPTY_BYTE_ARRAY else ret
    }

    private fun executeWasm(create: Boolean, code: ByteArray, data: ByteArray): ByteArray {
        // transfer to a wasm contract account
        if (callData.data.isEmpty()) {
            return ByteUtil.EMPTY_BYTE_ARRAY
        }
        val dbFunctions = DBFunctions(backend, callData.to)

        val hosts = Hosts(
                context = ContextHost(callData),
                reflect = Reflect(this),
                transfer = Transfer(backend, callData.to),
                db = dbFunctions,
                u256 = U256Host()
        )


        createStack(MAX_STACK_SIZE, MAX_FRAMES, MAX_LABELS).use { stack ->
            createMemory().use { mem ->
                create(code).use { module ->
                    val instance = ModuleInstance.builder()
                            .stackAllocator(stack)
                            .module(module)
                            .memory(mem)
                            .hooks(setOf(limit))
                            .hostFunctions(hosts.all)
                            .build()

                    val abi: Abi = module.customSections
                            .firstOrNull { it.name == WBI.ABI_SECTION_NAME }
                            ?.let { Abi.fromJson(String(it.data, StandardCharsets.UTF_8)) }
                            ?: throw RuntimeException("abi not found in creation code")

                    val rets: LongArray
                    val r: InjectResult = inject(create, abi, instance, data.hex())

                    // payable check
                    val payable = r.entry != null && r.entry.isPayable

                    if (!payable && !callData.value.isZero)
                        throw RuntimeException("function " + r.name + " is not payable")

                    if (!r.executable) return ByteUtil.EMPTY_BYTE_ARRAY


                    rets = instance.execute(r.name, *r.pointers)

                    val results: MutableList<Any> = mutableListOf()

                    val outputs: List<Abi.Entry.Param> = if (create) {
                        abi.findConstructor()?.outputs
                                ?: emptyList()
                    } else {
                        abi.findFunction { it.name == r.name }.outputs!!
                    }


                    // extract result
                    for (i in rets.indices) {
                        val type = outputs[i].type
                        when (type.name) {
                            "uint8", "uint16", "uint32", "uint64" -> {
                                results.add(BigInteger.valueOf(rets[i]))
                            }
                            "uint", "uint256" -> {
                                val u = peek(instance, rets[i].toInt(), WbiType.UINT_256) as Uint256
                                results.add(u.value)
                            }
                            "string" -> {
                                val s = peek(instance, rets[i].toInt(), WbiType.STRING) as String
                                results.add(s)
                            }
                            "address" -> {
                                val addr = peek(instance, rets[i].toInt(), WbiType.ADDRESS) as HexBytes
                                results.add(addr.bytes)
                            }
                            "bool" -> results.add(rets[i] != 0L)
                            "bytes" -> results.add((peek(instance, rets[i].toInt(), WbiType.BYTES) as HexBytes).bytes)
                            else -> {
                                if (type.name.endsWith("]") || type.name.endsWith(")")) {
                                    throw RuntimeException("array or tuple is not supported")
                                }
                                if (type.name != "bytes" && type.name.startsWith("bytes")) {
                                    val bytes = peek(instance, rets[i].toInt(), WbiType.BYTES_32) as HexBytes
                                    results.add(bytes.bytes)
                                } else {
                                    throw RuntimeException("unsupported type ${type.name}")
                                }
                            }
                        }
                    }
                    return Abi.Entry.Param.encodeList(outputs, *results.toTypedArray())
                }
            }
        }
    }

    companion object {
        const val MAX_FRAMES = 16384
        const val MAX_STACK_SIZE = MAX_FRAMES * 4
        const val MAX_LABELS = MAX_FRAMES * 4
        const val MAX_CALL_DEPTH = 8
        const val EVM_MAX_STACK_SIZE = 1024
        const val GAS_UNLIMITED = Long.MAX_VALUE.shr(8)
        const val WASM_ENABLED = false

        fun create(
                rd: RepositoryReader,
                backend: Backend,
                ctx: CallContext,
                callData: CallData,
                gasLimit: Long
        ): VMExecutor {
            var c = callData
            val n = backend.getNonce(ctx.origin)
            if (n != ctx.txNonce)
                throw RuntimeException("invalid nonce")
            if (callData.callType == CallType.CREATE)
                c = callData.copy(
                        to = HashUtil.calcNewAddr(
                                callData.caller.bytes,
                                ctx.txNonce.bytes()
                        ).hex()
                )
            return VMExecutor(rd, backend, ctx, c, Limit(gasLimit), mutableListOf())
        }

        // 16 mb
        const val EVM_MAX_MEMORY_SIZE = 1024 * 1024 * 16

        val CACHE: Cache<HexBytes, ByteArray> = CacheBuilder
                .newBuilder()
                .weigher { k: Any, v: Any -> (v as ByteArray).size + (k as HexBytes).size }
                .maximumWeight(1024L * 1024L * 8L) // 8mb cache for contracts
                .build()

        private val COUNTER = AtomicInteger()
        private val WASM_MAGIC = byteArrayOf(0x00, 0x61, 0x73, 0x6d)
        private val log = LoggerFactory.getLogger("vm")
        private var outDirectory = ""

        fun enableDebug(outDirectory: String) {
            this.outDirectory = outDirectory
        }

        private fun printStream(a: HexBytes): PrintStream? {
            if (outDirectory.isEmpty())
                return null
            val filename = String.format("%d.log", COUNTER.incrementAndGet())
            val dir = Paths.get(outDirectory, "0x" + a.hex).toFile()
            if (!dir.exists()) {
                dir.mkdir()
            }
            val path = Paths.get(outDirectory, "0x" + a.hex, filename)
            log.info("write vm debug log to file {}", path)
            val os = Files.newOutputStream(
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )
            return PrintStream(os)
        }
    }
}