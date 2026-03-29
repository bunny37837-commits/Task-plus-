package com.taskpulse.app.ai

import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class HeuristicAiTaskParser @Inject constructor() : AiTaskParser {

    override suspend fun parse(message: String): Result<TaskDraft> = runCatching {
        val raw = message.trim()
        require(raw.isNotBlank()) { "Please type what you want to be reminded about." }

        val lower = raw.lowercase(Locale.getDefault())
        val now = LocalDate.now()

        val date = when {
            "day after tomorrow" in lower -> now.plusDays(2)
            "tomorrow" in lower -> now.plusDays(1)
            else -> parseDate(lower) ?: now
        }

        val time = parseTime(lower) ?: LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)

        val recurrence = when {
            "every day" in lower || "daily" in lower -> RecurrenceType.DAILY
            "weekdays" in lower || "every weekday" in lower -> RecurrenceType.WEEKDAYS
            "every week" in lower || "weekly" in lower -> RecurrenceType.WEEKLY
            "every month" in lower || "monthly" in lower -> RecurrenceType.MONTHLY
            else -> RecurrenceType.NONE
        }

        val priority = when {
            "critical" in lower || "urgent" in lower -> Priority.CRITICAL
            "high" in lower -> Priority.HIGH
            "low" in lower -> Priority.LOW
            else -> Priority.MEDIUM
        }

        val title = raw
            .replace(Regex("(?i)remind me to\\s*"), "")
            .replace(Regex("(?i)remember to\\s*"), "")
            .replace(Regex("(?i)at\\s+\\d{1,2}(:\\d{2})?\\s*(am|pm)?"), "")
            .replace(Regex("(?i)(tomorrow|day after tomorrow|today)"), "")
            .replace(Regex("(?i)every day|daily|every week|weekly|every month|monthly|weekdays|every weekday"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Reminder" }

        TaskDraft(
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
            date = date,
            time = time,
            recurrence = recurrence,
            priority = priority,
            description = "Created from chat"
        )
    }

    private fun parseDate(text: String): LocalDate? {
        val slash = Regex("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b").find(text)
        if (slash != null) {
            val d = slash.groupValues[1].toIntOrNull() ?: return null
            val m = slash.groupValues[2].toIntOrNull() ?: return null
            val yRaw = slash.groupValues.getOrNull(3).orEmpty()
            val y = when {
                yRaw.isBlank() -> LocalDate.now().year
                yRaw.length == 2 -> 2000 + yRaw.toInt()
                else -> yRaw.toIntOrNull() ?: LocalDate.now().year
            }
            return runCatching { LocalDate.of(y, m, d) }.getOrNull()
        }
        return null
    }

    private fun parseTime(text: String): LocalTime? {
        val twelveHour = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").find(text)
        if (twelveHour != null) {
            var h = twelveHour.groupValues[1].toIntOrNull() ?: return null
            val min = twelveHour.groupValues[2].toIntOrNull() ?: 0
            val ap = twelveHour.groupValues[3]
            if (ap == "pm" && h != 12) h += 12
            if (ap == "am" && h == 12) h = 0
            return runCatching { LocalTime.of(h, min) }.getOrNull()
        }

        val twentyFour = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(text)
        if (twentyFour != null) {
            val value = twentyFour.value
            return runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()
        }

        return null
    }
}
