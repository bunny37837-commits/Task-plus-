package com.taskpulse.app.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.overlay.OverlayActivity

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""

        Log.d("TaskAlarmReceiver", "Alarm fired for taskId=$taskId title=$title")

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskPulse:AlarmWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = Notification.Builder(context, TaskPulseApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(if (desc.isBlank()) "Task reminder" else desc)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)
                .build()
            nm.notify(taskId.toInt(), notif)

            val popupIntent = Intent(context, OverlayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra("TASK_ID", taskId)
                putExtra("TASK_TITLE", title)
                putExtra("TASK_DESC", desc)
                putExtra("TASK_SHOW_OVERLAY", intent.getBooleanExtra("TASK_SHOW_OVERLAY", true))
                putExtra("TASK_VIBRATE", intent.getBooleanExtra("TASK_VIBRATE", true))
            }

            context.startActivity(popupIntent)
            Log.d("TaskAlarmReceiver", "OverlayActivity launched for taskId=$taskId")

        } catch (e: Exception) {
            Log.e("TaskAlarmReceiver", "Receiver failed", e)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
