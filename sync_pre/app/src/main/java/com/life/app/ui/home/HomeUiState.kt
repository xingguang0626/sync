package com.life.app.ui.home

import com.life.app.data.remote.ApiResult
import com.life.app.domain.model.HomePageData
import com.life.app.domain.model.Schedule
import com.life.app.ui.home.components.NluDraftPreview

data class HomeUiState(
    val state: ApiResult<HomePageData> = ApiResult.Loading,
    val isInputFocused: Boolean = false,
    // 冲突 BottomSheet
    val showConflictSheet: Boolean = false,
    val conflictSchedules: List<Schedule> = emptyList(),
    // 提醒 BottomSheet
    val showReminderSheet: Boolean = false,
    // NLU 确认卡片
    val showNluConfirm: Boolean = false,
    val nluDraft: NluDraftPreview? = null,
    val nluInputText: String = "",
    // 一次性错误提示（Snackbar/Toast 用，消费后清空）
    val errorMessage: String? = null,
    // 语音输入状态
    val isVoiceListening: Boolean = false,
    val voicePartialText: String = ""
)

sealed interface HomeUiEvent {
    data object Refresh : HomeUiEvent
    data class PlusClicked(val source: String) : HomeUiEvent
    data class SendInput(val text: String) : HomeUiEvent
    data class InputTextChanged(val text: String) : HomeUiEvent
    data class ScheduleClicked(val id: Long) : HomeUiEvent
    data object RetryLoad : HomeUiEvent
    // 冲突 BottomSheet
    data class ShowConflictSheet(val schedules: List<Schedule>) : HomeUiEvent
    data object DismissConflictSheet : HomeUiEvent
    data object AcceptConflictSuggestion : HomeUiEvent
    // 提醒 BottomSheet
    data object ShowReminderSheet : HomeUiEvent
    data object DismissReminderSheet : HomeUiEvent
    // NLU 确认卡片
    data object DismissNluConfirm : HomeUiEvent
    data object ConfirmNluDraft : HomeUiEvent
    // 错误提示已消费
    data object ErrorShown : HomeUiEvent
    // 语音输入
    data object OnVoiceClick : HomeUiEvent
    data object DismissVoice : HomeUiEvent
    data class VoicePartialResult(val text: String) : HomeUiEvent
    data class VoiceFinalResult(val text: String) : HomeUiEvent
    data class VoiceError(val message: String) : HomeUiEvent
}
