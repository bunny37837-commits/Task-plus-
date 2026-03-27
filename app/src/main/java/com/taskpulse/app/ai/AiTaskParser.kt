package com.taskpulse.app.ai

import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime

data class TaskDraft(
    val title: String,
    val description: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val priority: Priority = Priority.MEDIUM,
)

interface AiTaskParser {
    suspend fun parse(message: String): Result<TaskDraft>
}
