package com.life.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface TimelineItem {
    data class Single(val schedule: Schedule) : TimelineItem
    data class ConflictPair(val schedules: List<Schedule>) : TimelineItem
    data class ConflictGroup(val schedules: List<Schedule>) : TimelineItem
}

@Serializable
data class TimelineData(
    @SerialName("date") val date: String,
    @SerialName("items") val items: List<TimelineItem>
)