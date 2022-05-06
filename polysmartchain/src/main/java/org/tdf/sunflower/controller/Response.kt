package org.tdf.sunflower.controller

/**
 * json format:
 *
 *
 * {
 * "code": 500,
 * "data": "data",
 * "message": "success"
 * }
 */
data class Response<T>(val code: Int, val data: T, val message: String) {

    enum class Code(val code: Int, val message: String) {
        SUCCESS(200, "success"), INTERNAL_ERROR(500, "internal error");
    }

    companion object {
        fun <T> success(data: T): Response<T> {
            return Response(Code.SUCCESS.code, data, Code.SUCCESS.message)
        }

        fun <T> failed(code: Code, reason: String = code.message): Response<T?> {
            return Response(code.code, null, reason)
        }
    }
}