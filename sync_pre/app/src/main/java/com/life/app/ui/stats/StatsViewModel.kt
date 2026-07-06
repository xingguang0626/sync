package com.life.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.remote.ApiResult
import com.life.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val totalSchedules: Int = 0,
    val completedCount: Int = 0,
    val completionRate: Float = 0f,
    val totalFocusMinutes: Int = 0,
    val mostActiveHour: String = "--",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.fetchHomeData(HomeRepository.today())) {
                is ApiResult.Success -> {
                    val allSchedules = result.data.timelineItems.flatMap { item ->
                        when (item) {
                            is com.life.app.domain.model.TimelineItem.Single -> listOf(item.schedule)
                            is com.life.app.domain.model.TimelineItem.ConflictPair -> item.schedules
                            is com.life.app.domain.model.TimelineItem.ConflictGroup -> item.schedules
                        }
                    }
                    val total = allSchedules.size
                    val completed = allSchedules.count { it.isCompleted }
                    val rate = if (total > 0) completed.toFloat() / total else 0f
                    val focusMinutes = allSchedules.sumOf { it.durationMinutes }

                    // 最活跃时段：按 startTime 取整到小时，找最多的
                    val hourCounts = allSchedules.groupBy { it.startTime.substringBefore(":") }
                    val topHour = hourCounts.maxByOrNull { it.value.size }?.key ?: "--"
                    val activeLabel = if (topHour != "--") "${topHour}:00 - ${topHour}:59" else "--"

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalSchedules = total,
                            completedCount = completed,
                            completionRate = rate,
                            totalFocusMinutes = focusMinutes,
                            mostActiveHour = activeLabel
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
                ApiResult.Loading -> Unit
            }
        }
    }
}
