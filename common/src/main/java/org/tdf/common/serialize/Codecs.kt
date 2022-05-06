package org.tdf.common.serialize

import org.tdf.common.serialize.Codec.Companion.create
import org.tdf.common.util.decode
import org.tdf.common.util.hex
import org.tdf.common.util.rlp
import java.nio.charset.StandardCharsets
import java.util.function.Function

object Codecs {
    /**
     * Converter from string to byte array and vice versa
     */
    @JvmField
    val string = create(
        { it.toByteArray(StandardCharsets.UTF_8) }
    ) { String(it, StandardCharsets.UTF_8) }

    val identity: Codec<*> = create(Function.identity(), Function.identity())

    val hex = create({ it.bytes }) { it.hex() }

    fun <K> rlp(clazz: Class<K>): Codec<K> {
        return create({ it.rlp() }) { it.decode(clazz) }
    }
}