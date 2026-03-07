package com.taskpulse.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskpulse.app.domain.usecase.GetPendingTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime

@HiltWorker
class RescheduleTasksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getPendingTasksUseCase: GetPendingTasksUseCase,
    private val alarmScheduler: ExactAlarmScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = LocalDateTime.now()
            getPendingTasksUseCase().collect { tasks ->
                tasks.filter { it.scheduledDateTime.isAfter(now) }
                    .forEach { alarmScheduler.schedule(it) }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
