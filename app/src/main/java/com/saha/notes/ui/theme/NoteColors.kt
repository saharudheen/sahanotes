package com.saha.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class NoteColor(val name: String, val light: Color, val dark: Color)

/** Index 0 is "Default" and follows the app theme. */
val NoteColors = listOf(
    NoteColor("Default", Color.Unspecified, Color.Unspecified),
    NoteColor("Yellow", Color(0xFFFFF1AE), Color(0xFF574A12)),
    NoteColor("Orange", Color(0xFFFFD9B8), Color(0xFF66391A)),
    NoteColor("Red", Color(0xFFFFCBC8), Color(0xFF6B2828)),
    NoteColor("Pink", Color(0xFFF9CBE6), Color(0xFF5E2849)),
    NoteColor("Purple", Color(0xFFDDCFFF), Color(0xFF433068)),
    NoteColor("Blue", Color(0xFFC8DEFF), Color(0xFF213E66)),
    NoteColor("Teal", Color(0xFFC0ECE5), Color(0xFF1B4E46)),
    NoteColor("Green", Color(0xFFD5F0BE), Color(0xFF334F28)),
    NoteColor("Grey", Color(0xFFE4E2DE), Color(0xFF3D3B38)),
)

@Composable
fun noteBackground(index: Int): Color {
    val color = NoteColors.getOrElse(index) { NoteColors[0] }
    if (color.light == Color.Unspecified) return MaterialTheme.colorScheme.surfaceContainerHigh
    return if (LocalDarkTheme.current) color.dark else color.light
}
