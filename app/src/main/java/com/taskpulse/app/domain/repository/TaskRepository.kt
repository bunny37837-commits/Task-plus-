package com.taskpulse.app.domain.repository

import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksForDate(date: LocalDate): Flow<List<Task>>
    fun getUpcomingTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun completeTask(id: Long, completedAt: LocalDateTime)
    suspend fun snoozeTask(id: Long, snoozedUntil: LocalDateTime)
    suspend fun updateStatus(id: Long, status: TaskStatus)
    fun getPendingTasksForReschedule(): Flow<List<Task>>
}
