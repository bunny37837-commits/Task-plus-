package com.taskpulse.app.data.local.dao

import androidx.room.*
import com.taskpulse.app.data.local.entity.CategoryEntity
import com.taskpulse.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY scheduledDateTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE scheduledDateTime >= :start " +
            "AND scheduledDateTime < :end " +
            "ORDER BY scheduledDateTime ASC"
    )
    fun getTasksForDate(start: String, end: String): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE COALESCE(snoozedUntil, scheduledDateTime) >= :now " +
            "AND status IN ('PENDING', 'SNOOZED') " +
            "ORDER BY COALESCE(snoozedUntil, scheduledDateTime) ASC"
    )
    fun getUpcomingTasks(now: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status")
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = 'COMPLETED', completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun completeTask(id: Long, completedAt: String, now: String)

    @Query(
        "UPDATE tasks " +
            "SET status = 'SNOOZED', snoozedUntil = :until, updatedAt = :now " +
            "WHERE id = :id"
    )
    suspend fun snoozeTask(id: Long, until: String, now: String)

    @Query("UPDATE tasks SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: String)

    @Query(
        "SELECT * FROM tasks " +
            "WHERE status IN ('PENDING', 'SNOOZED') " +
            "AND COALESCE(snoozedUntil, scheduledDateTime) > :now"
    )
    fun getPendingTasksForReschedule(now: String): Flow<List<TaskEntity>>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
