package org.tdf.sunflower.service

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets

@Service
class HttpService {
    private val restTemplate = RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8)))

    /**
     * send get request
     *
     * @param httpHeaders header
     * @param uri         uri
     * @param parameters  The parameter followed by the question mark
     * @return response body
     */
    fun get(
        uri: String,
        httpHeaders: HttpHeaders = HttpHeaders.EMPTY,
        parameters: Map<String, String> = emptyMap()
    ): String {
        return request(HttpMethod.GET, httpHeaders, uri, parameters, "")
    }

    /**
     * make an http request
     *
     * @param method      request method get/post/put/patch/delete
     * @param httpHeaders header
     * @param uri         uri
     * @param query       The parameter followed by the question mark
     * @param body        request body
     * @return response body
     */
    fun request(
        method: HttpMethod,
        httpHeaders: HttpHeaders,
        uri: String,
        query: Map<String, String>,
        body: String
    ): String {
        val req = RequestEntity
            .method(method, buildURI(uri, query))
            .headers(httpHeaders)
            .body(body)
        return restTemplate
            .exchange(req, String::class.java)
            .body
    }

    private fun buildURI(uri: String, query: Map<String, String>): URI {
        val multiMap: MutableMap<String, List<String>> = HashMap()
        query.forEach { (x: String, y: String) -> multiMap[x] = listOf(y) }
        val builder = UriComponentsBuilder
            .newInstance()
            .uri(URI.create(uri))
            .queryParams(LinkedMultiValueMap(multiMap))
        return builder.build().toUri()
    }
}