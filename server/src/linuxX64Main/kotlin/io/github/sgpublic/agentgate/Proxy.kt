package io.github.sgpublic.agentgate

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond

private val requestHopByHopHeaders = setOf(
    HttpHeaders.Connection,
    "Keep-Alive",
    HttpHeaders.ProxyAuthenticate,
    HttpHeaders.ProxyAuthorization,
    HttpHeaders.TE,
    HttpHeaders.Trailer,
    HttpHeaders.TransferEncoding,
    HttpHeaders.Upgrade,
)
private val responseHopByHopHeaders = requestHopByHopHeaders + HttpHeaders.ContentLength
private val methodsWithoutBody = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)

internal suspend fun ApplicationCall.proxy(client: HttpClient, config: AgentGateCommand) {
    val contentLength = request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
    val maxSize = config.requestMaxSize?.let(::parseByteSize)
    if (maxSize != null && contentLength != null && contentLength > maxSize) {
        respond(HttpStatusCode.PayloadTooLarge, Result(-413, "请求体超过允许的最大大小"))
        return
    }

    val incomingRequest = request
    val targetUrl = URLBuilder().apply {
        takeFrom(config.targetUrl)
        pathSegments = request.path().trimStart('/').split('/')
        incomingRequest.queryParameters.forEach { name, values ->
            values.forEach { value -> parameters.append(name, value) }
        }
    }.build()
    client.prepareRequest(targetUrl) {
        method = incomingRequest.httpMethod
        headers {
            incomingRequest.headers.forEach { name, values ->
                if (name !in requestHopByHopHeaders && name != HttpHeaders.Host && name != HttpHeaders.ContentLength) {
                    values.forEach { value -> append(name, value) }
                }
            }
        }
        if (incomingRequest.httpMethod !in methodsWithoutBody) setBody(receive<ByteArray>())
    }.execute { response ->
        respondProxy(response)
    }
}

private suspend fun ApplicationCall.respondProxy(response: HttpResponse) {
    response.headers.forEach { name, values ->
        if (name !in responseHopByHopHeaders && name != HttpHeaders.ContentLength) {
            values.forEach { value -> this.response.header(name, value) }
        }
    }
    val upstreamBody = response.bodyAsChannel()
    respond(object : OutgoingContent.ReadChannelContent() {
        override val contentType = response.contentType()
        override val status = response.status

        override fun readFrom() = upstreamBody
    })
}

internal fun parseByteSize(value: String): Long? {
    val match = Regex("^(\\d+)([KMG]B)?$", RegexOption.IGNORE_CASE).matchEntire(value.trim()) ?: return null
    val amount = match.groupValues[1].toLongOrNull() ?: return null
    return amount * when (match.groupValues[2].uppercase()) {
        "KB" -> 1024
        "MB" -> 1024 * 1024
        "GB" -> 1024 * 1024 * 1024
        else -> 1
    }
}
