package com.life.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

private val repeatLabels = mapOf(
    "DAILY" to "每天",
    "WEEKLY" to "每周",
    "WEEKDAYS" to "工作日",
    "WEEKENDS" to "周末",
    "MONTHLY" to "每月"
)

@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    when {
        uiState.isLoading -> {
            Column(modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = AmberBase)
            }
        }
        uiState.error != null -> {
            Column(modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "加载失败", color = MaterialTheme.colorScheme.error)
                Text(text = uiState.error ?: "", style = MaterialTheme.typography.bodySmall,
                    color = WarmText2nd)
            }
        }
        uiState.schedulesByRepeat.isEmpty() -> {
            Column(modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.Repeat, contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = WarmText2nd)
                Spacer(Modifier.height(16.dp))
                Text(text = "暂无重复计划",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WarmText2nd)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.schedulesByRepeat.forEach { (key, schedules) ->
                    val label = repeatLabels[key] ?: key
                    item(key = key) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = WarmTextMain,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(schedules, key = { it.id }) { schedule ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = schedule.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmTextMain,
                                    fontWeight = FontWeight.Medium)
                                Text(text = "${schedule.startTime} - ${schedule.endTime} · ${schedule.priority.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmText2nd)
                            }
                        }
                    }
                }
            }
        }
    }
}
