package com.life.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.life.app.domain.model.Priority
import com.life.app.domain.model.Schedule
import com.life.app.ui.theme.PriorityColors
import com.life.app.ui.theme.PriorityPalette

/**
 * 日程卡片。视觉规则（对齐设计稿）：
 * - P0（最高优先级）：深蓝实色背景 + 白字 + ⭐（或 ▶ 进行中）
 * - P1：浅蓝背景 + 深色字 + 蓝色徽章
 * - P2：极浅灰背景 + 深色字 + 灰色徽章
 * - 已完成：透明度 0.5 + 删除线
 */
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

    val bg = if (isP0 || isInProgress) palette.bg else palette.soft
    val titleColor = if (isP0 || isInProgress) palette.text else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isP0 || isInProgress) {
        palette.text.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDone) bg.copy(alpha = 0.5f) else bg)
            .clickable { onClick(schedule.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // 左侧图标圆形
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isP0 || isInProgress) Color.White.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isInProgress) Icons.Filled.PlayArrow else ScheduleIcons.forSchedule(schedule),
                    contentDescription = null,
                    tint = if (isP0 || isInProgress) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))

            // 中部标题 + 时间
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    maxLines = 1
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime} · ${schedule.durationMinutes} 分钟",
                    color = timeColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            // 右侧：徽章 + 状态
            Column(horizontalAlignment = Alignment.End) {
                PriorityBadge(priority = schedule.priority, palette = palette, isOnDark = isP0 || isInProgress)
                Spacer(Modifier.size(4.dp))
                when {
                    isInProgress -> Text(
                        text = "进行中",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    isP0 -> Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "重要",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    else -> {}
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
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = priority.name,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}