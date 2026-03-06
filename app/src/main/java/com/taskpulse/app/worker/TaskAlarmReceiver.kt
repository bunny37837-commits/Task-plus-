package com.taskpulse.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.taskpulse.app.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.taskpulse.TASK_ALARM") return

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val showOverlay = intent.getBooleanExtra("TASK_SHOW_OVERLAY", true)

        val serviceIntent = Intent(context, OverlayService::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", intent.getStringExtra("TASK_TITLE"))
            putExtra("TASK_DESC", intent.getStringExtra("TASK_DESC"))
            putExtra("TASK_SHOW_OVERLAY", showOverlay)
            putExtra("TASK_VIBRATE", intent.getBooleanExtra("TASK_VIBRATE", true))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
