package com.life.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── 暖琥珀主色（Primary） ──
val AmberLight = Color(0xFFFFF1E6)
val AmberBase = Color(0xFFF5A623)
val AmberDark = Color(0xFFC17D11)
val AmberDeep = Color(0xFF835500)
val AmberOnPrimary = Color(0xFFFFFFFF)

// ── 薄暮蓝辅助色（Secondary） ──
val DuskLight = Color(0xFFEBF0F7)
val DuskBase = Color(0xFF6B8DB5)
val DuskDark = Color(0xFF3D5A80)

// ── 中性暖灰（Neutral） ──
val WarmBg = Color(0xFFFBF9F6)
val WarmCard = Color(0xFFFFFFFF)
val WarmSurface = Color(0xFFF2EFEC)
val WarmSurfaceHigh = Color(0xFFEFE6E2)
val WarmOutline = Color(0xFFB8B0A8)
val WarmOutlineVariant = Color(0xFFD7C3AE)
val WarmText2nd = Color(0xFF5C5550)
val WarmTextMain = Color(0xFF2C2825)
val WarmInverseSurface = Color(0xFF34302C)

// ── 语义色 ──
val SuccessMuted = Color(0xFF7FB069)
val ErrorWarm = Color(0xFFD4786E)
val ErrorContainer = Color(0xFFFFDAD6)
val InfoMuted = Color(0xFF7BA7C9)

// ── AI 卡片（琥珀系） ──
val AiCardBg = Color(0xFFFFF5EB)
val AiCardText = Color(0xFF835500)

// ── 兼容旧引用 ──
@Deprecated("Use AmberBase", ReplaceWith("AmberBase"))
val BluePrimary = AmberBase
@Deprecated("Use AmberDark", ReplaceWith("AmberDark"))
val BluePrimaryDark = AmberDark
@Deprecated("Use DuskBase", ReplaceWith("DuskBase"))
val BlueSecondary = DuskBase
@Deprecated("Use AmberOnPrimary", ReplaceWith("AmberOnPrimary"))
val BlueOnPrimary = AmberOnPrimary
@Deprecated("Use DuskDark", ReplaceWith("DuskDark"))
val BlueTertiary = DuskDark

@Deprecated("Use WarmCard", ReplaceWith("WarmCard"))
val SurfaceWhite = WarmCard
@Deprecated("Use WarmBg", ReplaceWith("WarmBg"))
val SurfaceLight = WarmBg
@Deprecated("Use AmberLight", ReplaceWith("AmberLight"))
val SurfaceSoftBlue = AmberLight

@Deprecated("Use WarmTextMain", ReplaceWith("WarmTextMain"))
val TextPrimary = WarmTextMain
@Deprecated("Use WarmText2nd", ReplaceWith("WarmText2nd"))
val TextSecondary = WarmText2nd
@Deprecated("Use WarmOutline", ReplaceWith("WarmOutline"))
val TextTertiary = WarmOutline
@Deprecated("Use AmberOnPrimary", ReplaceWith("AmberOnPrimary"))
val TextOnBlue = AmberOnPrimary

@Deprecated("Use WarmOutlineVariant", ReplaceWith("WarmOutlineVariant"))
val BorderLight = WarmOutlineVariant
@Deprecated("Use WarmOutline", ReplaceWith("WarmOutline"))
val BorderTimeline = WarmOutline

@Deprecated("Use SuccessMuted", ReplaceWith("SuccessMuted"))
val SuccessGreen = SuccessMuted
@Deprecated("Use AmberBase", ReplaceWith("AmberBase"))
val WarningOrange = AmberBase
@Deprecated("Use ErrorWarm", ReplaceWith("ErrorWarm"))
val ErrorRed = ErrorWarm
