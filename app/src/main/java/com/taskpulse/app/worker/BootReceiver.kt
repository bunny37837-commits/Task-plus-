package com.taskpulse.app.worker

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.work.*
import com.taskpulse.app.data.datastore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    private val tag = "BootReceiver"
    private val autoRescheduleMissedKey = booleanPreferencesKey("auto_reschedule_missed")

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(tag, "Boot receiver fired: action=${intent.action}")
        if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ||
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.miui.intent.action.BOOT_COMPLETED"
        ) {
            val shouldAutoReschedule = runBlocking {
                context.dataStore.data.first()[autoRescheduleMissedKey] ?: false
            }

            if (!shouldAutoReschedule) {
                Log.i(tag, "Auto-reschedule missed tasks is OFF; skipping reschedule work")
                return
            }

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
