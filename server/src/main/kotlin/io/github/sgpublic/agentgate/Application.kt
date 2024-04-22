package io.github.sgpublic.agentgate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import io.github.sgpublic.agentgate.filters.TokenFilterImpl
import io.github.sgpublic.agentgate.filters.WebFilterImpl
import io.github.sgpublic.kotlin.util.log
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
import java.util.*

object Config: CliktCommand(
    name = "agent-gate",
) {
    val AGENT_GATE_BASE_PATH: String by option("--base-path", envvar = "AGENT_GATE_BASE_PATH")
        .default("/agent-gate")
        .help("listening path")
        .check("listening path should be start with '/' and not be end with '/'") {
            it.startsWith("/") && !it.endsWith("/")
        }
    val AGENT_GATE_BASE_PORT: Int by option("--base-port", envvar = "AGENT_GATE_BASE_PORT")
        .int()
        .default(1180)
        .help("listening port")
    val AGENT_GATE_BASE_DEBUG: Boolean by option("--base-debug", envvar = "AGENT_GATE_BASE_DEBUG")
        .boolean()
        .default(false)
        .help("debug mode")

    val AGENT_GATE_TARGET_URL: String by option("--target-url", envvar = "AGENT_GATE_TARGET_URL")
        .required()
        .help("target service url")
        .check("target service url should be start with 'https://' or 'http://' and not be end with '/'") {
            (it.startsWith("https://") || it.startsWith("http://")) && !it.endsWith("/")
        }
    val AGENT_GATE_TARGET_HOME_PATH: String by option("--target-home-path", envvar = "AGENT_GATE_TARGET_HOME_PATH")
        .default("/")
        .help("target service home path")
        .check("target service home path should be start with '/'") {
            it.startsWith("/")
        }
    private val _AGENT_GATE_TARGET_LOGO: String? by option("--target-logo", envvar = "AGENT_GATE_TARGET_LOGO")
        .help("target service logo url")
        .check("target service logo only support local file or network file") {
            it.startsWith("/") || it.startsWith("file://") ||
                    it.startsWith("http://") || it.startsWith("https://")
        }
    val AGENT_GATE_TARGET_LOGO: String by lazy {
        if (_AGENT_GATE_TARGET_LOGO != null) {
            return@lazy _AGENT_GATE_TARGET_LOGO!!
        }
        var logo =  "/favicon.ico"
        HOME_HTML?.let { html ->
            val links = html.head().getElementsByTag("link")
            var size = -1
            for (link in links) {
                try {
                    val ref = link.attribute("rel").value
                    if (ref != "icon" && ref != "apple-touch-icon") {
                        continue
                    }
                    if (link.hasAttr("sizes")) {
                        val curSize = link.attribute("sizes").value
                            .split("x").first().toInt()
                        if (curSize <= size) {
                            continue
                        }
                        size = curSize
                    } else if (size < 0) {
                        size = 0
                    }
                    logo = link.attribute("href").value
                } catch (e: Exception) {
                    log.debug("failed reading icon from home page of target service", e)
                }
            }
        }
        return@lazy logo
    }
    private val _AGENT_GATE_TARGET_NAME: String? by option("--target-name", envvar = "AGENT_GATE_TARGET_NAME")
        .help("target service name")
    val AGENT_GATE_TARGET_NAME: String by lazy {
        if (_AGENT_GATE_TARGET_NAME != null) {
            return@lazy _AGENT_GATE_TARGET_NAME!!
        }
        var name = BuildConfig.APPLICATION_ID
        HOME_HTML?.let { html ->
            try {
                name = html.title()
            } catch (e: Exception) {
                log.debug("failed reading title from home page of target service", e)
            }
        }
        return@lazy name
    }

    val AGENT_GATE_AUTH_USERNAME: String by option("--auth-username", envvar = "AGENT_GATE_AUTH_USERNAME")
        .required()
        .help("auth username")
    val AGENT_GATE_AUTH_PASSWORD: String by option("--auth-password", envvar = "AGENT_GATE_AUTH_PASSWORD")
        .required()
        .help("auth password")

    val AGENT_GATE_TOKEN_SECURITY_KEY: String by option("--token-security-key", envvar = "AGENT_GATE_TOKEN_SECURITY_KEY")
        .default(UUID.randomUUID().toString())
        .help("token secret key")
    val AGENT_GATE_TOKEN_EXPIRE: Long by option("--token-expire", envvar = "AGENT_GATE_TOKEN_EXPIRE")
        .long()
        .default(3600 * 24)
        .help("token expire time")
    val AGENT_GATE_TOKEN_COOKIE_KEY: String by option("--token-cookie-key", envvar = "AGENT_GATE_TOKEN_COOKIE_KEY")
        .default("X-AgentGate-Auth")
        .help("token cookie key")

    const val AGENT_GATE_JJWT_ISSUER = "agent-gate"
    const val AGENT_GATE_JJWT_TAG_KEY = "jjwt-tag"

    override fun run() {
        if (_AGENT_GATE_TARGET_LOGO == null) {
            log.info("target-logo not set, try reading...")
            log.info("get target-logo: $AGENT_GATE_TARGET_LOGO")
        }
        if (_AGENT_GATE_TARGET_NAME == null) {
            log.info("target-name not set, try reading...")
            log.info("get target-name: $AGENT_GATE_TARGET_NAME")
        }
        Bootstrap(Application::class.java)
            .setPort(AGENT_GATE_BASE_PORT)
            .setDebug(AGENT_GATE_BASE_DEBUG)
            .run(currentContext.originalArgv.toTypedArray())
    }

    private val HOME_HTML: Document? by lazy {
        val homePath = "$AGENT_GATE_TARGET_URL$AGENT_GATE_TARGET_HOME_PATH"
        log.debug("getting home html from: $homePath")
        for (index in 0 until 5) {
            try {
                return@lazy Jsoup.connect(homePath).get()
            } catch (e: Exception) {
                log.warn("getting home html failed, retrying...", e)
            }
        }
        return@lazy null
    }
}

@SpringBootApplication
class Application {
    @Bean
    fun staticResourceRouter(): RouterFunction<ServerResponse> {
        return RouterFunctions.resources("${Config.AGENT_GATE_BASE_PATH}/web/**", ClassPathResource("public/"))
    }

    @Bean
    fun gatewayRouteLocator(builder: RouteLocatorBuilder, throttle: AddRequestHeaderGatewayFilterFactory): RouteLocator {
        return builder.routes()
            .route("static") { r ->
                r.path("${Config.AGENT_GATE_BASE_PATH}/web/**")
                    .filters {
                        it.filter(WebFilterImpl)
                    }
                    .uri("classpath:/public/")
            }
            .route("static") { r ->
                r.path("${Config.AGENT_GATE_BASE_PATH}/api/**")
                    .filters {
                        it.filter(TokenFilterImpl)
                    }
                    .uri("classpath:/public/")
            }
            .route("others") { r ->
                r.path("/**")
                    .filters {
                        it.filter(TokenFilterImpl)
                    }
                    .uri(Config.AGENT_GATE_TARGET_URL)
            }
            .build()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Config.main(args)
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
