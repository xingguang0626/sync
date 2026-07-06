package com.life.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AmberDeep,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberBase,
    onPrimaryContainer = AmberOnPrimary,
    secondary = DuskBase,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF375A7F),
    tertiary = Color(0xFF5E5E5C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB7B6B3),
    onTertiaryContainer = Color(0xFF474746),
    background = WarmBg,
    onBackground = WarmTextMain,
    surface = WarmBg,
    onSurface = WarmTextMain,
    surfaceVariant = WarmSurface,
    onSurfaceVariant = WarmText2nd,
    surfaceContainerLow = Color(0xFFFBF2ED),
    surfaceContainer = Color(0xFFF5ECE7),
    surfaceContainerHigh = WarmSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE9E1DC),
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF93000A),
    inverseSurface = WarmInverseSurface,
    inverseOnSurface = Color(0xFFF8EFEA),
    inversePrimary = Color(0xFFFFB955)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB955),
    onPrimary = Color(0xFF462A00),
    primaryContainer = AmberDark,
    onPrimaryContainer = Color(0xFFFFDDB4),
    secondary = Color(0xFFA7C9F4),
    onSecondary = Color(0xFF12344F),
    secondaryContainer = Color(0xFF2A4D6E),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFFC8C6C4),
    onTertiary = Color(0xFF30302E),
    background = Color(0xFF1E1B18),
    onBackground = Color(0xFFF2EFEC),
    surface = Color(0xFF1E1B18),
    onSurface = Color(0xFFF2EFEC),
    surfaceVariant = Color(0xFF524534),
    onSurfaceVariant = Color(0xFFD7C3AE),
    outline = Color(0xFF857462),
    outlineVariant = Color(0xFF524534),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF2EFEC),
    inverseOnSurface = Color(0xFF34302C),
    inversePrimary = AmberDeep
)

@Composable
fun SyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = SyncTypography,
        content = content
    )
}
