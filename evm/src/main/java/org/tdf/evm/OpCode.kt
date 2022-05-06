package org.tdf.evm

object OpCodes {
    val NAMES = arrayOfNulls<String>(256)

    fun nameOf(op: Int): String {
        return NAMES[op] ?: throw RuntimeException("unknown op code $op")
    }

    fun isOpCOde(op: Int): Boolean {
        return NAMES[op] != null
    }

    /**
     * Halts execution (0x00)
     */
    const val STOP = 0x00

    // ========================
    // Arithmetic Operations
    // ========================

    /**
     * (0x01) Addition operation
     */
    const val ADD = 0x01

    /**
     * (0x02) Multiplication operation
     */
    const val MUL = 0x02

    /**
     * (0x03) Subtraction operations
     */
    const val SUB = 0x03

    /**
     * (0x04) Integer division operation
     */
    const val DIV = 0x04

    /**
     * (0x05) Signed integer division operation
     */
    const val SDIV = 0x05

    /**
     * (0x06) Modulo remainder operation
     */
    const val MOD = 0x06

    /**
     * (0x07) Signed modulo remainder operation
     */
    const val SMOD = 0x07

    /**
     * (0x08) Addition combined with modulo remainder operation
     */
    const val ADDMOD = 0x08

    /**
     * (0x09) Multiplication combined with modulo remainder operation
     */
    const val MULMOD = 0x09

    /**
     * (0x0a) Exponential operation
     */
    const val EXP = 0x0a

    /**
     * (0x0b) Extend length of signed integer
     */
    const val SIGNEXTEND = 0x0b

    // ========================
    // Bitwise Logic & Comparison Operations
    // ========================

    /**
     * (0x10) Less-than comparison
     */
    const val LT = 0X10

    /**
     * (0x11) Greater-than comparison
     */
    const val GT = 0X11

    /**
     * (0x12) Signed less-than comparison
     */
    const val SLT = 0X12

    /**
     * (0x13) Signed greater-than comparison
     */
    const val SGT = 0X13

    /**
     * (0x14) Equality comparison
     */
    const val EQ = 0X14

    /**
     * (0x15) Negation operation
     */
    const val ISZERO = 0x15

    /**
     * (0x16) Bitwise AND operation
     */
    const val AND = 0x16

    /**
     * (0x17) Bitwise OR operation
     */
    const val OR = 0x17

    /**
     * (0x18) Bitwise XOR operation
     */
    const val XOR = 0x18

    /**
     * (0x19) Bitwise NOT operationr
     */
    const val NOT = 0x19

    /**
     * (0x1a) Retrieve single byte from word
     */
    const val BYTE = 0x1a

    /**
     * (0x1b) Shift left
     */
    const val SHL = 0x1b

    /**
     * (0x1c) Logical shift right
     */
    const val SHR = 0x1c

    /**
     * (0x1d) Arithmetic shift right
     */
    const val SAR = 0x1d

    // ========================
    // Cryptographic Operations
    // ========================

    /**
     * (0x20) Compute SHA3-256 hash
     */
    const val SHA3 = 0x20

    // ========================
    // Environment Information
    // ========================

    /**
     * (0x30) Get address of currently executing account
     */
    const val ADDRESS = 0x30

    /**
     * (0x31) Get balance of the given account
     */
    const val BALANCE = 0x31

    /**
     * (0x32) Get execution origination address
     */
    const val ORIGIN = 0x32

    /**
     * (0x33) Get caller address
     */
    const val CALLER = 0x33

    /**
     * (0x34) Get deposited value by the instruction/transaction responsible for
     * this execution
     */
    const val CALLVALUE = 0x34

    /**
     * (0x35) Get input data of current environment
     */
    const val CALLDATALOAD = 0x35

    /**
     * (0x36) Get size of input data in current environment
     */
    const val CALLDATASIZE = 0x36

    /**
     * (0x37) Copy input data in current environment to memory
     */
    const val CALLDATACOPY = 0x37

    /**
     * (0x38) Get size of code running in current environment
     */
    const val CODESIZE = 0x38

    /**
     * (0x39) Copy code running in current environment to memory
     */
    const val CODECOPY = 0x39

    /**
     * (0x3d) Get the size of return data
     */
    const val RETURNDATASIZE = 0x3d

    /**
     * (0x3e) Get the return data
     */
    const val RETURNDATACOPY = 0x3e

    /**
     * (0x3a) Get price of gas in current environment
     */
    const val GASPRICE = 0x3a

    /**
     * (0x3b) Get size of code running in current environment with given offset
     */
    const val EXTCODESIZE = 0x3b

    /**
     * (0x3c) Copy code running in current environment to memory with given offset
     */
    const val EXTCODECOPY = 0x3c

    /**
     * (0x3f) Returns the keccak256 hash of a contract’s code
     */
    const val EXTCODEHASH = 0x3f

    // ========================
    // Block Information
    // ========================

    /**
     * (0x40) Get hash of most recent complete block
     */
    const val BLOCKHASH = 0x40

    /**
     * (0x41) Get the block’s coinbase address
     */
    const val COINBASE = 0x41

    /**
     * (x042) Get the block’s timestamp
     */
    const val TIMESTAMP = 0x42

    /**
     * (0x43) Get the block’s number
     */
    const val NUMBER = 0x43

    /**
     * (0x44) Get the block’s difficulty
     */
    const val DIFFICULTY = 0x44

    /**
     * (0x45) Get the block’s gas limit
     */
    const val GASLIMIT = 0x45

    // ========================
    // Memory, Storage and Flow Operations
    // ========================

    const val CHAINID = 0x46

    const val SELFBALANCE = 0x47

    /**
     * (0x50) Remove item from stack
     */
    const val POP = 0x50

    /**
     * (0x51) Load word from memory
     */
    const val MLOAD = 0x51

    /**
     * (0x52) Save word to memory
     */
    const val MSTORE = 0x52

    /**
     * (0x53) Save byte to memory
     */
    const val MSTORE8 = 0x53

    /**
     * (0x54) Load word from storage
     */
    const val SLOAD = 0x54

    /**
     * (0x55) Save word to storage
     */
    const val SSTORE = 0x55

    /**
     * (0x56) Alter the program counter
     */
    const val JUMP = 0x56

    /**
     * (0x57) Conditionally alter the program counter
     */
    const val JUMPI = 0x57

    /**
     * (0x58) Get the program counter
     */
    const val PC = 0x58

    /**
     * (0x59) Get the size of active memory
     */
    const val MSIZE = 0x59

    /**
     * (0x5a) Get the amount of available gas
     */
    const val GAS = 0x5a

    /**
     * (0x5b) Tag a jump destination
     */
    const val JUMPDEST = 0x5b

    // ========================
    // Push Operations
    // ========================

    /**
     * (0x60) Place 1-byte item on stack
     */
    const val PUSH1 = 0x60

    /**
     * (0x61) Place 2-byte item on stack
     */
    const val PUSH2 = 0x61

    /**
     * (0x62) Place 3-byte item on stack
     */
    const val PUSH3 = 0x62

    /**
     * (0x63) Place 4-byte item on stack
     */
    const val PUSH4 = 0x63

    /**
     * (0x64) Place 5-byte item on stack
     */
    const val PUSH5 = 0x64

    /**
     * (0x65) Place 6-byte item on stack
     */
    const val PUSH6 = 0x65

    /**
     * (0x66) Place 7-byte item on stack
     */
    const val PUSH7 = 0x66

    /**
     * (0x67) Place 8-byte item on stack
     */
    const val PUSH8 = 0x67

    /**
     * (0x68) Place 9-byte item on stack
     */
    const val PUSH9 = 0x68

    /**
     * (0x69) Place 10-byte item on stack
     */
    const val PUSH10 = 0x69

    /**
     * (0x6a) Place 11-byte item on stack
     */
    const val PUSH11 = 0x6a

    /**
     * (0x6b) Place 12-byte item on stack
     */
    const val PUSH12 = 0x6b

    /**
     * (0x6c) Place 13-byte item on stack
     */
    const val PUSH13 = 0x6c

    /**
     * (0x6d) Place 14-byte item on stack
     */
    const val PUSH14 = 0x6d

    /**
     * (0x6e) Place 15-byte item on stack
     */
    const val PUSH15 = 0x6e

    /**
     * (0x6f) Place 16-byte item on stack
     */
    const val PUSH16 = 0x6f

    /**
     * (0x70) Place 17-byte item on stack
     */
    const val PUSH17 = 0x70

    /**
     * (0x71) Place 18-byte item on stack
     */
    const val PUSH18 = 0x71

    /**
     * (0x72) Place 19-byte item on stack
     */
    const val PUSH19 = 0x72

    /**
     * (0x73) Place 20-byte item on stack
     */
    const val PUSH20 = 0x73

    /**
     * (0x74) Place 21-byte item on stack
     */
    const val PUSH21 = 0x74

    /**
     * (0x75) Place 22-byte item on stack
     */
    const val PUSH22 = 0x75

    /**
     * (0x76) Place 23-byte item on stack
     */
    const val PUSH23 = 0x76

    /**
     * (0x77) Place 24-byte item on stack
     */
    const val PUSH24 = 0x77

    /**
     * (0x78) Place 25-byte item on stack
     */
    const val PUSH25 = 0x78

    /**
     * (0x79) Place 26-byte item on stack
     */
    const val PUSH26 = 0x79

    /**
     * (0x7a) Place 27-byte item on stack
     */
    const val PUSH27 = 0x7a

    /**
     * (0x7b) Place 28-byte item on stack
     */
    const val PUSH28 = 0x7b

    /**
     * (0x7c) Place 29-byte item on stack
     */
    const val PUSH29 = 0x7c

    /**
     * (0x7d) Place 30-byte item on stack
     */
    const val PUSH30 = 0x7d

    /**
     * (0x7e) Place 31-byte item on stack
     */
    const val PUSH31 = 0x7e

    /**
     * (0x7f) Place 32-byte (full word) item on stack
     */
    const val PUSH32 = 0x7f

    // ========================
    // Duplicate Nth item from the stack
    // ========================

    /**
     * (0x80) Duplicate 1st item on stack
     */
    const val DUP1 = 0x80

    /**
     * (0x81) Duplicate 2nd item on stack
     */
    const val DUP2 = 0x81

    /**
     * (0x82) Duplicate 3rd item on stack
     */
    const val DUP3 = 0x82

    /**
     * (0x83) Duplicate 4th item on stack
     */
    const val DUP4 = 0x83

    /**
     * (0x84) Duplicate 5th item on stack
     */
    const val DUP5 = 0x84

    /**
     * (0x85) Duplicate 6th item on stack
     */
    const val DUP6 = 0x85

    /**
     * (0x86) Duplicate 7th item on stack
     */
    const val DUP7 = 0x86

    /**
     * (0x87) Duplicate 8th item on stack
     */
    const val DUP8 = 0x87

    /**
     * (0x88) Duplicate 9th item on stack
     */
    const val DUP9 = 0x88

    /**
     * (0x89) Duplicate 10th item on stack
     */
    const val DUP10 = 0x89

    /**
     * (0x8a) Duplicate 11th item on stack
     */
    const val DUP11 = 0x8a

    /**
     * (0x8b) Duplicate 12th item on stack
     */
    const val DUP12 = 0x8b

    /**
     * (0x8c) Duplicate 13th item on stack
     */
    const val DUP13 = 0x8c

    /**
     * (0x8d) Duplicate 14th item on stack
     */
    const val DUP14 = 0x8d

    /**
     * (0x8e) Duplicate 15th item on stack
     */
    const val DUP15 = 0x8e

    /**
     * (0x8f) Duplicate 16th item on stack
     */
    const val DUP16 = 0x8f

    // ========================
    // Swap the Nth item from the stack with the top
    // ========================

    /**
     * (0x90) Exchange 2nd item from stack with the top
     */
    const val SWAP1 = 0x90

    /**
     * (0x91) Exchange 3rd item from stack with the top
     */
    const val SWAP2 = 0x91

    /**
     * (0x92) Exchange 4th item from stack with the top
     */
    const val SWAP3 = 0x92

    /**
     * (0x93) Exchange 5th item from stack with the top
     */
    const val SWAP4 = 0x93

    /**
     * (0x94) Exchange 6th item from stack with the top
     */
    const val SWAP5 = 0x94

    /**
     * (0x95) Exchange 7th item from stack with the top
     */
    const val SWAP6 = 0x95

    /**
     * (0x96) Exchange 8th item from stack with the top
     */
    const val SWAP7 = 0x96

    /**
     * (0x97) Exchange 9th item from stack with the top
     */
    const val SWAP8 = 0x97

    /**
     * (0x98) Exchange 10th item from stack with the top
     */
    const val SWAP9 = 0x98

    /**
     * (0x99) Exchange 11th item from stack with the top
     */
    const val SWAP10 = 0x99

    /**
     * (0x9a) Exchange 12th item from stack with the top
     */
    const val SWAP11 = 0x9a

    /**
     * (0x9b) Exchange 13th item from stack with the top
     */
    const val SWAP12 = 0x9b

    /**
     * (0x9c) Exchange 14th item from stack with the top
     */
    const val SWAP13 = 0x9c

    /**
     * (0x9d) Exchange 15th item from stack with the top
     */
    const val SWAP14 = 0x9d

    /**
     * (0x9e) Exchange 16th item from stack with the top
     */
    const val SWAP15 = 0x9e

    /**
     * (0x9f) Exchange 17th item from stack with the top
     */
    const val SWAP16 = 0x9f

    /**
     * (0xa[n]) log some data for some address with 0..n tags
     * data]
     */
    const val LOG0 = 0xa0

    const val LOG1 = 0xa1

    const val LOG2 = 0xa2

    const val LOG3 = 0xa3

    const val LOG4 = 0xa4

    // ========================
    // System operations
    // ========================

    /**
     * (0xf0) Create a new account with associated code
     */
    const val CREATE = 0xf0

    /**
     * (cxf1) Message-call into an account
     */
    const val CALL = 0xf1
    // [out_data_size] [out_data_start] [in_data_size] [in_data_start] [value]
    // [to_addr] [gas] CALL

    /**
     * (0xf2) Calls self, but grabbing the code from the TO argument instead of from
     * one's own address
     */
    const val CALLCODE = 0xf2

    /**
     * (0xf3) Halt execution returning output data
     */
    const val RETURN = 0xf3

    /**
     * (0xf4) similar in idea to CALLCODE, except that it propagates the sender and
     * value from the parent scope to the child scope, ie. the call created has the
     * same sender and value as the original call. also the Value parameter is
     * omitted for this opCode
     */
    const val DELEGATECALL = 0xf4

    /**
     * (0xf5) Skinny CREATE2, same as CREATE but with deterministic address
     */
    const val CREATE2 = 0xf5

    /**
     * opcode that can be used to call another contract (or itself) while
     * disallowing any modifications to the state during the call (and its subcalls,
     * if present). Any opcode that attempts to perform such a modification (see
     * below for details) will result in an exception instead of performing the
     * modification.
     */
    const val STATICCALL = 0xfa

    /**
     * (0xfd) The `REVERT` instruction will stop execution, roll back all state
     * changes done so far and provide a pointer to a memory section, which can be
     * interpreted as an error code or message. While doing so, it will not consume
     * all the remaining gas.
     */
    const val REVERT = 0xfd

    /**
     * (0xff) Halt execution and register account for later deletion
     */
    const val SUICIDE = 0xff

    init {
        NAMES[STOP] = "STOP"

        NAMES[ADD] = "ADD"

        NAMES[MUL] = "MUL"

        NAMES[SUB] = "SUB"




        NAMES[DIV] = "DIV"




        NAMES[SDIV] = "SDIV"




        NAMES[MOD] = "MOD"




        NAMES[SMOD] = "SMOD"




        NAMES[ADDMOD] = "ADDMOD"




        NAMES[MULMOD] = "MULMOD"




        NAMES[EXP] = "EXP"




        NAMES[SIGNEXTEND] = "SIGNEXTEND"

        // ========================
        // Bitwise Logic & Comparison Operations
        // ========================


        NAMES[LT] = "LT"




        NAMES[GT] = "GT"




        NAMES[SLT] = "SLT"




        NAMES[SGT] = "SGT"




        NAMES[EQ] = "EQ"




        NAMES[ISZERO] = "ISZERO"




        NAMES[AND] = "AND"




        NAMES[OR] = "OR"




        NAMES[XOR] = "XOR"




        NAMES[NOT] = "NOT"




        NAMES[BYTE] = "BYTE"




        NAMES[SHL] = "SHL"




        NAMES[SHR] = "SHR"




        NAMES[SAR] = "SAR"

        // ========================
        // Cryptographic Operations
        // ========================


        NAMES[SHA3] = "SHA3"

        // ========================
        // Environment Information
        // ========================


        NAMES[ADDRESS] = "ADDRESS"




        NAMES[BALANCE] = "BALANCE"




        NAMES[ORIGIN] = "ORIGIN"




        NAMES[CALLER] = "CALLER"


        NAMES[CALLVALUE] = "CALLVALUE"




        NAMES[CALLDATALOAD] = "CALLDATALOAD"




        NAMES[CALLDATASIZE] = "CALLDATASIZE"




        NAMES[CALLDATACOPY] = "CALLDATACOPY"




        NAMES[CODESIZE] = "CODESIZE"




        NAMES[CODECOPY] = "CODECOPY"




        NAMES[RETURNDATASIZE] = "RETURNDATASIZE"




        NAMES[RETURNDATACOPY] = "RETURNDATACOPY"




        NAMES[GASPRICE] = "GASPRICE"




        NAMES[EXTCODESIZE] = "EXTCODESIZE"




        NAMES[EXTCODECOPY] = "EXTCODECOPY"




        NAMES[EXTCODEHASH] = "EXTCODEHASH"

        // ========================
        // Block Information
        // ========================


        NAMES[BLOCKHASH] = "BLOCKHASH"




        NAMES[COINBASE] = "COINBASE"




        NAMES[TIMESTAMP] = "TIMESTAMP"




        NAMES[NUMBER] = "NUMBER"




        NAMES[DIFFICULTY] = "DIFFICULTY"




        NAMES[GASLIMIT] = "GASLIMIT"
        NAMES[CHAINID] = "CHAINID"
        NAMES[SELFBALANCE] = "SELFBALANCE"

        // ========================
        // Memory, Storage and Flow Operations
        // ========================


        NAMES[POP] = "POP"




        NAMES[MLOAD] = "MLOAD"




        NAMES[MSTORE] = "MSTORE"




        NAMES[MSTORE8] = "MSTORE8"




        NAMES[SLOAD] = "SLOAD"




        NAMES[SSTORE] = "SSTORE"




        NAMES[JUMP] = "JUMP"




        NAMES[JUMPI] = "JUMPI"




        NAMES[PC] = "PC"




        NAMES[MSIZE] = "MSIZE"




        NAMES[GAS] = "GAS"




        NAMES[JUMPDEST] = "JUMPDEST"

        NAMES[PUSH1] = "PUSH1"




        NAMES[PUSH2] = "PUSH2"




        NAMES[PUSH3] = "PUSH3"




        NAMES[PUSH4] = "PUSH4"




        NAMES[PUSH5] = "PUSH5"




        NAMES[PUSH6] = "PUSH6"




        NAMES[PUSH7] = "PUSH7"




        NAMES[PUSH8] = "PUSH8"




        NAMES[PUSH9] = "PUSH9"




        NAMES[PUSH10] = "PUSH10"




        NAMES[PUSH11] = "PUSH11"




        NAMES[PUSH12] = "PUSH12"




        NAMES[PUSH13] = "PUSH13"




        NAMES[PUSH14] = "PUSH14"




        NAMES[PUSH15] = "PUSH15"




        NAMES[PUSH16] = "PUSH16"




        NAMES[PUSH17] = "PUSH17"




        NAMES[PUSH18] = "PUSH18"




        NAMES[PUSH19] = "PUSH19"




        NAMES[PUSH20] = "PUSH20"




        NAMES[PUSH21] = "PUSH21"




        NAMES[PUSH22] = "PUSH22"




        NAMES[PUSH23] = "PUSH23"




        NAMES[PUSH24] = "PUSH24"




        NAMES[PUSH25] = "PUSH25"




        NAMES[PUSH26] = "PUSH26"




        NAMES[PUSH27] = "PUSH27"




        NAMES[PUSH28] = "PUSH28"




        NAMES[PUSH29] = "PUSH29"




        NAMES[PUSH30] = "PUSH30"




        NAMES[PUSH31] = "PUSH31"




        NAMES[PUSH32] = "PUSH32"

        // ========================
        // Duplicate Nth item from the stack
        // ========================


        NAMES[DUP1] = "DUP1"




        NAMES[DUP2] = "DUP2"




        NAMES[DUP3] = "DUP3"




        NAMES[DUP4] = "DUP4"




        NAMES[DUP5] = "DUP5"




        NAMES[DUP6] = "DUP6"




        NAMES[DUP7] = "DUP7"




        NAMES[DUP8] = "DUP8"




        NAMES[DUP9] = "DUP9"




        NAMES[DUP10] = "DUP10"




        NAMES[DUP11] = "DUP11"




        NAMES[DUP12] = "DUP12"




        NAMES[DUP13] = "DUP13"




        NAMES[DUP14] = "DUP14"




        NAMES[DUP15] = "DUP15"




        NAMES[DUP16] = "DUP16"

        // ========================
        // Swap the Nth item from the stack with the top
        // ========================


        NAMES[SWAP1] = "SWAP1"




        NAMES[SWAP2] = "SWAP2"




        NAMES[SWAP3] = "SWAP3"




        NAMES[SWAP4] = "SWAP4"




        NAMES[SWAP5] = "SWAP5"




        NAMES[SWAP6] = "SWAP6"




        NAMES[SWAP7] = "SWAP7"




        NAMES[SWAP8] = "SWAP8"




        NAMES[SWAP9] = "SWAP9"




        NAMES[SWAP10] = "SWAP10"




        NAMES[SWAP11] = "SWAP11"




        NAMES[SWAP12] = "SWAP12"




        NAMES[SWAP13] = "SWAP13"




        NAMES[SWAP14] = "SWAP14"




        NAMES[SWAP15] = "SWAP15"




        NAMES[SWAP16] = "SWAP16"

        NAMES[LOG0] = "LOG0"

        NAMES[LOG1] = "LOG1"

        NAMES[LOG2] = "LOG2"

        NAMES[LOG3] = "LOG3"

        NAMES[LOG4] = "LOG4"

        // ========================
        // System operations
        // ========================


        NAMES[CREATE] = "CREATE"




        NAMES[CALL] = "CALL"
        // [out_data_size] [out_data_start] [in_data_size] [in_data_start] [value]
        // [to_addr] [gas] CALL


        NAMES[CALLCODE] = "CALLCODE"




        NAMES[RETURN] = "RETURN"

        NAMES[DELEGATECALL] = "DELEGATECALL"

        NAMES[CREATE2] = "CREATE2"


        NAMES[STATICCALL] = "STATICCALL"


        NAMES[REVERT] = "REVERT"

        NAMES[SUICIDE] = "SUICIDE"
    }
}

