package com.saha.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261A00),
    secondaryContainer = Color(0xFFF3E2BC),
    onSecondaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5),
    surfaceContainer = Color(0xFFF7F1E7),
    surfaceContainerHigh = Color(0xFFF1EBE0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF5BF48),
    onPrimary = Color(0xFF402D00),
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondaryContainer = Color(0xFF4F4532),
    onSecondaryContainer = Color(0xFFF3E2BC),
    background = Color(0xFF17140F),
    surface = Color(0xFF17140F),
    surfaceContainer = Color(0xFF221F19),
    surfaceContainerHigh = Color(0xFF2C2922),
)

@Composable
fun NotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
