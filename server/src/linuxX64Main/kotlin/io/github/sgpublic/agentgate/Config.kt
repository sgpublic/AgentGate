package io.github.sgpublic.agentgate

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long

internal object AgentGateCommand : CoreSuspendingCliktCommand(name = "agent-gate") {
    val port by option("--port", envvar = "AGENT_GATE_PORT").int().default(1180)
    val targetUrl by option("--target-url", envvar = "AGENT_GATE_TARGET_URL").required()
    val configuredTargetLogo by option("--target-logo", envvar = "AGENT_GATE_TARGET_LOGO")
    val configuredTargetName by option("--target-name", envvar = "AGENT_GATE_TARGET_NAME")
    val targetWaitRetryDuration by option("--target-wait-retry-duration", envvar = "AGENT_GATE_TARGET_WAIT_RETRY_DURATION")
        .long()
        .default(5000)
    val targetWaitRetryTimes by option("--target-wait-retry-times", envvar = "AGENT_GATE_TARGET_WAIT_RETRY_TIMES")
        .int()
        .default(10)
    val authUsername by option("--auth-username", envvar = "AGENT_GATE_AUTH_USERNAME").required()
    val authPassword by option("--auth-password", envvar = "AGENT_GATE_AUTH_PASSWORD").required()
    val authAllowBasic by option("--auth-allow-basic", envvar = "AGENT_GATE_AUTH_ALLOW_BASIC").flag()
    val sessionCookieKey by option("--session-cookie-key", envvar = "AGENT_GATE_SESSION_COOKIE_KEY")
        .default("X-AgentGate-Session")
    val sessionExpire by option("--session-expire", envvar = "AGENT_GATE_SESSION_EXPIRE")
        .long()
        .default(3600 * 24)
    val requestMaxSize by option("--request-max-size", envvar = "AGENT_GATE_REQUEST_MAX_SIZE")

    override suspend fun run() {
        require((targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) && !targetUrl.endsWith('/')) {
            "target URL must start with http:// or https:// and must not end with /"
        }
        require(targetWaitRetryDuration > 0) { "target wait retry duration must be greater than 0" }
        require(targetWaitRetryTimes != 0) { "target wait retry times must not be 0" }
        val configuredRequestMaxSize = requestMaxSize
        require(configuredRequestMaxSize == null || parseByteSize(configuredRequestMaxSize) != null) {
            "request max size must be a byte count or use KB, MB, or GB"
        }
        startAgentGate(this)
    }
}
