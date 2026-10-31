package com.saha.notes.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppDrawer(
    section: Section,
    category: String?,
    categories: List<String>,
    counts: Map<Section, Int>,
    onSection: (Section) -> Unit,
    onCategory: (String?) -> Unit,
    onCalendar: () -> Unit,
    onManageCategories: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
) {
    var categoriesOpen by rememberSaveable { mutableStateOf(false) }

    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DrawerHeader()

            DrawerGroup {
                DrawerRow(
                    icon = Icons.Rounded.Description,
                    label = "All Notes",
                    selected = section == Section.ALL && category == null,
                    count = counts[Section.ALL],
                    onClick = { onCategory(null) },
                    trailing = {
                        IconButton(onClick = { categoriesOpen = !categoriesOpen }) {
                            Icon(
                                if (categoriesOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (categoriesOpen) "Hide categories" else "Show categories",
                            )
                        }
                    },
                )
                if (categoriesOpen) {
                    categories.forEach { name ->
                        DrawerRow(
                            icon = Icons.Rounded.Folder,
                            label = name,
                            selected = section == Section.ALL && category == name,
                            indent = true,
                            onClick = { onCategory(name) },
                        )
                    }
                }
                DrawerRow(Icons.Rounded.CalendarMonth, "Calendar", onClick = onCalendar)
            }

            DrawerGroup {
                DrawerRow(
                    Icons.Rounded.Alarm, "Reminders",
                    selected = section == Section.REMINDERS, count = counts[Section.REMINDERS],
                    onClick = { onSection(Section.REMINDERS) },
                )
                DrawerRow(
                    Icons.Rounded.Star, "Favorites",
                    selected = section == Section.FAVORITES, count = counts[Section.FAVORITES],
                    onClick = { onSection(Section.FAVORITES) },
                )
                DrawerRow(Icons.Rounded.Tag, "Categories", onClick = onManageCategories)
                DrawerRow(
                    Icons.Rounded.Archive, "Archive",
                    selected = section == Section.ARCHIVE, count = counts[Section.ARCHIVE],
                    onClick = { onSection(Section.ARCHIVE) },
                )
                DrawerRow(
                    Icons.Rounded.Delete, "Trash",
                    selected = section == Section.TRASH, count = counts[Section.TRASH],
                    onClick = { onSection(Section.TRASH) },
                )
            }

            DrawerGroup {
                DrawerRow(Icons.Rounded.Backup, "Backup & Restore", onClick = onBackup)
                DrawerRow(Icons.Rounded.Settings, "Settings", onClick = onSettings)
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = primary)) { append("Saha") }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) { append("Notes") }
            },
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.primaryContainer))
                )
                .padding(18.dp)
        ) {
            Column {
                Text(
                    "Write it, say it, remember it.",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Notes, reminders & voice typing",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun DrawerGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp), content = content)
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    count: Int? = null,
    indent: Boolean = false,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = if (indent) 44.dp else 14.dp, end = 6.dp)
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(if (indent) 20.dp else 24.dp))
        Spacer(Modifier.width(18.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = if (indent) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
        )
        if (count != null && count > 0) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        trailing?.invoke()
    }
}
