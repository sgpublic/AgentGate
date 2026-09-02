package io.github.sgpublic.agentgate

import io.github.sgpublic.embedraw.EmbeddedRawResources
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.set
import io.ktor.server.sessions.sessions
import io.ktor.serialization.kotlinx.json.json

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
            val login = call.receive<LoginDto>()
            if (login.username == config.authUsername && login.password == config.authPassword) {
                call.sessions.set(AuthSession())
                call.respond(Result(200, "success."))
            } else {
                call.respondWrongPassword()
            }
        }
        get("/api/info") {
            call.respond(
                Info(
                    versionName = "2.0.0",
                    serviceName = targetMetadata.name,
                    serviceLogo = targetMetadata.logo,
                ),
            )
        }
        post("/api/logout") {
            call.sessions.clear<AuthSession>()
            call.respond(Result(200, "success."))
        }
        get("/") {
            if (call.isAuthenticated(config)) {
                call.proxy(client, config)
            } else {
                call.respondEmbeddedResource(resources, "index.html")
            }
        }
        get("{path...}") {
            val path = call.request.path()
            if (call.isAuthenticated(config)) {
                call.proxy(client, config)
            } else if (targetMetadata.logo == path && path.startsWith('/')) {
                call.proxy(client, config)
            } else if (path.contains('.') && call.respondEmbeddedResource(resources, path.trimStart('/'))) {
                return@get
            } else {
                call.respondRedirect("/", permanent = false)
            }
        }
    }
}
