package com.saha.notes.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saha.notes.NotesApp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A dialog with a list of radio-button options. */
@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEachIndexed { i, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(i) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = i == selected, onClick = { onSelect(i) })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Pick a category for a note ("" means none). */
@Composable
fun CategoryPickerDialog(
    categories: List<String>,
    current: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf("No category") + categories
    val selected = if (current.isNullOrBlank()) 0 else categories.indexOf(current) + 1
    ChoiceDialog(
        title = "Move to category",
        options = options,
        selected = if (current == null) -1 else selected,
        onSelect = { i -> onPick(if (i == 0) "" else categories[i - 1]) },
        onDismiss = onDismiss,
    )
}

@Composable
fun ManageCategoriesDialog(
    categories: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
    startWithAdd: Boolean = false,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (startWithAdd) "New category" else "Categories") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!startWithAdd) {
                    Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                        if (categories.isEmpty()) {
                            Text("No categories yet.", style = MaterialTheme.typography.bodyMedium)
                        }
                        categories.forEach { name ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = { onRemove(name) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove $name")
                                }
                            }
                        }
                    }
                    Text(
                        "Removing a category keeps its notes; they just lose the label.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.replace("\n", "") },
                    label = { Text("New category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) onAdd(newName.trim())
                    newName = ""
                    if (startWithAdd) onDismiss()
                },
                enabled = newName.isNotBlank(),
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (startWithAdd) "Cancel" else "Done") } },
    )
}

/** Export / import of all notes as a JSON file the user picks. */
class BackupActions(val exportNotes: () -> Unit, val importNotes: () -> Unit)

@Composable
fun rememberBackupActions(): BackupActions {
    val context = LocalContext.current
    val app = context.applicationContext as NotesApp
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching { app.repository.exportTo(uri) }
                .onSuccess { Toast.makeText(context, "Backed up $it notes", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { app.repository.importFrom(uri) }
                .onSuccess {
                    val msg = if (it == 0) "No new notes in that backup" else "Imported $it notes"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(context, "That isn't a Saha Notes backup file", Toast.LENGTH_LONG).show() }
        }
    }
    return remember(exportLauncher, importLauncher) {
        BackupActions(
            exportNotes = {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                exportLauncher.launch("SahaNotes-backup-$date.json")
            },
            importNotes = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
    }
}
