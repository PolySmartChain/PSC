package org.tdf.sunflower.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object MapperUtil {
    @JvmField
    val OBJECT_MAPPER: ObjectMapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(JsonParser.Feature.ALLOW_COMMENTS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}