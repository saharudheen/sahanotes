package com.saha.notes

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saha.notes.reminder.EXTRA_NOTE_ID
import com.saha.notes.ui.calendar.CalendarScreen
import com.saha.notes.ui.editor.NoteEditorScreen
import com.saha.notes.ui.list.NoteListScreen
import com.saha.notes.ui.settings.SettingsScreen
import com.saha.notes.ui.theme.NotesTheme
import com.saha.notes.ui.theme.isAppDark

class MainActivity : ComponentActivity() {

    /** Intents from reminder taps or "Share to Saha Notes", handled once by the nav graph. */
    private val incoming = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) incoming.value = intent
        val settings = (application as NotesApp).settings
        setContent {
            val dark = isAppDark(settings.theme)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            NotesTheme(dark = dark, textScale = settings.textScale) { AppNavigation(incoming) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming.value = intent
    }
}

private fun editorRoute(id: Long, voice: Boolean = false, category: String? = null): String {
    val base = "editor?id=$id&voice=$voice"
    return if (category.isNullOrBlank()) base else "$base&category=${Uri.encode(category)}"
}

@Composable
private fun AppNavigation(incoming: MutableState<Intent?>) {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as NotesApp

    LaunchedEffect(incoming.value) {
        val intent = incoming.value ?: return@LaunchedEffect
        incoming.value = null
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId > 0) {
            nav.navigate(editorRoute(noteId)) { popUpTo("list") }
        } else if (intent.action == Intent.ACTION_SEND) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val shared = listOf(subject, text).filter { it.isNotBlank() }.joinToString("\n")
            if (shared.isNotBlank()) {
                app.sharedText = shared
                nav.navigate(editorRoute(0)) { popUpTo("list") }
            }
        }
    }

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            NoteListScreen(
                onOpenNote = { id -> nav.navigate(editorRoute(id)) },
                onNewNote = { voice, category -> nav.navigate(editorRoute(0, voice, category)) },
                onOpenCalendar = { nav.navigate("calendar") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable(
            route = "editor?id={id}&voice={voice}&category={category}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                navArgument("voice") { type = NavType.BoolType; defaultValue = false },
                navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            NoteEditorScreen(onBack = { nav.popBackStack() })
        }
        composable("calendar") {
            CalendarScreen(
                onBack = { nav.popBackStack() },
                onOpenNote = { id -> nav.navigate(editorRoute(id)) },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
