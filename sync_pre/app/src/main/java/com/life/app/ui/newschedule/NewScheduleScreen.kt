package com.life.app.ui.newschedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.life.app.domain.model.Priority
import com.life.app.ui.theme.AmberBase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScheduleScreen(
    onNavigateBack: () -> Unit,
    scheduleId: Long? = null,
    viewModel: NewScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 编辑模式：加载已有日程
    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            viewModel.loadForEdit(scheduleId)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }

    val saveError = uiState.errorFor("_save")
    LaunchedEffect(saveError) {
        if (saveError != null) {
            snackbarHostState.showSnackbar(saveError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑日程" else "新建日程") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // 标题
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onEvent(NewScheduleEvent.TitleChanged(it)) },
                label = { Text("日程名称 *") },
                placeholder = { Text("例如：晨跑、小组会议") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.errorFor(NewScheduleViewModel.FIELD_TITLE) != null,
                supportingText = uiState.errorFor(NewScheduleViewModel.FIELD_TITLE)?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(12.dp))

            // ── 日期：年/月/日 三个输入框 ──
            Text(
                text = "日期 *",
                style = MaterialTheme.typography.labelLarge,
                color = if (uiState.errorFor(NewScheduleViewModel.FIELD_DATE) != null)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.dateYear,
                    onValueChange = { viewModel.onEvent(NewScheduleEvent.DateYearChanged(it)) },
                    label = { Text("年") },
                    placeholder = { Text("2026") },
                    modifier = Modifier.weight(1.3f),
                    singleLine = true,
                    isError = uiState.errorFor(NewScheduleViewModel.FIELD_DATE) != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = uiState.dateMonth,
                    onValueChange = { viewModel.onEvent(NewScheduleEvent.DateMonthChanged(it)) },
                    label = { Text("月") },
                    placeholder = { Text("07") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.errorFor(NewScheduleViewModel.FIELD_DATE) != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = uiState.dateDay,
                    onValueChange = { viewModel.onEvent(NewScheduleEvent.DateDayChanged(it)) },
                    label = { Text("日") },
                    placeholder = { Text("05") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.errorFor(NewScheduleViewModel.FIELD_DATE) != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
            }
            uiState.errorFor(NewScheduleViewModel.FIELD_DATE)?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 开始时间：时/分 两个输入框（24小时制） ──
            Text(
                text = "开始时间 *（24小时制）",
                style = MaterialTheme.typography.labelLarge,
                color = if (uiState.errorFor(NewScheduleViewModel.FIELD_START_TIME) != null)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.timeHour,
                    onValueChange = { viewModel.onEvent(NewScheduleEvent.TimeHourChanged(it)) },
                    label = { Text("时") },
                    placeholder = { Text("8") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.errorFor(NewScheduleViewModel.FIELD_START_TIME) != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                Text(
                    text = "：",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = uiState.timeMinute,
                    onValueChange = { viewModel.onEvent(NewScheduleEvent.TimeMinuteChanged(it)) },
                    label = { Text("分") },
                    placeholder = { Text("00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = uiState.errorFor(NewScheduleViewModel.FIELD_START_TIME) != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
            }
            uiState.errorFor(NewScheduleViewModel.FIELD_START_TIME)?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 持续时间
            OutlinedTextField(
                value = uiState.durationMinutes,
                onValueChange = { viewModel.onEvent(NewScheduleEvent.DurationChanged(it)) },
                label = { Text("持续时间（分钟）") },
                placeholder = { Text("例如 45、90") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.errorFor(NewScheduleViewModel.FIELD_DURATION) != null,
                supportingText = uiState.errorFor(NewScheduleViewModel.FIELD_DURATION)?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                } ?: { Text("不填则使用任务类型默认时长", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(16.dp))

            // 优先级
            Text(
                text = "优先级",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.selectableGroup()) {
                Priority.entries.forEach { p ->
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = uiState.priority == p,
                                onClick = { viewModel.onEvent(NewScheduleEvent.PriorityChanged(p)) },
                                role = Role.RadioButton
                            )
                            .padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.priority == p,
                            onClick = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${p.name} · ${p.displayName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 备注
            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.onEvent(NewScheduleEvent.NoteChanged(it)) },
                label = { Text("备注（可选）") },
                placeholder = { Text("补充说明…") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.onEvent(NewScheduleEvent.Save) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberBase),
                enabled = uiState.canSave && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("保存中…")
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isEditMode) "保存修改" else "保存日程")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewScheduleScreenEmptyPreview() {
    MaterialTheme {
        NewScheduleScreen(onNavigateBack = {})
    }
}
