package com.life.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.mock.MockApi
import com.life.app.data.remote.ApiResult
import com.life.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        loadHome(MockApi.MOCK_DATE)
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.Refresh -> loadHome(MockApi.MOCK_DATE)
            HomeUiEvent.RetryLoad -> loadHome(MockApi.MOCK_DATE)
            is HomeUiEvent.PlusClicked -> { /* TODO 第二阶段：跳 NewSchedulePage */ }
            is HomeUiEvent.SendInput -> { /* TODO 第三阶段：调 nlu.parse */ }
            is HomeUiEvent.ScheduleClicked -> { /* TODO 第二阶段：详情弹窗 */ }
            is HomeUiEvent.ConflictGroupClicked -> { /* TODO 第二阶段：冲突二级菜单 */ }
            is HomeUiEvent.ReminderClicked -> { /* TODO 第三阶段：AI 二级菜单 */ }
            is HomeUiEvent.AdoptReminderSuggestion -> { /* TODO 第三阶段：执行建议 */ }
            is HomeUiEvent.ViewReminderAdjustment -> { /* TODO 第三阶段：查看调整 */ }
        }
    }

    private fun loadHome(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(state = ApiResult.Loading) }
            _uiState.update { it.copy(state = repository.fetchHomeData(date)) }
        }
    }
}