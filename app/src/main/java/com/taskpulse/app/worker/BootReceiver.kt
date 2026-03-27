package com.taskpulse.app.worker

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*

class BootReceiver : BroadcastReceiver() {
    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(tag, "Boot receiver fired: action=${intent.action}")
        if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ||
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.miui.intent.action.BOOT_COMPLETED"
        ) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reschedule_tasks",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleTasksWorker>()
                    .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            Log.i(tag, "Reschedule work enqueued")
        }
    }
}
