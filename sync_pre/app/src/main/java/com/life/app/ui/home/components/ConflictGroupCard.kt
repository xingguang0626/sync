package com.life.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * 冲突组卡片（3+ 日程）：点击弹出 ConflictBottomSheet 查看详情。
 */
@Composable
fun ConflictGroupCard(
    schedules: List<Schedule>,
    onScheduleClick: (Long) -> Unit,
    onConflictGroupClick: (List<Schedule>) -> Unit,
    modifier: Modifier = Modifier
) {
    require(schedules.size >= 3) { "ConflictGroupCard 必须是 3+ 个日程" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            .clickable { onConflictGroupClick(schedules) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${schedules.size} 个日程时间冲突",
                style = MaterialTheme.typography.labelMedium,
                color = ErrorRed,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = schedules.joinToString("、") { it.title },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = "点击查看冲突详情",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
