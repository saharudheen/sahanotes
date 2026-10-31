package com.saha.notes.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** App preferences. Each value is Compose state, so the UI updates as soon as it changes. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = mutableIntStateOf(prefs.getInt("theme", THEME_SYSTEM))
    var theme: Int
        get() = _theme.intValue
        set(value) {
            _theme.intValue = value; prefs.edit().putInt("theme", value).apply()
        }

    private val _textSize = mutableIntStateOf(prefs.getInt("text_size", 1))
    var textSize: Int
        get() = _textSize.intValue
        set(value) {
            _textSize.intValue = value; prefs.edit().putInt("text_size", value).apply()
        }
    val textScale: Float get() = TEXT_SCALES.getOrElse(textSize) { 1f }

    private val _sort = mutableIntStateOf(prefs.getInt("sort", SORT_MODIFIED))
    var sort: Int
        get() = _sort.intValue
        set(value) {
            _sort.intValue = value; prefs.edit().putInt("sort", value).apply()
        }

    private val _gridView = mutableStateOf(prefs.getBoolean("grid_view", true))
    var gridView: Boolean
        get() = _gridView.value
        set(value) {
            _gridView.value = value; prefs.edit().putBoolean("grid_view", value).apply()
        }

    /** BCP-47 tag for voice typing, e.g. "en-IN" or "ta-IN". */
    private val _voiceLanguage = mutableStateOf(prefs.getString("voice_language", "en-IN") ?: "en-IN")
    var voiceLanguage: String
        get() = _voiceLanguage.value
        set(value) {
            _voiceLanguage.value = value; prefs.edit().putString("voice_language", value).apply()
        }

    private val _categories = mutableStateOf(
        (prefs.getString("categories", null) ?: "Home\nWork\nPersonal")
            .split("\n").filter { it.isNotBlank() }
    )
    var categories: List<String>
        get() = _categories.value
        set(value) {
            val clean = value.map { it.replace("\n", " ").trim() }.filter { it.isNotEmpty() }.distinct()
            _categories.value = clean
            prefs.edit().putString("categories", clean.joinToString("\n")).apply()
        }

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
        val THEME_LABELS = listOf("System default", "Light", "Dark")

        val TEXT_SIZE_LABELS = listOf("Small", "Normal", "Large", "Extra large")
        val TEXT_SCALES = listOf(0.9f, 1f, 1.15f, 1.3f)

        const val SORT_MODIFIED = 0
        const val SORT_CREATED = 1
        const val SORT_TITLE = 2
        val SORT_LABELS = listOf("Date modified", "Date created", "Title (A–Z)")

        val VOICE_LANGUAGES = listOf("en-IN" to "English", "ta-IN" to "தமிழ் (Tamil)")
    }
}
