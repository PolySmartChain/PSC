package org.tdf.sunflower.vm

import org.tdf.common.types.Uint256
import org.tdf.common.util.*
import org.tdf.lotusvm.Module
import org.tdf.lotusvm.ModuleInstance
import org.tdf.sunflower.vm.abi.Abi
import org.tdf.sunflower.vm.abi.WbiType
import java.math.BigInteger
import java.nio.charset.StandardCharsets

/**
 * TODO:
 * 1. instead of json, use custom sections named with __abi like foo(address,address)returns(uint256)
 * ident=[a-zA-Z_][a-zA-Z0-9_]{0,127}
 * type = (address|uintx|intx|bytes|string)
 * $ident\((($type,)*$type)?\)(returns\($type\))?
 * 2. use wasmer instead of lotusvm
 */
object WBI {
    const val ABI_SECTION_NAME = "__abi";
    const val INIT_SECTION_NAME = "__init"
    const val WBI_MALLOC = "__malloc"
    const val WBI_PEEK = "__peek"
    const val WBI_CHANGE_TYPE = "__change_t"
    const val WBI_MALLOC_256 = "__malloc_256"
    const val WBI_MALLOC_512 = "__malloc_512"

    val REVERSED = setOf(WBI_MALLOC, WBI_PEEK, WBI_CHANGE_TYPE, WBI_MALLOC_256, WBI_MALLOC_512)


    fun dropInit(code: ByteArray): ByteArray {
        return code;
    }

    fun extractInitData(m: Module): ByteArray {
        return m.customSections.firstOrNull { it.name == INIT_SECTION_NAME }?.data ?: ByteUtil.EMPTY_BYTE_ARRAY
    }

    // the __init section is dropped before inject
    fun inject(create: Boolean, abi: Abi, i: ModuleInstance, input: HexBytes): InjectResult {
        var function: String? = null
        var entry: Abi.Entry? = null
        var encoded: HexBytes? = null

        // 2. for contract create, constructor is not necessarily
        if (create && abi.findConstructor() != null) {
            entry = abi.findConstructor()
            function = "init"
            encoded = input
        }

        // 3. for contract call, find function by signature
        if (!create) {
            val sig = input.bytes.selector()
            val f = abi.findFunction { x: Abi.Function -> x.encodeSignature().contentEquals(sig) }!!
            entry = f
            function = f.name
            // drop signature parts
            encoded = input.bytes.unselect().hex()
        }

        // params == null -> abi not found
        if (entry == null)
            return InjectResult(function ?: "", entry, LongArray(0), false)

        // malloc param types
        val inputs = Abi.Entry.Param.decodeList(entry.inputs, encoded!!.bytes)
        val ret = LongArray(entry.inputs.size)
        for (j in inputs.indices) {
            val p = entry.inputs[j]
            when (p.type.name) {
                "uint8", "uint16", "uint32", "uint64" -> {
                    ret[j] = (inputs[j] as BigInteger).longValueExact()
                }
                "uint", "uint256" -> {
                    val b = inputs[j] as BigInteger
                    ret[j] = malloc(i, Uint256.of(b)).toLong()
                }
                "string" -> {
                    val s = inputs[j] as String
                    ret[j] = malloc(i, s).toLong()
                }
                "address" -> {
                    val addr = inputs[j] as ByteArray
                    ret[j] = mallocAddress(i, addr.hex()).toLong()
                }
                else -> {
                    if (p.type.name.endsWith("]") || p.type.name.endsWith(")")) {
                        throw RuntimeException("array or tuple is not supported yet")
                    }
                    if (p.type.name.startsWith("bytes")) {
                        val data = inputs[j] as ByteArray
                        ret[j] = mallocBytes(i, data.hex()).toLong()
                    }
                }
            }
        }
        return InjectResult(function!!, entry, ret, true)
    }

    // String / U256 / HexBytes
    @JvmStatic
    fun peek(instance: ModuleInstance, offset: Int, type: Long): Any {
        val startAndLen = instance.execute(WBI_PEEK, offset.toLong(), type)[0]
        val start = (startAndLen ushr 32).toInt()
        val len = startAndLen.toInt()
        val bin = ByteArray(len)
        instance.memory.read(start, bin)
        when (type) {
            WbiType.STRING -> {
                return String(bin, StandardCharsets.UTF_8)
            }
            WbiType.UINT_256 -> {
                return Uint256.of(bin)
            }
            WbiType.BYTES, WbiType.ADDRESS, WbiType.BYTES_32 -> {
                return bin.hex()
            }
        }
        throw RuntimeException("unexpected")
    }

    fun mallocWords(instance: ModuleInstance, data: LongArray): Int {
        val p =
        when(data.size) {
            4 -> instance.execute(WBI_MALLOC_256, *data)[0]
            8 -> instance.execute(WBI_MALLOC_512, *data)[0]
            else -> throw RuntimeException("invalid size")
        }
        if(p > Int.MAX_VALUE)
            throw RuntimeException("malloc failed: pointer is negative")
        return p.toInt()
    }

    fun malloc(instance: ModuleInstance, type: Long, bin: ByteArray): Int {
        val ptr = instance.execute(WBI_MALLOC, bin.size.toLong())[0]
        instance.memory.write(ptr.toInt(), bin)
        val p = instance.execute(WBI_CHANGE_TYPE, type, ptr, bin.size.toLong())[0]
        val r = p.toInt()
        if (r < 0) throw RuntimeException("malloc failed: pointer is negative")
        return r
    }

    fun malloc(instance: ModuleInstance, s: String): Int {
        val bin = s.toByteArray(StandardCharsets.UTF_8)
        return malloc(instance, WbiType.STRING, bin)
    }

    fun malloc(instance: ModuleInstance, s: Uint256): Int {
        val bin = s.byte32
        val data = LongArray(4)
        data[0] = BigEndian.decodeInt64(bin, 0)
        data[1] = BigEndian.decodeInt64(bin, 8)
        data[2] = BigEndian.decodeInt64(bin, 16)
        data[3] = BigEndian.decodeInt64(bin, 24)
        return mallocWords(instance, data)
    }

    fun mallocBytes(instance: ModuleInstance, bin: HexBytes): Int {
        return malloc(instance, WbiType.BYTES, bin.bytes)
    }

    fun mallocAddress(instance: ModuleInstance, address: HexBytes): Int {
        return malloc(instance, WbiType.ADDRESS, address.bytes)
    }

    class InjectResult(val name: String, val entry: Abi.Entry?, val pointers: LongArray, val executable: Boolean);
}