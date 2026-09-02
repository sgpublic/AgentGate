package io.github.sgpublic.agentgate

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val agentGateJson = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

@Serializable
internal data class AuthSession(val authenticated: Boolean = true)

@Serializable
internal data class LoginDto(val username: String, val password: String)

@Serializable
internal data class Result(val code: Int, val message: String)

@Serializable
internal data class Info(
    val versionName: String,
    val serviceName: String,
    val serviceLogo: String,
)

internal fun ApplicationCall.isAuthenticated(config: AgentGateCommand): Boolean {
    if (sessions.get<AuthSession>()?.authenticated == true) return true
    if (!config.authAllowBasic) return false

    val authorization = request.headers[HttpHeaders.Authorization] ?: return false
    if (!authorization.startsWith("Basic ")) return false
    val decoded = authorization.removePrefix("Basic ").decodeBase64() ?: return false
    val separator = decoded.indexOf(':')
    return separator >= 0 && decoded.substring(0, separator) == config.authUsername &&
        decoded.substring(separator + 1) == config.authPassword
}

internal suspend fun ApplicationCall.respondWrongPassword() {
    respond(HttpStatusCode.Unauthorized, Result(-110102, "用户名或密码错误"))
}

private fun String.decodeBase64(): String? = runCatching {
    kotlin.io.encoding.Base64.decode(this).decodeToString()
}.getOrNull()
