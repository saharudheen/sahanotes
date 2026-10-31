package com.saha.notes.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saha.notes.data.Note
import com.saha.notes.ui.components.Fmt
import com.saha.notes.ui.components.ReminderChip
import com.saha.notes.ui.components.shareNote
import com.saha.notes.ui.theme.noteBackground
import kotlinx.coroutines.launch

@Composable
fun NoteListScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: (voice: Boolean) -> Unit,
    vm: NotesViewModel = viewModel(),
) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            ListTopBar(
                query = query,
                onQueryChange = vm::setQuery,
                gridView = vm.gridView,
                onToggleView = vm::toggleView,
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = { onNewNote(true) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) { Icon(Icons.Rounded.Mic, contentDescription = "New voice note") }
                FloatingActionButton(onClick = { onNewNote(false) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "New note")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = notes ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(searching = query.isNotBlank(), modifier = Modifier.padding(padding))
            return@Scaffold
        }
        val pinned = list.filter { it.pinned }
        val others = list.filterNot { it.pinned }
        val showHeaders = pinned.isNotEmpty() && others.isNotEmpty()

        val onDelete: (Note) -> Unit = { note ->
            vm.delete(note)
            scope.launch {
                val result = snackbar.showSnackbar("Note deleted", "Undo", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) vm.restore(note)
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (vm.gridView) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp, end = 12.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 150.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
        ) {
            noteSection("Pinned", pinned, showHeaders || pinned.isNotEmpty(), vm.gridView, onOpenNote, vm::togglePin, onDelete) {
                shareNote(context, it.title, it.content)
            }
            noteSection("Others", others, showHeaders, vm.gridView, onOpenNote, vm::togglePin, onDelete) {
                shareNote(context, it.title, it.content)
            }
        }
    }
}

private fun LazyStaggeredGridScope.noteSection(
    label: String,
    notes: List<Note>,
    showHeader: Boolean,
    grid: Boolean,
    onOpen: (Long) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onShare: (Note) -> Unit,
) {
    if (notes.isEmpty()) return
    if (showHeader) {
        item(key = "header_$label", span = StaggeredGridItemSpan.FullLine) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 6.dp, top = 8.dp),
            )
        }
    }
    items(notes, key = { it.id }) { note ->
        NoteCard(
            note = note,
            grid = grid,
            onClick = { onOpen(note.id) },
            onTogglePin = { onTogglePin(note) },
            onShare = { onShare(note) },
            onDelete = { onDelete(note) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun ListTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    gridView: Boolean,
    onToggleView: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "Notes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search notes") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
                IconButton(onClick = onToggleView) {
                    Icon(
                        if (gridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                        contentDescription = if (gridView) "List view" else "Grid view",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    grid: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Box(modifier) {
        Surface(
            shape = shape,
            color = noteBackground(note.colorIndex),
            border = if (note.colorIndex == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true }),
        ) {
            Column(Modifier.padding(14.dp)) {
                if (note.title.isNotBlank() || note.pinned) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (note.pinned) {
                            Icon(
                                Icons.Rounded.PushPin, contentDescription = "Pinned",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
                if (note.content.isNotBlank()) {
                    if (note.title.isNotBlank()) Spacer(Modifier.height(6.dp))
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (grid) 10 else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    )
                }
                note.reminderAt?.let {
                    Spacer(Modifier.height(10.dp))
                    ReminderChip(at = it, repeat = note.repeat)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    Fmt.short(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(if (note.pinned) "Unpin" else "Pin") },
                leadingIcon = { Icon(if (note.pinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin, null) },
                onClick = { menuOpen = false; onTogglePin() },
            )
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = { menuOpen = false; onShare() },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (searching) Icons.Rounded.Search else Icons.Rounded.EditNote,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (searching) "No matching notes" else "No notes yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (searching) "Try a different word." else "Tap + to write a note, or the mic to speak one.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
