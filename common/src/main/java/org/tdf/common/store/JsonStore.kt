package org.tdf.common.store

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission

class JsonStore(private val jsonFile: String, private val mapper: ObjectMapper) : BatchStore<String, JsonNode>,
    IterableStore<String, JsonNode> {

    companion object {
        val defaultPosixPermissions: MutableSet<PosixFilePermission>

        init {
            defaultPosixPermissions = HashSet()
            defaultPosixPermissions.add(PosixFilePermission.OWNER_READ)
            defaultPosixPermissions.add(PosixFilePermission.OWNER_WRITE)
        }
    }

    lateinit var node: MutableMap<String, JsonNode>

    private fun sync() {
        if (jsonFile == "\$memory") return
        val bin = mapper.writeValueAsBytes(node)
        Files.write(
            Paths.get(jsonFile),
            bin,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        try {
            Files.setPosixFilePermissions(Paths.get(jsonFile), defaultPosixPermissions)
        } catch (ignored: Exception) {
        }
    }

    private fun load() {
        node = HashMap()
        if (jsonFile == "\$memory") return
        val f = File(jsonFile)
        if (!f.exists()) {
            return
        }
        var n: ObjectNode? = null
        try {
            n = mapper.readValue(f, JsonNode::class.java) as ObjectNode
        } catch (ignored: Exception) {
        }
        if (n == null) {
            return
        }
        val it = n.fields()
        while (it.hasNext()) {
            val (key, value) = it.next()
            node[key] = value
        }
    }

    override operator fun get(k: String): JsonNode? {
        return node[k]
    }

    fun clear() {
        node.clear()
    }

    override operator fun set(k: String, v: JsonNode) {
        node[k] = v
    }

    override fun remove(k: String) {
        node.remove(k)
    }

    override fun flush() {
        sync()
    }

    override fun putAll(rows: Collection<Map.Entry<String, JsonNode>>) {
        rows.forEach { node[it.key] = it.value }
    }

    override fun iterator(): Iterator<Map.Entry<String, JsonNode>> {
        return node.entries.iterator()
    }

    init {
        load()
    }
}