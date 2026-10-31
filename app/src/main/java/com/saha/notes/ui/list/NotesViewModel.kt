package com.saha.notes.ui.list

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saha.notes.NotesApp
import com.saha.notes.data.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as NotesApp).repository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Null until the first load finishes, so the empty state doesn't flash. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>?> = _query
        .flatMapLatest { repo.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var gridView by mutableStateOf(repo.gridView)
        private set

    fun setQuery(value: String) {
        _query.value = value
    }

    fun toggleView() {
        gridView = !gridView
        repo.gridView = gridView
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repo.save(note.copy(pinned = !note.pinned)) }
    }

    fun delete(note: Note) {
        viewModelScope.launch { repo.delete(note) }
    }

    fun restore(note: Note) {
        viewModelScope.launch { repo.restore(note) }
    }
}
