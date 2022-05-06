package org.tdf.sunflower.vm.abi

object WbiType {
    const val UINT_256: Long = 0xec13d6d1L // keccak(uint256)
    const val ADDRESS: Long = 0x421683f8L // keccak(address)
    const val STRING: Long = 0x97fc4627L // keccak(string)
    const val BYTES: Long = 0xb963e9b4L // keccak(bytes)
    const val BYTES_32: Long = 0x9878dbb4L// keccak(bytes32)
}