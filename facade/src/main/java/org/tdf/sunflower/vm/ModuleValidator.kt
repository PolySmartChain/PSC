package org.tdf.sunflower.vm

import org.tdf.lotusvm.Module
import org.tdf.lotusvm.types.ExportType
import org.tdf.lotusvm.types.ImportType
import org.tdf.sunflower.vm.abi.Abi
import java.nio.charset.StandardCharsets

object ModuleValidator {
    fun validate(m: Module, update: Boolean) {
        // 1. exact one abi section found
        val customs = m.customSections
        val abiSections = customs.filter { c -> c.name == WBI.ABI_SECTION_NAME }
        if (abiSections.size != 1)
            throw RuntimeException("expect exactly one __abi section, while ${abiSections.size} found")
        val abiJson = String(
            abiSections[0].data,
            StandardCharsets.UTF_8
        )
        val abi = Abi.fromJson(abiJson)
        // when constructor found, __init section must exists
        val inits = customs.filter { c -> c.name == WBI.INIT_SECTION_NAME }

        if (inits.size > 1)
            throw RuntimeException("too many __init sections")

        val constructor = abi.findConstructor()
        // when __init section found, constructor should exists
        if (inits.isNotEmpty() && constructor == null)
            throw RuntimeException("constructor not found")

        if (constructor != null && inits.isEmpty() && !update) {
            // when update __init section will be dropped
            throw RuntimeException("constructor found, while __init section not found")
        }

        // all function in abi should be exported, and vice versa
        // arity in abi is equal to abi
        val abiFunctions = mutableMapOf<String, Pair<Int, Int>>()
        val moduleFunctions = mutableMapOf<String, Pair<Int, Int>>()

        for (entry in abi) {
            if (entry.type == Abi.Entry.Type.constructor) {
                abiFunctions["init"] = Pair(entry.inputs.size, entry.outputs.size)
            }
            if (entry.type == Abi.Entry.Type.function) {
                abiFunctions[entry.name!!] = Pair(entry.inputs.size, entry.outputs.size)
            }
        }

        val hostsCount: Int = m.importSection?.imports?.filter { x -> x.type == ImportType.TYPE_INDEX }?.count() ?: 0
        for (export in m.exportSection?.exports ?: emptyList()) {
            // skip wbi functions
            if (WBI.REVERSED.contains(export.name))
                continue
            if (export.type == ExportType.FUNCTION_INDEX) {
                val typeIdx = m.functionSection!!.typeIndices[export.index - hostsCount]
                val type = m.typeSection!!.functionTypes[typeIdx]
                moduleFunctions[export.name] = Pair(type.parameterTypes.size, type.resultTypes.size)
            }
        }

        if (moduleFunctions.size != abiFunctions.size)
            throw RuntimeException("functions in abi is not equal to module")

        for (entry in abiFunctions) {
            val md = moduleFunctions[entry.key]!!
            if (entry.value.second > 1 || md.second > 1)
                throw RuntimeException("wasm function should return at most one output")
            if (md != entry.value) {
                throw RuntimeException("""arity not equal for function name ${entry.key} abi = ${entry.value} module = $md""")
            }
        }
    }
}