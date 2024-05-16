import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import io.github.torrentpicker.findEnv
import io.github.torrentpicker.GIT_HEAD
import io.github.torrentpicker.COMMIT_COUNT_VERSION
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(ag.plugins.docker.api)
    alias(ag.plugins.buildconfig)

    alias(ag.plugins.spring.boot)
    alias(ag.plugins.spring.deps)

    alias(ag.plugins.kotlin.plugin.jvm)
    alias(ag.plugins.kotlin.plugin.serialization)
    alias(ag.plugins.kotlin.plugin.spring)

    application
}

group = "${rootProject.group}.agentgate"
version = rootProject.version

val mMainClass = "$group.Application"
application {
    mainClass = mMainClass
}
springBoot {
    mainClass = mMainClass
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(ag.kotlin.reflect)
    implementation(ag.kotlinx.serialization.json)

    implementation(ag.spring.cloud.starter.gateway)

    implementation(ag.uniktx.kotlin.common)
    implementation(ag.uniktx.kotlin.logback)

    implementation(ag.clikt)
    implementation(ag.jsoup)

    implementation(ag.jjwt.api)
    runtimeOnly(ag.jjwt.impl)
    runtimeOnly(ag.jjwt.gson)
}

val archiveName = "agent-gate"

tasks.bootJar {
    archiveBaseName = archiveName
}

dependencyManagement {
    imports {
        mavenBom(ag.spring.cloud.deps.get().toString())
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

buildTimeConfig {
    config {
        packageName = "$group"
        objectName = "BuildConfig"
        destination = project.layout.buildDirectory.asFile

        configProperties {
            val VERSION_NAME: String by string("$version")
            val VERSION_CODE: Int by int(COMMIT_COUNT_VERSION)
            val COMMIT_ID: String by string(GIT_HEAD)
            val APPLICATION_ID: String by string(rootProject.name)
        }
    }
}

tasks {
    val clean by getting
    val assembleDist by getting {
        dependsOn(":web:runBuild")
    }

    val installDist by getting

    val dockerCreateDockerfile by creating(Dockerfile::class) {
        group = "docker"
        from("openjdk:17-slim-bullseye")
        workingDir("/app")
        copy {
            copyFile("./install/$archiveName", "/app")
        }
        runCommand(listOf(
            "useradd -u 1000 runner",
            "apt-get update",
            "apt-get install findutils -y",
            "chown -R runner:runner /app"
        ).joinToString(" &&\\\n "))
        user("runner")
        volume("/app/config.yaml")
        entryPoint("/app/bin/$archiveName")
    }

    val tag = "mhmzx/$archiveName"
    val dockerBuildImage by creating(DockerBuildImage::class) {
        group = "docker"
        dependsOn(assembleDist, installDist, dockerCreateDockerfile)
        inputDir = project.file("./build")
        dockerFile = dockerCreateDockerfile.destFile
        images.add("$tag:$version")
        images.add("$tag:latest")
        noCache = true
    }

    val dockerPushImageOfficial by creating(DockerPushImage::class) {
        group = "docker"
        dependsOn(dockerBuildImage)
        images.add("$tag:${rootProject.version}")
        images.add("$tag:latest")
    }
}

docker {
    registryCredentials {
        username = findEnv("publishing.docker.username")
        password = findEnv("publishing.docker.password")
        email = findEnv("publishing.developer.email")
    }
}

