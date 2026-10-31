package com.saha.notes.ui.list

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saha.notes.data.AppSettings
import com.saha.notes.data.Note
import com.saha.notes.data.NoteRepository
import com.saha.notes.ui.components.CategoryPickerDialog
import com.saha.notes.ui.components.ChoiceDialog
import com.saha.notes.ui.components.ManageCategoriesDialog
import com.saha.notes.ui.components.NoteCard
import com.saha.notes.ui.components.rememberBackupActions
import kotlinx.coroutines.launch

@Composable
fun NoteListScreen(
    onOpenNote: (Long) -> Unit,
    onNewNote: (voice: Boolean, category: String?) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: NotesViewModel = viewModel(),
) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val section by vm.section.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    val settings = vm.settings

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val backup = rememberBackupActions()

    var searching by rememberSaveable { mutableStateOf(false) }
    var showSort by rememberSaveable { mutableStateOf(false) }
    var showManageCategories by rememberSaveable { mutableStateOf(false) }
    var showAddCategory by rememberSaveable { mutableStateOf(false) }
    var showMoveTo by rememberSaveable { mutableStateOf(false) }
    var showEmptyTrash by rememberSaveable { mutableStateOf(false) }
    var showDeleteForever by rememberSaveable { mutableStateOf(false) }

    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }
    val stopSearch: () -> Unit = {
        searching = false
        vm.setQuery("")
    }

    BackHandler(enabled = vm.selecting) { vm.clearSelection() }
    BackHandler(enabled = searching && !vm.selecting) { stopSearch() }
    BackHandler(enabled = section != Section.ALL && !searching && !vm.selecting) { vm.openSection(Section.ALL) }

    val trashWithUndo: () -> Unit = {
        val trashed = vm.trashSelected()
        if (trashed.isNotEmpty()) scope.launch {
            val label = if (trashed.size == 1) "Note moved to Trash" else "${trashed.size} notes moved to Trash"
            val result = snackbar.showSnackbar(label, "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) vm.undoTrash(trashed)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !vm.selecting,
        drawerContent = {
            AppDrawer(
                section = section,
                category = category,
                categories = settings.categories,
                counts = counts,
                onSection = { closeDrawer(); stopSearch(); vm.openSection(it) },
                onCategory = { closeDrawer(); stopSearch(); vm.openCategory(it) },
                onCalendar = { closeDrawer(); onOpenCalendar() },
                onManageCategories = { closeDrawer(); showManageCategories = true },
                onBackup = { closeDrawer(); onOpenSettings() },
                onSettings = { closeDrawer(); onOpenSettings() },
            )
        },
    ) {
        Scaffold(
            topBar = {
                when {
                    vm.selecting -> SelectionTopBar(
                        count = vm.selectedIds.size,
                        section = section,
                        allPinned = vm.allSelectedPinned,
                        allFavorite = vm.allSelectedFavorite,
                        onClose = vm::clearSelection,
                        onSelectAll = vm::selectAll,
                        onPin = vm::pinSelected,
                        onFavorite = vm::favoriteSelected,
                        onArchive = vm::archiveSelected,
                        onMove = { showMoveTo = true },
                        onTrash = trashWithUndo,
                        onRestore = vm::restoreSelected,
                        onDeleteForever = { showDeleteForever = true },
                    )

                    searching -> SearchTopBar(query = query, onQueryChange = vm::setQuery, onClose = stopSearch)

                    else -> MainTopBar(
                        title = if (section == Section.ALL) Section.ALL.title else section.title,
                        section = section,
                        gridView = settings.gridView,
                        onMenu = { scope.launch { drawerState.open() } },
                        onSearch = { searching = true },
                        onCalendar = onOpenCalendar,
                        onToggleView = vm::toggleView,
                        onSelect = { vm.startSelection() },
                        onSort = { showSort = true },
                        onBackup = backup.exportNotes,
                        onImport = backup.importNotes,
                        onSettings = onOpenSettings,
                        onEmptyTrash = { showEmptyTrash = true },
                    )
                }
            },
            floatingActionButton = {
                if (!vm.selecting && section != Section.TRASH && section != Section.ARCHIVE) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SmallFloatingActionButton(
                            onClick = { onNewNote(true, category) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) { Icon(Icons.Rounded.Mic, contentDescription = "New voice note") }
                        FloatingActionButton(
                            onClick = { onNewNote(false, category) },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) { Icon(Icons.Rounded.Add, contentDescription = "New note", Modifier.size(30.dp)) }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                if (section == Section.ALL && !searching) {
                    CategoryChips(
                        categories = settings.categories,
                        selected = category,
                        onSelect = vm::openCategory,
                        onAdd = { showAddCategory = true },
                    )
                }
                if (section == Section.TRASH) {
                    Text(
                        "Notes in Trash are deleted for good after ${NoteRepository.TRASH_DAYS} days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                val list = notes
                when {
                    list == null -> Unit
                    list.isEmpty() -> EmptyState(section = section, searching = query.isNotBlank(), categoryName = category)
                    else -> NotesGrid(
                        notes = list,
                        grid = settings.gridView,
                        showSections = section == Section.ALL || section == Section.FAVORITES,
                        selecting = vm.selecting,
                        selectedIds = vm.selectedIds,
                        bottomPadding = padding.calculateBottomPadding(),
                        onClick = { note ->
                            if (vm.selecting) vm.toggleSelected(note.id) else onOpenNote(note.id)
                        },
                        onLongClick = { note ->
                            if (vm.selecting) vm.toggleSelected(note.id) else vm.startSelection(note.id)
                        },
                    )
                }
            }
        }
    }

    if (showSort) {
        ChoiceDialog(
            title = "Sort by",
            options = AppSettings.SORT_LABELS,
            selected = settings.sort,
            onSelect = { settings.sort = it; showSort = false },
            onDismiss = { showSort = false },
        )
    }
    if (showManageCategories || showAddCategory) {
        ManageCategoriesDialog(
            categories = settings.categories,
            onAdd = vm::addCategory,
            onRemove = vm::removeCategory,
            onDismiss = { showManageCategories = false; showAddCategory = false },
            startWithAdd = showAddCategory,
        )
    }
    if (showMoveTo) {
        CategoryPickerDialog(
            categories = settings.categories,
            current = null,
            onPick = { vm.moveSelectedTo(it); showMoveTo = false },
            onDismiss = { showMoveTo = false },
        )
    }
    if (showEmptyTrash) {
        AlertDialog(
            onDismissRequest = { showEmptyTrash = false },
            title = { Text("Empty Trash?") },
            text = { Text("All notes in Trash will be deleted for good.") },
            confirmButton = {
                TextButton(onClick = { vm.emptyTrash(); showEmptyTrash = false }) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showEmptyTrash = false }) { Text("Cancel") } },
        )
    }
    if (showDeleteForever) {
        AlertDialog(
            onDismissRequest = { showDeleteForever = false },
            title = { Text("Delete for good?") },
            text = { Text("${vm.selectedIds.size} note(s) will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteSelectedForever(); showDeleteForever = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteForever = false }) { Text("Cancel") } },
        )
    }
}

// ---------------------------------------------------------------- Top bars

@Composable
private fun MainTopBar(
    title: String,
    section: Section,
    gridView: Boolean,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
    onToggleView: () -> Unit,
    onSelect: () -> Unit,
    onSort: () -> Unit,
    onBackup: () -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit,
    onEmptyTrash: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) { Icon(Icons.Rounded.Menu, contentDescription = "Menu") }
            Text(
                title,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (section == Section.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
            if (section == Section.TRASH) {
                IconButton(onClick = onEmptyTrash) { Icon(Icons.Rounded.DeleteSweep, contentDescription = "Empty Trash") }
            } else {
                IconButton(onClick = onCalendar) { Icon(Icons.Rounded.CalendarMonth, contentDescription = "Calendar") }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More options") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MenuItem(if (gridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView, if (gridView) "List View" else "Grid View") {
                        menuOpen = false; onToggleView()
                    }
                    MenuItem(Icons.Rounded.CheckBox, "Select") { menuOpen = false; onSelect() }
                    MenuItem(Icons.Rounded.SwapVert, "Sort") { menuOpen = false; onSort() }
                    MenuItem(Icons.Rounded.Backup, "Backup") { menuOpen = false; onBackup() }
                    MenuItem(Icons.Rounded.FileDownload, "Import") { menuOpen = false; onImport() }
                    MenuItem(Icons.Rounded.Settings, "Settings") { menuOpen = false; onSettings() }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close search") }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).padding(end = 12.dp).focusRequester(focus),
                placeholder = { Text("Search notes") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear") }
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
        }
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    section: Section,
    allPinned: Boolean,
    allFavorite: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    onMove: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val enabled = count > 0
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Cancel selection") }
            Text(
                if (count == 0) "Select notes" else "$count selected",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (section == Section.TRASH) {
                IconButton(onClick = onSelectAll) { Icon(Icons.Rounded.SelectAll, contentDescription = "Select all") }
                IconButton(onClick = onRestore, enabled = enabled) { Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "Restore") }
                IconButton(onClick = onDeleteForever, enabled = enabled) { Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete for good") }
            } else {
                IconButton(onClick = onPin, enabled = enabled) {
                    Icon(if (allPinned) Icons.Outlined.PushPin else Icons.Rounded.PushPin, contentDescription = if (allPinned) "Unpin" else "Pin")
                }
                IconButton(onClick = onArchive, enabled = enabled) {
                    Icon(
                        if (section == Section.ARCHIVE) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                        contentDescription = if (section == Section.ARCHIVE) "Unarchive" else "Archive",
                    )
                }
                IconButton(onClick = onTrash, enabled = enabled) { Icon(Icons.Rounded.Delete, contentDescription = "Move to Trash") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MenuItem(Icons.Rounded.SelectAll, "Select all") { menuOpen = false; onSelectAll() }
                        if (enabled) {
                            MenuItem(
                                if (allFavorite) Icons.Outlined.StarBorder else Icons.Rounded.Star,
                                if (allFavorite) "Remove from Favorites" else "Add to Favorites",
                            ) { menuOpen = false; onFavorite() }
                            MenuItem(Icons.Rounded.Folder, "Move to category") { menuOpen = false; onMove() }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Content

@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onAdd: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { CategoryChip("All", selected == null) { onSelect(null) } }
        items(categories, key = { it }) { name ->
            CategoryChip(name, selected == name) { onSelect(name) }
        }
        item {
            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    Icons.Rounded.Add, contentDescription = "Add category",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NotesGrid(
    notes: List<Note>,
    grid: Boolean,
    showSections: Boolean,
    selecting: Boolean,
    selectedIds: Set<Long>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onClick: (Note) -> Unit,
    onLongClick: (Note) -> Unit,
) {
    val pinned = if (showSections) notes.filter { it.pinned } else emptyList()
    val others = if (showSections) notes.filterNot { it.pinned } else notes
    val headers = pinned.isNotEmpty() && others.isNotEmpty()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(if (grid) 2 else 1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = bottomPadding + 150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
    ) {
        noteSection("Pinned", pinned, pinned.isNotEmpty(), grid, selecting, selectedIds, onClick, onLongClick)
        noteSection("Others", others, headers, grid, selecting, selectedIds, onClick, onLongClick)
    }
}

private fun LazyStaggeredGridScope.noteSection(
    label: String,
    notes: List<Note>,
    showHeader: Boolean,
    grid: Boolean,
    selecting: Boolean,
    selectedIds: Set<Long>,
    onClick: (Note) -> Unit,
    onLongClick: (Note) -> Unit,
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
            selected = selecting && note.id in selectedIds,
            onClick = { onClick(note) },
            onLongClick = { onLongClick(note) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun EmptyState(section: Section, searching: Boolean, categoryName: String?) {
    val (icon, title, subtitle) = when {
        searching -> Triple(Icons.Rounded.Search, "No matching notes", "Try a different word.")
        section == Section.REMINDERS -> Triple(Icons.Rounded.Alarm, "No reminders", "Open a note and tap the bell to set one.")
        section == Section.FAVORITES -> Triple(Icons.Rounded.Star, "No favorites", "Tap the star in a note to add it here.")
        section == Section.ARCHIVE -> Triple(Icons.Rounded.Archive, "Archive is empty", "Archived notes are kept here, out of the way.")
        section == Section.TRASH -> Triple(Icons.Rounded.Delete, "Trash is empty", "Deleted notes stay here for ${NoteRepository.TRASH_DAYS} days.")
        categoryName != null -> Triple(Icons.Rounded.Folder, "Nothing in $categoryName", "Tap + to add a note to this category.")
        else -> Triple(Icons.Rounded.EditNote, "No notes yet", "Tap + to write a note, or the mic to speak one.")
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(120.dp))
    }
}
