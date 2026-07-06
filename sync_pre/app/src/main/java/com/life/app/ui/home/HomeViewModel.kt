package com.life.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.remote.ApiResult
import com.life.app.data.remote.dto.CreateScheduleDto
import com.life.app.data.remote.mapper.toDraftPreview
import com.life.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val voiceAsr = VoiceAsrHelper()
    private var asrCollectJob: Job? = null

    init {
        loadHome(HomeRepository.today())
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.Refresh -> {
                loadHome(HomeRepository.today())
            }
            HomeUiEvent.RetryLoad -> {
                loadHome(HomeRepository.today())
            }
            is HomeUiEvent.PlusClicked -> { /* 已由 MainActivity 直接导航处理 */ }
            is HomeUiEvent.ScheduleClicked -> { /* 详情页导航由 HomeScreen 直接处理 */ }
            is HomeUiEvent.InputTextChanged -> {
                _uiState.update { it.copy(nluInputText = event.text) }
            }
            is HomeUiEvent.ShowConflictSheet -> {
                _uiState.update {
                    it.copy(showConflictSheet = true, conflictSchedules = event.schedules)
                }
            }
            HomeUiEvent.DismissConflictSheet -> {
                _uiState.update { it.copy(showConflictSheet = false, conflictSchedules = emptyList()) }
            }
            HomeUiEvent.AcceptConflictSuggestion -> {
                val schedules = _uiState.value.conflictSchedules
                if (schedules.size < 2) return
                val toPostpone = schedules.minByOrNull { s ->
                    when (s.priority) {
                        com.life.app.domain.model.Priority.P0 -> 0
                        com.life.app.domain.model.Priority.P1 -> 1
                        com.life.app.domain.model.Priority.P2 -> 2
                    }
                } ?: return
                viewModelScope.launch {
                    when (val result = repository.postponeSchedule(toPostpone.id)) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(showConflictSheet = false, conflictSchedules = emptyList())
                            }
                            loadHome(HomeRepository.today())
                        }
                        is ApiResult.Failure -> {
                            _uiState.update {
                                it.copy(errorMessage = "顺延失败：${result.error}")
                            }
                        }
                        ApiResult.Loading -> Unit
                    }
                }
            }
            HomeUiEvent.ShowReminderSheet -> {
                _uiState.update { it.copy(showReminderSheet = true) }
            }
            HomeUiEvent.DismissReminderSheet -> {
                _uiState.update { it.copy(showReminderSheet = false) }
            }
            is HomeUiEvent.SendInput -> {
                val trimmed = event.text.trim().replace(Regex("\\s+"), " ")
                viewModelScope.launch {
                    when (val result = repository.parseNlu(trimmed)) {
                        is ApiResult.Success -> {
                            Log.d("Sync", "[NLU] intent=${result.data.intent} conf=${result.data.confidence}")
                            val preview = result.data.toDraftPreview()
                            _uiState.update {
                                it.copy(
                                    showNluConfirm = true,
                                    nluDraft = preview,
                                    nluInputText = event.text
                                )
                            }
                        }
                        is ApiResult.Failure -> {
                            _uiState.update {
                                it.copy(errorMessage = "NLU 解析失败：${result.error}")
                            }
                        }
                        ApiResult.Loading -> Unit
                    }
                }
            }
            HomeUiEvent.DismissNluConfirm -> {
                _uiState.update { it.copy(showNluConfirm = false, nluDraft = null, nluInputText = "") }
            }
            HomeUiEvent.ErrorShown -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            HomeUiEvent.ConfirmNluDraft -> {
                val draft = _uiState.value.nluDraft ?: return
                viewModelScope.launch {
                    val dto = CreateScheduleDto(
                        title = draft.title,
                        date = draft.date,
                        startTime = draft.startTime,
                        duration = calculateDuration(draft.startTime, draft.endTime),
                        priority = draft.priority,
                        note = ""
                    )
                    val result = repository.createSchedule(dto)
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(showNluConfirm = false, nluDraft = null, nluInputText = "")
                            }
                            loadHome(HomeRepository.today())
                        }
                        is ApiResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    showNluConfirm = false,
                                    nluDraft = null,
                                    nluInputText = "",
                                    errorMessage = "NLU 创建失败：${result.error}"
                                )
                            }
                        }
                        ApiResult.Loading -> Unit
                    }
                }
            }
            // ── 语音输入事件 ──
            HomeUiEvent.OnVoiceClick -> {
                if (_uiState.value.isVoiceListening) {
                    // 已在录音 → 手动停止，取当前部分结果作为最终结果
                    voiceAsr.stop()
                    val partialText = _uiState.value.voicePartialText
                    if (partialText.isNotBlank()) {
                        onEvent(HomeUiEvent.VoiceFinalResult(partialText))
                    } else {
                        // 没有识别到任何文字，直接重置状态
                        asrCollectJob?.cancel()
                        _uiState.update { it.copy(isVoiceListening = false) }
                    }
                } else {
                    // 开始录音
                    voiceAsr.start()
                    startAsrCollect()
                    _uiState.update { it.copy(isVoiceListening = true, voicePartialText = "") }
                }
            }
            HomeUiEvent.DismissVoice -> {
                voiceAsr.stop()
                asrCollectJob?.cancel()
                _uiState.update { it.copy(isVoiceListening = false, voicePartialText = "") }
            }
            is HomeUiEvent.VoicePartialResult -> {
                _uiState.update { it.copy(voicePartialText = event.text) }
            }
            is HomeUiEvent.VoiceFinalResult -> {
                _uiState.update {
                    it.copy(isVoiceListening = false, voicePartialText = "", nluInputText = event.text)
                }
                onEvent(HomeUiEvent.SendInput(event.text))
            }
            is HomeUiEvent.VoiceError -> {
                Log.e("Sync", "[Voice] ${event.message}")
                _uiState.update {
                    it.copy(isVoiceListening = false, voicePartialText = "", errorMessage = "语音：${event.message}")
                }
            }
        }
    }

    private fun startAsrCollect() {
        asrCollectJob?.cancel()
        asrCollectJob = viewModelScope.launch {
            voiceAsr.state.collect { state ->
                when (state) {
                    is VoiceAsrHelper.AsrState.PartialResult -> {
                        _uiState.update { it.copy(voicePartialText = state.text) }
                    }
                    is VoiceAsrHelper.AsrState.FinalResult -> {
                        onEvent(HomeUiEvent.VoiceFinalResult(state.text))
                        asrCollectJob?.cancel()
                    }
                    is VoiceAsrHelper.AsrState.Error -> {
                        onEvent(HomeUiEvent.VoiceError(state.message))
                        asrCollectJob?.cancel()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadHome(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = ApiResult.Loading) }
            _uiState.update { it.copy(state = repository.fetchHomeData(date)) }
        }
    }

    private fun calculateDuration(start: String, end: String): Int {
        fun String.toTotalMinutes(): Int {
            val parts = split(":")
            return ((parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60) +
                   (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        val total = end.toTotalMinutes() - start.toTotalMinutes()
        return total.coerceAtLeast(15)
    }

    override fun onCleared() {
        super.onCleared()
        voiceAsr.stop()
    }
}
