package com.taskpulse.app.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.overlay.OverlayService

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        // WakeLock — device ko jagaye rakho jab tak service start ho
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskPulse:AlarmWakeLock"
        )
        wakeLock.acquire(10_000L) // max 10 seconds

        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc  = intent.getStringExtra("TASK_DESC") ?: ""

        val serviceIntent = Intent(context, OverlayService::class.java)
            .putExtra("TASK_ID", taskId)
            .putExtra("TASK_TITLE", title)
            .putExtra("TASK_DESC", desc)
            .putExtra("TASK_SHOW_OVERLAY", intent.getBooleanExtra("TASK_SHOW_OVERLAY", true))
            .putExtra("TASK_VIBRATE", intent.getBooleanExtra("TASK_VIBRATE", true))

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Fallback: plain notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = Notification.Builder(context, TaskPulseApp.CHANNEL_REMINDER)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(desc)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)
                .build()
            nm.notify(taskId.toInt(), notif)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
