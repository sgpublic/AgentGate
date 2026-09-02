import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.tasks.Sync

plugins {
    alias(libs.plugins.docker)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.embed.raw)
}

kotlin {
    linuxX64 {
        compilations.named("main") {
            cinterops.create("embedRaw") {
                defFile(layout.buildDirectory.file("embedRaw/agent_gate_web.def").get().asFile)
            }
        }
        binaries {
            executable {
                entryPoint = "io.github.sgpublic.agentgate.main"
                linkerOpts(layout.buildDirectory.file("embedRaw/agent_gate_web.o").get().asFile.absolutePath)
            }
        }
    }

    sourceSets {
        linuxX64Main.dependencies {
            implementation(libs.clikt)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.sessions)
            implementation(libs.embed.raw.core)
            implementation(libs.embed.raw.ktor)
        }
    }
}

embedRaw {
    sourceDirectory.set(project(":web").layout.projectDirectory.dir("build"))
    exclude.add("**/*.map")
    baseName.set("agent_gate_web")
}

tasks.named("embedRaw") {
    dependsOn(":web:runBuild")
}

val nativeExecutable = layout.buildDirectory.file("bin/linuxX64/releaseExecutable/server.kexe")
val prepareDockerContext = tasks.register<Sync>("prepareDockerContext") {
    dependsOn("linkReleaseExecutableLinuxX64")
    from(nativeExecutable) {
        rename { "agent-gate" }
    }
    into(layout.buildDirectory.dir("docker"))
}

val createDockerfile = tasks.register<Dockerfile>("createDockerfile") {
    dependsOn(prepareDockerContext)
    destFile.set(layout.buildDirectory.file("docker/Dockerfile"))
    from("ubuntu:26.04")
    runCommand("useradd --create-home agentgate")
    copyFile("agent-gate", "/usr/local/bin/agent-gate")
    runCommand("chmod 755 /usr/local/bin/agent-gate")
    user("agentgate")
    exposePort(1180)
    entryPoint("/usr/local/bin/agent-gate")
}

tasks.register<DockerBuildImage>("dockerBuildImage") {
    dependsOn(createDockerfile)
    inputDir = layout.buildDirectory.dir("docker").get().asFile
    dockerFile = createDockerfile.get().destFile
    images.add("agent-gate:${rootProject.version}")
}
