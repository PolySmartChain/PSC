package org.tdf.sunflower.controller

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

/**
 * Global rest response body json formatter
 * e.g.
 *
 *
 * {"user": "name"}
 * ->  {
 * "code": 200,
 * "message": "success",
 * "data": {
 * "user": "name"
 * }
 * }
 *
 *
 * byte[] or String will be ignored
 */
@RestControllerAdvice
class CommonAdvice : ResponseBodyAdvice<Any?> {
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any {
        return if (body is ByteArray ||
            body is CharSequence ||
            body is Response<*>
        ) {
            body
        } else try {
            Response.success(body)
        } catch (e: Exception) {
            e.printStackTrace()
            Response.Code.INTERNAL_ERROR.message
        }
    }

}