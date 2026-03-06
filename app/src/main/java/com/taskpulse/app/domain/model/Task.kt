package com.taskpulse.app.domain.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: Category? = null,
    val priority: Priority = Priority.MEDIUM,
    val scheduledDateTime: LocalDateTime,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceDays: List<Int> = emptyList(), // 1=Mon..7=Sun
    val recurrenceEndDate: LocalDate? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val reminderSound: String = "default",
    val vibrate: Boolean = true,
    val showOverlay: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

data class Category(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
)

enum class Priority(val label: String, val colorHex: String) {
    LOW("Low", "#636E72"),
    MEDIUM("Medium", "#FDCB6E"),
    HIGH("High", "#E17055"),
    CRITICAL("Critical", "#D63031");
}

enum class TaskStatus {
    PENDING, COMPLETED, MISSED, SNOOZED, CANCELLED
}

enum class RecurrenceType(val label: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom"),
}
