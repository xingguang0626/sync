package com.life.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.life.app.data.remote.ApiResult
import com.life.app.domain.model.HomePageData
import com.life.app.ui.home.components.BottomTabBar
import com.life.app.ui.home.components.EmptyState
import com.life.app.ui.home.components.HomeInputBar
import com.life.app.ui.home.components.HomeTab
import com.life.app.ui.home.components.HomeTopBar
import com.life.app.ui.home.components.ReminderCard
import com.life.app.ui.home.components.TimelineSection

@Composable
fun HomeScreen(
    onNavigateToNewSchedule: () -> Unit,
    onNavigateToLifestyle: () -> Unit,
    onNavigateToPreset: () -> Unit,
    onNavigateToScheduleDetail: (Long) -> Unit,
    onNavigateToConflictMenu: (List<Long>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToNewSchedule = onNavigateToNewSchedule,
        onNavigateToLifestyle = onNavigateToLifestyle,
        onNavigateToPreset = onNavigateToPreset,
        onNavigateToScheduleDetail = onNavigateToScheduleDetail,
        onNavigateToConflictMenu = onNavigateToConflictMenu
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onNavigateToNewSchedule: () -> Unit,
    onNavigateToLifestyle: () -> Unit,
    onNavigateToPreset: () -> Unit,
    onNavigateToScheduleDetail: (Long) -> Unit,
    onNavigateToConflictMenu: (List<Long>) -> Unit
) {
    Scaffold(
        bottomBar = { BottomTabBar(current = HomeTab.HOME) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState.state) {
                ApiResult.Loading -> LoadingContent()
                is ApiResult.Failure -> ErrorContent(message = state.error, onRetry = { onEvent(HomeUiEvent.RetryLoad) })
                is ApiResult.Success -> HomeLoadedContent(
                    data = state.data,
                    onEvent = onEvent,
                    onNavigateToNewSchedule = onNavigateToNewSchedule,
                    onNavigateToLifestyle = onNavigateToLifestyle,
                    onNavigateToPreset = onNavigateToPreset,
                    onNavigateToScheduleDetail = onNavigateToScheduleDetail,
                    onNavigateToConflictMenu = onNavigateToConflictMenu
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            HomeInputBar(
                value = "",
                onChange = { },
                onSend = { text -> onEvent(HomeUiEvent.SendInput(text)) },
                onPlusClick = { onEvent(HomeUiEvent.PlusClicked("manual")) },
                onVoiceClick = { /* TODO 第三阶段接入语音 */ }
            )
        }
    }
}

@Composable
private fun HomeLoadedContent(
    data: HomePageData,
    onEvent: (HomeUiEvent) -> Unit,
    onNavigateToNewSchedule: () -> Unit,
    onNavigateToLifestyle: () -> Unit,
    onNavigateToPreset: () -> Unit,
    onNavigateToScheduleDetail: (Long) -> Unit,
    onNavigateToConflictMenu: (List<Long>) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        HomeTopBar(
            date = data.date,
            weekday = data.weekday,
            greeting = data.greeting,
            onClickLifestyle = onNavigateToLifestyle,
            onClickPreset = onNavigateToPreset
        )

        data.topReminder?.let { reminder ->
            ReminderCard(
                reminder = reminder,
                onAdoptSuggestion = { onEvent(HomeUiEvent.AdoptReminderSuggestion(reminder.type.name)) },
                onViewAdjustment = { onEvent(HomeUiEvent.ViewReminderAdjustment(reminder.type.name)) }
            )
        }

        if (data.empty) {
            EmptyState(onPlusClick = onNavigateToNewSchedule)
        } else {
            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                TimelineSection(
                    items = data.timelineItems,
                    onScheduleClick = { id -> onEvent(HomeUiEvent.ScheduleClicked(id)) },
                    onConflictGroupClick = { ids -> onNavigateToConflictMenu(ids) }
                )
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}