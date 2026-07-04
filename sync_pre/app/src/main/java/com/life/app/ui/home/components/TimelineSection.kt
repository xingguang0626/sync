package com.life.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.life.app.domain.model.TimelineItem
import com.life.app.ui.theme.ErrorRed
import com.life.app.ui.theme.PriorityColors

/**
 * 时间轴主容器。每个 item 一行：左侧时间戳 + 圆点，右侧卡片。
 * ConflictPair / ConflictGroup 用专门的 ConflictXxxCard 渲染。
 */
@Composable
fun TimelineSection(
    items: List<TimelineItem>,
    onScheduleClick: (Long) -> Unit,
    onConflictGroupClick: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            when (item) {
                is TimelineItem.Single -> {
                    val s = item.schedule
                    TimelineRow(
                        startTime = s.startTime,
                        barColor = PriorityColors.forLevel(s.priority).bar,
                        content = {
                            ScheduleCard(
                                schedule = s,
                                onClick = onScheduleClick
                            )
                        }
                    )
                }
                is TimelineItem.ConflictPair -> {
                    val first = item.schedules.first()
                    TimelineRow(
                        startTime = first.startTime,
                        barColor = ErrorRed,
                        content = {
                            ConflictPairCard(
                                schedules = item.schedules,
                                onScheduleClick = onScheduleClick,
                                onConflictGroupClick = onConflictGroupClick
                            )
                        }
                    )
                }
                is TimelineItem.ConflictGroup -> {
                    val first = item.schedules.first()
                    TimelineRow(
                        startTime = first.startTime,
                        barColor = ErrorRed,
                        content = {
                            ConflictGroupCard(
                                schedules = item.schedules,
                                onScheduleClick = onScheduleClick,
                                onConflictGroupClick = onConflictGroupClick
                            )
                        }
                    )
                }
            }
            if (index < items.lastIndex) Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun TimelineRow(
    startTime: String,
    barColor: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左侧时间
        Box(
            modifier = Modifier
                .width(48.dp)
                .padding(top = 14.dp, end = 8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = startTime,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // 中间圆点 + 竖线（用 Box 实现）
        Box(
            modifier = Modifier
                .size(width = 12.dp, height = 56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        // 右侧内容
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}