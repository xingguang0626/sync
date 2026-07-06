package com.life.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.life.app.data.remote.ApiResult
import com.life.app.domain.model.HomePageData
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.home.components.BottomTabBar
import com.life.app.ui.home.components.NluDraftPreview
import com.life.app.ui.home.components.ConflictBottomSheet
import com.life.app.ui.home.components.EmptyState
import com.life.app.ui.home.components.HomeInputBar
import com.life.app.ui.home.components.HomeTab
import com.life.app.ui.home.components.HomeTopBar
import com.life.app.ui.home.components.NluConfirmCard
import com.life.app.ui.home.components.ReminderBottomSheet
import com.life.app.ui.home.components.ReminderCard
import com.life.app.ui.home.components.TimelineSection

@Composable
fun HomeScreen(
    onNavigateToNewSchedule: (NluDraftPreview?) -> Unit,
    onNavigateToLifestyle: () -> Unit,
    onNavigateToPreset: () -> Unit,
    onNavigateToScheduleDetail: (Long) -> Unit,
    onTabSelected: (HomeTab) -> Unit = {},
    currentTab: HomeTab = HomeTab.HOME,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 每次进入时强制刷新数据
    DisposableEffect(Unit) {
        viewModel.onEvent(HomeUiEvent.Refresh)
        onDispose { }
    }

    // 录音权限
    val hasAudioPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingVoiceAfterPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission.value = granted
        if (granted && pendingVoiceAfterPermission) {
            pendingVoiceAfterPermission = false
            viewModel.onEvent(HomeUiEvent.OnVoiceClick)
        }
    }

    val onVoiceClick: () -> Unit = {
        if (!hasAudioPermission.value) {
            pendingVoiceAfterPermission = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.onEvent(HomeUiEvent.OnVoiceClick)
        }
    }

    // 一次性错误提示
    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.onEvent(HomeUiEvent.ErrorShown)
        }
    }

    HomeContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToNewSchedule = onNavigateToNewSchedule,
        onNavigateToLifestyle = onNavigateToLifestyle,
        onNavigateToPreset = onNavigateToPreset,
        onNavigateToScheduleDetail = onNavigateToScheduleDetail,
        onTabSelected = onTabSelected,
        currentTab = currentTab,
        onVoiceClick = onVoiceClick
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onNavigateToNewSchedule: (NluDraftPreview?) -> Unit,
    onNavigateToLifestyle: () -> Unit,
    onNavigateToPreset: () -> Unit,
    onNavigateToScheduleDetail: (Long) -> Unit,
    onTabSelected: (HomeTab) -> Unit = {},
    currentTab: HomeTab = HomeTab.HOME,
    onVoiceClick: () -> Unit = {}
) {
    Scaffold(
        bottomBar = { BottomTabBar(current = currentTab, onTabSelected = onTabSelected) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 可滚动内容区
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState.state) {
                    ApiResult.Loading -> LoadingContent()
                    is ApiResult.Failure -> ErrorContent(
                        message = state.error,
                        onRetry = { onEvent(HomeUiEvent.RetryLoad) }
                    )
                    is ApiResult.Success -> HomeLoadedContent(
                        data = state.data,
                        onEvent = onEvent,
                        onNavigateToNewSchedule = onNavigateToNewSchedule,
                        onNavigateToLifestyle = onNavigateToLifestyle,
                        onNavigateToPreset = onNavigateToPreset,
                        onNavigateToScheduleDetail = onNavigateToScheduleDetail
                    )
                }
            }

            // NLU 确认卡片
            if (uiState.showNluConfirm && uiState.nluDraft != null) {
                NluConfirmCard(
                    draft = uiState.nluDraft,
                    onConfirm = { onEvent(HomeUiEvent.ConfirmNluDraft) },
                    onModify = {
                        val draft = uiState.nluDraft
                        onEvent(HomeUiEvent.DismissNluConfirm)
                        onNavigateToNewSchedule(draft)
                    }
                )
            }

            // 语音实时预览条
            AnimatedVisibility(
                visible = uiState.isVoiceListening && uiState.voicePartialText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberBase.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎙️",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = uiState.voicePartialText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmText2nd,
                        maxLines = 2
                    )
                }
            }

            // 底部固定输入栏
            HomeInputBar(
                value = uiState.nluInputText,
                onChange = { text -> onEvent(HomeUiEvent.InputTextChanged(text)) },
                onSend = { text -> onEvent(HomeUiEvent.SendInput(text)) },
                onPlusClick = { onNavigateToNewSchedule(null) },
                onVoiceClick = onVoiceClick,
                isVoiceListening = uiState.isVoiceListening
            )
        }
    }

    // ── 冲突 BottomSheet ──
    if (uiState.showConflictSheet && uiState.conflictSchedules.isNotEmpty()) {
        ConflictBottomSheet(
            schedules = uiState.conflictSchedules,
            suggestion = if (uiState.conflictSchedules.size >= 3) {
                "建议保留高优先级日程，将其它日程顺延 30 分钟"
            } else {
                "建议将低优先级日程「${uiState.conflictSchedules.last().title}」顺延到 ${uiState.conflictSchedules.first().endTime} 开始"
            },
            onDismiss = { onEvent(HomeUiEvent.DismissConflictSheet) },
            onAcceptSuggestion = { onEvent(HomeUiEvent.AcceptConflictSuggestion) },
            onManualAdjust = {
                onEvent(HomeUiEvent.DismissConflictSheet)
                uiState.conflictSchedules.firstOrNull()?.let {
                    onNavigateToScheduleDetail(it.id)
                }
            },
            onKeepAll = { onEvent(HomeUiEvent.DismissConflictSheet) }
        )
    }

    // ── 提醒 BottomSheet ──
    if (uiState.showReminderSheet) {
        val reminder = (uiState.state as? ApiResult.Success)?.data?.topReminder
        if (reminder != null) {
            ReminderBottomSheet(
                reminder = reminder,
                onDismiss = { onEvent(HomeUiEvent.DismissReminderSheet) },
                onEditSchedule = { id ->
                    onEvent(HomeUiEvent.DismissReminderSheet)
                    onNavigateToScheduleDetail(id)
                }
            )
        }
    }
}

@Composable
private fun HomeLoadedContent(
    data: HomePageData,
    onEvent: (HomeUiEvent) -> Unit,
    onNavigateToNewSchedule: (NluDraftPreview?) -> Unit,
    onNavigateToLifestyle: () -> Unit,
    onNavigateToPreset: () -> Unit,
    onNavigateToScheduleDetail: (Long) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                onViewDetails = { onEvent(HomeUiEvent.ShowReminderSheet) },
                onEditSchedule = {
                    if (reminder.relatedScheduleIds.size == 1) {
                        onNavigateToScheduleDetail(reminder.relatedScheduleIds.first())
                    } else {
                        onEvent(HomeUiEvent.ShowReminderSheet)
                    }
                }
            )
        }

        if (data.empty) {
            EmptyState(onPlusClick = { onNavigateToNewSchedule(null) })
        } else {
            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                TimelineSection(
                    items = data.timelineItems,
                    onScheduleClick = { id -> onNavigateToScheduleDetail(id) },
                    onConflictGroupClick = { schedules ->
                        onEvent(HomeUiEvent.ShowConflictSheet(schedules))
                    }
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
