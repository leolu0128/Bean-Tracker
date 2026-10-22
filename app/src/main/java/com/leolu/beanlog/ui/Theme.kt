package com.leolu.beanlog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.leolu.beanlog.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF6F4E37),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3DFCF),
    onPrimaryContainer = Color(0xFF2B1709),
    secondary = Color(0xFF8A6A55),
    secondaryContainer = Color(0xFFEFE0D5),
    onSecondaryContainer = Color(0xFF2E1C11),
    background = Color(0xFFFBF8F5),
    surface = Color(0xFFFBF8F5),
    surfaceContainerLow = Color(0xFFF5EFEA),
    surfaceContainer = Color(0xFFF0E9E3),
    surfaceContainerHigh = Color(0xFFEAE2DB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE2BFA6),
    onPrimary = Color(0xFF422A19),
    primaryContainer = Color(0xFF5A3F2C),
    onPrimaryContainer = Color(0xFFF3DFCF),
    secondary = Color(0xFFD9C2B2),
    secondaryContainer = Color(0xFF4A382C),
    onSecondaryContainer = Color(0xFFEFE0D5),
    background = Color(0xFF1A1411),
    surface = Color(0xFF1A1411),
    surfaceContainerLow = Color(0xFF221B17),
    surfaceContainer = Color(0xFF27201B),
    surfaceContainerHigh = Color(0xFF322A25),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun BeanLogTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
