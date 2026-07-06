package com.life.app.ui.home.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.life.app.domain.model.Priority
import com.life.app.domain.model.Schedule
import com.life.app.domain.model.ScheduleStatus
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberDark
import com.life.app.ui.theme.AmberDeep
import com.life.app.ui.theme.DuskDark
import com.life.app.ui.theme.PriorityColors
import com.life.app.ui.theme.PriorityPalette
import com.life.app.ui.theme.WarmOutline
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd

@Composable
fun ScheduleCard(
    schedule: Schedule,
    onClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = PriorityColors.forLevel(schedule.priority)
    val isP0 = schedule.priority == Priority.P0
    val isInProgress = schedule.isInProgress
    val isDone = schedule.isCompleted

    val accentColor = when {
        isInProgress -> AmberBase
        schedule.priority == Priority.P0 -> AmberBase
        schedule.priority == Priority.P1 -> DuskDark
        else -> WarmOutline
    }

    val cardAlpha = if (isDone) 0.5f else 1f

    // 进行中：呼吸光晕
    val glowAlpha by if (isInProgress) {
        val transition = rememberInfiniteTransition(label = "scheduleGlow")
        transition.animateFloat(
            initialValue = 0.03f,
            targetValue = 0.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // 呼吸光晕背景（仅进行中）
        if (isInProgress) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AmberBase.copy(alpha = glowAlpha))
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = cardAlpha),
            border = BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(schedule.id) }
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
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isDone) 0.5f else 1f
                        ),
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        maxLines = 1
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = "${schedule.startTime} - ${schedule.endTime} · ${schedule.durationMinutes} 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmText2nd.copy(alpha = if (isDone) 0.5f else 1f),
                        maxLines = 1
                    )
                }

                // 右侧：状态或优先级标签
                when {
                    isInProgress -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = AmberBase,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "进行中",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    isP0 -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AmberDeep.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = schedule.priority.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberDeep
                            )
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = palette.badgeBg
                        ) {
                            Text(
                                text = schedule.priority.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.badgeText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(
    priority: Priority,
    palette: PriorityPalette,
    isOnDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isOnDark) Color.White.copy(alpha = 0.25f) else palette.badgeBg
    val fg = if (isOnDark) Color.White else palette.badgeText
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Text(
            text = priority.name,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ── Previews ──────────────────────────────────────────

private val previewP0Schedule = Schedule(
    id = 1, title = "Deadline 提交", date = "2026-07-04",
    startTime = "22:00", endTime = "23:00", durationMinutes = 60,
    priority = Priority.P0, status = ScheduleStatus.PENDING
)

private val previewP1Schedule = Schedule(
    id = 2, title = "学习数学", date = "2026-07-04",
    startTime = "20:00", endTime = "21:30", durationMinutes = 90,
    priority = Priority.P1, status = ScheduleStatus.PENDING
)

private val previewP2Schedule = Schedule(
    id = 3, title = "散步", date = "2026-07-04",
    startTime = "18:00", endTime = "18:30", durationMinutes = 30,
    priority = Priority.P2, status = ScheduleStatus.PENDING
)

@Preview(showBackground = true)
@Composable
private fun P0CardPreview() {
    MaterialTheme {
        ScheduleCard(schedule = previewP0Schedule, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun P1CardPreview() {
    MaterialTheme {
        ScheduleCard(schedule = previewP1Schedule, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun P2CardPreview() {
    MaterialTheme {
        ScheduleCard(schedule = previewP2Schedule, onClick = {})
    }
}
