package com.taskpulse.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskpulse.app.domain.usecase.GetPendingTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

@HiltWorker
class RescheduleTasksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getPendingTasksUseCase: GetPendingTasksUseCase,
    private val alarmScheduler: ExactAlarmScheduler,
) : CoroutineWorker(context, params) {
    private val tag = "RescheduleTasksWorker"

    override suspend fun doWork(): Result {
        return try {
            val now = LocalDateTime.now()
            val tasks = getPendingTasksUseCase().first() // ✅ sirf ek baar read
            val futureTasks = tasks.filter { (it.snoozedUntil ?: it.scheduledDateTime).isAfter(now) }
            Log.i(
                tag,
                "Reschedule started: totalPending=${tasks.size}, futurePending=${futureTasks.size}"
            )
            var successCount = 0
            futureTasks.forEach {
                if (alarmScheduler.schedule(it)) successCount++
            }
            Log.i(tag, "Reschedule finished: attempted=${futureTasks.size}, scheduled=$successCount")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Reschedule failed", e)
            Result.retry()
        }
    }
}
