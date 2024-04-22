package io.github.sgpublic.agentgate.core.logback

import io.github.sgpublic.agentgate.Config
import io.github.sgpublic.kotlin.core.logback.filter.ConsoleFilter

class ConsoleFilter: ConsoleFilter(
    Config.AGENT_GATE_BASE_DEBUG,
    "io.github.sgpublic.agentgate"
)