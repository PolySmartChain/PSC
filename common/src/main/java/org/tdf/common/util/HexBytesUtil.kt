package org.tdf.common.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

internal object HexBytesUtil {
    internal class HexBytesSerializer : StdSerializer<HexBytes>(HexBytes::class.java) {
        override fun serialize(value: HexBytes, jgen: JsonGenerator, provider: SerializerProvider) {
            jgen.writeString("0x" + value.hex)
        }
    }

    internal class HexBytesDeserializer : StdDeserializer<HexBytes>(HexBytes::class.java) {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): HexBytes {
            val node = p.codec.readTree<JsonNode>(p)
            if (node.isNull) return HexBytes.empty()
            var encoded = node.asText()
            if (encoded == null || encoded.isEmpty()) {
                return HexBytes.empty()
            }
            if (encoded.startsWith("0x")) {
                encoded = encoded.substring(2)
            }
            return encoded.hex()
        }
    }
}