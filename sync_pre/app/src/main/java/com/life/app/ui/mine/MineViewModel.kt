package com.life.app.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.life.app.data.remote.ApiResult
import com.life.app.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MineViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _clearState = MutableStateFlow<ClearState>(ClearState.Idle)
    val clearState: StateFlow<ClearState> = _clearState.asStateFlow()

    private val _settings = MutableStateFlow(mapOf("default_priority" to "P1", "default_duration" to "60"))
    val settings: StateFlow<Map<String, String>> = _settings.asStateFlow()

    private val _settingsUpdating = MutableStateFlow(false)
    val settingsUpdating: StateFlow<Boolean> = _settingsUpdating.asStateFlow()

    init {
        loadSettings()
    }

    fun clearAll() {
        viewModelScope.launch {
            _clearState.value = ClearState.Loading
            when (repository.clearAll()) {
                is ApiResult.Success -> _clearState.value = ClearState.Success
                is ApiResult.Failure -> _clearState.value = ClearState.Failure
                is ApiResult.Loading -> {}
            }
        }
    }

    fun resetClearState() {
        _clearState.value = ClearState.Idle
    }

    private fun loadSettings() {
        viewModelScope.launch {
            when (val result = repository.getSettings()) {
                is ApiResult.Success -> _settings.value = result.data
                else -> {}
            }
        }
    }

    fun updatePriority(value: String) {
        viewModelScope.launch {
            _settingsUpdating.value = true
            when (val result = repository.updateSettings(mapOf("default_priority" to value))) {
                is ApiResult.Success -> _settings.value = result.data
                else -> {}
            }
            _settingsUpdating.value = false
        }
    }

    fun updateDuration(value: String) {
        viewModelScope.launch {
            _settingsUpdating.value = true
            when (val result = repository.updateSettings(mapOf("default_duration" to value))) {
                is ApiResult.Success -> _settings.value = result.data
                else -> {}
            }
            _settingsUpdating.value = false
        }
    }
}

sealed interface ClearState {
    data object Idle : ClearState
    data object Loading : ClearState
    data object Success : ClearState
    data object Failure : ClearState
}
