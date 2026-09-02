import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    alias(libs.plugins.node)
}

node {
    download = true
    version = "22.14.0"
    pnpmVersion = "10.15.1"
    nodeProjectDir = projectDir
}

tasks.register<PnpmTask>("runStart") {
    args = listOf("run", "start")
}

tasks.named<PnpmTask>("pnpmInstall") {
    args.add("--force")
}

tasks.register<PnpmTask>("runBuild") {
    dependsOn(tasks.named("pnpmInstall"))
    args = listOf("run", "build")
}
