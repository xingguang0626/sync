package com.life.app.ui.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberDeep
import com.life.app.ui.theme.WarmText2nd
import kotlin.random.Random

@Composable
fun ModeFeedbackScreen(
    mode: ModeType,
    onBackToHome: () -> Unit,
    feedbackViewModel: ModeFeedbackViewModel = hiltViewModel()
) {
    val feedback = rememberFeedback(mode)
    val uiState by feedbackViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = mode.icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = AmberBase
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "${mode.label}模式结束",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = feedback,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = WarmText2nd,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        if (uiState.isPostponing) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = AmberBase)
            Text(
                text = "正在顺延日程…",
                style = MaterialTheme.typography.bodySmall,
                color = WarmText2nd,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else if (uiState.postponedCount > 0) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "已顺延 ${uiState.postponedCount} 个日程",
                style = MaterialTheme.typography.titleSmall,
                color = AmberBase
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBackToHome,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberBase)
            ) {
                Text("返回首页")
            }
        } else if (uiState.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "顺延失败：${uiState.error}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onBackToHome,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("返回首页", color = AmberDeep)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { feedbackViewModel.postponeFollowingSchedules() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberBase)
                ) {
                    Text("重试")
                }
                Spacer(Modifier.weight(1f))
            }
        } else {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "后续日程是否顺延？",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmText2nd
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onBackToHome,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("不顺延", color = AmberDeep)
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { feedbackViewModel.postponeFollowingSchedules() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberBase)
                ) {
                    Text("顺延后续安排")
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun rememberFeedback(mode: ModeType): String {
    val feedbacks = positiveFeedbackFor(mode)
    return feedbacks[Random.nextInt(feedbacks.size)]
}

@Preview(showBackground = true)
@Composable
private fun StudyFeedbackPreview() {
    MaterialTheme {
        ModeFeedbackScreen(
            mode = ModeType.STUDY,
            onBackToHome = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RelaxFeedbackPreview() {
    MaterialTheme {
        ModeFeedbackScreen(
            mode = ModeType.RELAX,
            onBackToHome = {}
        )
    }
}
