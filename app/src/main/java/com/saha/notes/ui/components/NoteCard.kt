package com.saha.notes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saha.notes.data.Note
import com.saha.notes.ui.theme.noteBackground

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    grid: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = noteBackground(note.colorIndex),
        border = when {
            selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            note.colorIndex == 0 -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            else -> null
        },
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            val showTopRow = note.title.isNotBlank() || note.pinned || note.favorite || selected
            if (showTopRow) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.favorite) {
                        Icon(Icons.Rounded.Star, "Favorite", Modifier.size(16.dp), tint = FavoriteGold)
                    }
                    if (note.pinned) {
                        Icon(Icons.Rounded.PushPin, "Pinned", Modifier.size(16.dp), tint = muted)
                    }
                    if (selected) {
                        Icon(
                            Icons.Rounded.CheckCircle, "Selected", Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (note.content.isNotBlank()) {
                if (note.title.isNotBlank()) Spacer(Modifier.height(6.dp))
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (grid) 10 else 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                )
            }
            note.reminderAt?.let {
                Spacer(Modifier.height(10.dp))
                ReminderChip(at = it, repeat = note.repeat)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Fmt.short(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
                if (note.category.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.Folder, null, Modifier.size(12.dp), tint = muted)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        note.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val FavoriteGold = androidx.compose.ui.graphics.Color(0xFFF2A900)
