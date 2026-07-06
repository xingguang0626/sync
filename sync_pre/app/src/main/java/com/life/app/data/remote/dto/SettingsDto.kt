package com.life.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    @SerialName("ok") val ok: Boolean,
    @SerialName("data") val data: Map<String, String>? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class SettingsUpdateBody(
    @SerialName("default_priority") val defaultPriority: String? = null,
    @SerialName("default_duration") val defaultDuration: String? = null
)
