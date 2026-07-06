package com.life.app.ui.newschedule

import com.life.app.domain.model.Priority

data class NewScheduleUiState(
    val title: String = "",
    // 日期：年/月/日 三个独立字段
    val dateYear: String = "",
    val dateMonth: String = "",
    val dateDay: String = "",
    // 时间：时/分 两个独立字段
    val timeHour: String = "",
    val timeMinute: String = "00",
    val durationMinutes: String = "",
    val priority: Priority = Priority.P1,
    val note: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isEditMode: Boolean = false,
    val editScheduleId: Long? = null
) {
    /** 组合后的日期字符串 YYYY-MM-DD */
    val date: String get() {
        if (dateYear.isBlank() || dateMonth.isBlank() || dateDay.isBlank()) return ""
        val y = dateYear.trim().padStart(4, '0')
        val m = dateMonth.trim().padStart(2, '0')
        val d = dateDay.trim().padStart(2, '0')
        return "$y-$m-$d"
    }

    /** 组合后的开始时间字符串 HH:MM */
    val startTime: String get() {
        if (timeHour.isBlank()) return ""
        val h = timeHour.trim().padStart(2, '0')
        val mi = timeMinute.ifBlank { "00" }.trim().padStart(2, '0')
        return "$h:$mi"
    }

    val hasErrors: Boolean get() = errors.isNotEmpty()
    val canSave: Boolean get() = title.isNotBlank() && dateYear.isNotBlank()
            && dateMonth.isNotBlank() && dateDay.isNotBlank() && timeHour.isNotBlank()

    fun errorFor(field: String): String? = errors[field]
}

sealed interface NewScheduleEvent {
    data class TitleChanged(val value: String) : NewScheduleEvent
    data class DateYearChanged(val value: String) : NewScheduleEvent
    data class DateMonthChanged(val value: String) : NewScheduleEvent
    data class DateDayChanged(val value: String) : NewScheduleEvent
    data class TimeHourChanged(val value: String) : NewScheduleEvent
    data class TimeMinuteChanged(val value: String) : NewScheduleEvent
    data class DurationChanged(val value: String) : NewScheduleEvent
    data class PriorityChanged(val value: Priority) : NewScheduleEvent
    data class NoteChanged(val value: String) : NewScheduleEvent
    data object Save : NewScheduleEvent
    data object ClearErrors : NewScheduleEvent
}
