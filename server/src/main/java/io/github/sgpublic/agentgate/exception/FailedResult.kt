package io.github.sgpublic.agentgate.exception

import io.github.sgpublic.agentgate.core.utils.JsonGlobal
import org.springframework.http.server.reactive.ServerHttpResponse
import kotlinx.serialization.Serializable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * @author sgpublic
 * @Date 2023/2/8 9:15
 */
class FailedResult private constructor(
    private val code: Int,
    override val message: String,
): RuntimeException(message) {
    companion object {
        val AnonymousDenied get() = FailedResult(-4050, "请登陆后再试")

        val InternalServerError get() = FailedResult(-5002, "服务器内部错误")
    }

    fun resp(): RespResult {
        return RespResult(code, message)
    }

    object Auth {
        val WrongPassword get() = FailedResult(-110102, "用户不存在或密码错误")
        val ExpiredToken get() = FailedResult(-110105, "无效的 token")
    }

}

val SuccessResult = RespResult(200, "success.")

@Serializable
data class RespResult(
    val code: Int,
    val message: String,
)

fun ServerHttpResponse.write(result: FailedResult): Mono<Void> {
    return write(result.resp())
}

fun ServerHttpResponse.write(result: RespResult): Mono<Void> {
    return write(JsonGlobal.encodeToString(
        RespResult.serializer(), result
    ))
}

fun ServerHttpResponse.write(result: String): Mono<Void> {
    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    return writeWith(Mono.just(bufferFactory().wrap(
        result.toByteArray()
    )))
}
