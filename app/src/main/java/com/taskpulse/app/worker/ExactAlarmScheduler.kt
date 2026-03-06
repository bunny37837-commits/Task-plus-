package com.taskpulse.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskpulse.app.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExactAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
) {
    fun schedule(task: Task) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "com.taskpulse.TASK_ALARM"
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_DESC", task.description)
            putExtra("TASK_SHOW_OVERLAY", task.showOverlay)
            putExtra("TASK_VIBRATE", task.vibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerMillis = task.scheduledDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback to inexact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancel(taskId: Long) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = "com.taskpulse.TASK_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
