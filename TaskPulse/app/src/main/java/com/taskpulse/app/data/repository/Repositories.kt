package com.taskpulse.app.data.repository

import com.taskpulse.app.data.local.dao.CategoryDao
import com.taskpulse.app.data.local.dao.TaskDao
import com.taskpulse.app.data.local.entity.CategoryEntity
import com.taskpulse.app.data.local.entity.TaskEntity
import com.taskpulse.app.domain.model.Category
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.model.TaskStatus
import com.taskpulse.app.domain.repository.CategoryRepository
import com.taskpulse.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Mappers ────────────────────────────────────────────────────────────────

fun TaskEntity.toDomain(category: Category?) = Task(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    scheduledDateTime = scheduledDateTime,
    recurrence = recurrence,
    recurrenceDays = recurrenceDays.removeSurrounding("[", "]")
        .split(",").mapNotNull { it.trim().toIntOrNull() },
    recurrenceEndDate = recurrenceEndDate,
    status = status,
    completedAt = completedAt,
    snoozedUntil = snoozedUntil,
    reminderSound = reminderSound,
    vibrate = vibrate,
    showOverlay = showOverlay,
    createdAt = createdAt,
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    categoryId = category?.id,
    priority = priority,
    scheduledDateTime = scheduledDateTime,
    recurrence = recurrence,
    recurrenceDays = "[${recurrenceDays.joinToString(",")}]",
    recurrenceEndDate = recurrenceEndDate,
    status = status,
    completedAt = completedAt,
    snoozedUntil = snoozedUntil,
    reminderSound = reminderSound,
    vibrate = vibrate,
    showOverlay = showOverlay,
    createdAt = createdAt,
    updatedAt = LocalDateTime.now(),
)

fun CategoryEntity.toDomain() = Category(id, name, colorHex, iconName)
fun Category.toEntity() = CategoryEntity(id, name, colorHex, iconName)

// ─── TaskRepositoryImpl ─────────────────────────────────────────────────────

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
) : TaskRepository {

    private val dtFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private suspend fun resolveCategory(id: Long?) = id?.let { categoryDao.getCategoryById(it)?.toDomain() }

    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { list ->
            list.map { it.toDomain(resolveCategory(it.categoryId)) }
        }

    override fun getTasksForDate(date: LocalDate): Flow<List<Task>> =
        taskDao.getTasksForDate(date.format(dateFormatter)).map { list ->
            list.map { it.toDomain(resolveCategory(it.categoryId)) }
        }

    override fun getUpcomingTasks(): Flow<List<Task>> =
        taskDao.getUpcomingTasks(LocalDateTime.now().format(dtFormatter)).map { list ->
            list.map { it.toDomain(resolveCategory(it.categoryId)) }
        }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.getTasksByStatus(status.name).map { list ->
            list.map { it.toDomain(resolveCategory(it.categoryId)) }
        }

    override suspend fun getTaskById(id: Long): Task? =
        taskDao.getTaskById(id)?.let { it.toDomain(resolveCategory(it.categoryId)) }

    override suspend fun insertTask(task: Task): Long = taskDao.insertTask(task.toEntity())

    override suspend fun updateTask(task: Task) = taskDao.updateTask(task.toEntity())

    override suspend fun deleteTask(task: Task) = taskDao.deleteTask(task.toEntity())

    override suspend fun completeTask(id: Long, completedAt: LocalDateTime) {
        val now = LocalDateTime.now().format(dtFormatter)
        taskDao.completeTask(id, completedAt.format(dtFormatter), now)
    }

    override suspend fun snoozeTask(id: Long, snoozedUntil: LocalDateTime) {
        val now = LocalDateTime.now().format(dtFormatter)
        taskDao.snoozeTask(id, snoozedUntil.format(dtFormatter), now)
    }

    override suspend fun updateStatus(id: Long, status: TaskStatus) {
        taskDao.updateStatus(id, status.name, LocalDateTime.now().format(dtFormatter))
    }

    override fun getPendingTasksForReschedule(): Flow<List<Task>> =
        taskDao.getPendingTasksForReschedule(LocalDateTime.now().format(dtFormatter)).map { list ->
            list.map { it.toDomain(resolveCategory(it.categoryId)) }
        }
}

// ─── CategoryRepositoryImpl ─────────────────────────────────────────────────

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { it.map { e -> e.toDomain() } }

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    override suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category.toEntity())
}
