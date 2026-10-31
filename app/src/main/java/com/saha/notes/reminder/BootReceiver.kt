package com.saha.notes.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saha.notes.NotesApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are wiped on reboot or app update, so put every reminder back. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as NotesApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                app.database.noteDao().withReminders().forEach { note ->
                    val at = note.reminderAt ?: return@forEach
                    // A reminder missed while the phone was off fires a few seconds from now.
                    ReminderScheduler.schedule(context, note.id, maxOf(at, now + 5_000))
                }
            } finally {
                pending.finish()
            }
        }
    }
}
