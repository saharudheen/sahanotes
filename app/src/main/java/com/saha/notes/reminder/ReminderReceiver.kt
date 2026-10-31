package com.saha.notes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.saha.notes.NotesApp
import com.saha.notes.data.Repeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        if (noteId <= 0) return
        val app = context.applicationContext as NotesApp
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = app.database.noteDao()
                when (intent.action) {
                    ReminderScheduler.ACTION_SNOOZE -> {
                        NotificationManagerCompat.from(context).cancel(noteId.toInt())
                        ReminderScheduler.schedule(
                            context, noteId,
                            System.currentTimeMillis() + ReminderScheduler.SNOOZE_MILLIS,
                            snoozed = true
                        )
                    }

                    ReminderScheduler.ACTION_SNOOZED_FIRE -> {
                        dao.get(noteId)?.takeIf { !it.inTrash }?.let { Notifications.showReminder(context, it) }
                    }

                    ReminderScheduler.ACTION_FIRE -> {
                        val note = dao.get(noteId) ?: return@launch
                        if (note.inTrash) return@launch
                        Notifications.showReminder(context, note)
                        val next = ReminderScheduler.nextOccurrence(
                            note.reminderAt ?: System.currentTimeMillis(), note.repeat
                        )
                        if (next != null) {
                            dao.update(note.copy(reminderAt = next))
                            ReminderScheduler.schedule(context, noteId, next)
                        } else {
                            dao.update(note.copy(reminderAt = null, repeat = Repeat.NONE))
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
