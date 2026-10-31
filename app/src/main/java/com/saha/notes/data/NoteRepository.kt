package com.saha.notes.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.saha.notes.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/** Single place that writes notes, so reminders always stay in sync with the database. */
class NoteRepository(private val context: Context, private val dao: NoteDao) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun observe(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    suspend fun get(id: Long): Note? = dao.get(id)

    /** Inserts or updates and returns the note id. */
    suspend fun save(note: Note): Long {
        val id = if (note.id == 0L) dao.insert(note) else {
            dao.update(note); note.id
        }
        syncReminder(id, note.reminderAt)
        return id
    }

    /** Puts a deleted note back with its original id (used by "Undo"). */
    suspend fun restore(note: Note) {
        dao.insert(note)
        syncReminder(note.id, note.reminderAt)
    }

    suspend fun delete(note: Note) {
        ReminderScheduler.cancel(context, note.id)
        NotificationManagerCompat.from(context).cancel(note.id.toInt())
        dao.delete(note)
    }

    private fun syncReminder(id: Long, at: Long?) {
        if (at != null && at > System.currentTimeMillis()) {
            ReminderScheduler.schedule(context, id, at)
        } else {
            ReminderScheduler.cancel(context, id)
        }
    }

    var gridView: Boolean
        get() = prefs.getBoolean("grid_view", true)
        set(value) = prefs.edit().putBoolean("grid_view", value).apply()

    /** BCP-47 tag for voice typing, e.g. "en-IN" or "ta-IN". */
    var voiceLanguage: String
        get() = prefs.getString("voice_language", "en-IN") ?: "en-IN"
        set(value) = prefs.edit().putString("voice_language", value).apply()
}
