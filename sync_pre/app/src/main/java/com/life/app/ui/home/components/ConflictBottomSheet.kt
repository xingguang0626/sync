package com.life.app.ui.home.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.life.app.domain.model.Priority
import com.life.app.domain.model.Schedule
import com.life.app.domain.model.ScheduleStatus
import com.life.app.domain.model.RepeatType
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberDark
import com.life.app.ui.theme.AmberDeep
import com.life.app.ui.theme.AmberLight
import com.life.app.ui.theme.DuskDark
import com.life.app.ui.theme.ErrorContainer
import com.life.app.ui.theme.WarmOutline
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmSurface
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictBottomSheet(
    schedules: List<Schedule>,
    suggestion: String?,
    onDismiss: () -> Unit,
    onAcceptSuggestion: () -> Unit,
    onManualAdjust: () -> Unit,
    onKeepAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            // 自定义拖拽手柄
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(WarmOutline.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // ── 标题行 ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${schedules.size} 个日程时间冲突",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── 冲突日程列表 ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                schedules.forEach { schedule ->
                    ConflictItem(schedule = schedule)
                }
            }

            // ── 建议区域 ──
            if (suggestion != null) {
                Spacer(Modifier.height(24.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "glowAlpha"
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AmberBase.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberBase.copy(alpha = 0.2f)),
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = AmberBase,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmText2nd,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }

            // ── 操作按钮 ──
            Spacer(Modifier.height(32.dp))

            // 主操作：采纳建议
            Button(
                onClick = onAcceptSuggestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberBase),
                enabled = suggestion != null
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "采纳建议",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(10.dp))

            // 次要操作：手动调整
            OutlinedButton(
                onClick = onManualAdjust,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(listOf(WarmOutlineVariant, WarmOutlineVariant))
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "手动调整",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberDeep
                )
            }

            Spacer(Modifier.height(4.dp))

            // 三级操作：仍然保留
            TextButton(
                onClick = onKeepAll,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "仍然保留",
                    style = MaterialTheme.typography.labelLarge,
                    color = WarmText2nd
                )
            }
        }
    }
}

@Composable
private fun ConflictItem(schedule: Schedule) {
    val accentColor = when (schedule.priority) {
        Priority.P0 -> AmberBase
        Priority.P1 -> DuskDark
        Priority.P2 -> WarmOutline
    }

    val badgeBg = when (schedule.priority) {
        Priority.P0 -> AmberDeep.copy(alpha = 0.12f)
        Priority.P1 -> AmberLight
        Priority.P2 -> WarmSurface
    }

    val badgeText = when (schedule.priority) {
        Priority.P0 -> AmberDeep
        Priority.P1 -> AmberDeep
        Priority.P2 -> WarmText2nd
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(14.dp))

            // 标题 + 时间
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmText2nd
                )
            }

            // 优先级标签
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBg
            ) {
                Text(
                    text = schedule.priority.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────

private val previewConflictPair = listOf(
    Schedule(
        id = 3, title = "学习", date = "2026-07-03",
        startTime = "15:00", endTime = "16:30", durationMinutes = 90,
        priority = Priority.P1, status = ScheduleStatus.PENDING
    ),
    Schedule(
        id = 4, title = "小组会议", date = "2026-07-03",
        startTime = "15:30", endTime = "16:00", durationMinutes = 30,
        priority = Priority.P0, status = ScheduleStatus.PENDING
    )
)

private val previewConflictGroup = listOf(
    Schedule(1, "复习数学", "2026-07-03", "15:00", "16:30", 90, Priority.P1, ScheduleStatus.PENDING),
    Schedule(2, "小组会议", "2026-07-03", "15:00", "16:00", 60, Priority.P1, ScheduleStatus.PENDING),
    Schedule(3, "Deadline", "2026-07-03", "15:30", "16:00", 30, Priority.P0, ScheduleStatus.PENDING)
)

@Preview(showBackground = true)
@Composable
private fun ConflictPairPreview() {
    MaterialTheme {
        ConflictBottomSheet(
            schedules = previewConflictPair,
            suggestion = "建议将「学习」顺延到 16:30 开始",
            onDismiss = {},
            onAcceptSuggestion = {},
            onManualAdjust = {},
            onKeepAll = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConflictGroupPreview() {
    MaterialTheme {
        ConflictBottomSheet(
            schedules = previewConflictGroup,
            suggestion = "建议保留 P0「Deadline」不变，将其它日程顺延",
            onDismiss = {},
            onAcceptSuggestion = {},
            onManualAdjust = {},
            onKeepAll = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConflictNoSuggestionPreview() {
    MaterialTheme {
        ConflictBottomSheet(
            schedules = previewConflictPair,
            suggestion = null,
            onDismiss = {},
            onAcceptSuggestion = {},
            onManualAdjust = {},
            onKeepAll = {}
        )
    }
}
