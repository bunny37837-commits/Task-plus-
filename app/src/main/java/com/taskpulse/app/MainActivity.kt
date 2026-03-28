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
import com.taskpulse.app.presentation.navigation.TaskPulseNavGraph
import com.taskpulse.app.presentation.ui.theme.TaskPulseTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { requestOverlayPermission() }

    private var batteryOptRequested = false
    private val prefs by lazy { getSharedPreferences("taskpulse_bootstrap", MODE_PRIVATE) }

    companion object {
        private const val KEY_AUTOSTART_PROMPTED = "autostart_prompted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAllPermissions()
        setContent {
            TaskPulseTheme {
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
            !isIgnoringBatteryOptimizations()
        ) {
            requestExactAlarmPermission()
            return
        }

        maybeOpenAutoStartSettingsOnFirstRun()
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

    private fun maybeOpenAutoStartSettingsOnFirstRun() {
        if (prefs.getBoolean(KEY_AUTOSTART_PROMPTED, false)) return
        if (!Settings.canDrawOverlays(this)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val opened = openAutoStartSettings()
        if (opened) {
            prefs.edit().putBoolean(KEY_AUTOSTART_PROMPTED, true).apply()
        }
    }

    private fun openAutoStartSettings(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val intents = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> listOf(
                Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                Intent("miui.intent.action.APP_PERM_EDITOR").putExtra("extra_pkgname", packageName)
            )

            manufacturer.contains("oppo") -> listOf(
                Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                Intent().setClassName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            )

            manufacturer.contains("vivo") -> listOf(
                Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            )

            manufacturer.contains("realme") -> listOf(
                Intent().setClassName("com.realme.securitycenter", "com.realme.securitycenter.permission.startup.StartupAppListActivity")
            )

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            )

            manufacturer.contains("samsung") -> listOf(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName"))
            )

            else -> listOf(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName"))
            )
        }

        intents.forEach { intent ->
            try {
                startActivity(intent)
                return true
            } catch (_: Exception) {
            }
        }
        return false
    }
}
