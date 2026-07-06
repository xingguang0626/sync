package com.life.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
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
            }
        }
        else -> {
            Column(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "今日统计",
                    style = MaterialTheme.typography.titleLarge,
                    color = WarmTextMain,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                StatCard(
                    icon = Icons.Outlined.CheckCircle,
                    title = "完成率",
                    value = "${(uiState.completionRate * 100).toInt()}%",
                    subtitle = "${uiState.completedCount} / ${uiState.totalSchedules} 项"
                )
                StatCard(
                    icon = Icons.Outlined.AccessTime,
                    title = "专注时长",
                    value = formatMinutes(uiState.totalFocusMinutes),
                    subtitle = "${uiState.totalSchedules} 个日程"
                )
                StatCard(
                    icon = Icons.Outlined.TrendingUp,
                    title = "最活跃时段",
                    value = uiState.mostActiveHour,
                    subtitle = "日程最集中的时段"
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AmberBase
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmText2nd)
                Text(text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = WarmTextMain,
                    fontWeight = FontWeight.Bold)
                Text(text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmText2nd)
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${minutes}m"
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    MaterialTheme {
        StatsScreen()
    }
}
