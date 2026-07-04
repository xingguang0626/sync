package com.life.app.data.remote.dto

import com.life.app.domain.model.Schedule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RawTimelineItemDto(
    @SerialName("type") val type: String,
    @SerialName("schedule") val schedule: Schedule? = null,
    @SerialName("schedules") val schedules: List<Schedule>? = null
)

@Serializable
data class TimelineDataDto(
    @SerialName("date") val date: String,
    @SerialName("items") val items: List<RawTimelineItemDto>
)

@Serializable
data class TimelineResponseDto(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("data") val data: TimelineDataDto? = null,
    @SerialName("error") val error: String? = null
)