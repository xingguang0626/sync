package com.life.app.ui.modes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.ui.graphics.vector.ImageVector

enum class ModeType(
    val label: String,
    val defaultDurationMinutes: Int,
    val icon: ImageVector
) {
    STUDY("学习", 90, Icons.AutoMirrored.Filled.MenuBook),
    RELAX("放松", 30, Icons.Filled.SelfImprovement),
    NAP("小憩", 20, Icons.Filled.Bedtime)
}

fun positiveFeedbackFor(mode: ModeType): List<String> = when (mode) {
    ModeType.STUDY -> listOf(
        "专注的时光总是过得很快，你做得很好。",
        "每一次投入都在靠近目标，继续保持。",
        "学习结束！休息一下，犒劳自己吧。"
    )
    ModeType.RELAX -> listOf(
        "放松一下，重新出发。",
        "短暂的休息是为了更好的开始。",
        "身心都轻盈了不少吧？"
    )
    ModeType.NAP -> listOf(
        "小憩结束，精神满满。",
        "充电完成，准备继续吧。",
        "好的休息是高效的前提。"
    )
}
