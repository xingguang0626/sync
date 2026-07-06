package com.life.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberDeep
import com.life.app.ui.theme.AmberLight
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

data class NluDraftPreview(
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val priority: String,
    val repeat: String = "无",
    val confidence: Float = 0.9f,
    val uncertainFields: Set<String> = emptySet()
)

@Composable
fun NluConfirmCard(
    draft: NluDraftPreview,
    onConfirm: () -> Unit,
    onModify: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowConfidence = draft.confidence < 0.6f

    if (isLowConfidence) {
        LowConfidencePrompt(onGoToManual = onModify, modifier = modifier)
        return
    }

    val isMediumConfidence = draft.confidence < 0.85f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = AmberLight.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "我理解为：",
                style = MaterialTheme.typography.labelMedium,
                color = AmberDeep
            )

            Spacer(Modifier.height(12.dp))

            DraftField("日程", draft.title, draft.uncertainFields.contains("title"))
            DraftField("时间", "${draft.date} ${draft.startTime} - ${draft.endTime}",
                draft.uncertainFields.contains("time"))
            DraftField("优先级", draft.priority, draft.uncertainFields.contains("priority"))
            DraftField("重复", draft.repeat, draft.uncertainFields.contains("repeat"))

            if (isMediumConfidence) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "部分字段不确定，点击「修改」进行调整",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onModify,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("修改一下", color = AmberDeep)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberBase)
                ) {
                    Text("确认添加")
                }
            }
        }
    }
}

@Composable
private fun DraftField(
    label: String,
    value: String,
    isUncertain: Boolean
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmText2nd
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUncertain)
                MaterialTheme.colorScheme.error
            else
                WarmTextMain
        )
    }
}

@Composable
private fun LowConfidencePrompt(
    onGoToManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "我不太确定你的意思",
                style = MaterialTheme.typography.titleSmall,
                color = WarmTextMain
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "请补充更多信息，或者点击下方按钮手动填写完整表单。",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmText2nd
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onGoToManual,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberBase)
            ) {
                Text("手动新建日程")
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun HighConfidencePreview() {
    MaterialTheme {
        NluConfirmCard(
            draft = NluDraftPreview(
                title = "Deadline",
                date = "2026-07-03",
                startTime = "22:00",
                endTime = "23:00",
                priority = "P0",
                confidence = 0.92f
            ),
            onConfirm = {},
            onModify = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediumConfidencePreview() {
    MaterialTheme {
        NluConfirmCard(
            draft = NluDraftPreview(
                title = "学习",
                date = "2026-07-03",
                startTime = "20:00",
                endTime = "21:30",
                priority = "P1",
                confidence = 0.72f,
                uncertainFields = setOf("time", "priority")
            ),
            onConfirm = {},
            onModify = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LowConfidencePreview() {
    MaterialTheme {
        NluConfirmCard(
            draft = NluDraftPreview(
                title = "那个",
                date = "",
                startTime = "",
                endTime = "",
                priority = "P1",
                confidence = 0.35f
            ),
            onConfirm = {},
            onModify = {}
        )
    }
}
