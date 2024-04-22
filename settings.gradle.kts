/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 * For more detailed information on multi-project builds, please refer to https://docs.gradle.org/8.5/userguide/building_swift_projects.html in the Gradle documentation.
 * This project uses @Incubating APIs which are subject to change.
 */

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    // https://docs.gradle.org/current/userguide/platforms.html#sec:importing-catalog-from-file
    versionCatalogs {
        val ag by creating {
            from(files("./gradle/ag.versions.toml"))
        }
    }
}

rootProject.name = "AgentGate"
include("server", "web")