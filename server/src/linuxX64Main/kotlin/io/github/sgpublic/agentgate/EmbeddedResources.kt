package io.github.sgpublic.agentgate

import io.github.sgpublic.embedraw.EmbeddedRawResources
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondBytes

internal suspend fun ApplicationCall.respondEmbeddedResource(resources: EmbeddedRawResources, path: String): Boolean {
    val bytes = resources[path] ?: return false
    val contentType = when (path.substringAfterLast('.', "").lowercase()) {
        "html" -> ContentType.Text.Html
        "js", "mjs" -> ContentType.Application.JavaScript
        "css" -> ContentType.Text.CSS
        "json" -> ContentType.Application.Json
        "svg" -> ContentType.Image.SVG
        "png" -> ContentType.Image.PNG
        "jpg", "jpeg" -> ContentType.Image.JPEG
        "gif" -> ContentType.Image.GIF
        "ico" -> ContentType.Image.XIcon
        "woff" -> ContentType("font", "woff")
        "woff2" -> ContentType("font", "woff2")
        else -> ContentType.Application.OctetStream
    }
    respondBytes(bytes, contentType)
    return true
}
