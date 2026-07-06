package com.life.app.ui.plan

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

data class PlanUiState(
    val schedulesByRepeat: Map<String, List<Schedule>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.fetchHomeData(HomeRepository.today())) {
                is ApiResult.Success -> {
                    val repeating = result.data.timelineItems
                        .flatMap { item ->
                            when (item) {
                                is com.life.app.domain.model.TimelineItem.Single -> listOf(item.schedule)
                                is com.life.app.domain.model.TimelineItem.ConflictPair -> item.schedules
                                is com.life.app.domain.model.TimelineItem.ConflictGroup -> item.schedules
                            }
                        }
                        .filter { it.repeat != com.life.app.domain.model.RepeatType.NONE }
                        .groupBy { it.repeat.name }

                    _uiState.update { it.copy(isLoading = false, schedulesByRepeat = repeating) }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}
