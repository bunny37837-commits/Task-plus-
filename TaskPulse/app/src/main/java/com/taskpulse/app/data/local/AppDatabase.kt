package com.taskpulse.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taskpulse.app.data.local.converter.DateTimeConverters
import com.taskpulse.app.data.local.dao.CategoryDao
import com.taskpulse.app.data.local.dao.TaskDao
import com.taskpulse.app.data.local.entity.CategoryEntity
import com.taskpulse.app.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
}
