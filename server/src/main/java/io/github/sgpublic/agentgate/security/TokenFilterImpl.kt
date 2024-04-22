package io.github.sgpublic.agentgate.security

import io.github.sgpublic.agentgate.core.utils.findEnv
import io.github.sgpublic.agentgate.exception.FailedResult
import io.github.sgpublic.agentgate.exception.write
import io.github.sgpublic.kotlin.util.log
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

val AGENT_GATE_AUTH_PATH: String by lazy {
    findEnv("AGENT_GATE_AUTH_PATH", "/agent-gate")
}

object WebFilterImpl: GatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request: ServerHttpRequest = exchange.request
        val path: String = request.path.value()
        log.info("${request.method} $path")
        if (path == "$AGENT_GATE_AUTH_PATH/web") {
            return exchange.response
                .also {
                    it.statusCode = HttpStatus.MOVED_PERMANENTLY
                    it.headers.location = URI.create("${AGENT_GATE_AUTH_PATH}/web/")
                }
                .setComplete()
        }
        if (path == "$AGENT_GATE_AUTH_PATH/web/") {
            return exchange.response
                .also {
                    it.statusCode = HttpStatus.MOVED_PERMANENTLY
                    it.headers.location = URI.create("${AGENT_GATE_AUTH_PATH}/web/index.html")
                }
                .setComplete()
        }
        return chain.filter(exchange)
    }
}

object TokenFilterImpl: GatewayFilter {
    private val pass = setOf(
        "/favicon.ico"
    )

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request: ServerHttpRequest = exchange.request

        val path: String = request.path.value()
        log.info("${request.method} $path")

        if (pass.contains(path)) {
            return chain.filter(exchange)
        }

        // 如果访问的接口为登录接口则执行认证
        if (path.startsWith("$AGENT_GATE_AUTH_PATH/login") && request.method == HttpMethod.POST) {
            return exchange.checkAuth()
        }
//        if (path.startsWith("$AGENT_GATE_AUTH_PATH/web")) {
//            return chain.filter(exchange)
//        }

        val auth = request.cookies[AGENT_GATE_HEADER_KEY]
            ?.firstOrNull()?.value
        val errorType = if (auth != null) {
            auth.checkTag()
        } else {
            FailedResult.Auth.ExpiredToken
        }
        if (errorType == null) {
            return chain.filter(exchange)
        }

        return exchange.response
            .also {
                it.statusCode = HttpStatus.MOVED_TEMPORARILY
                it.headers.location = URI.create("${AGENT_GATE_AUTH_PATH}/web/")
            }
            .write(errorType)
    }
}