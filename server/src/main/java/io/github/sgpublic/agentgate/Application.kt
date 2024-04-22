package io.github.sgpublic.agentgate

import io.github.sgpublic.agentgate.core.utils.findEnv
import io.github.sgpublic.agentgate.security.AGENT_GATE_AUTH_PATH
import io.github.sgpublic.agentgate.security.TokenFilterImpl
import io.github.sgpublic.agentgate.security.WebFilterImpl
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse


@SpringBootApplication
class Application {
    private val AGENT_GATE_TARGET: String by lazy {
        return@lazy findEnv("AGENT_GATE_TARGET")
            ?: throw IllegalArgumentException("缺少转发目标配置！")
    }

    @Bean
    fun staticResourceRouter(): RouterFunction<ServerResponse> {
        return RouterFunctions.resources("${AGENT_GATE_AUTH_PATH}/web/**", ClassPathResource("public/"))
    }

    @Bean
    fun gatewayRouteLocator(builder: RouteLocatorBuilder, throttle: AddRequestHeaderGatewayFilterFactory): RouteLocator {
        return builder.routes()
            .route("static") { r ->
                r.path("${AGENT_GATE_AUTH_PATH}/web/**")
                    .filters {
                        it.filter(WebFilterImpl)
                    }
                    .uri("classpath:/public/")
            }
            .route("others") { r ->
                r.path("/**")
                    .filters {
                        it.filter(TokenFilterImpl)
                    }
                    .uri(AGENT_GATE_TARGET)
            }
            .build()
    }

    companion object {
        val AGENT_GATE_DEBUG: Boolean by lazy {
            findEnv("AGENT_GATE_DEBUG", false)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            Bootstrap(Application::class.java)
                .setPort(findEnv("AGENT_GATE_PORT", 1180))
                .setDebug(AGENT_GATE_DEBUG)
                .run(args)
        }
    }
}


private class Bootstrap(clazz: Class<*>) {
    private val application: SpringApplication = SpringApplication(clazz)
    private val properties: MutableMap<String, Any> = HashMap()

    fun setDatasource(
        dbHost: String, dbDatabase: String,
        dbUsername: String, dbPassword: String
    ): Bootstrap {
        properties["spring.datasource.username"] = dbUsername
        properties["spring.datasource.password"] = dbPassword
        properties["spring.datasource.url"] = "jdbc:mariadb://$dbHost/$dbDatabase"
        return this
    }

    fun setPort(port: Int): Bootstrap {
        properties["server.port"] = port
        return this
    }

    fun setDebug(isDebug: Boolean): Bootstrap {
        if (isDebug) {
            properties["spring.profiles.active"] = "dev"
        } else {
            properties["spring.profiles.active"] = "prod"
        }
        return this
    }

    fun test(): Bootstrap {
        properties["spring.profiles.active"] = "test"
        return this
    }

    fun others(block: MutableMap<String, Any>.() -> Unit): Bootstrap {
        block.invoke(properties)
        return this
    }

    fun run(args: Array<String>): ApplicationContext {
        application.setDefaultProperties(properties)
        return application.run(*args)
    }
}
