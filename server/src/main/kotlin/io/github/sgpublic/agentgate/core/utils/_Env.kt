package io.github.sgpublic.agentgate.core.utils


fun <T: Any> findEnv(name: String, def: T): T {
    val env = System.getenv(name)
        ?.takeIf { it.isNotBlank() }
    return when (def) {
        is String -> env
        is Double -> env?.toDoubleOrNull()
        is Int -> env?.toIntOrNull()
        is Float -> env?.toFloatOrNull()
        is Long -> env?.toLongOrNull()
        is Boolean -> env?.lowercase()?.toBooleanStrictOrNull()
        else -> throw IllegalArgumentException("不支持的 type")
    } as T? ?: def
}

inline fun <reified T: Any> findEnv(name: String): T? {
    val env = System.getenv(name)
        ?.takeIf { it.isNotBlank() }
    return (when (T::class) {
        String::class -> env
        Double::class -> env?.toDoubleOrNull()
        Int::class -> env?.toIntOrNull()
        Float::class -> env?.toFloatOrNull()
        Long::class -> env?.toLongOrNull()
        Boolean::class -> env?.lowercase()?.toBooleanStrictOrNull()
        else -> throw IllegalArgumentException("不支持的 type")
    }) as T?
}
