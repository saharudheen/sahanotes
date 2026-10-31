package com.saha.notes.data

import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.saha.notes.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Single place that writes notes, so reminders always stay in sync with the database. */
class NoteRepository(
    private val context: Context,
    private val dao: NoteDao,
    private val settings: AppSettings,
) {
    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    suspend fun get(id: Long): Note? = dao.get(id)

    /** Inserts or updates and returns the note id. */
    suspend fun save(note: Note): Long {
        val id = if (note.id == 0L) dao.insert(note) else {
            dao.update(note); note.id
        }
        syncReminder(note.copy(id = id))
        return id
    }

    suspend fun updateAll(notes: List<Note>) {
        if (notes.isEmpty()) return
        dao.updateAll(notes)
        notes.forEach { syncReminder(it) }
    }

    suspend fun moveToTrash(notes: List<Note>) {
        val now = System.currentTimeMillis()
        notes.forEach { cancelReminder(it.id) }
        dao.updateAll(notes.map { it.copy(deletedAt = now) })
    }

    suspend fun restoreFromTrash(notes: List<Note>) {
        val restored = notes.map { it.copy(deletedAt = null) }
        dao.updateAll(restored)
        restored.forEach { syncReminder(it) }
    }

    suspend fun deleteForever(notes: List<Note>) {
        notes.forEach { cancelReminder(it.id) }
        dao.deleteAll(notes)
    }

    suspend fun emptyTrash() = dao.emptyTrash()

    /** Notes stay in Trash for 30 days. */
    suspend fun purgeOldTrash() = dao.purgeTrash(System.currentTimeMillis() - TRASH_DAYS * 24 * 3_600_000L)

    suspend fun removeCategory(name: String) {
        dao.clearCategory(name)
        settings.categories = settings.categories - name
    }

    private fun syncReminder(note: Note) {
        val at = note.reminderAt
        if (!note.inTrash && at != null && at > System.currentTimeMillis()) {
            ReminderScheduler.schedule(context, note.id, at)
        } else {
            ReminderScheduler.cancel(context, note.id)
        }
    }

    private fun cancelReminder(id: Long) {
        ReminderScheduler.cancel(context, id)
        NotificationManagerCompat.from(context).cancel(id.toInt())
    }

    // ---------- Backup ----------

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val notes = dao.all().filter { !it.inTrash }
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject()
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("colorIndex", n.colorIndex)
                    .put("pinned", n.pinned)
                    .put("favorite", n.favorite)
                    .put("archived", n.archived)
                    .put("category", n.category)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
                    .put("reminderAt", n.reminderAt ?: JSONObject.NULL)
                    .put("repeat", n.repeat)
            )
        }
        val root = JSONObject()
            .put("app", "SahaNotes")
            .put("version", 1)
            .put("categories", JSONArray(settings.categories))
            .put("notes", array)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Can't open the file")
        stream.bufferedWriter().use { it.write(root.toString(2)) }
        notes.size
    }

    /** Adds notes from a backup file, skipping ones that are already here. Returns how many were added. */
    suspend fun importFrom(uri: Uri): Int {
        val text = withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openInputStream(uri) ?: error("Can't open the file")
            stream.bufferedReader().use { it.readText() }
        }
        val root = JSONObject(text)
        val array = root.getJSONArray("notes")
        val existing = dao.all().map { Triple(it.createdAt, it.title, it.content) }.toHashSet()
        var added = 0
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val note = Note(
                title = o.optString("title"),
                content = o.optString("content"),
                colorIndex = o.optInt("colorIndex", 0),
                pinned = o.optBoolean("pinned", false),
                favorite = o.optBoolean("favorite", false),
                archived = o.optBoolean("archived", false),
                category = o.optString("category", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                reminderAt = if (o.isNull("reminderAt")) null else o.optLong("reminderAt"),
                repeat = o.optInt("repeat", Repeat.NONE),
            )
            if (Triple(note.createdAt, note.title, note.content) in existing) continue
            save(note)
            added++
        }
        root.optJSONArray("categories")?.let { cats ->
            val names = (0 until cats.length()).map { cats.optString(it) }
            settings.categories = settings.categories + names
        }
        return added
    }

    companion object {
        const val TRASH_DAYS = 30
    }
}
