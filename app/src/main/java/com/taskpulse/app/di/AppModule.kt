package com.taskpulse.app.di

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import com.taskpulse.app.ai.AiTaskParser
import com.taskpulse.app.ai.GeminiAiTaskParser
import com.taskpulse.app.data.local.AppDatabase
import com.taskpulse.app.data.local.dao.CategoryDao
import com.taskpulse.app.data.local.dao.TaskDao
import com.taskpulse.app.data.repository.CategoryRepositoryImpl
import com.taskpulse.app.data.repository.TaskRepositoryImpl
import com.taskpulse.app.domain.repository.CategoryRepository
import com.taskpulse.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "taskpulse.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAiTaskParser(impl: GeminiAiTaskParser): AiTaskParser
}
