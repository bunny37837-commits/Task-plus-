package com.taskpulse.app.overlay

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.media.AudioAttributes
import android.media.AudioManager
import android.content.pm.ServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.taskpulse.app.MainActivity
import com.taskpulse.app.R
import com.taskpulse.app.TaskPulseApp
import com.taskpulse.app.alert.AlertActivity
import com.taskpulse.app.domain.usecase.CompleteTaskUseCase
import com.taskpulse.app.domain.usecase.SnoozeTaskUseCase
import com.taskpulse.app.worker.ExactAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {
    private val tag = "OverlayService"

    @Inject lateinit var completeTaskUseCase: CompleteTaskUseCase
    @Inject lateinit var snoozeTaskUseCase: SnoozeTaskUseCase
    @Inject lateinit var alarmScheduler: ExactAlarmScheduler

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var autoDismissJob: Job? = null
    private var ringtone: Ringtone? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId      = intent?.getLongExtra("TASK_ID", -1L) ?: -1L
        val title       = intent?.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc        = intent?.getStringExtra("TASK_DESC") ?: ""
        val showOverlay = intent?.getBooleanExtra("TASK_SHOW_OVERLAY", true) ?: true
        val vibrate     = intent?.getBooleanExtra("TASK_VIBRATE", true) ?: true

        Log.i(
            tag,
            "Service started: taskId=$taskId, showOverlay=$showOverlay, vibrate=$vibrate"
        )

        try {
            Log.i(tag, "Entering startForegroundWithNotification: taskId=$taskId")
            startForegroundWithNotification(taskId, title, desc)
            Log.i(tag, "startForegroundWithNotification succeeded: taskId=$taskId")
        } catch (e: Exception) {
            Log.e(
                tag,
                "startForegroundWithNotification failed: taskId=$taskId, " +
                    "exception=${e.javaClass.simpleName}, message=${e.message}",
                e
            )
            Log.w(tag, "Triggering notification fallback after foreground start failure: taskId=$taskId")
            launchAlertFallback(taskId, title, desc)
            stopSelf()
            return START_NOT_STICKY
        }

        Log.i(tag, "Before vibration stage: taskId=$taskId, vibrate=$vibrate")
        if (vibrate) doVibrate()
        Log.i(tag, "After vibration stage: taskId=$taskId, vibrate=$vibrate")
        playRingtone()

        if (showOverlay) showOverlay(taskId, title, desc)

        // Auto-dismiss after 2 minutes
        autoDismissJob = serviceScope.launch {
            delay(120_000)
            dismiss()
        }

        return START_NOT_STICKY
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(TaskPulseApp.CHANNEL_REMINDERS) == null) {
                val channel = NotificationChannel(
                    TaskPulseApp.CHANNEL_REMINDERS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Task reminder alerts"
                    enableVibration(true)
                    setShowBadge(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun startForegroundWithNotification(taskId: Long, title: String, desc: String) {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = Notification.Builder(this, TaskPulseApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(desc.ifBlank { "Tap to open" })
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
        val notificationId = taskId.toInt().coerceAtLeast(1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(notificationId, notif)
        }
        Log.i(tag, "Foreground notification started: taskId=$taskId")
    }

    private fun doVibrate() {
        val pattern = longArrayOf(0, 700, 300, 700, 300, 700)
        Log.i(tag, "Vibration attempt started")
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val reminderChannel = nm.getNotificationChannel(TaskPulseApp.CHANNEL_REMINDERS)
            val interruptionFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm.currentInterruptionFilter.toString()
            } else {
                "unsupported"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vm.defaultVibrator
                Log.i(
                    tag,
                    "Vibration context: hasVibrator=${vibrator.hasVibrator()}, " +
                        "ringerMode=${audioManager.ringerMode}, interruptionFilter=$interruptionFilter, " +
                        "channelExists=${reminderChannel != null}, " +
                        "channelShouldVibrate=${reminderChannel?.shouldVibrate()}"
                )
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1),
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                Log.i(
                    tag,
                    "Vibration context: hasVibrator=${vibrator.hasVibrator()}, " +
                        "ringerMode=${audioManager.ringerMode}, interruptionFilter=$interruptionFilter, " +
                        "channelExists=${reminderChannel != null}, " +
                        "channelShouldVibrate=${reminderChannel?.shouldVibrate()}"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, -1),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
            Log.i(tag, "Vibration started successfully")
        } catch (e: Exception) {
            Log.e(tag, "Vibration failed", e)
        }
    }

    private fun playRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    it.isLooping = false
                }
                it.play()
            }
            Log.i(tag, "Ringtone started")
        } catch (e: Exception) {
            Log.e(tag, "Ringtone start failed", e)
        }
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
            ringtone = null
            Log.i(tag, "Ringtone stopped")
        } catch (e: Exception) {
            Log.e(tag, "Ringtone stop failed", e)
        }
    }

    private fun showOverlay(taskId: Long, title: String, desc: String) {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80 // slight margin from top like Truecaller
        }

        val view = ComposeView(this).apply {
            val lifecycleOwner = object : SavedStateRegistryOwner {
                val lc   = LifecycleRegistry(this)
                val ctrl = SavedStateRegistryController.create(this)
                override val lifecycle: Lifecycle get() = lc
                override val savedStateRegistry get() = ctrl.savedStateRegistry
                init {
                    ctrl.performAttach()
                    ctrl.performRestore(null)
                    lc.currentState = Lifecycle.State.RESUMED
                }
            }
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            })

            val coroutineContext = AndroidUiDispatcher.CurrentThread
            val recomposer = Recomposer(coroutineContext)
            setParentCompositionContext(recomposer)
            serviceScope.launch(coroutineContext) {
                recomposer.runRecomposeAndApplyChanges()
            }

            setContent {
                OverlayScreen(
                    taskTitle = title,
                    taskDesc  = desc,
                    onSnooze  = { minutes ->
                        serviceScope.launch {
                            try {
                                snoozeTaskUseCase(taskId, minutes)
                            } finally {
                                dismiss()
                            }
                        }
                    },
                    onComplete = {
                        serviceScope.launch {
                            try {
                                completeTaskUseCase(taskId)
                            } finally {
                                dismiss()
                            }
                        }
                    }
                )
            }
        }

        overlayView = view
        try {
            Log.i(tag, "Before overlay addView: taskId=$taskId")
            windowManager.addView(view, params)
            Log.i(tag, "Overlay addView success: taskId=$taskId")
            Log.i(tag, "After overlay addView success: taskId=$taskId")
        } catch (e: Exception) {
            Log.e(tag, "Overlay addView failed: taskId=$taskId", e)
            Log.w(tag, "Before fallback notification post: taskId=$taskId")
            launchAlertFallback(taskId, title, desc)
            dismiss()
        }
    }

    private fun launchAlertFallback(taskId: Long, title: String, desc: String) {
        Log.w(tag, "Launching alert fallback: taskId=$taskId")
        Log.w(tag, "Before fallback notification post: taskId=$taskId")

        val fullScreenIntent = Intent(this, AlertActivity::class.java).apply {
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
            this,
            taskId.toInt().coerceAtLeast(1),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            fullScreenPendingIntentOptions()
        )

        val canUseFullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            getSystemService(NotificationManager::class.java)
                .canUseFullScreenIntent()
                .also { allowed ->
                    Log.i(tag, "Full-screen intent eligibility: allowed=$allowed, taskId=$taskId")
                }
        } else {
            Log.i(tag, "Full-screen intent eligibility: allowed=true, taskId=$taskId, api=<34")
            true
        }

        val notification = NotificationCompat.Builder(this, TaskPulseApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(desc.ifBlank { "Task reminder" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .apply {
                if (canUseFullScreenIntent) {
                    setFullScreenIntent(fullScreenPendingIntent, true)
                }
            }
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(taskId.toInt().coerceAtLeast(1), notification)
        Log.i(
            tag,
            "Alert fallback notification posted: taskId=$taskId, fullScreen=$canUseFullScreenIntent"
        )
    }

    private fun fullScreenPendingIntentOptions() =
        ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
        }.toBundle()

    private fun dismiss() {
        autoDismissJob?.cancel()
        stopRingtone()
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        Log.i(tag, "Service dismiss called")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
        serviceScope.cancel()
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        Log.i(tag, "Service destroyed")
    }
}
