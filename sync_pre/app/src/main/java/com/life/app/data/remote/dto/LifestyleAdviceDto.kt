package com.life.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LifestyleAdviceDto(
    @SerialName("ok") val ok: Boolean,
    @SerialName("data") val data: LifestyleAdviceDataDto? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class LifestyleAdviceDataDto(
    @SerialName("advice") val advice: String,
    @SerialName("model") val model: String = ""
)
