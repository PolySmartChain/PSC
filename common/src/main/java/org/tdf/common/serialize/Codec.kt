package org.tdf.common.serialize

import java.util.function.Function

/**
 * encoder and decoder from a type to another type
 *
 * @param <K> type
</K> */
interface Codec<K> {
    val encoder: Function<in K, ByteArray>
    val decoder: Function<ByteArray, out K>

    companion object {
        @JvmStatic
        fun <K> identity(): Codec<K> {
            return Codecs.identity as Codec<K>
        }

        fun <K> create(
            encoder: Function<in K, ByteArray>,
            decoder: Function<ByteArray, out K>
        ): Codec<K> {
            return CodecImpl(encoder, decoder)
        }
    }
}

internal class CodecImpl<K>(
    override val encoder: Function<in K, ByteArray>,
    override val decoder: Function<ByteArray, out K>
) : Codec<K>