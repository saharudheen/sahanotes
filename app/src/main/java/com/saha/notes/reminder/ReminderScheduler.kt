package com.saha.notes.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.saha.notes.data.Repeat
import java.util.Calendar

const val EXTRA_NOTE_ID = "note_id"

object ReminderScheduler {
    const val ACTION_FIRE = "com.saha.notes.REMINDER_FIRE"
    const val ACTION_SNOOZED_FIRE = "com.saha.notes.REMINDER_SNOOZED_FIRE"
    const val ACTION_SNOOZE = "com.saha.notes.REMINDER_SNOOZE"
    const val SNOOZE_MILLIS = 10 * 60 * 1000L

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun schedule(context: Context, noteId: Long, at: Long, snoozed: Boolean = false) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, noteId, if (snoozed) ACTION_SNOOZED_FIRE else ACTION_FIRE)
        if (canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            // Without "Alarms & reminders" permission Android may delay this by a few minutes.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, noteId, ACTION_FIRE))
        alarmManager.cancel(pendingIntent(context, noteId, ACTION_SNOOZED_FIRE))
    }

    private fun pendingIntent(context: Context, noteId: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_NOTE_ID, noteId)
        return PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next time a repeating reminder should fire after [now], or null for one-time reminders. */
    fun nextOccurrence(from: Long, repeat: Int, now: Long = System.currentTimeMillis()): Long? {
        val field = when (repeat) {
            Repeat.DAILY -> Calendar.DAY_OF_YEAR
            Repeat.WEEKLY -> Calendar.WEEK_OF_YEAR
            Repeat.MONTHLY -> Calendar.MONTH
            else -> return null
        }
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        while (cal.timeInMillis <= now) cal.add(field, 1)
        return cal.timeInMillis
    }
}
