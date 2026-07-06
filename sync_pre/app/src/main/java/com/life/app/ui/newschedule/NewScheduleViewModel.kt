package com.life.app.ui.newschedule

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.remote.ApiResult
import com.life.app.data.remote.dto.CreateScheduleDto
import com.life.app.data.repository.HomeRepository
import com.life.app.domain.model.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NewScheduleViewModel @Inject constructor(
    private val repository: HomeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NewScheduleUiState().withTodayDefaults().applyNluPrefill(
            title = savedStateHandle.get<String>("nluTitle") ?: "",
            date = savedStateHandle.get<String>("nluDate") ?: "",
            startTime = savedStateHandle.get<String>("nluStartTime") ?: "",
            endTime = savedStateHandle.get<String>("nluEndTime") ?: "",
            priority = savedStateHandle.get<String>("nluPriority") ?: ""
        )
    )
    val uiState: StateFlow<NewScheduleUiState> = _uiState.asStateFlow()

    init {
        loadDefaultSettings()
    }

    private fun loadDefaultSettings() {
        viewModelScope.launch {
            when (val result = repository.getSettings()) {
                is ApiResult.Success -> {
                    val settings = result.data
                    val defaultPriority = settings["default_priority"]?.let { p ->
                        Priority.entries.find { it.name == p }
                    } ?: Priority.P1
                    val defaultDuration = settings["default_duration"] ?: "60"
                    _uiState.update {
                        it.copy(
                            priority = if (it.priority == Priority.P1 && it.title.isBlank()) defaultPriority else it.priority,
                            durationMinutes = if (it.durationMinutes.isBlank()) defaultDuration else it.durationMinutes
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /** 如果是编辑模式，加载已有日程并预填表单 */
    fun loadForEdit(scheduleId: Long) {
        viewModelScope.launch {
            when (val result = repository.getSchedule(scheduleId)) {
                is ApiResult.Success -> {
                    val s = result.data
                    // 解析日期 YYYY-MM-DD → 年/月/日
                    val dateParts = s.date.split("-")
                    // 解析时间 HH:MM → 时/分
                    val timeParts = s.startTime.split(":")
                    _uiState.update {
                        it.copy(
                            editScheduleId = scheduleId,
                            title = s.title,
                            dateYear = dateParts.getOrElse(0) { "" },
                            dateMonth = dateParts.getOrElse(1) { "" },
                            dateDay = dateParts.getOrElse(2) { "" },
                            timeHour = timeParts.getOrElse(0) { "" },
                            timeMinute = timeParts.getOrElse(1) { "00" },
                            durationMinutes = s.durationMinutes.toString(),
                            priority = s.priority,
                            note = s.note ?: "",
                            isEditMode = true
                        )
                    }
                }
                is ApiResult.Failure -> {
                    Log.e("Sync", "[NewScheduleVM] 加载日程失败: ${result.error}")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun onEvent(event: NewScheduleEvent) {
        when (event) {
            is NewScheduleEvent.TitleChanged -> updateField { it.copy(title = event.value) }
            is NewScheduleEvent.DateYearChanged -> updateField { it.copy(dateYear = event.value.take(4)) }
            is NewScheduleEvent.DateMonthChanged -> updateField { it.copy(dateMonth = event.value.take(2)) }
            is NewScheduleEvent.DateDayChanged -> updateField { it.copy(dateDay = event.value.take(2)) }
            is NewScheduleEvent.TimeHourChanged -> updateField { it.copy(timeHour = event.value.take(2)) }
            is NewScheduleEvent.TimeMinuteChanged -> updateField { it.copy(timeMinute = event.value.take(2)) }
            is NewScheduleEvent.DurationChanged -> updateField { it.copy(durationMinutes = event.value) }
            is NewScheduleEvent.PriorityChanged -> updateField { it.copy(priority = event.value) }
            is NewScheduleEvent.NoteChanged -> updateField { it.copy(note = event.value) }
            NewScheduleEvent.ClearErrors -> _uiState.update { it.copy(errors = emptyMap()) }
            NewScheduleEvent.Save -> save()
        }
    }

    private fun updateField(transform: (NewScheduleUiState) -> NewScheduleUiState) {
        _uiState.update { current ->
            val next = transform(current)
            val changedFields = fieldsThatChanged(current, next)
            next.copy(errors = current.errors - changedFields)
        }
    }

    private fun fieldsThatChanged(
        prev: NewScheduleUiState,
        next: NewScheduleUiState
    ): Set<String> {
        val changed = mutableSetOf<String>()
        if (prev.title != next.title) changed += FIELD_TITLE
        if (prev.dateYear != next.dateYear || prev.dateMonth != next.dateMonth || prev.dateDay != next.dateDay)
            changed += FIELD_DATE
        if (prev.timeHour != next.timeHour || prev.timeMinute != next.timeMinute)
            changed += FIELD_START_TIME
        if (prev.durationMinutes != next.durationMinutes) changed += FIELD_DURATION
        return changed
    }

    private fun save() {
        val state = _uiState.value
        val errors = validate(state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        val dto = CreateScheduleDto(
            title     = state.title.trim(),
            date      = state.date,
            startTime = state.startTime,
            duration  = state.durationMinutes.toInt(),
            priority  = state.priority.name,
            repeat    = "none",
            note      = state.note.trim()
        )

        if (state.isEditMode && state.editScheduleId != null) {
            saveEdit(state.editScheduleId, dto)
        } else {
            saveCreate(dto)
        }
    }

    private fun saveCreate(dto: CreateScheduleDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = repository.createSchedule(dto)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is ApiResult.Failure -> {
                    Log.e("Sync", "[NewScheduleVM] 创建失败: ${result.error}")
                    _uiState.update {
                        it.copy(isSaving = false, errors = mapOf("_save" to result.error))
                    }
                }
                else -> {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    private fun saveEdit(id: Long, dto: CreateScheduleDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = repository.updateSchedule(id, dto)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is ApiResult.Failure -> {
                    Log.e("Sync", "[NewScheduleVM] 更新失败: ${result.error}")
                    _uiState.update {
                        it.copy(isSaving = false, errors = mapOf("_save" to result.error))
                    }
                }
                else -> {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    private fun validate(state: NewScheduleUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (state.title.isBlank()) {
            errors[FIELD_TITLE] = "标题不能为空"
        }
        if (state.dateYear.isBlank() || state.dateMonth.isBlank() || state.dateDay.isBlank()) {
            errors[FIELD_DATE] = "日期不能为空"
        } else {
            val y = state.dateYear.toIntOrNull()
            val m = state.dateMonth.toIntOrNull()
            val d = state.dateDay.toIntOrNull()
            if (y == null || m == null || d == null || m !in 1..12 || d !in 1..31) {
                errors[FIELD_DATE] = "日期不合法"
            }
        }
        if (state.timeHour.isBlank()) {
            errors[FIELD_START_TIME] = "开始时间不能为空"
        } else {
            val h = state.timeHour.toIntOrNull()
            val mi = state.timeMinute.toIntOrNull() ?: 0
            if (h == null || h !in 0..23 || mi !in 0..59) {
                errors[FIELD_START_TIME] = "时间不合法"
            }
        }
        if (state.durationMinutes.isNotBlank()) {
            val mins = state.durationMinutes.toIntOrNull()
            if (mins == null || mins <= 0) {
                errors[FIELD_DURATION] = "持续时间必须为正整数（分钟）"
            }
        }

        return errors
    }

    companion object {
        const val FIELD_TITLE = "title"
        const val FIELD_DATE = "date"
        const val FIELD_START_TIME = "startTime"
        const val FIELD_DURATION = "duration"
    }
}

/** 用今天的日期填充 year/month/day */
private fun NewScheduleUiState.withTodayDefaults(): NewScheduleUiState {
    val today = LocalDate.now()
    return copy(
        dateYear = today.year.toString(),
        dateMonth = today.monthValue.toString().padStart(2, '0'),
        dateDay = today.dayOfMonth.toString().padStart(2, '0')
    )
}

/** 用 NLU 解析结果预填表单字段 */
private fun NewScheduleUiState.applyNluPrefill(
    title: String,
    date: String,
    startTime: String,
    endTime: String,
    priority: String
): NewScheduleUiState {
    if (title.isBlank() && date.isBlank() && startTime.isBlank()) return this

    var result = this
    if (title.isNotBlank()) result = result.copy(title = title)
    if (date.isNotBlank()) {
        val parts = date.split("-")
        if (parts.size == 3) {
            result = result.copy(
                dateYear = parts[0],
                dateMonth = parts[1],
                dateDay = parts[2]
            )
        }
    }
    if (startTime.isNotBlank()) {
        val timeParts = startTime.split(":")
        if (timeParts.size >= 1) result = result.copy(timeHour = timeParts[0])
        if (timeParts.size >= 2) result = result.copy(timeMinute = timeParts[1])
    }
    // 从 endTime - startTime 推算持续时间
    if (startTime.isNotBlank() && endTime.isNotBlank()) {
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startH = startParts.getOrNull(0)?.toIntOrNull()
        val startM = startParts.getOrNull(1)?.toIntOrNull() ?: 0
        val endH = endParts.getOrNull(0)?.toIntOrNull()
        val endM = endParts.getOrNull(1)?.toIntOrNull() ?: 0
        if (startH != null && endH != null) {
            val totalMinutes = (endH * 60 + endM) - (startH * 60 + startM)
            if (totalMinutes > 0) result = result.copy(durationMinutes = totalMinutes.toString())
        }
    }
    if (priority.isNotBlank()) {
        val p = Priority.entries.find { it.name == priority }
            ?: Priority.entries.find { it.displayName == priority }
        if (p != null) result = result.copy(priority = p)
    }
    return result
}
