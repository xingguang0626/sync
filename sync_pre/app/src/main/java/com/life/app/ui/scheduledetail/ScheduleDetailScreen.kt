package com.life.app.ui.scheduledetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.life.app.data.remote.ApiResult
import com.life.app.domain.model.Priority
import com.life.app.domain.model.Schedule
import com.life.app.domain.model.ScheduleStatus
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberDark
import com.life.app.ui.theme.AmberDeep
import com.life.app.ui.theme.AmberLight
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    scheduleId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: ScheduleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(scheduleId) {
        viewModel.loadSchedule(scheduleId)
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onNavigateBack()
    }

    val actionComplete = uiState.actionState is ApiResult.Success
    LaunchedEffect(actionComplete) {
        if (actionComplete && (uiState.actionState as? ApiResult.Success)?.data != null) {
            viewModel.clearActionResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.loadError != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.loadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmText2nd
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadSchedule(scheduleId) }) {
                            Text("重试")
                        }
                    }
                }
            }
            uiState.schedule != null -> {
                ScheduleDetailContent(
                    schedule = uiState.schedule!!,
                    isActionLoading = uiState.isActionLoading,
                    onMarkComplete = { viewModel.markComplete() },
                    onEdit = { onNavigateToEdit(scheduleId) },
                    onDelete = { viewModel.deleteSchedule() },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ScheduleDetailContent(
    schedule: Schedule,
    isActionLoading: Boolean,
    onMarkComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除日程") },
            text = { Text("确认删除「${schedule.title}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题卡片
        DetailCard {
            Column(modifier = Modifier.padding(4.dp)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = WarmTextMain,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when (schedule.status) {
                            ScheduleStatus.COMPLETED -> AmberLight
                            ScheduleStatus.IN_PROGRESS -> AmberBase.copy(alpha = 0.15f)
                            else -> AmberDeep.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = schedule.status.displayName,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (schedule.status) {
                                ScheduleStatus.COMPLETED -> AmberDark
                                ScheduleStatus.IN_PROGRESS -> AmberBase
                                else -> AmberDeep
                            }
                        )
                    }
                }
            }
        }

        // 时间信息
        DetailCard {
            DetailRow(icon = Icons.Filled.CalendarToday, label = "日期", value = schedule.date)
            DetailDivider()
            DetailRow(icon = Icons.Filled.Schedule, label = "时间", value = "${schedule.startTime} - ${schedule.endTime}")
            DetailDivider()
            DetailRow(icon = Icons.Filled.Schedule, label = "时长", value = "${schedule.durationMinutes} 分钟")
        }

        // 属性信息
        DetailCard {
            DetailRow(icon = Icons.Filled.Flag, label = "优先级", value = "${schedule.priority.name} · ${schedule.priority.displayName}")
            DetailDivider()
            DetailRow(icon = Icons.Filled.Repeat, label = "重复", value = repeatLabel(schedule))
        }

        // 备注
        if (!schedule.note.isNullOrBlank()) {
            DetailCard {
                Column {
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmText2nd
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = schedule.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmTextMain
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 操作按钮
        if (schedule.status != ScheduleStatus.COMPLETED) {
            Button(
                onClick = onMarkComplete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberBase),
                enabled = !isActionLoading
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("标记完成", style = MaterialTheme.typography.labelLarge)
            }
        }

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            enabled = !isActionLoading
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("编辑日程", style = MaterialTheme.typography.labelLarge, color = AmberDeep)
        }

        TextButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isActionLoading
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("删除日程", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = WarmText2nd
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmText2nd,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = WarmTextMain,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailDivider() {
    Spacer(Modifier.height(4.dp))
    androidx.compose.material3.HorizontalDivider(
        color = WarmOutlineVariant.copy(alpha = 0.3f)
    )
    Spacer(Modifier.height(4.dp))
}

private fun repeatLabel(schedule: Schedule): String = when (schedule.repeat) {
    com.life.app.domain.model.RepeatType.NONE -> "不重复"
    com.life.app.domain.model.RepeatType.DAILY -> "每天"
    com.life.app.domain.model.RepeatType.WEEKLY -> "每周"
    com.life.app.domain.model.RepeatType.WEEKDAYS -> "每个工作日"
    com.life.app.domain.model.RepeatType.WEEKENDS -> "每周末"
    com.life.app.domain.model.RepeatType.MONTHLY -> "每月"
}

@Preview(showBackground = true)
@Composable
private fun ScheduleDetailPreview() {
    MaterialTheme {
        ScheduleDetailScreen(
            scheduleId = 1L,
            onNavigateBack = {},
            onNavigateToEdit = {},
            viewModel = hiltViewModel()
        )
    }
}
