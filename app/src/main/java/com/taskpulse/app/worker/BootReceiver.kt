package com.taskpulse.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reschedule_tasks",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleTasksWorker>()
                    .setInitialDelay(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
        }
    }
}
