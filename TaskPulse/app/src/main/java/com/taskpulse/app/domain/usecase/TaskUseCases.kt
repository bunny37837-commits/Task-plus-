package com.taskpulse.app.domain.usecase

import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.model.TaskStatus
import com.taskpulse.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(task: Task): Long = repo.insertTask(task)
}

class UpdateTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(task: Task) = repo.updateTask(task)
}

class DeleteTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(task: Task) = repo.deleteTask(task)
}

class GetAllTasksUseCase @Inject constructor(private val repo: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repo.getAllTasks()
}

class GetTasksForDateUseCase @Inject constructor(private val repo: TaskRepository) {
    operator fun invoke(date: LocalDate): Flow<List<Task>> = repo.getTasksForDate(date)
}

class GetUpcomingTasksUseCase @Inject constructor(private val repo: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repo.getUpcomingTasks()
}

class GetTaskByIdUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(id: Long): Task? = repo.getTaskById(id)
}

class CompleteTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(id: Long) = repo.completeTask(id, LocalDateTime.now())
}

class SnoozeTaskUseCase @Inject constructor(private val repo: TaskRepository) {
    suspend operator fun invoke(id: Long, minutes: Int) {
        val until = LocalDateTime.now().plusMinutes(minutes.toLong())
        repo.snoozeTask(id, until)
    }
}

class GetMissedTasksUseCase @Inject constructor(private val repo: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repo.getTasksByStatus(TaskStatus.MISSED)
}

class GetPendingTasksUseCase @Inject constructor(private val repo: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repo.getPendingTasksForReschedule()
}
