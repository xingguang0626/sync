package com.life.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.life.app.domain.model.Priority

/**
 * 暖琥珀系优先级色板。像茶的颜色从浓到淡。
 *
 * P0 = 深琥珀（最高优先级） / P1 = 中琥珀（中等） / P2 = 暖灰（一般）
 *
 * UI 规范：
 * - P0 卡片：深琥珀实色背景 + 白字
 * - P1 卡片：浅琥珀背景 + 深色文字
 * - P2 卡片：暖灰背景 + 深色文字
 * - 进行中：P0 同色 + 呼吸光晕
 */
data class PriorityPalette(
    val bg: Color,
    val bar: Color,
    val text: Color,
    val soft: Color,
    val badgeBg: Color,
    val badgeText: Color
)

object PriorityColors {

    val P0 = PriorityPalette(
        bg = AmberDark,
        bar = AmberDeep,
        text = Color.White,
        soft = Color(0xFFFFF5EB),
        badgeBg = Color.White.copy(alpha = 0.25f),
        badgeText = Color.White
    )

    val P1 = PriorityPalette(
        bg = AmberLight,
        bar = AmberBase,
        text = WarmTextMain,
        soft = Color(0xFFFFF9F2),
        badgeBg = Color(0xFFFFE4CC),
        badgeText = AmberDeep
    )

    val P2 = PriorityPalette(
        bg = WarmSurface,
        bar = WarmOutline,
        text = WarmTextMain,
        soft = Color(0xFFF5F3F0),
        badgeBg = Color(0xFFE8E3DE),
        badgeText = WarmText2nd
    )

    fun forLevel(p: Priority): PriorityPalette = when (p) {
        Priority.P0 -> P0
        Priority.P1 -> P1
        Priority.P2 -> P2
    }
}
