package com.saha.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Epoch millis of the next reminder, or null when no reminder is set. */
    val reminderAt: Long? = null,
    val repeat: Int = Repeat.NONE,
) {
    /** True when everything the user can see is the same (timestamps ignored). */
    fun sameContentAs(other: Note): Boolean =
        copy(createdAt = 0, updatedAt = 0) == other.copy(createdAt = 0, updatedAt = 0)
}

object Repeat {
    const val NONE = 0
    const val DAILY = 1
    const val WEEKLY = 2
    const val MONTHLY = 3

    val options = listOf(NONE to "Once", DAILY to "Daily", WEEKLY to "Weekly", MONTHLY to "Monthly")

    fun label(value: Int): String = options.firstOrNull { it.first == value }?.second ?: "Once"
}
