package com.taskpulse.app.overlay

import android.app.Notification
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.app.NotificationManager
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.app.PendingIntent
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.app.Service
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Context
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Intent
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.graphics.PixelFormat
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.media.AudioAttributes
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.media.RingtoneManager
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.os.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.view.WindowManager
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.Recomposer
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ComposeView
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.taskpulse.app.MainActivity
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.taskpulse.app.R
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.taskpulse.app.TaskPulseApp
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import javax.inject.Inject
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var completeTaskUseCase: com.taskpulse.app.domain.usecase.CompleteTaskUseCase

    @Inject
    lateinit var snoozeTaskUseCase: com.taskpulse.app.domain.usecase.SnoozeTaskUseCase

    @Inject
    lateinit var alarmScheduler: com.taskpulse.app.worker.ExactAlarmScheduler

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var autoDismissJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getLongExtra("TASK_ID", -1L) ?: return START_NOT_STICKY
        if (taskId == -1L) return START_NOT_STICKY

        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val taskDesc = intent.getStringExtra("TASK_DESC") ?: ""
        val showOverlay = intent.getBooleanExtra("TASK_SHOW_OVERLAY", true)
        val vibrate = intent.getBooleanExtra("TASK_VIBRATE", true)

        startForeground(NOTIFICATION_ID, buildForegroundNotification(taskTitle, taskId))

        if (vibrate) vibrate()
        playSound()

        if (showOverlay && canDrawOverlays()) {
            showOverlayWindow(taskId, taskTitle, taskDesc)
        }

        // Auto-dismiss after 60s → mark missed
        autoDismissJob = serviceScope.launch {
            delay(60_000)
            dismiss()
        }

        return START_NOT_STICKY
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else true
    }

    private fun showOverlayWindow(taskId: Long, taskTitle: String, taskDesc: String) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        )

        // Create a lifecycle-aware owner for Compose
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.start()

        overlayView = ComposeView(this).also { composeView ->
            composeView.setViewTreeLifecycleOwner(lifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(lifecycleOwner)
            composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            composeView.setContent {
                com.taskpulse.app.presentation.ui.theme.TaskPulseTheme {
                    OverlayScreen(
                        taskTitle = taskTitle,
                        taskDesc = taskDesc,
                        onComplete = {
                            serviceScope.launch {
                                completeTaskUseCase(taskId)
                                alarmScheduler.cancel(taskId)
                            }
                            dismiss()
                        },
                        onSnooze = { minutes ->
                            serviceScope.launch {
                                snoozeTaskUseCase(taskId, minutes)
                                // Reschedule with new time
                            }
                            dismiss()
                        },
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            dismiss()
        }
    }

    private fun buildForegroundNotification(title: String, taskId: Long): Notification {
        val tapIntent = Intent(this, MainActivity::class.java)
        val tapPi = PendingIntent.getActivity(this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, TaskPulseApp.CHANNEL_OVERLAY)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("TaskPulse")
            .setContentText("Reminder: $title")
            .setContentIntent(tapPi)
            .build()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
        }
    }

    private fun playSound() {
        try {
            val ringtone = RingtoneManager.getRingtone(
                this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            )
            ringtone?.play()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun dismiss() {
        autoDismissJob?.cancel()
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
            overlayView = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        overlayView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) { }
        }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}

// Minimal lifecycle owner for ComposeView outside Activity
class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
