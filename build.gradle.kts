plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.embed.raw) apply false
    alias(libs.plugins.node) apply false
    alias(libs.plugins.docker) apply false
}

group = "io.github.sgpublic"
version = "2.0.0"
