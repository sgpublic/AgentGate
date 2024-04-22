package io.github.sgpublic.agentgate.controller

import io.github.sgpublic.agentgate.Config
import io.github.sgpublic.agentgate.core.utils.JsonGlobal
import io.github.sgpublic.agentgate.exception.FailedResult
import io.github.sgpublic.agentgate.exception.SuccessResult
import io.github.sgpublic.agentgate.exception.write
import io.github.sgpublic.agentgate.service.AuthService.createNewToken
import io.github.sgpublic.agentgate.service.InfoJson
import io.github.sgpublic.agentgate.service.LoginDto
import io.github.sgpublic.kotlin.util.log
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


object ApiController {
    val agentGateApi = hashMapOf<Pair<String, HttpMethod>, ServerWebExchange.() -> Mono<Void>>(
        ("/login" to HttpMethod.POST) to { checkAuth() },
        ("/info" to HttpMethod.GET) to { info() },
    )

    fun ServerWebExchange.checkAuth(): Mono<Void> {
        return DataBufferUtils.join(request.body)
            .flatMap { body ->
                val rawBody: String = body.asInputStream(true).use {
                    it.reader().readText()
                }

                val dto = JsonGlobal.decodeFromString(LoginDto.serializer(), rawBody)

                if (dto.username == Config.AGENT_GATE_AUTH_USERNAME && dto.password == Config.AGENT_GATE_AUTH_PASSWORD) {
                    response.headers.add(
                        "Set-Cookie",
                        "${Config.AGENT_GATE_TOKEN_COOKIE_KEY}=${createNewToken()}; " +
                                "Max-Age=${Config.AGENT_GATE_TOKEN_EXPIRE}; " +
                                "Path=/; "
                    )
                    return@flatMap response.write(SuccessResult)
                } else {
                    return@flatMap response.write(FailedResult.Auth.WrongPassword)
                }
            }
    }

    fun ServerWebExchange.info(): Mono<Void> {
        log.debug("get agent-gate info: $InfoJson")
        return response.write(InfoJson)
    }
}
