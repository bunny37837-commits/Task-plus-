package com.taskpulse.app.worker

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.overlay.OverlayService

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc  = intent.getStringExtra("TASK_DESC") ?: ""

        Log.d("TaskAlarmReceiver", "Alarm fired for taskId=$taskId")

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TaskPulse:AlarmWakeLock")
        wakeLock.acquire(10000)

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

            val serviceIntent = Intent(context, OverlayService::class.java)
                .putExtra("TASK_ID", taskId)
                .putExtra("TASK_TITLE", title)
                .putExtra("TASK_DESC", desc)

            context.startForegroundService(serviceIntent)

        } catch (e: Exception) {
            Log.e("TaskAlarmReceiver", "Receiver error", e)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
