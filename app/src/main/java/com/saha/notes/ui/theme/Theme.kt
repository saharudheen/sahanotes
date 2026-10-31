package com.saha.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.saha.notes.data.AppSettings

private val LightColors = lightColorScheme(
    primary = Color(0xFF00A3B8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4F1F7),
    onPrimaryContainer = Color(0xFF00363E),
    secondary = Color(0xFF4A6368),
    secondaryContainer = Color(0xFFD3EEF2),
    onSecondaryContainer = Color(0xFF05343B),
    background = Color(0xFFF6FCFD),
    surface = Color(0xFFF6FCFD),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEDF6F7),
    surfaceContainerHigh = Color(0xFFE5F2F4),
    outlineVariant = Color(0xFFC3D6D9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD8EA),
    onPrimary = Color(0xFF00363E),
    primaryContainer = Color(0xFF004F59),
    onPrimaryContainer = Color(0xFFC4F1F7),
    secondary = Color(0xFFB1CBD0),
    secondaryContainer = Color(0xFF334B50),
    onSecondaryContainer = Color(0xFFD3EEF2),
    background = Color(0xFF0E1415),
    surface = Color(0xFF0E1415),
    surfaceContainerLow = Color(0xFF161D1E),
    surfaceContainer = Color(0xFF1A2122),
    surfaceContainerHigh = Color(0xFF242B2C),
    outlineVariant = Color(0xFF3F484A),
)

/** Whether the app is drawing in dark mode (follows the in-app Theme setting, not just the phone). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isAppDark(themeSetting: Int): Boolean = when (themeSetting) {
    AppSettings.THEME_LIGHT -> false
    AppSettings.THEME_DARK -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun NotesTheme(dark: Boolean, textScale: Float = 1f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDarkTheme provides dark,
        LocalDensity provides Density(density.density, density.fontScale * textScale),
    ) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
