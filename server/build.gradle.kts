import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.kotlin.dsl.register

plugins {
    alias(libs.plugins.docker)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.embed.raw)
    alias(libs.plugins.buildkonfig)
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

buildkonfig {
    packageName = "io.github.sgpublic.agentgate"
    defaultConfigs {
        buildConfigField(STRING, "VERSION", rootProject.version.toString())
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
val releaseTag = "v${rootProject.version}"
val releaseArchive = tasks.register<Tar>("releaseArchive") {
    dependsOn("linkReleaseExecutableLinuxX64")
    archiveFileName.set("agent-gate_${rootProject.version}_linux_x64.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    compression = Compression.GZIP
    from(nativeExecutable) {
        rename { "agent-gate" }
        filePermissions { unix("755") }
    }
}

val validateReleaseTag = tasks.register("validateReleaseTag") {
    doLast {
        val eventTag = providers.environmentVariable("GITHUB_REF_NAME").orNull
        require(eventTag == null || eventTag == releaseTag) {
            "GitHub tag $eventTag does not match project version tag $releaseTag"
        }
        val head = providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.get().trim()
        val taggedCommit = providers.exec {
            commandLine("gh", "api", "repos/sgpublic/AgentGate/commits/$releaseTag", "--jq", ".sha")
        }.standardOutput.asText.get().trim()
        require(head == taggedCommit) {
            "GitHub tag $releaseTag points to $taggedCommit, not the current commit $head"
        }
    }
}

tasks.register<Exec>("githubRelease") {
    group = "publishing"
    description = "Create the GitHub release and upload the Linux x64 archive."
    dependsOn(validateReleaseTag, releaseArchive)
    doFirst {
        commandLine(
            "gh", "release", "create", releaseTag,
            releaseArchive.get().archiveFile.get().asFile.absolutePath,
            "--repo", "sgpublic/AgentGate",
            "--verify-tag", "--generate-notes",
        )
    }
}

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

val imageName = "mhmzx/agent-gate"
val imageTags = listOf(
    "$imageName:${rootProject.version}",
    "$imageName:${rootProject.version.toString().substringBefore('.')}",
    "$imageName:latest",
)

val dockerBuildImage = tasks.register<DockerBuildImage>("dockerBuildImage") {
    dependsOn(createDockerfile)
    inputDir = layout.buildDirectory.dir("docker").get().asFile
    dockerFile = createDockerfile.get().destFile
    images.addAll(imageTags)
}

tasks.register<DockerPushImage>("dockerPushImage") {
    dependsOn(dockerBuildImage)
    images.addAll(imageTags)
}
