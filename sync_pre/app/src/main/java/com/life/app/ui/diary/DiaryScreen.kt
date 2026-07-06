package com.life.app.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.BorderStroke
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = hiltViewModel()
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
        uiState.completedSchedules.isEmpty() -> {
            Column(modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.Book, contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = WarmText2nd)
                Spacer(Modifier.height(16.dp))
                Text(text = "今天还没有完成的日程",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WarmText2nd)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "今日记录",
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmTextMain,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(uiState.completedSchedules, key = { it.id }) { schedule ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = AmberBase
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schedule.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmTextMain,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${schedule.startTime} - ${schedule.endTime} · ${schedule.priority.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmText2nd
                                )
                                if (!schedule.note.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = schedule.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmText2nd
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryScreenPreview() {
    MaterialTheme {
        DiaryScreen()
    }
}
