package com.taskpulse.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
    private val TAG = "ExactAlarmScheduler"

    fun hasExactAlarmPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun schedule(task: Task) {
        if (!hasExactAlarmPermission()) {
            Log.w(TAG, "No exact alarm permission for task ${task.id}")
            return
        }
        try {
            val triggerAt = task.snoozedUntil ?: task.scheduledDateTime
            val triggerTime = triggerAt
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            Log.d(
                TAG,
                "Scheduling alarm: taskId=${task.id}, title=${task.title}, triggerAt=$triggerTime, " +
                    "showOverlay=${task.showOverlay}, vibrate=${task.vibrate}, " +
                    "snoozedUntil=${task.snoozedUntil}"
            )

            val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                action = "com.taskpulse.TASK_ALARM"
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_DESC", task.description)
                putExtra("TASK_SHOW_OVERLAY", task.showOverlay)
                putExtra("TASK_VIBRATE", task.vibrate)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, task.id.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            // setAlarmClock — highest priority, HyperOS/MIUI bypass
            // System clock icon dikhta hai, Doze ignore karta hai
            val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
            alarmManager.setAlarmClock(alarmInfo, pendingIntent)

            Log.i(TAG, "Alarm scheduled successfully: taskId=${task.id}, triggerAt=$triggerTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: taskId=${task.id}", e)
        }
    }

    fun cancel(taskId: Long) {
        try {
            val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                action = "com.taskpulse.TASK_ALARM"
            }
            val pi = PendingIntent.getBroadcast(
                context, taskId.toInt(), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
            alarmManager.cancel(pi)
            pi.cancel()
            Log.i(TAG, "Cancelled alarm: taskId=$taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm: taskId=$taskId", e)
        }
    }
}
