package com.saha.notes

import android.app.Application
import androidx.room.Room
import com.saha.notes.data.NoteDatabase
import com.saha.notes.data.NoteRepository
import com.saha.notes.reminder.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NotesApp : Application() {

    /** Outlives screens, so a note still saves after its editor closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: NoteDatabase by lazy {
        Room.databaseBuilder(this, NoteDatabase::class.java, "notes.db").build()
    }

    val repository: NoteRepository by lazy { NoteRepository(this, database.noteDao()) }

    /** Text shared from another app, picked up by the next new note. */
    var sharedText: String? = null

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
    }
}
