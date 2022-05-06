package org.tdf.evm

import java.io.PrintStream
import java.math.BigInteger
import kotlin.math.min

internal val emptyByteArray = ByteArray(0)
internal val emptyAddress = ByteArray(20)

class EvmContext(
    // transaction.sender
    val origin: ByteArray = emptyAddress,

    // current block number
    val number: Long = 0,

    // chain id
    val chainId: Int = 0,
    val difficulty: BigInteger = BigInteger.ZERO,
    // gas price
    val gasPrice: BigInteger = BigInteger.ZERO,
    val timestamp: Long = 0,
    val coinbase: ByteArray = emptyAddress,
    val blockHashMap: Map<Long, ByteArray> = emptyMap(),
    val mstore8Block: Long? = null
)

/**
 * for transaction deploy
 */
class EvmCallData(
    val caller: ByteArray = emptyAddress,
    val receipt: ByteArray = emptyAddress,
    val value: BigInteger = BigInteger.ZERO,
    val input: ByteArray = emptyByteArray,
    val code: ByteArray = emptyByteArray,
)

interface EvmHost {
    val digest: Digest

    // get balance of address, address.length = 20
    fun getBalance(address: ByteArray): BigInteger

    fun getStorage(address: ByteArray, key: ByteArray): ByteArray
    fun setStorage(address: ByteArray, key: ByteArray, value: ByteArray)

    fun getCode(addr: ByteArray): ByteArray

    fun getCodeSize(addr: ByteArray): Int

    fun call(
        caller: ByteArray,
        receipt: ByteArray,
        input: ByteArray,
        // when static call = true, value is always zero
        value: BigInteger = BigInteger.ZERO,
        staticCall: Boolean = false
    ): ByteArray

    fun delegate(
        originCaller: ByteArray,
        originContract: ByteArray,
        delegateAddr: ByteArray,
        input: ByteArray,
    ): ByteArray

    fun drop(address: ByteArray)

    /**
     * create contract, return the address of new contract, execute create2 when salt is nonnull
     */
    fun create(caller: ByteArray, value: BigInteger, createCode: ByteArray, salt: ByteArray? = null): ByteArray


    fun log(contract: ByteArray, data: ByteArray, topics: List<ByteArray>)
}

fun interface EvmHook {
    fun onOp(op: Int, extraInfo: Long)
}

class Interpreter(
    val host: EvmHost,
    val ctx: EvmContext,
    val callData: EvmCallData,
    private val vmLog: PrintStream? = null,
    private val hook: EvmHook? = null,
    maxStackSize: Int = Int.MAX_VALUE,
    maxMemorySize: Int = Int.MAX_VALUE,
    var id: Int = 0,
) {
    var pc: Int = 0
    private val stack: Stack = HardForkStack(maxStackSize, ctx)
    private val memory = MemoryImpl(maxMemorySize)
    private var ret: ByteArray = emptyByteArray

    var op: Int = 0
    var opInfo: Long = 0

    private var memOff: Long = 0
    private var memLen: Long = 0
    private var key: ByteArray = emptyByteArray
    private var codeSizeLast = emptyByteArray


    // TODO: code segment check
    private fun jump(dst: Long) {
        if (dst < 0 || dst >= callData.code.size)
            throw RuntimeException("jump destination overflow")
        if (callData.code[dst.toInt()].toUByte().toInt() != OpCodes.JUMPDEST)
            throw RuntimeException("jump destination is not JUMPDEST")
        this.pc = dst.toInt()
    }


    fun execute(): ByteArray {
        logInfo()

        while (pc < callData.code.size) {
            op = callData.code[pc].toUByte().toInt()
            opInfo = 0
            beforeExecute()

            when (op) {
                OpCodes.STOP -> {
                    afterExecute()
                    return ret
                }

                // arithmetic, pure
                OpCodes.ADD -> stack.add()
                OpCodes.MUL -> stack.mul()
                OpCodes.SUB -> stack.sub()
                OpCodes.DIV -> stack.div()
                OpCodes.SDIV -> stack.signedDiv()
                OpCodes.MOD -> stack.mod()
                OpCodes.SMOD -> stack.signedMod()
                OpCodes.ADDMOD -> stack.addMod()
                OpCodes.MULMOD -> stack.mulMod()
                OpCodes.EXP -> {
                    opInfo = ((stack.backBigInt(0).bitLength() + 7 ) / 8).toLong()
                    stack.exp()
                }
                OpCodes.SIGNEXTEND -> stack.signExtend()
                OpCodes.LT -> stack.lt()
                OpCodes.GT -> stack.gt()
                OpCodes.SLT -> stack.slt()
                OpCodes.SGT -> stack.sgt()
                OpCodes.EQ -> stack.eq()
                OpCodes.ISZERO -> stack.isZero()
                OpCodes.AND -> stack.and()
                OpCodes.OR -> stack.or()
                OpCodes.XOR -> stack.xor()
                OpCodes.NOT -> stack.not()
                OpCodes.BYTE -> stack.byte()
                OpCodes.SHL -> stack.shl()
                OpCodes.SHR -> stack.shr()
                OpCodes.SAR -> stack.sar()
                OpCodes.SHA3 -> {
                    opInfo = stack.backU32(1)
                    stack.sha3(memory, host.digest)
                }
                // address is left padded
                OpCodes.ADDRESS -> stack.pushLeftPadding(callData.receipt)
                OpCodes.BALANCE -> stack.push(host.getBalance(stack.popAddress()))
                OpCodes.ORIGIN -> stack.pushLeftPadding(ctx.origin)
                OpCodes.CALLER -> stack.pushLeftPadding(callData.caller)
                OpCodes.CALLVALUE -> stack.push(callData.value)

                // call data load is right padded
                OpCodes.CALLDATALOAD -> stack.callDataLoad(callData.input)
                OpCodes.CALLDATASIZE -> stack.pushInt(callData.input.size)

                // right padded
                OpCodes.CALLDATACOPY -> {
                    opInfo = stack.backU32(2)
                    stack.dataCopy(memory, callData.input)
                }
                OpCodes.CODESIZE -> stack.pushInt(callData.code.size)
                OpCodes.CODECOPY -> {
                    opInfo = stack.backU32(2)
                    stack.dataCopy(memory, callData.code)
                }
                OpCodes.RETURNDATASIZE -> stack.pushInt(ret.size)
                OpCodes.RETURNDATACOPY -> {
                    opInfo = stack.backU32(2)
                    stack.dataCopy(memory, ret)
                }
                OpCodes.GASPRICE -> stack.push(ctx.gasPrice)
                OpCodes.EXTCODESIZE -> {
                    val ad = stack.popAddress()
                    val sz = host.getCodeSize(ad)
                    stack.pushInt(sz)
                    if (sz == 0) {
                       this.codeSizeLast = ad
                    }
                }

                OpCodes.EXTCODECOPY -> {
                    opInfo = stack.backU32(2)
                    stack.dataCopy(memory, host.getCode(stack.popAddress()))
                }

                OpCodes.EXTCODEHASH -> {
                    val code = host.getCode(stack.popAddress())
                    val bytes = ByteArray(SlotUtils.SLOT_BYTE_ARRAY_SIZE)
                    host.digest.digest(code, 0, code.size, bytes, 0)
                    stack.pushLeftPadding(bytes)
                }
                OpCodes.COINBASE -> {
                   stack.pushLeftPadding(ctx.coinbase)
                }
                OpCodes.BLOCKHASH  -> {
                    val h = stack.popU32()
                    val hash = ctx.blockHashMap[h]
                    if(hash == null)
                        stack.pushZero()
                    else
                        stack.pushLeftPadding(hash)
                }
                OpCodes.TIMESTAMP -> stack.pushLong(ctx.timestamp)
                OpCodes.NUMBER -> stack.pushLong(ctx.number)
                OpCodes.DIFFICULTY -> stack.push(ctx.difficulty)
                OpCodes.GASLIMIT -> stack.push(SlotUtils.NEGATIVE_ONE)
                OpCodes.CHAINID -> stack.pushInt(ctx.chainId)
                OpCodes.SELFBALANCE -> stack.push(host.getBalance(callData.receipt))
                OpCodes.POP -> stack.drop()
                OpCodes.MLOAD -> stack.mload(memory)
                OpCodes.MSTORE -> stack.mstore(memory)
                OpCodes.MSTORE8 -> stack.mstore8(memory)
                OpCodes.SLOAD -> stack.pushLeftPadding(host.getStorage(callData.receipt, stack.popBytes()))
                OpCodes.SSTORE -> {
                    if(host.getStorage(callData.receipt, stack.back(0)).isNotEmpty())
                        opInfo = 1
                    host.setStorage(callData.receipt, stack.popBytes(), stack.popBytes())
                }
                OpCodes.JUMP -> {
                    jump(stack.popU32())
                    afterExecute()
                    continue
                }
                OpCodes.JUMPI -> {
                    val dst = stack.popU32()
                    val cond = stack.popU32() != 0L
                    if (cond) {
                        jump(dst)
                        afterExecute()
                        continue
                    } else {
                        pc++
                        afterExecute()
                        continue
                    }
                }
                OpCodes.PC -> stack.pushInt(pc)
                OpCodes.MSIZE -> stack.pushInt(memory.size)
                OpCodes.GAS -> stack.pushZero()
                OpCodes.JUMPDEST -> {

                }
                // push(code[pc+1:pc+1+n]), push n is right padded
                in OpCodes.PUSH1..OpCodes.PUSH32 -> {
                    val size = op - OpCodes.PUSH1 + 1
                    val n = min(size, callData.code.size - pc - 1)
                    stack.pushLeftPadding(callData.code, pc + 1, n)
                    pc += size
                }
                in OpCodes.DUP1..OpCodes.DUP16 -> stack.dup(op - OpCodes.DUP1 + 1)
                in OpCodes.SWAP1..OpCodes.SWAP16 -> stack.swap(op - OpCodes.SWAP1 + 1)

                // TODO: Test log operation
                in OpCodes.LOG0..OpCodes.LOG4 -> {
                    val n = op - OpCodes.LOG0
                    val topics = mutableListOf<ByteArray>()
                    opInfo = stack.backU32(1)
                    val data = stack.popMemory(memory)
                    for (i in 0 until n) {
                        topics.add(stack.popBytes())
                    }
                    host.log(callData.receipt, data, topics)
                }

                OpCodes.RETURN -> {
                    ret = stack.popMemory(memory)
                    afterExecute()
                    return ret
                }
                OpCodes.REVERT -> {
                    val off = stack.popU32()
                    val size = stack.popU32()
                    afterExecute()
                    throw RevertException(this.id, callData.receipt, memory.resizeAndCopy(off, size), host.digest, this.codeSizeLast)
                }
                OpCodes.STATICCALL, OpCodes.CALL, OpCodes.DELEGATECALL -> call(op)
                OpCodes.CREATE -> create()
                OpCodes.CREATE2 -> create(true)
                else -> throw RuntimeException("unsupported op $op")
            }
            afterExecute()
            pc++
        }
        return emptyByteArray
    }


    fun ByteArray.hex(start: Int = 0, end: Int = this.size): String {
        return "0x" + this.sliceArray(start until min(end, this.size)).joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    private val ByteArray.bnHex: String
        get() {
            return BigInteger(1, this).hex
        }

    val BigInteger.hex: String
        get() {
            return "0x" + this.toString(16)
        }

    private fun ByteArray.sha3(): ByteArray {
        val out = ByteArray(SlotUtils.SLOT_BYTE_ARRAY_SIZE)
        host.digest.digest(this, 0, this.size, out, 0)
        return out
    }

    private fun logInfo() {
        vmLog?.let {
            it.println("go pc = $pc input = ${callData.input.hex()} code size = ${callData.code.size} caller = ${callData.caller.hex()} receipt = ${callData.receipt.hex()} value = ${callData.value}")
            it.println("code = ${callData.code.hex()}")
            it.println("code hash = ${callData.code.sha3().hex()}")
        }
    }

    private fun beforeExecute() {
        vmLog?.let {
            it.println("before execute op ${OpCodes.nameOf(op)} pc = $pc")

            if (op >= OpCodes.PUSH1 && op <= OpCodes.PUSH32) {
                var n = op - OpCodes.PUSH1 + 1
                val start = pc + 1
                val max = callData.code.size - start
                if (n > max) {
                    it.println("push$n overflow, roll back to push$max")
                    n = max
                }
                it.println("push ${callData.code.hex(start, start + n)} into stack")
                return
            }

            when (op) {
                OpCodes.MLOAD -> {
                    val off = stack.backBigInt()
                    it.println("mload off = $off stack.push(mem[$off:$off+32]), mem.cap = ${memory.size}")
                }
                OpCodes.ADDRESS -> it.println("push address = ${callData.receipt.hex()}")
                OpCodes.SSTORE -> {
                    key = stack.back(0)
                    val value = stack.back(1)
                    it.println("sstore key = ${key.hex()}, value = ${value.hex()}")
                }
                OpCodes.SLOAD -> {
                    val loc = stack.back(0)
                    val value = host.getStorage(callData.receipt, loc)
                    val value32 = if (value.isEmpty()) {
                        ByteArray(32)
                    } else {
                        value
                    }
                    it.println("sload key = ${loc.hex()}, value = ${value32.hex()}")
                }
                OpCodes.CODECOPY -> {
                    memOff = stack.backU32()
                    val codeOff = stack.backU32(1)
                    memLen = stack.backU32(2)

                    it.println(
                        "codecopy into memory, mem[$memOff:$memOff+$memLen] = code[$codeOff:$codeOff+$memLen] mem.cap = ${memory.size} value = ${
                            callData.code.hex(
                                codeOff.toInt(), codeOff.toInt() + memLen.toInt()
                            )
                        }"
                    )
                }
                OpCodes.MSTORE -> {
                    memOff = stack.backU32()
                    val value = stack.back(1).bnHex

                    it.println("mstore $value into memory, mem offset = $memOff mem.cap = ${memory.size}")
                }
                OpCodes.CALLVALUE -> {
                    it.println("callvalue ${callData.value} into stack")
                }
                OpCodes.CALLDATASIZE -> {
                    it.println("calldatasize ${callData.input.size} into stack")
                }
                OpCodes.CALLDATALOAD -> {
                    val off = stack.backU32()
                    if (off < 0 || off + 32 > callData.input.size) {
                        it.println("call data load overflows")
                    }
                    val dat = getData(callData.input, off, 32)
                    it.println("calldataload data off = ${off} len = 32, input len = ${callData.input.size}, value = ${dat.hex()}")
                }
                OpCodes.JUMPI -> {
                    if (stack.backU32(1) == 0L) {
                        it.println("jumpi cond = 0, no jump")
                    } else {
                        it.println("jumpi cond = 1, jump to ${stack.backU32(0)}")
                    }
                }
                OpCodes.JUMP -> it.println(
                    "jump to dest ${stack.backBigInt()} op = ${
                        OpCodes.nameOf(
                            callData.code[stack.backU32().toInt()].toUByte().toInt()
                        )
                    }"
                )
                OpCodes.JUMPDEST -> {
                }
                in OpCodes.LOG0..OpCodes.LOG4 -> {
                    it.println("log${op - OpCodes.LOG0} mem off = ${stack.backBigInt(0)} len = ${stack.backBigInt(1)}, mem.cap = ${memory.size}")
                }
                OpCodes.RETURN -> {
                    val off = if (stack.backU32() < 0) {
                        0
                    } else {
                        stack.backU32().toInt()
                    }
                    val size = if (stack.backU32(1) < 0) {
                        0
                    } else {
                        stack.backU32(1).toInt()
                    }
                    val value = ByteArray(size)
                    memory.read(off, value)
                    it.println("return offset = $off size = $size value = ${value.hex()}")
                }
            }
        }
    }

    fun afterExecute() {
        hook?.onOp(op, opInfo)

        vmLog?.let {
            it.println("after execute op ${OpCodes.nameOf(op)} pc = $pc")

            val stackData = "[" + (0 until stack.size).map { bn -> stack.get(bn).hex }.joinToString(",") + "]"
            it.println("stack = $stackData")

            when (op) {
                OpCodes.CALL, OpCodes.STATICCALL, OpCodes.DELEGATECALL -> {
                    it.println(
                        "${OpCodes.nameOf(op)} success memOff = $memOff menLen = $memLen ret = ${
                            memory.data.hex(
                                memOff.toInt(),
                                (memOff + memLen).toInt()
                            )
                        }"
                    )
                }
                OpCodes.SSTORE -> it.println(
                    "sstore success key = ${key.hex()}, value = ${
                        host.getStorage(
                            callData.receipt,
                            key
                        ).hex()
                    }"
                )
                OpCodes.CODECOPY -> it.println(
                    "codecopy success mem.cap = ${memory.size}, mem[${memOff}:${memOff}+${memLen}] = ${
                        memory.copy(
                            memOff.toInt(),
                            (memOff + memLen).toInt()
                        ).hex()
                    }"
                )
                OpCodes.MSTORE -> it.println(
                    "mstore success, mem[${memOff}:${memOff}+32] = ${
                        memory.copy(
                            memOff.toInt(),
                            (memOff + 32).toInt()
                        ).hex()
                    } mem.cap = ${memory.size}"
                )
                OpCodes.RETURN -> {
                }
            }
        }
    }

    private fun getData(input: ByteArray, off: Long, len: Long): ByteArray {
        val offInt = unsignedMin(off, input.size.toLong()).toInt()
        val lenInt = unsignedMin(len, (input.size - offInt).toLong()).toInt()
        val r = ByteArray(len.toInt())
        System.arraycopy(input, offInt, r, 0, lenInt)
        return r
    }

    private fun unsignedMin(x: Long, y: Long): Long {
        return if (x + Long.MIN_VALUE < y + Long.MIN_VALUE) {
            x
        } else {
            y
        }
    }

    fun create(create2: Boolean = false) {

        val value = stack.popBigInt()
        val off = stack.popU32()
        val size = stack.popU32()
        val salt: ByteArray? = if (create2) {
            stack.popBytes()
        } else {
            null
        }
        val input = memory.resizeAndCopy(off, size)
        val addr = host.create(callData.receipt, value, input, salt)
        stack.pushLeftPadding(addr)
    }

    fun call(op: Int) {
        val temp = stack.popBigInt()
        val addr = stack.popAddress()
        val value = if (op == OpCodes.CALL) {
            stack.popBigInt()
        } else {
            BigInteger.ZERO
        }
        val inOff = stack.popU32()
        val inSize = stack.popU32()
        val retOff = stack.popU32()
        val retSize = stack.popU32()

        memOff = retOff
        memLen = retSize

        memory.resize(inOff, inSize)

        val input = memory.copy(inOff.toInt(), (inOff + inSize).toInt())

        vmLog?.println(
            "${
                OpCodes.nameOf(op).lowercase()
            } address ${addr.hex()} args = ${input.hex()} value = $value retOffset = $retOff retSize = $retSize temp = $temp mem.cap = ${memory.size}"
        )

        when (op) {
            OpCodes.CALL, OpCodes.STATICCALL -> {
                ret = host.call(callData.receipt, addr, input, value, op == OpCodes.STATICCALL)
            }
            OpCodes.DELEGATECALL -> {
                ret = host.delegate(callData.caller, callData.receipt, addr, input)
            }
        }


        memory.resize(retOff, retSize)
        memory.write(retOff.toInt(), ret, 0, min(retSize.toInt(), ret.size))
        stack.pushOne()
    }
}