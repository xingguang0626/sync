package com.life.app.ui.modes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.life.app.ui.theme.AmberLight
import com.life.app.ui.theme.WarmBg
import kotlinx.coroutines.delay

@Composable
fun ModeTimerScreen(
    mode: ModeType,
    onEnd: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSeconds = mode.defaultDurationMinutes * 60
    var remaining by remember { mutableIntStateOf(totalSeconds) }
    val isRunning = remaining > 0

    val progressColor by animateColorAsState(
        targetValue = when {
            remaining < 60 -> Color(0xFFD4786E)
            remaining < 180 -> Color(0xFFF4A261)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "timerColor"
    )

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000L)
            remaining--
        }
    }

    LaunchedEffect(remaining) {
        if (remaining == 0) {
            delay(800L)
            onEnd()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        WarmBg,
                        AmberLight.copy(alpha = 0.25f),
                        WarmBg
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = progressColor
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "${mode.label}模式",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = formatTime(remaining),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = progressColor
            )

            Spacer(Modifier.height(32.dp))

            if (isRunning) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ) {
                    Text("结束模式")
                }
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun StudyTimerPreview() {
    MaterialTheme {
        ModeTimerScreen(
            mode = ModeType.STUDY,
            onEnd = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NapTimerPreview() {
    MaterialTheme {
        ModeTimerScreen(
            mode = ModeType.NAP,
            onEnd = {},
            onCancel = {}
        )
    }
}
