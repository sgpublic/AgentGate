package io.github.sgpublic.agentgate.core.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json


@OptIn(ExperimentalSerializationApi::class)
val JsonGlobal = Json {
    encodeDefaults = true
    explicitNulls = false
    useAlternativeNames = true
    ignoreUnknownKeys = true
    decodeEnumsCaseInsensitive = true
}
