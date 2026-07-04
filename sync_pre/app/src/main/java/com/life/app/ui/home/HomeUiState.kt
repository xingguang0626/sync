package com.life.app.ui.home

import com.life.app.data.remote.ApiResult
import com.life.app.domain.model.HomePageData

data class HomeUiState(
    val state: ApiResult<HomePageData> = ApiResult.Loading,
    val isInputFocused: Boolean = false
)

sealed interface HomeUiEvent {
    data object Refresh : HomeUiEvent
    data class PlusClicked(val source: String) : HomeUiEvent           // "manual" | "voice"
    data class SendInput(val text: String) : HomeUiEvent
    data class ScheduleClicked(val id: Long) : HomeUiEvent
    data class ConflictGroupClicked(val scheduleIds: List<Long>) : HomeUiEvent
    data class ReminderClicked(val type: String) : HomeUiEvent
    data class AdoptReminderSuggestion(val type: String) : HomeUiEvent
    data class ViewReminderAdjustment(val type: String) : HomeUiEvent
    data object RetryLoad : HomeUiEvent
}