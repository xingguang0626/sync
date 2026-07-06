package com.life.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.life.app.domain.model.Schedule
import com.life.app.ui.theme.ErrorRed

/**
 * 冲突对卡片（恰好 2 个日程）：左右并列显示。
 */
@Composable
fun ConflictPairCard(
    schedules: List<Schedule>,
    onScheduleClick: (Long) -> Unit,
    onConflictGroupClick: (List<Schedule>) -> Unit,
    modifier: Modifier = Modifier
) {
    require(schedules.size == 2) { "ConflictPairCard 必须是 2 个日程" }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            .clickable { onConflictGroupClick(schedules) }
            .padding(12.dp)
    ) {
        // 顶部警示标签
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "2 个日程时间冲突",
                style = MaterialTheme.typography.labelMedium,
                color = ErrorRed,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.size(8.dp))
        // 左右并列两个日程（紧凑模式）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            schedules.forEach { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onClick = onScheduleClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}