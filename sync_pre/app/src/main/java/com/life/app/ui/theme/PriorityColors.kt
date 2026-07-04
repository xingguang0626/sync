package com.life.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.life.app.domain.model.Priority

/**
 * 蓝色系不同深浅。严格符合"同色系不同深浅"原则。
 *
 * P0 = 蓝 900（最深） / P1 = 蓝 700（中等） / P2 = 浅灰蓝（最浅）
 *
 * UI 规范：
 * - P0 卡片：深蓝实色背景 + 白字 + ⭐（最高优先级视觉）
 * - P1 卡片：浅蓝背景 + 深色文字 + 中等优先级标签
 * - P2 卡片：极浅灰背景 + 深色文字 + 一般优先级标签
 * - 进行中：P0 同色 + ▶ 播放图标
 */
data class PriorityPalette(
    /** 卡片背景 */
    val bg: Color,
    /** 左侧色块 / 装饰条 */
    val bar: Color,
    /** 标题文字 */
    val text: Color,
    /** 软背景（未选中 / 已完成） */
    val soft: Color,
    /** 标签徽章背景 */
    val badgeBg: Color,
    /** 标签徽章文字 */
    val badgeText: Color
)

object PriorityColors {

    val P0 = PriorityPalette(
        bg = Color(0xFF0D47A1),       // 蓝 900
        bar = Color(0xFF002171),       // 蓝 950
        text = Color(0xFFFFFFFF),      // 白
        soft = Color(0xFFE3F2FD),      // 蓝 50
        badgeBg = Color(0xFFFFFFFF),
        badgeText = Color(0xFF0D47A1)
    )

    val P1 = PriorityPalette(
        bg = Color(0xFFE3F2FD),       // 蓝 50
        bar = Color(0xFF1976D2),       // 蓝 700
        text = Color(0xFF1F2937),      // 深灰
        soft = Color(0xFFF8FAFC),      // 极浅灰
        badgeBg = Color(0xFFBBDEFB),
        badgeText = Color(0xFF0D47A1)
    )

    val P2 = PriorityPalette(
        bg = Color(0xFFF8FAFC),       // 极浅灰
        bar = Color(0xFFCBD5E1),       // 浅灰
        text = Color(0xFF374151),      // 中灰
        soft = Color(0xFFFAFAFA),
        badgeBg = Color(0xFFE2E8F0),
        badgeText = Color(0xFF64748B)
    )

    fun forLevel(p: Priority): PriorityPalette = when (p) {
        Priority.P0 -> P0
        Priority.P1 -> P1
        Priority.P2 -> P2
    }
}