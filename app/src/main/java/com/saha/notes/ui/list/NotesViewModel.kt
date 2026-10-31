package com.saha.notes.ui.list

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saha.notes.NotesApp
import com.saha.notes.data.AppSettings
import com.saha.notes.data.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Section(val title: String) {
    ALL("Saha Notes"),
    REMINDERS("Reminders"),
    FAVORITES("Favorites"),
    ARCHIVE("Archive"),
    TRASH("Trash"),
}

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val notesApp = app as NotesApp
    private val repo = notesApp.repository
    val settings = notesApp.settings

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _section = MutableStateFlow(Section.ALL)
    val section: StateFlow<Section> = _section.asStateFlow()

    /** Selected category chip, or null for "All". */
    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    private val allNotes: StateFlow<List<Note>?> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until the first load finishes, so the empty state doesn't flash. */
    val notes: StateFlow<List<Note>?> = combine(
        allNotes, _query, _section, _category, snapshotFlow { settings.sort },
    ) { all, q, s, c, sort ->
        all?.let { filterAndSort(it, q, s, c, sort) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val counts: StateFlow<Map<Section, Int>> = allNotes.map { list ->
        val live = list.orEmpty().filter { !it.inTrash }
        mapOf(
            Section.ALL to live.count { !it.archived },
            Section.REMINDERS to live.count { it.reminderAt != null },
            Section.FAVORITES to live.count { it.favorite && !it.archived },
            Section.ARCHIVE to live.count { it.archived },
            Section.TRASH to list.orEmpty().count { it.inTrash },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ---------- Navigation within the list ----------

    fun setQuery(value: String) {
        _query.value = value
    }

    fun openSection(section: Section) {
        _section.value = section
        _category.value = null
        clearSelection()
    }

    fun openCategory(name: String?) {
        _section.value = Section.ALL
        _category.value = name
        clearSelection()
    }

    fun toggleView() {
        settings.gridView = !settings.gridView
    }

    // ---------- Selection ----------

    var selecting by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf(emptySet<Long>())
        private set

    fun startSelection(firstId: Long? = null) {
        selecting = true
        selectedIds = setOfNotNull(firstId)
    }

    fun toggleSelected(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun selectAll() {
        selectedIds = notes.value.orEmpty().map { it.id }.toSet()
    }

    fun clearSelection() {
        selecting = false
        selectedIds = emptySet()
    }

    private fun selectedNotes(): List<Note> = notes.value.orEmpty().filter { it.id in selectedIds }

    val allSelectedPinned: Boolean get() = selectedNotes().let { it.isNotEmpty() && it.all { n -> n.pinned } }
    val allSelectedFavorite: Boolean get() = selectedNotes().let { it.isNotEmpty() && it.all { n -> n.favorite } }

    fun pinSelected() = updateSelected { notes ->
        val pin = !notes.all { it.pinned }
        notes.map { it.copy(pinned = pin) }
    }

    fun favoriteSelected() = updateSelected { notes ->
        val fav = !notes.all { it.favorite }
        notes.map { it.copy(favorite = fav) }
    }

    fun archiveSelected() = updateSelected { notes ->
        val archive = _section.value != Section.ARCHIVE
        notes.map { it.copy(archived = archive, pinned = if (archive) false else it.pinned) }
    }

    fun moveSelectedTo(category: String) = updateSelected { notes -> notes.map { it.copy(category = category) } }

    /** Returns the trashed notes so the screen can offer Undo. */
    fun trashSelected(): List<Note> {
        val notes = selectedNotes()
        clearSelection()
        viewModelScope.launch { repo.moveToTrash(notes) }
        return notes
    }

    fun restoreSelected() {
        val notes = selectedNotes()
        clearSelection()
        viewModelScope.launch { repo.restoreFromTrash(notes) }
    }

    fun deleteSelectedForever() {
        val notes = selectedNotes()
        clearSelection()
        viewModelScope.launch { repo.deleteForever(notes) }
    }

    private fun updateSelected(change: (List<Note>) -> List<Note>) {
        val notes = selectedNotes()
        if (notes.isEmpty()) return
        clearSelection()
        viewModelScope.launch { repo.updateAll(change(notes)) }
    }

    // ---------- Other actions ----------

    fun undoTrash(notes: List<Note>) {
        viewModelScope.launch { repo.restoreFromTrash(notes) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repo.emptyTrash() }
    }

    fun addCategory(name: String) {
        settings.categories = settings.categories + name
    }

    fun removeCategory(name: String) {
        if (_category.value == name) _category.value = null
        viewModelScope.launch { repo.removeCategory(name) }
    }
}

private fun filterAndSort(all: List<Note>, query: String, section: Section, category: String?, sort: Int): List<Note> {
    val q = query.trim()
    val filtered = all.filter { n ->
        val inSection = when (section) {
            Section.ALL -> !n.inTrash && !n.archived && (category == null || n.category == category)
            Section.REMINDERS -> !n.inTrash && n.reminderAt != null
            Section.FAVORITES -> !n.inTrash && !n.archived && n.favorite
            Section.ARCHIVE -> !n.inTrash && n.archived
            Section.TRASH -> n.inTrash
        }
        inSection && (q.isEmpty() || n.title.contains(q, ignoreCase = true) || n.content.contains(q, ignoreCase = true))
    }
    return when (section) {
        Section.TRASH -> filtered.sortedByDescending { it.deletedAt }
        Section.REMINDERS -> filtered.sortedBy { it.reminderAt }
        else -> {
            val bySort = when (sort) {
                AppSettings.SORT_CREATED -> compareByDescending<Note> { it.createdAt }
                AppSettings.SORT_TITLE -> compareBy<Note> { it.title.ifBlank { it.content }.lowercase() }
                else -> compareByDescending<Note> { it.updatedAt }
            }
            filtered.sortedWith(compareByDescending<Note> { it.pinned }.then(bySort))
        }
    }
}
