package io.github.sgpublic.agentgate

import io.github.sgpublic.embedraw.EmbeddedRawResources
import io.ktor.client.HttpClient
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set

internal fun Application.agentGate(
    config: AgentGateCommand,
    targetMetadata: TargetMetadataValue,
    resources: EmbeddedRawResources,
    client: HttpClient,
) {
    install(ContentNegotiation) { json(agentGateJson) }
    install(Sessions) {
        cookie<AuthSession>(config.sessionCookieKey, SessionStorageMemory()) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = config.sessionExpire.takeIf { it >= 0 }
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    routing {
        post("/api/login") {
            if (call.isAuthenticated(config)) {
                call.proxy(client, config)
            } else {
                val login = call.receive<LoginDto>()
                if (login.username == config.authUsername && login.password == config.authPassword) {
                    call.sessions.set(AuthSession())
                    call.respond(Result(200, "success."))
                } else {
                    call.respondWrongPassword()
                }
            }
        }
        get("/api/info") {
            if (call.isAuthenticated(config)) {
                call.proxy(client, config)
            } else {
                call.respond(
                    Info(
                        versionName = BuildKonfig.VERSION,
                        serviceName = targetMetadata.name,
                        serviceLogo = targetMetadata.logo,
                    ),
                )
            }
        }
        post("/api/logout") {
            if (call.isAuthenticated(config)) {
                call.proxy(client, config)
            } else {
                call.sessions.clear<AuthSession>()
                call.respond(Result(200, "success."))
            }
        }
        get("/") {
            if (call.isAuthenticated(config)) {
                call.proxy(client, config)
            } else {
                call.respondEmbeddedResource(resources, "index.html")
            }
        }
        get("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources, allowPublicResources = true) }
        post("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources) }
        put("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources) }
        patch("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources) }
        delete("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources) }
        head("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources) }
        options("{path...}") { call.proxyOrRedirect(client, config, targetMetadata, resources) }
    }
}

private suspend fun ApplicationCall.proxyOrRedirect(
    client: HttpClient,
    config: AgentGateCommand,
    targetMetadata: TargetMetadataValue,
    resources: EmbeddedRawResources,
    allowPublicResources: Boolean = false,
) {
    val path = request.path()
    if (isAuthenticated(config)) {
        proxy(client, config)
    } else if (allowPublicResources && targetMetadata.logo == path && path.startsWith('/')) {
        proxy(client, config)
    } else if (allowPublicResources && path.contains('.') && respondEmbeddedResource(resources, path.trimStart('/'))) {
        return
    } else {
        respondRedirect("/", permanent = false)
    }
}
