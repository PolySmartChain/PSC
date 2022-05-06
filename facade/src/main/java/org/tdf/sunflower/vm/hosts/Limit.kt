package org.tdf.sunflower.vm.hosts

import org.tdf.evm.EvmHook
import org.tdf.evm.OpCodes
import org.tdf.lotusvm.common.OpCode
import org.tdf.lotusvm.runtime.Hook
import org.tdf.lotusvm.runtime.HostFunction
import org.tdf.lotusvm.runtime.Memory.Companion.PAGE_SIZE
import org.tdf.lotusvm.runtime.ModuleInstanceImpl

class Limit(val gasLimit: Long) : Hook, EvmHook {


    var runtimeGas: Long = 0
        private set

    var initialGas: Long = 0

    val totalGas: Long get() = initialGas + runtimeGas

    override fun onInstruction(ins: OpCode, module: ModuleInstanceImpl) {
        runtimeGas += 100
        if (totalGas > gasLimit) throw RuntimeException("gas overflow")
    }

    override fun onHostFunction(function: HostFunction, module: ModuleInstanceImpl) {
        runtimeGas += 100
        if (totalGas > gasLimit) throw RuntimeException("gas overflow")
    }

    override fun onNewFrame() {}
    override fun onFrameExit() {}
    override fun onMemoryGrow(beforeGrow: Int, afterGrow: Int) {
        if (afterGrow > MAX_MEMORY) println("memory size overflow")
    }

    // 1 evm op = 20 gas
    override fun onOp(op: Int, extraInfo: Long) {
        when (op) {
            // https://github.com/crytic/evm-opcodes
            // https://docs.google.com/spreadsheets/d/1n6mRqkBz3iWcOlRem_mO09GtSKEKrAsfO7Frgx18pNU/edit#gid=0
            OpCodes.STOP, OpCodes.REVERT, OpCodes.RETURN -> runtimeGas += 0
            OpCodes.JUMPDEST -> runtimeGas += 1
            OpCodes.ADDRESS, in OpCodes.ORIGIN..OpCodes.CALLVALUE, OpCodes.CODESIZE, OpCodes.GASPRICE,
            OpCodes.RETURNDATASIZE, in OpCodes.COINBASE..OpCodes.POP,
            OpCodes.CALLDATASIZE,
            in OpCodes.PC..OpCodes.GAS,
            -> runtimeGas += 2
            OpCodes.ADD, OpCodes.SUB, OpCodes.CALLDATALOAD -> runtimeGas += 3
            in OpCodes.LT..OpCodes.SAR, OpCodes.MLOAD, OpCodes.MSTORE, OpCodes.MSTORE8 -> runtimeGas += 3
            in OpCodes.PUSH1..OpCodes.SWAP16 -> runtimeGas += 3
            OpCodes.MUL, in OpCodes.DIV..OpCodes.SMOD, OpCodes.SIGNEXTEND -> runtimeGas += 5
            OpCodes.ADDMOD, OpCodes.MULMOD, OpCodes.JUMP -> runtimeGas += 8
            OpCodes.JUMPI -> runtimeGas += 10
            OpCodes.EXP -> runtimeGas += if (extraInfo == 0L) 50 else 50 * extraInfo
            OpCodes.SHA3 -> runtimeGas += 30 + 6 * extraInfo / 32
            OpCodes.CALLDATACOPY, OpCodes.EXTCODECOPY, OpCodes.CODECOPY, OpCodes.RETURNDATACOPY -> {
               val x = if(op == OpCodes.EXTCODECOPY) 700 else 3
                runtimeGas += x
                runtimeGas += extraInfo * 3
            }
            OpCodes.BLOCKHASH -> runtimeGas += 20
            OpCodes.BALANCE, OpCodes.EXTCODESIZE, OpCodes.EXTCODEHASH -> runtimeGas += 700
            OpCodes.SSTORE -> runtimeGas += (if (extraInfo > 0) 20000 else 5000)
            OpCodes.SLOAD -> runtimeGas += 800
            in OpCodes.LOG0..OpCodes.LOG4
            -> runtimeGas += 375 + (op - OpCodes.LOG0) * 375 + 8 * extraInfo
            OpCodes.CREATE, OpCodes.CREATE2 -> runtimeGas += 32000
            OpCodes.STATICCALL, OpCodes.CALL, OpCodes.DELEGATECALL, OpCodes.CALLCODE -> runtimeGas += 40
            else -> runtimeGas += 5
        }
        if (totalGas > gasLimit) throw RuntimeException("gas overflow total gas = $totalGas, gasLimit = $gasLimit op = $op")
    }


    companion object {
        const val MAX_MEMORY: Int = 256 * PAGE_SIZE // memory is limited to less than 256 page = 16mb
    }
}