package com.life.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * POST /api/nlu/parse 的请求体。
 */
@Serializable
data class NluRequestDto(
    @SerialName("text") val text: String,
    @SerialName("today_date") val todayDate: String
)