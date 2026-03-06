package com.taskpulse.app.data.local.converter

import androidx.room.TypeConverter
import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.RecurrenceType
import com.taskpulse.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeConverters {
    private val dtFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter fun fromDateTime(value: LocalDateTime?): String? = value?.format(dtFormatter)
    @TypeConverter fun toDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it, dtFormatter) }

    @TypeConverter fun fromDate(value: LocalDate?): String? = value?.format(dateFormatter)
    @TypeConverter fun toDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, dateFormatter) }

    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String): Priority = Priority.valueOf(value)

    @TypeConverter fun fromStatus(value: TaskStatus): String = value.name
    @TypeConverter fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter fun fromRecurrence(value: RecurrenceType): String = value.name
    @TypeConverter fun toRecurrence(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}
