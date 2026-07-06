package com.life.app.ui.scheduledetail

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

@HiltViewModel
class ScheduleDetailViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleDetailUiState())
    val uiState: StateFlow<ScheduleDetailUiState> = _uiState.asStateFlow()

    fun loadSchedule(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadState = ApiResult.Loading) }
            _uiState.update { it.copy(loadState = repository.getSchedule(id)) }
        }
    }

    fun markComplete() {
        val schedule = (_uiState.value.loadState as? ApiResult.Success)?.data ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionState = ApiResult.Loading) }
            when (val result = repository.markComplete(schedule.id)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            actionState = ApiResult.Success(Unit),
                            loadState = ApiResult.Success(result.data)
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(actionState = ApiResult.Failure(result.error)) }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun deleteSchedule() {
        val schedule = (_uiState.value.loadState as? ApiResult.Success)?.data ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionState = ApiResult.Loading) }
            when (val result = repository.deleteSchedule(schedule.id)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(actionState = ApiResult.Success(Unit), deleted = true) }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(actionState = ApiResult.Failure(result.error)) }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearActionResult() {
        _uiState.update { it.copy(actionState = ApiResult.Success(Unit)) }
    }
}

data class ScheduleDetailUiState(
    val loadState: ApiResult<Schedule> = ApiResult.Loading,
    val actionState: ApiResult<Unit> = ApiResult.Success(Unit),
    val deleted: Boolean = false
) {
    val schedule: Schedule? get() = (loadState as? ApiResult.Success)?.data
    val isLoading: Boolean get() = loadState is ApiResult.Loading
    val loadError: String? get() = (loadState as? ApiResult.Failure)?.error
    val isActionLoading: Boolean get() = actionState is ApiResult.Loading
}
