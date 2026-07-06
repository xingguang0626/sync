package com.life.app.ui.mine

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit = {},
    viewModel: MineViewModel = hiltViewModel()
) {
    var notificationEnabled by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }
    val clearState by viewModel.clearState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // 清空成功后跳回首页
    LaunchedEffect(clearState) {
        if (clearState is ClearState.Success) {
            Toast.makeText(context, "所有日程已清空", Toast.LENGTH_SHORT).show()
            viewModel.resetClearState()
            onNavigateHome()
        }
        if (clearState is ClearState.Failure) {
            Toast.makeText(context, "清空失败，请检查网络连接", Toast.LENGTH_SHORT).show()
            viewModel.resetClearState()
        }
    }

    val currentPriority = settings["default_priority"] ?: "P1"
    val currentDuration = settings["default_duration"] ?: "60"
    val priorityLabel = when (currentPriority) {
        "P0" -> "P0（高优先）"
        "P1" -> "P1（中优先）"
        "P2" -> "P2（低优先）"
        else -> currentPriority
    }
    val durationLabel = "$currentDuration 分钟"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(24.dp))

        // 通知设置
        SettingsSection("通知设置") {
            SettingsRow(
                label = "提醒通知",
                subtitle = "开启后接收日程冲突、晚间 P0 等提醒",
                onClick = { notificationEnabled = !notificationEnabled },
                trailing = {
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it }
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 默认日程设置
        SettingsSection("默认日程设置") {
            SettingsRow(
                label = "默认优先级",
                subtitle = priorityLabel,
                icon = Icons.Filled.Schedule,
                onClick = { showPriorityPicker = true },
                trailing = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
            SettingsRow(
                label = "默认持续时间",
                subtitle = durationLabel,
                icon = Icons.Filled.Schedule,
                onClick = { showDurationPicker = true },
                trailing = {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 数据管理
        SettingsSection("数据管理") {
            SettingsRow(
                label = "清空本地数据",
                subtitle = "删除所有日程，不可恢复",
                icon = Icons.Filled.DeleteForever,
                onClick = { showClearDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 关于
        SettingsSection("关于") {
            SettingsRow(
                label = "关于 Sync",
                subtitle = "版本 1.0.0 · MVP",
                icon = Icons.Filled.Info,
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }

    // 清空确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空本地数据") },
            text = { Text("此操作将删除所有日程，不可恢复。确认清空吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearAll()
                }) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 优先级选择弹窗
    if (showPriorityPicker) {
        PriorityPickerDialog(
            current = currentPriority,
            onSelect = { value ->
                viewModel.updatePriority(value)
                showPriorityPicker = false
            },
            onDismiss = { showPriorityPicker = false }
        )
    }

    // 持续时间选择弹窗
    if (showDurationPicker) {
        DurationPickerDialog(
            current = currentDuration,
            onSelect = { value ->
                viewModel.updateDuration(value)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
    }
}

@Composable
private fun PriorityPickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("P0" to "高优先", "P1" to "中优先", "P2" to "低优先")
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择默认优先级") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == value, onClick = { selected = value })
                        Spacer(Modifier.width(8.dp))
                        Text("$value · $label", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DurationPickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("15" to "15 分钟", "30" to "30 分钟", "45" to "45 分钟", "60" to "1 小时", "90" to "1.5 小时", "120" to "2 小时")
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择默认持续时间") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == value, onClick = { selected = value })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.width(36.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MineScreenPreview() {
    MaterialTheme {
        MineScreen()
    }
}
