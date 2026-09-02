package io.github.sgpublic.agentgate

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal class TargetMetadata(private val config: AgentGateCommand) {
    suspend fun load(): TargetMetadataValue {
        val html = fetchHomeHtml()
        val name = config.configuredTargetName ?: html?.extractTitle() ?: "AgentGate"
        val logo = config.configuredTargetLogo ?: html?.extractIcon() ?: "/favicon.ico"
        return TargetMetadataValue(name, logo)
    }

    private suspend fun fetchHomeHtml(): String? {
        val client = HttpClient(ClientCIO)
        var retries = config.targetWaitRetryTimes
        while (retries != 0) {
            try {
                val response = client.get("${config.targetUrl}/")
                if (response.status.isSuccess()) return response.bodyAsText()
            } catch (_: Exception) {
                // The target may not be ready when AgentGate starts.
            }
            if (retries > 0) retries--
            if (retries != 0) delay(config.targetWaitRetryDuration.milliseconds)
        }
        return null
    }
}

internal data class TargetMetadataValue(val name: String, val logo: String)

private fun String.extractTitle(): String? =
    Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(this)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)

private fun String.extractIcon(): String? =
    Regex("<link[^>]+rel=[\"'][^\"']*(?:icon|apple-touch-icon)[^\"']*[\"'][^>]*href=[\"']([^\"']+)", RegexOption.IGNORE_CASE)
        .find(this)?.groupValues?.get(1)
