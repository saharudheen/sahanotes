package com.saha.notes.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saha.notes.NotesApp
import com.saha.notes.data.Note
import com.saha.notes.ui.components.NoteCard
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ReminderDot = Color(0xFFFF8A3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, onOpenNote: (Long) -> Unit) {
    val app = LocalContext.current.applicationContext as NotesApp
    val flow = remember { app.repository.observeAll() }
    val all by flow.collectAsStateWithLifecycle(initialValue = emptyList())

    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()

    val zone = ZoneId.systemDefault()
    val live = remember(all) { all.filter { !it.inTrash } }
    val notesByDay = remember(live) { live.groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() } }
    val remindersByDay = remember(live) {
        live.filter { it.reminderAt != null }
            .groupBy { Instant.ofEpochMilli(it.reminderAt!!).atZone(zone).toLocalDate() }
    }
    val dayNotes: List<Note> = (remindersByDay[selected].orEmpty() + notesByDay[selected].orEmpty()).distinctBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { month = YearMonth.now(); selected = today }) { Text("Today") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        MonthHeader(
                            month = month,
                            onPrev = { month = month.minusMonths(1) },
                            onNext = { month = month.plusMonths(1) },
                        )
                        WeekdayRow()
                        MonthGrid(
                            month = month,
                            selected = selected,
                            today = today,
                            hasNote = { notesByDay.containsKey(it) },
                            hasReminder = { remindersByDay.containsKey(it) },
                            onSelect = { selected = it },
                        )
                        Row(
                            Modifier.padding(top = 8.dp, start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Dot(MaterialTheme.colorScheme.primary)
                            Text("  Note created    ", style = MaterialTheme.typography.labelSmall)
                            Dot(ReminderDot)
                            Text("  Reminder", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item {
                Text(
                    selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            if (dayNotes.isEmpty()) {
                item {
                    Text(
                        "No notes or reminders on this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            items(dayNotes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    grid = false,
                    selected = false,
                    onClick = { onOpenNote(note.id) },
                    onLongClick = { onOpenNote(note.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month") }
        Text(
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month") }
    }
}

private val WeekOrder = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

@Composable
private fun WeekdayRow() {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        WeekOrder.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    hasNote: (LocalDate) -> Boolean,
    hasReminder: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday first
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Column {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val date = month.atDay(dayNum)
                            DayCell(
                                day = dayNum,
                                isSelected = date == selected,
                                isToday = date == today,
                                note = hasNote(date),
                                reminder = hasReminder(date),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isSelected: Boolean, isToday: Boolean, note: Boolean, reminder: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isSelected) primary else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
            if (note) Dot(if (isSelected) MaterialTheme.colorScheme.onPrimary else primary)
            if (reminder) Dot(ReminderDot)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(5.dp).clip(CircleShape).background(color))
}
