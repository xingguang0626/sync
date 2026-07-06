package com.life.app.ui.home.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.life.app.domain.model.Reminder
import com.life.app.domain.model.ReminderType
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberDark
import com.life.app.ui.theme.AmberDeep
import com.life.app.ui.theme.AmberLight
import com.life.app.ui.theme.WarmOutline
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onEditSchedule: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
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
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AmberLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = AmberBase,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = reminder.type.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── 提醒详情 ──
            Text(
                text = reminder.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── 建议区域 ──
            if (reminder.suggestion != null) {
                Spacer(Modifier.height(20.dp))

                val infiniteTransition = rememberInfiniteTransition(label = "reminderGlow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.04f,
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
                    color = AmberBase.copy(alpha = glowAlpha),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberBase.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "建议操作",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = reminder.suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmText2nd
                        )
                    }
                }
            }

            // ── 关联日程 ──
            if (reminder.relatedScheduleIds.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "关联日程",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmText2nd,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                reminder.relatedScheduleIds.forEach { id ->
                    Text(
                        text = "日程 #$id",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // ── 操作按钮 ──
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    onDismiss()
                    // 打开第一个关联日程进行修改（冲突场景用户会先看到关联日程列表）
                    reminder.relatedScheduleIds.firstOrNull()?.let { onEditSchedule(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberBase),
                enabled = reminder.relatedScheduleIds.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "修改相关日程", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp)
            ) {
                Text(
                    text = "关闭",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberDeep
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun EveningP0Preview() {
    MaterialTheme {
        ReminderBottomSheet(
            reminder = Reminder(
                type = ReminderType.EVENING_P0,
                priority = 1,
                message = "今晚有高优先级任务「Deadline」，建议开启加班模式，并顺延低优先级安排。",
                suggestion = "将「晚读」顺延到明天 21:00，为 Deadline 留出完整时间段。",
                relatedScheduleIds = listOf(1, 2)
            ),
            onDismiss = {},
            onEditSchedule = {}
        )
    }
}
