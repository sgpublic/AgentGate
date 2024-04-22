package io.github.sgpublic.agentgate.core.logback

import io.github.sgpublic.agentgate.Application
import io.github.sgpublic.kotlin.core.logback.filter.ConsoleFilter

class ConsoleFilter: ConsoleFilter(
    Application.AGENT_GATE_DEBUG,
    "io.github.sgpublic.agentgate"
)