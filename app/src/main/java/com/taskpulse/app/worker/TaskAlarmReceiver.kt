package com.taskpulse.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.alert.AlertActivity

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskPulse:AlarmWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            val fullScreenIntent = Intent(context, AlertActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("TASK_ID", taskId)
                putExtra("TASK_TITLE", title)
                putExtra("TASK_DESC", desc)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                taskId.toInt(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, TaskPulseApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(if (desc.isBlank()) "Task reminder" else desc)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(taskId.toInt(), notification)

            // Extra launch attempt for OEMs that ignore fullScreenIntent
            context.startActivity(fullScreenIntent)

        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
