package com.life.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReminderType {
    @SerialName("time_conflict") TIME_CONFLICT,
    @SerialName("evening_p0") EVENING_P0,
    @SerialName("overtime") OVERTIME,
    @SerialName("late_end") LATE_END,
    @SerialName("long_focus") LONG_FOCUS,
    @SerialName("move_pending") MOVE_PENDING;

    val displayName: String get() = when (this) {
        TIME_CONFLICT -> "时间冲突"
        EVENING_P0 -> "晚间 P0"
        OVERTIME -> "任务超时"
        LATE_END -> "结束过晚"
        LONG_FOCUS -> "连续学习过久"
        MOVE_PENDING -> "未完成顺延"
    }
}

@Serializable
data class Reminder(
    @SerialName("type") val type: ReminderType,
    @SerialName("priority") val priority: Int,
    @SerialName("message") val message: String,
    @SerialName("suggestion") val suggestion: String? = null,
    @SerialName("related_schedule_ids") val relatedScheduleIds: List<Long> = emptyList()
)