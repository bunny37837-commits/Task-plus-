package com.taskpulse.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.taskpulse.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reschedule_tasks",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleTasksWorker>().build(),
            )
        }
    }
}

@HiltWorker
class RescheduleTasksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: ExactAlarmScheduler,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val tasks = taskRepository.getPendingTasksForReschedule().first()
            tasks.forEach { task ->
                alarmScheduler.schedule(task)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
