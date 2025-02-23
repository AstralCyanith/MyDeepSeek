@file:Suppress("PropertyName", "unused", "SpellCheckingInspection")

package moe.tachyon.config

import kotlinx.serialization.Serializable
import moe.tachyon.dataClass.ModelConfig

@Serializable
data class SystemConfig(
    val apiKey: String,
    val defaultModel: ModelConfig
)

var systemConfig: SystemConfig by config("system.yml",
    SystemConfig("", ModelConfig(0.7, 0.95, 50, 0.0, emptyList(), emptyList()))
)