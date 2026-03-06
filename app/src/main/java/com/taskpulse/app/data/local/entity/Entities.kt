package com.taskpulse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.RecurrenceType
import com.taskpulse.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val scheduledDateTime: LocalDateTime,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceDays: String = "[]",
    val recurrenceEndDate: LocalDate? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: LocalDateTime? = null,
    val snoozedUntil: LocalDateTime? = null,
    val reminderSound: String = "default",
    val vibrate: Boolean = true,
    val showOverlay: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
)
