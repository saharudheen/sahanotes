package com.saha.notes.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.saha.notes.NotesApp
import com.saha.notes.data.AppSettings
import com.saha.notes.reminder.ReminderScheduler
import com.saha.notes.ui.components.ChoiceDialog
import com.saha.notes.ui.components.ManageCategoriesDialog
import com.saha.notes.ui.components.rememberBackupActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as NotesApp
    val s = app.settings
    val backup = rememberBackupActions()
    val scope = rememberCoroutineScope()

    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    // Re-read permission status when coming back from system settings.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
    val notificationsOn = remember(resumeTick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exactAlarmsOn = remember(resumeTick) { ReminderScheduler.canScheduleExact(context) }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item { SectionTitle("Appearance") }
            item { SettingRow(Icons.Rounded.DarkMode, "Theme", AppSettings.THEME_LABELS[s.theme]) { dialog = "theme" } }
            item { SettingRow(Icons.Rounded.FormatSize, "Text size", AppSettings.TEXT_SIZE_LABELS[s.textSize]) { dialog = "text" } }
            item {
                SwitchRow(Icons.Rounded.GridView, "Grid view", "Show notes in two columns", s.gridView) { s.gridView = it }
            }

            item { SectionTitle("Notes") }
            item { SettingRow(Icons.Rounded.SwapVert, "Sort notes by", AppSettings.SORT_LABELS[s.sort]) { dialog = "sort" } }
            item { SettingRow(Icons.Rounded.Tag, "Categories", s.categories.joinToString(", ").ifEmpty { "None" }) { dialog = "categories" } }
            item {
                val lang = AppSettings.VOICE_LANGUAGES.firstOrNull { it.first == s.voiceLanguage }?.second ?: "English"
                SettingRow(Icons.Rounded.Translate, "Voice typing language", lang) { dialog = "voice" }
            }

            item { SectionTitle("Reminders") }
            item {
                SettingRow(
                    Icons.Rounded.Notifications, "Notifications",
                    if (notificationsOn) "On" else "Off — reminders won't show. Tap to turn on.",
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingRow(
                        Icons.Rounded.Alarm, "Alarms & reminders",
                        if (exactAlarmsOn) "Allowed — reminders ring on time"
                        else "Not allowed — reminders may be a few minutes late. Tap to allow.",
                    ) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
            }

            item { SectionTitle("Backup & Restore") }
            item { SettingRow(Icons.Rounded.Backup, "Back up notes", "Save all notes to a file (Drive, Downloads…)", onClick = backup.exportNotes) }
            item { SettingRow(Icons.Rounded.FileDownload, "Restore from backup", "Add notes from a backup file", onClick = backup.importNotes) }

            item { SectionTitle("Storage") }
            item { SettingRow(Icons.Rounded.DeleteSweep, "Empty Trash", "Delete everything in Trash now") { dialog = "trash" } }

            item { SectionTitle("About") }
            item { SettingRow(Icons.Rounded.Info, "Saha Notes", "Version $versionName", onClick = null) }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    when (dialog) {
        "theme" -> ChoiceDialog("Theme", AppSettings.THEME_LABELS, s.theme, { s.theme = it; dialog = null }, { dialog = null })
        "text" -> ChoiceDialog("Text size", AppSettings.TEXT_SIZE_LABELS, s.textSize, { s.textSize = it; dialog = null }, { dialog = null })
        "sort" -> ChoiceDialog("Sort notes by", AppSettings.SORT_LABELS, s.sort, { s.sort = it; dialog = null }, { dialog = null })
        "voice" -> ChoiceDialog(
            "Voice typing language",
            AppSettings.VOICE_LANGUAGES.map { it.second },
            AppSettings.VOICE_LANGUAGES.indexOfFirst { it.first == s.voiceLanguage },
            { s.voiceLanguage = AppSettings.VOICE_LANGUAGES[it].first; dialog = null },
            { dialog = null },
        )
        "categories" -> ManageCategoriesDialog(
            categories = s.categories,
            onAdd = { s.categories = s.categories + it },
            onRemove = { name -> scope.launch { app.repository.removeCategory(name) } },
            onDismiss = { dialog = null },
        )
        "trash" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Empty Trash?") },
            text = { Text("All notes in Trash will be deleted for good.") },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    scope.launch {
                        app.repository.emptyTrash()
                        Toast.makeText(context, "Trash emptied", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Empty Trash", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
