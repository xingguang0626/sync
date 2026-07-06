package com.life.app.ui.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.remote.ApiResult
import com.life.app.data.repository.HomeRepository
import com.life.app.domain.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModeFeedbackUiState(
    val isPostponing: Boolean = false,
    val postponedCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ModeFeedbackViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModeFeedbackUiState())
    val uiState: StateFlow<ModeFeedbackUiState> = _uiState.asStateFlow()

    fun postponeFollowingSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPostponing = true) }
            when (val result = repository.fetchHomeData(HomeRepository.today())) {
                is ApiResult.Success -> {
                    val pendingSchedules = result.data.timelineItems
                        .flatMap { item ->
                            when (item) {
                                is com.life.app.domain.model.TimelineItem.Single -> listOf(item.schedule)
                                is com.life.app.domain.model.TimelineItem.ConflictPair -> item.schedules
                                is com.life.app.domain.model.TimelineItem.ConflictGroup -> item.schedules
                            }
                        }
                        .filter { it.status == com.life.app.domain.model.ScheduleStatus.PENDING }

                    var count = 0
                    for (schedule in pendingSchedules) {
                        when (repository.postponeSchedule(schedule.id)) {
                            is ApiResult.Success -> count++
                            is ApiResult.Failure -> { /* 跳过失败的，继续顺延其他 */ }
                            ApiResult.Loading -> Unit
                        }
                    }
                    _uiState.update { it.copy(isPostponing = false, postponedCount = count) }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isPostponing = false, error = result.error) }
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}
