package org.tdf.sunflower.controller

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Global Exception handler
 * e.g.
 *
 *
 * throw new RuntimeException("failed") in RestController
 * -> {
 * "code": 500,
 * "message": "failed",
 * "data": ""
 * }
 */
@ControllerAdvice
class ExceptionAdvice {
    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun notFoundException(e: Exception): Any {
        e.printStackTrace()
        return Response.failed<Any>(Response.Code.INTERNAL_ERROR, e.message ?: "unknown")
    }
}