package io.github.sgpublic.agentgate.security

import io.github.sgpublic.agentgate.core.utils.JsonGlobal
import io.github.sgpublic.agentgate.core.utils.findEnv
import io.github.sgpublic.agentgate.exception.FailedResult
import io.github.sgpublic.agentgate.exception.SuccessResult
import io.github.sgpublic.agentgate.exception.write
import io.github.sgpublic.kotlin.core.util.MD5_FULL_COMPRESSED
import io.github.sgpublic.kotlin.core.util.toGson
import io.github.sgpublic.kotlin.util.log
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.serialization.Serializable
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.ResponseCookie
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*
import javax.crypto.SecretKey


private val AGENT_GATE_USERNAME: String by lazy {
    findEnv("AGENT_GATE_USERNAME")
        ?: throw IllegalArgumentException("缺少用户名配置!")
}
private val AGENT_GATE_PASSWORD: String by lazy {
    findEnv("AGENT_GATE_PASSWORD")
        ?: throw IllegalArgumentException("缺少密码配置!")
}
private val AGENT_GATE_TOKEN_SECURITY_KEY: String by lazy {
    findEnv("AGENT_GATE_TOKEN_SECURITY_KEY", UUID.randomUUID().toString())
}
private val AGENT_GATE_TOKEN_EXPIRE: Long by lazy {
    findEnv("AGENT_GATE_TOKEN_EXPIRE", 3600 * 24)
}
val AGENT_GATE_HEADER_KEY: String by lazy {
    findEnv("AGENT_GATE_HEADER_KEY", "X-AgentGate-Auth")
}
private const val AGENT_GATE_JJWT_ISSUER = "agent-gate"
private const val AGENT_GATE_JJWT_TAG_KEY = "jjwt-tag"

fun newJjwtTag(issuedAt: Date): String {
    return "$AGENT_GATE_USERNAME:$AGENT_GATE_PASSWORD@${issuedAt.time}[$AGENT_GATE_TOKEN_SECURITY_KEY]"
        .MD5_FULL_COMPRESSED
}

private val secretKey: SecretKey by lazy {
    Keys.hmacShaKeyFor(AGENT_GATE_TOKEN_SECURITY_KEY.toByteArray())
}
private val parser: JwtParser by lazy {
    return@lazy Jwts.parser().verifyWith(secretKey).build()
}

fun String.checkTag(): FailedResult? {
    try {
        val claims: Claims = parser.parseSignedClaims(this).payload
        if (claims.expiration.before(Date())) {
            log.debug("token 过期")
            return null
        }
        if (claims.issuer != AGENT_GATE_JJWT_ISSUER) {
            log.debug("未知的 token 签发者")
            return null
        }
        val tag = claims.get(AGENT_GATE_JJWT_TAG_KEY, String::class.java)
        if (tag == newJjwtTag(claims.issuedAt)) {
            return null
        }
        log.info("token 无效")
    } catch (e: JwtException) {
        log.debug("未知错误", e)
    } catch (e: IllegalArgumentException) {
        log.debug("未知错误", e)
    } catch (e: ClassCastException) {
        log.debug("未知错误", e)
    }
    return FailedResult.Auth.ExpiredToken
}

@Serializable
data class LoginDto(
    val username: String,
    val password: String,
    val rememberMe: Boolean = false,
)

internal fun ServerWebExchange.checkAuth(): Mono<Void> {
    return DataBufferUtils.join(request.body)
        .flatMap { body ->
            val rawBody: String = body.asInputStream(true).use {
                it.reader().readText()
            }
            val dto = JsonGlobal.decodeFromString(LoginDto.serializer(), rawBody)

            return@flatMap if (dto.username == AGENT_GATE_USERNAME && dto.password == AGENT_GATE_PASSWORD) {
                response.write(FailedResult.Auth.WrongPassword)
            } else {
                response.headers.add("Set-Cookie", "$AGENT_GATE_HEADER_KEY=${createNewToken()};")
                response.write(SuccessResult)
            }
        }
}

private fun createNewToken(): String {
    val date = Date()
    return Jwts.builder()
        .claims()
        .issuer(AGENT_GATE_JJWT_ISSUER)
        .add(AGENT_GATE_JJWT_TAG_KEY, newJjwtTag(date))
        .and()
        .issuedAt(date)
        .expiration(Date(date.time + AGENT_GATE_TOKEN_EXPIRE * 1000))
        .signWith(secretKey)
        .compact()
}