package com.taskpulse.app

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.data.datastore.AppDataStore
import com.taskpulse.app.presentation.navigation.TaskPulseNavGraph
import com.taskpulse.app.presentation.ui.theme.TaskPulseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appDataStore: AppDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { requestOverlayPermission() }

    private var batteryOptRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAllPermissions()
        setContent {
            val darkTheme = appDataStore.darkThemeFlow.collectAsStateWithLifecycle(initialValue = true).value
            TaskPulseTheme(darkTheme = darkTheme) {
                TaskPulseNavGraph()
            }
        }
    }

    private fun requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        requestOverlayPermission()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }
        requestExactAlarmPermission()
    }

    override fun onResume() {
        super.onResume()
        // Sirf ek baar battery maangega + already allowed check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            Settings.canDrawOverlays(this) && 
            !batteryOptRequested && 
            !isIgnoringBatteryOptimizations()) {
            requestExactAlarmPermission()
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm: PowerManager = getSystemService(PowerManager::class.java)
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }
        requestBatteryOptimization()
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || batteryOptRequested) return
        if (isIgnoringBatteryOptimizations()) return

        batteryOptRequested = true
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
