package io.github.sgpublic.agentgate.filters

import io.github.sgpublic.agentgate.Config
import io.github.sgpublic.agentgate.controller.ApiController
import io.github.sgpublic.agentgate.exception.FailedResult
import io.github.sgpublic.agentgate.exception.write
import io.github.sgpublic.agentgate.service.AuthService.checkTag
import io.github.sgpublic.kotlin.util.log
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

object TokenFilterImpl: GatewayFilter {
    private val pass = buildSet {
        if (Config.AGENT_GATE_TARGET_LOGO.startsWith("/")) {
            add(Config.AGENT_GATE_TARGET_LOGO)
        }
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request: ServerHttpRequest = exchange.request

        val path: String = request.path.value()
        log.info("${request.method} $path")

        if (pass.contains(path)) {
            return chain.filter(exchange)
        }

        if (path.startsWith("${Config.AGENT_GATE_BASE_PATH}/api")) {
            val apiPath = path.replace("${Config.AGENT_GATE_BASE_PATH}/api", "")
            val func = ApiController.agentGateApi[apiPath to request.method]
            return if (func == null) {
                exchange.response
                    .also {
                        it.statusCode = HttpStatus.NOT_FOUND
                    }
                    .setComplete()
            } else {
                exchange.func()
            }
        }

        val auth = request.cookies[Config.AGENT_GATE_TOKEN_COOKIE_KEY]
            ?.firstOrNull()?.value
        val errorType = if (auth != null) {
            checkTag(auth)
        } else {
            FailedResult.Auth.ExpiredToken
        }
        if (errorType == null) {
            return chain.filter(exchange)
        }

        return exchange.response
            .also {
                it.statusCode = HttpStatus.MOVED_TEMPORARILY
                it.headers.location = URI.create("${Config.AGENT_GATE_BASE_PATH}/web/")
            }
            .write(errorType)
    }
}