package com.life.app.ui.modes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.life.app.ui.theme.AmberBase
import com.life.app.ui.theme.AmberLight
import com.life.app.ui.theme.WarmOutlineVariant
import com.life.app.ui.theme.WarmText2nd
import com.life.app.ui.theme.WarmTextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(
    onNavigateBack: () -> Unit,
    onModeSelected: (ModeType) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预设模式") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "选择一个模式开始",
                style = MaterialTheme.typography.bodyLarge,
                color = WarmText2nd
            )

            ModeType.entries.forEach { mode ->
                ModeCard(
                    mode = mode,
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: ModeType,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AmberLight.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, WarmOutlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = AmberBase
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleMedium,
                color = WarmTextMain,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "默认 ${mode.defaultDurationMinutes} 分钟",
                style = MaterialTheme.typography.bodySmall,
                color = WarmText2nd
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModeSelectScreenPreview() {
    MaterialTheme {
        ModeSelectScreen(
            onNavigateBack = {},
            onModeSelected = {}
        )
    }
}
