package com.taskpulse.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskpulse.app.data.datastore.AppDataStore
import com.taskpulse.app.domain.model.TaskStatus
import com.taskpulse.app.domain.model.nextRecurringOccurrence
import com.taskpulse.app.domain.usecase.GetAllTasksUseCase
import com.taskpulse.app.domain.usecase.UpdateTaskUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

@HiltWorker
class MissedTasksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val alarmScheduler: ExactAlarmScheduler,
    private val appDataStore: AppDataStore,
) : CoroutineWorker(context, params) {
    private val tag = "MissedTasksWorker"

    override suspend fun doWork(): Result {
        return try {
            val now = LocalDateTime.now()
            val graceSeconds = 30L
            val overdueCutoff = now.minusSeconds(graceSeconds)
            val autoReschedule = appDataStore.shouldAutoRescheduleMissed()
            val tasks = getAllTasksUseCase().first()
            val overdue = tasks.filter { task ->
                task.status == TaskStatus.PENDING || task.status == TaskStatus.SNOOZED
            }.filter { task ->
                val triggerAt = task.snoozedUntil ?: task.scheduledDateTime
                !triggerAt.isAfter(overdueCutoff)
            }

            var markedMissed = 0
            var rescheduled = 0

            overdue.forEach { task ->
                val nextOccurrence = if (autoReschedule) task.nextRecurringOccurrence(now) else null
                if (nextOccurrence != null) {
                    val updated = task.copy(
                        scheduledDateTime = nextOccurrence,
                        snoozedUntil = null,
                        status = TaskStatus.PENDING,
                        completedAt = null,
                    )
                    updateTaskUseCase(updated)
                    alarmScheduler.cancel(task.id)
                    alarmScheduler.schedule(updated)
                    rescheduled++
                } else {
                    updateTaskUseCase(task.copy(status = TaskStatus.MISSED, snoozedUntil = null))
                    alarmScheduler.cancel(task.id)
                    markedMissed++
                }
            }

            Log.i(
                tag,
                "Missed task scan complete: overdue=${overdue.size}, markedMissed=$markedMissed, rescheduled=$rescheduled, autoReschedule=$autoReschedule"
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Missed task scan failed", e)
            Result.retry()
        }
    }
}
