package io.github.sgpublic.agentgate

import agent_gate_web._binary_agent_gate_web_bin_start
import com.github.ajalt.clikt.command.main
import io.github.sgpublic.embedraw.embeddedRawResources
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal suspend fun startAgentGate(config: AgentGateCommand) {
    val targetMetadata = TargetMetadata(config).load()
    val resources = embeddedRawResources(_binary_agent_gate_web_bin_start)
    val client = HttpClient(ClientCIO)

    embeddedServer(ServerCIO, port = config.port) {
        agentGate(config, targetMetadata, resources, client)
    }.start(wait = true)
}

fun main(args: Array<String>) = kotlinx.coroutines.runBlocking { AgentGateCommand.main(args) }
