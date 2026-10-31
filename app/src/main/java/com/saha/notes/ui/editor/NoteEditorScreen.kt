package com.saha.notes.ui.editor

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saha.notes.reminder.ReminderScheduler
import com.saha.notes.ui.components.CategoryPickerDialog
import com.saha.notes.ui.components.ColorPalette
import com.saha.notes.ui.components.Fmt
import com.saha.notes.ui.components.ReminderChip
import com.saha.notes.ui.components.shareNote
import com.saha.notes.ui.theme.noteBackground

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(onBack: () -> Unit, vm: EditorViewModel = viewModel()) {
    val context = LocalContext.current
    val background by animateColorAsState(noteBackground(vm.colorIndex), label = "noteBackground")
    var showPalette by rememberSaveable { mutableStateOf(false) }
    var showReminder by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showCategory by rememberSaveable { mutableStateOf(false) }
    var showExactAlarmInfo by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    // Save when the user switches apps or the screen turns off.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.saveInBackground() }

    // ---------- Voice typing (Google speech recognizer; supports English and Tamil) ----------
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { vm.insertText(it) }
        }
    }
    val startVoice: () -> Unit = {
        val tamil = vm.voiceLanguage.startsWith("ta")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, vm.voiceLanguage)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (tamil) "பேசுங்கள்…" else "Speak now…")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Voice typing isn't available. Install or update the Google app.", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(vm.loaded) {
        if (vm.loaded && vm.startWithVoice && !vm.voiceLaunched) {
            vm.voiceLaunched = true
            startVoice()
        }
    }

    // ---------- Reminders ----------
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notifications are off, so reminders won't show.", Toast.LENGTH_LONG).show()
        }
        showReminder = true
    }
    val openReminder: () -> Unit = {
        if (vm.isBlank) {
            Toast.makeText(context, "Write something first", Toast.LENGTH_SHORT).show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showReminder = true
        }
    }

    Scaffold(
        containerColor = background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (vm.inTrash) {
                        IconButton(onClick = {
                            vm.restoreFromTrash()
                            Toast.makeText(context, "Note restored", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "Restore") }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete for good")
                        }
                    } else {
                        IconButton(onClick = { vm.pinned = !vm.pinned }) {
                            Icon(
                                if (vm.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (vm.pinned) "Unpin" else "Pin",
                            )
                        }
                        IconButton(onClick = { vm.favorite = !vm.favorite }) {
                            Icon(
                                if (vm.favorite) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (vm.favorite) "Remove from Favorites" else "Add to Favorites",
                                tint = if (vm.favorite) Color(0xFFF2A900) else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = openReminder) {
                            Icon(
                                if (vm.reminderAt != null) Icons.Rounded.NotificationsActive else Icons.Rounded.AddAlert,
                                contentDescription = "Reminder",
                            )
                        }
                        IconButton(onClick = { showPalette = !showPalette }) {
                            Icon(Icons.Rounded.Palette, contentDescription = "Colour")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Category") },
                                    leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                                    onClick = { menuOpen = false; showCategory = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(if (vm.archived) "Unarchive" else "Archive") },
                                    leadingIcon = { Icon(if (vm.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, null) },
                                    onClick = {
                                        menuOpen = false
                                        if (vm.isBlank) {
                                            Toast.makeText(context, "Write something first", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val msg = if (vm.archived) "Moved out of Archive" else "Archived"
                                            vm.toggleArchive(onDone = onBack)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                    onClick = {
                                        menuOpen = false
                                        shareNote(context, vm.title, vm.content.text)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Move to Trash") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = {
                                        menuOpen = false
                                        val hadText = !vm.isBlank
                                        vm.moveToTrash(onDone = onBack)
                                        if (hadText) Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!vm.inTrash) EditorBottomBar(
                background = background,
                info = "Edited ${Fmt.short(vm.updatedAt)} · ${vm.wordCount} words",
                tamil = vm.voiceLanguage.startsWith("ta"),
                onToggleLanguage = vm::toggleVoiceLanguage,
                onMic = startVoice,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (vm.inTrash) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "This note is in Trash. Restore it to edit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.restoreFromTrash() }) { Text("Restore") }
                }
            }
            AnimatedVisibility(visible = showPalette && !vm.inTrash) {
                ColorPalette(selected = vm.colorIndex, onSelect = { vm.colorIndex = it })
            }

            val textColor = MaterialTheme.colorScheme.onSurface
            val hintColor = textColor.copy(alpha = 0.4f)

            BasicTextField(
                value = vm.title,
                onValueChange = { vm.title = it },
                readOnly = vm.inTrash,
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = textColor, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (vm.title.isEmpty()) {
                            Text("Title", style = MaterialTheme.typography.headlineSmall, color = hintColor)
                        }
                        inner()
                    }
                },
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable(enabled = !vm.inTrash) { showCategory = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        vm.category.ifBlank { "No category" },
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                vm.reminderAt?.let { at ->
                    ReminderChip(at = at, repeat = vm.repeat, onClick = if (vm.inTrash) null else openReminder)
                }
            }

            BasicTextField(
                value = vm.content,
                onValueChange = { vm.content = it },
                readOnly = vm.inTrash,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp)
                    .padding(bottom = 24.dp),
                decorationBox = { inner ->
                    Box {
                        if (vm.content.text.isEmpty()) {
                            Text(
                                "Start typing, or tap the mic to speak",
                                style = MaterialTheme.typography.bodyLarge,
                                color = hintColor,
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }

    if (showReminder) {
        ReminderDialog(
            initialAt = vm.reminderAt,
            initialRepeat = vm.repeat,
            onDismiss = { showReminder = false },
            onSet = { at, repeat ->
                showReminder = false
                vm.setReminder(at, repeat)
                Toast.makeText(context, "Reminder set for ${Fmt.reminder(at, repeat)}", Toast.LENGTH_SHORT).show()
                if (!ReminderScheduler.canScheduleExact(context)) showExactAlarmInfo = true
            },
            onRemove = if (vm.reminderAt != null) {
                { showReminder = false; vm.clearReminder() }
            } else null,
        )
    }

    if (showExactAlarmInfo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AlertDialog(
            onDismissRequest = { showExactAlarmInfo = false },
            title = { Text("Allow on-time reminders") },
            text = {
                Text("Your reminder is set, but Android may delay it by a few minutes. Turn on \"Alarms & reminders\" for Saha Notes so it rings exactly on time.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmInfo = false
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmInfo = false }) { Text("Not now") }
            },
        )
    }

    if (showCategory) {
        CategoryPickerDialog(
            categories = vm.categories,
            current = vm.category,
            onPick = { vm.category = it; showCategory = false },
            onDismiss = { showCategory = false },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete for good?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.deleteForever(onDone = onBack)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditorBottomBar(
    background: Color,
    info: String,
    tamil: Boolean,
    onToggleLanguage: () -> Unit,
    onMic: () -> Unit,
) {
    Surface(color = background) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = onToggleLanguage,
                label = { Text(if (tamil) "தமிழ்" else "English") },
                leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
            Text(
                info,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
            )
            FilledIconButton(onClick = onMic, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Mic, contentDescription = "Voice typing")
            }
        }
    }
}
