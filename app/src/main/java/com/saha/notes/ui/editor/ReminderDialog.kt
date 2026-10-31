package com.saha.notes.ui.editor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saha.notes.data.Repeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun calendarAt(ms: Long): Calendar = Calendar.getInstance().apply { timeInMillis = ms }

private fun todayAt(hour: Int, plusDays: Int = 0): Long = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, plusDays)
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun nextHour(): Long = Calendar.getInstance().apply {
    add(Calendar.HOUR_OF_DAY, 1)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun ReminderDialog(
    initialAt: Long?,
    initialRepeat: Int,
    onDismiss: () -> Unit,
    onSet: (at: Long, repeat: Int) -> Unit,
    onRemove: (() -> Unit)?,
) {
    val context = LocalContext.current
    var at by remember {
        mutableLongStateOf(initialAt?.takeIf { it > System.currentTimeMillis() } ?: nextHour())
    }
    var repeat by remember { mutableIntStateOf(initialRepeat) }
    val now = System.currentTimeMillis()
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val timeFormat = remember { DateFormat.getTimeFormat(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
        title = { Text("Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SuggestionChip(onClick = { at = System.currentTimeMillis() + 3_600_000L }, label = { Text("In 1 hour") })
                    val tonight = todayAt(20)
                    if (tonight > now) {
                        SuggestionChip(onClick = { at = tonight }, label = { Text("Tonight 8 PM") })
                    }
                    SuggestionChip(onClick = { at = todayAt(9, plusDays = 1) }, label = { Text("Tomorrow 9 AM") })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val c = calendarAt(at)
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> at = calendarAt(at).apply { set(y, m, d) }.timeInMillis },
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
                            ).apply { datePicker.minDate = System.currentTimeMillis() - 1_000 }.show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.CalendarToday, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(dateFormat.format(Date(at)), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            val c = calendarAt(at)
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    at = calendarAt(at).apply {
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, min)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                },
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
                                DateFormat.is24HourFormat(context),
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(timeFormat.format(Date(at)), maxLines = 1)
                    }
                }

                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Repeat.options.forEach { (value, label) ->
                        FilterChip(
                            selected = repeat == value,
                            onClick = { repeat = value },
                            label = { Text(label) },
                        )
                    }
                }

                if (at <= now) {
                    Text(
                        "Pick a time in the future.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSet(at, repeat) }, enabled = at > now) { Text("Set") }
        },
        dismissButton = {
            Row {
                if (onRemove != null) TextButton(onClick = onRemove) { Text("Remove") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
