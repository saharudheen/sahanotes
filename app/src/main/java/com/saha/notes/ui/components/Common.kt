package com.saha.notes.ui.components

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.saha.notes.data.Repeat
import com.saha.notes.ui.theme.NoteColors
import com.saha.notes.ui.theme.noteBackground
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {
    fun short(ms: Long): String {
        val pattern = when {
            DateUtils.isToday(ms) -> "h:mm a"
            sameYear(ms) -> "d MMM"
            else -> "d MMM yyyy"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
    }

    fun reminder(ms: Long, repeat: Int): String {
        val pattern = if (sameYear(ms)) "EEE, d MMM · h:mm a" else "d MMM yyyy · h:mm a"
        val text = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
        return if (repeat == Repeat.NONE) text else "$text · ${Repeat.label(repeat)}"
    }

    private fun sameYear(ms: Long): Boolean {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = ms }
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    }
}

fun shareNote(context: Context, title: String, content: String) {
    val text = listOf(title, content).filter { it.isNotBlank() }.joinToString("\n\n")
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share note"))
}

@Composable
fun ReminderChip(at: Long, repeat: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val past = at < System.currentTimeMillis()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            Fmt.reminder(at, repeat),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (past) 0.5f else 0.85f),
        )
    }
}

@Composable
fun ColorPalette(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NoteColors.indices.forEach { i ->
            val isSelected = i == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(noteBackground(i))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(Icons.Rounded.Check, contentDescription = NoteColors[i].name, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
