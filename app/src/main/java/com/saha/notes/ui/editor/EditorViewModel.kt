package com.saha.notes.ui.editor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.saha.notes.NotesApp
import com.saha.notes.data.Note
import com.saha.notes.data.Repeat
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds one note while it's being edited. Changes are saved automatically when
 * the app goes to the background and when the editor closes — there's no Save button.
 */
class EditorViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val notesApp = app as NotesApp
    private val repo = notesApp.repository
    private val settings = notesApp.settings
    private val saveLock = Mutex()

    val startWithVoice: Boolean = handle.get<Boolean>("voice") ?: false
    var voiceLaunched = false

    var noteId by mutableLongStateOf(handle.get<Long>("id") ?: 0L)
        private set
    var title by mutableStateOf("")
    var content by mutableStateOf(TextFieldValue(""))
    var colorIndex by mutableIntStateOf(0)
    var pinned by mutableStateOf(false)
    var favorite by mutableStateOf(false)
    var category by mutableStateOf(handle.get<String>("category").orEmpty())
    var archived by mutableStateOf(false)
        private set
    var inTrash by mutableStateOf(false)
        private set
    var reminderAt by mutableStateOf<Long?>(null)
        private set
    var repeat by mutableIntStateOf(Repeat.NONE)
        private set
    var updatedAt by mutableLongStateOf(System.currentTimeMillis())
        private set
    var loaded by mutableStateOf(false)
        private set
    val voiceLanguage: String get() = settings.voiceLanguage
    val categories: List<String> get() = settings.categories

    private var createdAt = System.currentTimeMillis()
    private var deletedAt: Long? = null
    private var saved: Note? = null
    private var deleted = false

    val isBlank: Boolean get() = title.isBlank() && content.text.isBlank()

    val wordCount: Int
        get() = content.text.split(Regex("\\s+")).count { it.isNotBlank() }

    init {
        viewModelScope.launch {
            val id = noteId
            if (id > 0) {
                repo.get(id)?.let { n ->
                    title = n.title
                    content = TextFieldValue(n.content)
                    colorIndex = n.colorIndex
                    pinned = n.pinned
                    favorite = n.favorite
                    category = n.category
                    archived = n.archived
                    deletedAt = n.deletedAt
                    inTrash = n.inTrash
                    reminderAt = n.reminderAt
                    repeat = n.repeat
                    createdAt = n.createdAt
                    updatedAt = n.updatedAt
                    saved = n
                }
            } else {
                notesApp.sharedText?.let {
                    content = TextFieldValue(it, TextRange(it.length))
                    notesApp.sharedText = null
                }
            }
            loaded = true
        }
    }

    /** Puts dictated text where the cursor is, adding a space if needed. */
    fun insertText(spoken: String) {
        val value = content
        val start = value.selection.min
        val end = value.selection.max
        val before = value.text.substring(0, start)
        val after = value.text.substring(end)
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        val inserted = lead + spoken.trim()
        content = TextFieldValue(before + inserted + after, TextRange(start + inserted.length))
    }

    fun toggleVoiceLanguage() {
        settings.voiceLanguage = if (voiceLanguage.startsWith("ta")) "en-IN" else "ta-IN"
    }

    fun setReminder(at: Long, repeatMode: Int) {
        reminderAt = at
        repeat = repeatMode
        val snap = snapshot()
        viewModelScope.launch { persist(snap) }
    }

    fun clearReminder() {
        reminderAt = null
        repeat = Repeat.NONE
        val snap = snapshot()
        viewModelScope.launch { persist(snap) }
    }

    fun saveInBackground() {
        val snap = snapshot()
        notesApp.appScope.launch { persist(snap) }
    }

    fun toggleArchive(onDone: () -> Unit) {
        archived = !archived
        if (archived) pinned = false
        val snap = snapshot()
        viewModelScope.launch {
            persist(snap)
            onDone()
        }
    }

    fun moveToTrash(onDone: () -> Unit) {
        val snap = snapshot()
        viewModelScope.launch {
            persist(snap)
            saveLock.withLock {
                saved?.let { repo.moveToTrash(listOf(it)) }
                deleted = true
            }
            onDone()
        }
    }

    fun restoreFromTrash() {
        viewModelScope.launch {
            saveLock.withLock {
                saved?.let {
                    repo.restoreFromTrash(listOf(it))
                    saved = it.copy(deletedAt = null)
                }
                deletedAt = null
                inTrash = false
            }
        }
    }

    fun deleteForever(onDone: () -> Unit) {
        viewModelScope.launch {
            saveLock.withLock {
                deleted = true
                saved?.let { repo.deleteForever(listOf(it)) }
            }
            onDone()
        }
    }

    override fun onCleared() {
        if (!deleted) {
            val snap = snapshot()
            notesApp.appScope.launch { persist(snap) }
        }
    }

    private fun snapshot() = Note(
        id = noteId,
        title = title,
        content = content.text,
        colorIndex = colorIndex,
        pinned = pinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reminderAt = reminderAt,
        repeat = repeat,
        favorite = favorite,
        archived = archived,
        deletedAt = deletedAt,
        category = category,
    )

    private suspend fun persist(snap: Note): Unit = saveLock.withLock {
        if (deleted || !loaded || inTrash) return@withLock
        val current = snap.copy(id = noteId)
        val previous = saved

        if (current.title.isBlank() && current.content.isBlank()) {
            // Empty notes are discarded, like most note apps.
            if (previous != null) {
                repo.deleteForever(listOf(previous))
                saved = null
                noteId = 0
            }
            return@withLock
        }
        if (previous != null && previous.sameContentAs(current)) return@withLock

        val textChanged = previous == null ||
            previous.title != current.title || previous.content != current.content
        val toSave = if (textChanged) current.copy(updatedAt = System.currentTimeMillis()) else current
        val id = repo.save(toSave)
        noteId = id
        updatedAt = toSave.updatedAt
        saved = toSave.copy(id = id)
    }
}
