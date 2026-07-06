package com.life.app.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.remote.ApiResult
import com.life.app.data.repository.HomeRepository
import com.life.app.domain.model.HomePageData
import com.life.app.domain.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LifestyleUiState(
    val todayStatus: String = "加载中…",
    val recentActivity: String = "加载中…",
    val suggestion: String = "要让 Sync 给你一些健康生活的建议吗？",
    val isLoading: Boolean = true,
    val isAiLoading: Boolean = false
)

@HiltViewModel
class LifestyleViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LifestyleUiState())
    val uiState: StateFlow<LifestyleUiState> = _uiState.asStateFlow()

    private var aiJob: Job? = null

    init { load() }

    fun load() {
        aiJob?.cancel()
        _uiState.update { it.copy(isLoading = true, isAiLoading = false) }

        viewModelScope.launch {
            when (val result = repository.fetchHomeData(HomeRepository.today())) {
                is ApiResult.Success -> {
                    val data = result.data
                    val allSchedules = data.allSchedules()

                    val completed = allSchedules.count { it.isCompleted }
                    val total = allSchedules.size
                    val focusMinutes = allSchedules.sumOf { it.durationMinutes }
                    val allDone = total > 0 && completed == total

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            todayStatus = generateStatus(completed, total, focusMinutes),
                            recentActivity = generateActivity(completed, total, focusMinutes, allDone),
                            suggestion = "要让 Sync 给你一些健康生活的建议吗？"
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false,
                        todayStatus = "暂无法获取数据",
                        recentActivity = "请检查网络连接",
                        suggestion = "要让 Sync 给你一些健康生活的建议吗？") }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun requestAiAdvice() {
        aiJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAiLoading = true,
                    suggestion = "请稍待 15 秒左右，Sync 正在尽力给你最完美的回答~"
                )
            }
            when (val result = repository.getLifestyleAdvice()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(suggestion = result.data, isAiLoading = false)
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            suggestion = "AI 建议暂时无法生成，请检查后端服务是否运行。",
                            isAiLoading = false
                        )
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun generateStatus(completed: Int, total: Int, focus: Int): String {
        if (total == 0) return "今天还没有安排日程，去首页添加吧。"
        return "今天你完成了 $completed 项日程，专注时长 ${formatMinutes(focus)}。保持节奏！"
    }

    private fun generateActivity(completed: Int, total: Int, focus: Int, allDone: Boolean): String {
        if (total == 0) return "本周暂无数据记录。"
        if (allDone) return "今天全部完成，了不起！"
        val rate = if (total > 0) completed * 100 / total else 0
        return "今日完成率 ${rate}%，专注总时长 ${formatMinutes(focus)}。"
    }

    private fun formatMinutes(minutes: Int): String {
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "${h} 小时" else "${h} 小时 ${m} 分钟"
        } else {
            "$minutes 分钟"
        }
    }
}

// 辅助扩展函数
fun HomePageData.allSchedules(): List<Schedule> {
    return timelineItems.flatMap { item ->
        when (item) {
            is com.life.app.domain.model.TimelineItem.Single -> listOf(item.schedule)
            is com.life.app.domain.model.TimelineItem.ConflictPair -> item.schedules
            is com.life.app.domain.model.TimelineItem.ConflictGroup -> item.schedules
        }
    }
}

fun HomePageData.hasConflicts(): Boolean {
    return timelineItems.any {
        it is com.life.app.domain.model.TimelineItem.ConflictPair ||
        it is com.life.app.domain.model.TimelineItem.ConflictGroup
    }
}
