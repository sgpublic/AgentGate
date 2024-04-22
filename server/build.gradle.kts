import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import io.github.torrentpicker.findEnv
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    alias(ag.plugins.docker.api)

    alias(ag.plugins.spring.boot)
    alias(ag.plugins.spring.deps)

    alias(ag.plugins.kotlin.plugin.jvm)
    alias(ag.plugins.kotlin.plugin.serialization)
    alias(ag.plugins.kotlin.plugin.spring)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val archiveName = "agent-gate"

tasks.bootJar {
    archiveBaseName = archiveName
}

springBoot {
    mainClass = "io.github.sgpublic.agentgate.Application"
}

dependencyManagement {
    imports {
        mavenBom(ag.spring.cloud.deps.get().toString())
    }
}

dependencies {
    implementation(ag.kotlin.reflect)
    implementation(ag.kotlinx.serialization.json)

    implementation(ag.spring.cloud.starter.gateway)

    implementation(ag.uniktx.kotlin.common)
    implementation(ag.uniktx.kotlin.logback)

    implementation(ag.jjwt.api)
    runtimeOnly(ag.jjwt.impl)
    runtimeOnly(ag.jjwt.gson)
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

