package io.github.sgpublic.agentgate.filters

import io.github.sgpublic.agentgate.Config
import io.github.sgpublic.agentgate.service.AuthService.checkTag
import io.github.sgpublic.kotlin.util.log
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI


object WebFilterImpl: GatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request: ServerHttpRequest = exchange.request
        val path: String = request.path.value()
        log.info("${request.method} $path")
        val auth = request.cookies[Config.AGENT_GATE_TOKEN_COOKIE_KEY]
            ?.firstOrNull()?.value
        if (auth != null && checkTag(auth) == null) {
            return exchange.response
                .also {
                    it.statusCode = HttpStatus.FOUND
                    it.headers.location = URI.create("${Config.AGENT_GATE_BASE_PATH}${Config.AGENT_GATE_TARGET_HOME_PATH}")
                }
                .setComplete()
        }
        if (path == "${Config.AGENT_GATE_BASE_PATH}/web") {
            return exchange.response
                .also {
                    it.statusCode = HttpStatus.MOVED_PERMANENTLY
                    it.headers.location = URI.create("${Config.AGENT_GATE_BASE_PATH}/web/")
                }
                .setComplete()
        }
        if (path == "${Config.AGENT_GATE_BASE_PATH}/web/") {
            return exchange.response
                .also {
                    it.statusCode = HttpStatus.MOVED_PERMANENTLY
                    it.headers.location = URI.create("${Config.AGENT_GATE_BASE_PATH}/web/index.html")
                }
                .setComplete()
        }
        return chain.filter(exchange)
    }
}