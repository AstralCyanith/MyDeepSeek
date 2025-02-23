@file:Suppress("PropertyName", "unused", "SpellCheckingInspection")

package moe.tachyon.config

import kotlinx.serialization.Serializable
import moe.tachyon.dataClass.DefaultModelConfig

@Serializable
data class SystemConfig(
    val apiKey: String,
    val defaultModel: DefaultModelConfig,
    val apiUrl: String,
)

var systemConfig: SystemConfig by config("system.yml",
    SystemConfig(
        "",
        DefaultModelConfig(0.7, 0.95, 50, 0.0, emptyList(), emptyList()),
        "https://api.siliconflow.cn/v1",
    )
)