package com.taskpulse.app.worker

import android.app.ActivityOptions
import android.app.KeyguardManager
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
import com.taskpulse.app.domain.usecase.CompleteTaskUseCase
import com.taskpulse.app.domain.usecase.SnoozeTaskUseCase
import com.taskpulse.app.overlay.OverlayService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {
    private val tag = "TaskAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COMPLETE -> handleCompleteAction(context, intent)
            ACTION_SNOOZE -> handleSnoozeAction(context, intent)
            ACTION_DISMISS -> handleDismissAction(context, intent)
            ACTION_TASK_ALARM -> handleAlarm(context, intent)
            else -> Log.w(tag, "Ignoring unexpected action: action=${intent.action}")
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
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
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaskPulse:AlarmWakeLock")

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
                    val needsFullScreenAssist = !pm.isInteractive || keyguardManager.isKeyguardLocked
                    Log.i(tag, "Overlay service start request sent: taskId=$taskId")
                    postNotification(
                        context = context,
                        taskId = taskId,
                        title = title,
                        desc = desc,
                        fullScreen = needsFullScreenAssist,
                        vibrate = vibrate,
                    )
                } catch (e: Exception) {
                    Log.e(tag, "Overlay service start failed, using full-screen fallback: taskId=$taskId", e)
                    postNotification(context, taskId, title, desc, fullScreen = true, vibrate = vibrate)
                }
            } else {
                Log.w(
                    tag,
                    "Overlay unavailable, using full-screen fallback: taskId=$taskId, " +
                        "showOverlay=$showOverlay, canDrawOverlays=$canDrawOverlays"
                )
                postNotification(context, taskId, title, desc, fullScreen = true, vibrate = vibrate)
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
        fullScreen: Boolean,
        vibrate: Boolean,
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

        val requestCode = taskRequestCode(taskId)

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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            fullScreenPendingIntentOptions()
        )

        val completeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra("TASK_ID", taskId)
        }
        val snoozeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("TASK_ID", taskId)
            putExtra("SNOOZE_MINUTES", DEFAULT_SNOOZE_MINUTES)
        }

        val dismissIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra("TASK_ID", taskId)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode xor 0x1A2B,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode xor 0x2B3C,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode xor 0x3C4D,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, TaskPulseApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(if (desc.isBlank()) "Task reminder" else desc)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(fullScreenPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Snooze ${DEFAULT_SNOOZE_MINUTES}m", snoozePendingIntent)

        if (!vibrate) {
            builder.setSilent(true)
            builder.setDefaults(0)
            builder.setVibrate(longArrayOf(0L))
        }

        if (fullScreen && canUseFullScreenIntent) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        Log.i(
            tag,
            "Posting fallback notification: taskId=$taskId, requestedFullScreen=$fullScreen, " +
                "effectiveFullScreen=${fullScreen && canUseFullScreenIntent}, vibrate=$vibrate"
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(requestCode, builder.build())
    }

    private fun handleCompleteAction(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    TaskAlarmReceiverEntryPoint::class.java
                )
                entryPoint.completeTaskUseCase().invoke(taskId)
                cancelNotification(context, taskId)
                Log.i(tag, "Notification action complete: taskId=$taskId")
            } catch (e: Exception) {
                Log.e(tag, "Failed to complete task from notification action: taskId=$taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnoozeAction(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId <= 0L) return
        val minutes = intent.getIntExtra("SNOOZE_MINUTES", DEFAULT_SNOOZE_MINUTES).coerceAtLeast(1)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    TaskAlarmReceiverEntryPoint::class.java
                )
                entryPoint.snoozeTaskUseCase().invoke(taskId, minutes)
                cancelNotification(context, taskId)
                Log.i(tag, "Notification action snooze: taskId=$taskId, minutes=$minutes")
            } catch (e: Exception) {
                Log.e(tag, "Failed to snooze task from notification action: taskId=$taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleDismissAction(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId <= 0L) return
        cancelNotification(context, taskId)
        Log.i(tag, "Notification dismissed: taskId=$taskId")
    }

    private fun cancelNotification(context: Context, taskId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(taskRequestCode(taskId))
    }

    private fun fullScreenPendingIntentOptions() =
        ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
        }.toBundle()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskAlarmReceiverEntryPoint {
        fun completeTaskUseCase(): CompleteTaskUseCase
        fun snoozeTaskUseCase(): SnoozeTaskUseCase
    }

    companion object {
        private const val ACTION_TASK_ALARM = "com.taskpulse.TASK_ALARM"
        const val ACTION_COMPLETE = "com.taskpulse.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE = "com.taskpulse.ACTION_SNOOZE_TASK"
        const val ACTION_DISMISS = "com.taskpulse.ACTION_DISMISS_TASK_NOTIFICATION"
        const val DEFAULT_SNOOZE_MINUTES = 10
    }
}
