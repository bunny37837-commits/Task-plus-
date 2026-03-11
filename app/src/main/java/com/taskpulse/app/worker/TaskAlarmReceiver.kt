package com.taskpulse.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.alert.AlertActivity
import com.taskpulse.app.overlay.OverlayService

class TaskAlarmReceiver : BroadcastReceiver() {
    private val tag = "TaskAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") {
            Log.w(tag, "Ignoring unexpected action: action=${intent.action}")
            return
        }

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) {
            Log.e(tag, "Ignoring alarm with missing taskId")
            return
        }

        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""
        val showOverlay = intent.getBooleanExtra("TASK_SHOW_OVERLAY", true)
        val vibrate = intent.getBooleanExtra("TASK_VIBRATE", true)
        val canDrawOverlays = Settings.canDrawOverlays(context)

        Log.i(
            tag,
            "Receiver fired: taskId=$taskId, showOverlay=$showOverlay, vibrate=$vibrate, " +
                "canDrawOverlays=$canDrawOverlays"
        )

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskPulse:AlarmWakeLock"
        )

        wakeLock.acquire(10_000L)

        try {
            if (showOverlay && canDrawOverlays) {
                val serviceIntent = Intent(context, OverlayService::class.java).apply {
                    putExtra("TASK_ID", taskId)
                    putExtra("TASK_TITLE", title)
                    putExtra("TASK_DESC", desc)
                    putExtra("TASK_SHOW_OVERLAY", true)
                    putExtra("TASK_VIBRATE", vibrate)
                }

                try {
                    Log.i(tag, "Starting overlay service: taskId=$taskId")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.i(tag, "Overlay service start request sent: taskId=$taskId")
                    postNotification(context, taskId, title, desc, fullScreen = false)
                } catch (e: Exception) {
                    Log.e(tag, "Overlay service start failed, using full-screen fallback: taskId=$taskId", e)
                    postNotification(context, taskId, title, desc, fullScreen = true)
                }
            } else {
                Log.w(
                    tag,
                    "Overlay unavailable, using full-screen fallback: taskId=$taskId, " +
                        "showOverlay=$showOverlay, canDrawOverlays=$canDrawOverlays"
                )
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
        val canUseFullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java)
                .canUseFullScreenIntent()
                .also { allowed ->
                    Log.i(tag, "Full-screen intent eligibility: allowed=$allowed, taskId=$taskId")
                }
        } else {
            Log.i(tag, "Full-screen intent eligibility: allowed=true, taskId=$taskId, api=<34")
            true
        }

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

        if (fullScreen && canUseFullScreenIntent) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        Log.i(
            tag,
            "Posting fallback notification: taskId=$taskId, requestedFullScreen=$fullScreen, " +
                "effectiveFullScreen=${fullScreen && canUseFullScreenIntent}"
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(requestCode, builder.build())
    }
}
