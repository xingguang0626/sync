package com.life.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseDto<T>(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("data") val data: T? = null,
    @SerialName("error") val error: String? = null
)