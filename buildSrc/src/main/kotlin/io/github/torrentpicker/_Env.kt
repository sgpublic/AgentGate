package io.github.torrentpicker

import org.gradle.api.Project

fun Project.findEnv(name: String): String {
    return findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
            ?: System.getenv(name.replace(".", "_"))
}