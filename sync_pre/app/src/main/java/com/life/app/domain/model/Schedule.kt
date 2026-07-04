package com.life.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ScheduleStatus {
    @SerialName("pending") PENDING,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED;

    val displayName: String get() = when (this) {
        PENDING -> "待办"
        IN_PROGRESS -> "进行中"
        COMPLETED -> "已完成"
        CANCELLED -> "已取消"
    }
}

@Serializable
enum class RepeatType {
    @SerialName("none") NONE,
    @SerialName("daily") DAILY,
    @SerialName("weekly") WEEKLY,
    @SerialName("weekdays") WEEKDAYS,
    @SerialName("weekends") WEEKENDS,
    @SerialName("monthly") MONTHLY
}

@Serializable
data class Schedule(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("date") val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("duration") val durationMinutes: Int,
    @SerialName("priority") val priority: Priority,
    @SerialName("status") val status: ScheduleStatus,
    @SerialName("repeat") val repeat: RepeatType = RepeatType.NONE,
    @SerialName("note") val note: String? = null
) {
    val isCompleted: Boolean get() = status == ScheduleStatus.COMPLETED
    val isInProgress: Boolean get() = status == ScheduleStatus.IN_PROGRESS
}