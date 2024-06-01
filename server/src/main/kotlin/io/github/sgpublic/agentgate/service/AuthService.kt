package io.github.sgpublic.agentgate.service

import io.github.sgpublic.agentgate.Config
import io.github.sgpublic.agentgate.exception.FailedResult
import io.github.sgpublic.kotlin.core.util.BASE_64
import io.github.sgpublic.kotlin.core.util.MD5_FULL_COMPRESSED
import io.github.sgpublic.kotlin.core.util.ORIGIN_BASE64
import io.github.sgpublic.kotlin.util.log
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.serialization.Serializable
import org.jsoup.Connection.Base
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Serializable
data class LoginDto(
    val username: String,
    val password: String,
    val rememberMe: Boolean = false,
)

object AuthService {
    fun newJjwtTag(issuedAt: Date): String {
        return "${Config.AGENT_GATE_AUTH_USERNAME}:${Config.AGENT_GATE_AUTH_PASSWORD}@${issuedAt.time}[${Config.AGENT_GATE_TOKEN_SECURITY_KEY}]"
            .MD5_FULL_COMPRESSED
    }

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Config.AGENT_GATE_TOKEN_SECURITY_KEY.toByteArray())
    }
    private val parser: JwtParser by lazy {
        return@lazy Jwts.parser().verifyWith(secretKey).build()
    }

    fun checkBasicAuth(basic: String): FailedResult? {
        val (username, password) = basic.substring(6)
            .ORIGIN_BASE64.toString(StandardCharsets.UTF_8)
            .split(":")
        return FailedResult.Auth.WrongPassword.takeUnless {
            username == Config.AGENT_GATE_AUTH_USERNAME && password == Config.AGENT_GATE_AUTH_PASSWORD
        }
    }

    fun checkTag(token: String): FailedResult? {
        try {
            val claims: Claims = parser.parseSignedClaims(token).payload
            if (claims.expiration.before(Date())) {
                log.debug("token expired!")
                return FailedResult.Auth.ExpiredToken
            }
            if (claims.issuer != Config.AGENT_GATE_JJWT_ISSUER) {
                log.debug("unknown token issuer")
                return FailedResult.Auth.ExpiredToken
            }
            val tag = claims.get(Config.AGENT_GATE_JJWT_TAG_KEY, String::class.java)
            val trueTag = newJjwtTag(claims.issuedAt)
            if (tag == trueTag) {
                return null
            }
            log.info("token invalid!")
        } catch (e: JwtException) {
            log.debug("unknown error", e)
        } catch (e: IllegalArgumentException) {
            log.debug("unknown error", e)
        } catch (e: ClassCastException) {
            log.debug("unknown error", e)
        }
        return FailedResult.Auth.ExpiredToken
    }

    fun createNewToken(): String {
        val date = Date()
        date.time = date.time / 1000 * 1000
        return Jwts.builder()
            .claims()
            .issuedAt(date)
            .issuer(Config.AGENT_GATE_JJWT_ISSUER)
            .add(Config.AGENT_GATE_JJWT_TAG_KEY, newJjwtTag(date))
            .and()
            .expiration(Date(date.time + Config.AGENT_GATE_TOKEN_EXPIRE * 1000))
            .signWith(secretKey)
            .compact()
    }
}