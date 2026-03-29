package com.taskpulse.app.domain.model

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
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

fun Task.nextRecurringOccurrence(referenceTime: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
    if (recurrence == RecurrenceType.NONE || recurrence == RecurrenceType.CUSTOM) return null

    var next = scheduledDateTime
    while (!next.isAfter(referenceTime)) {
        next = when (recurrence) {
            RecurrenceType.DAILY -> next.plusDays(1)
            RecurrenceType.WEEKDAYS -> next.nextWeekday()
            RecurrenceType.WEEKLY -> next.plusWeeks(1)
            RecurrenceType.MONTHLY -> next.plusMonths(1)
            RecurrenceType.NONE, RecurrenceType.CUSTOM -> return null
        }
    }

    return if (recurrenceEndDate != null && next.toLocalDate().isAfter(recurrenceEndDate)) {
        null
    } else {
        next
    }
}

private fun LocalDateTime.nextWeekday(): LocalDateTime {
    var candidate = plusDays(1)
    while (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
        candidate = candidate.plusDays(1)
    }
    return candidate
}
