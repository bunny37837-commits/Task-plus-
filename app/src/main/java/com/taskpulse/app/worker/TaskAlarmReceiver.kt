package com.taskpulse.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.alert.AlertActivity
import com.taskpulse.app.overlay.OverlayService

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""
        val showOverlay = intent.getBooleanExtra("TASK_SHOW_OVERLAY", true)

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskPulse:AlarmWakeLock"
        )

        wakeLock.acquire(10_000L)

        try {
            if (showOverlay && Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, OverlayService::class.java).apply {
                    putExtra("TASK_ID", taskId)
                    putExtra("TASK_TITLE", title)
                    putExtra("TASK_DESC", desc)
                    putExtra("TASK_SHOW_OVERLAY", true)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    postNotification(context, taskId, title, desc, fullScreen = false)
                } catch (e: Exception) {
                    postNotification(context, taskId, title, desc, fullScreen = true)
                }
            } else {
                postNotification(context, taskId, title, desc, fullScreen = true)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun postNotification(
        context: Context,
        taskId: Long,
        title: String,
        desc: String,
        fullScreen: Boolean
    ) {
        val requestCode = if (taskId in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
            taskId.toInt()
        } else {
            (taskId % Int.MAX_VALUE).toInt()
        }

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
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, TaskPulseApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(if (desc.isBlank()) "Task reminder" else desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)

        if (fullScreen) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(requestCode, builder.build())
    }
}
// ci trigger
