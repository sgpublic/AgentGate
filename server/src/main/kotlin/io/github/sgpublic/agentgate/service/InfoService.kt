package io.github.sgpublic.agentgate.service

import io.github.sgpublic.agentgate.BuildConfig
import io.github.sgpublic.agentgate.Config
import io.github.sgpublic.agentgate.core.utils.JsonGlobal
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Info(
    val versionName: String = BuildConfig.VERSION_NAME,
    val versionCode: Int = BuildConfig.VERSION_CODE,
    val commitId: String = BuildConfig.COMMIT_ID,
    val serviceName: String = Config.AGENT_GATE_TARGET_NAME,
    val serviceLogo: String,
)

val InfoJson by lazy {
    JsonGlobal.encodeToString(Info.serializer(), Info(
        serviceLogo = Config.AGENT_GATE_TARGET_LOGO
            .takeIf { !it.startsWith("file://") }
            ?: "${Config.AGENT_GATE_BASE_PATH}${File(Config.AGENT_GATE_TARGET_LOGO).extension}",
    ))
}
