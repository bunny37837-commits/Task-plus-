package com.taskpulse.app.presentation.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.taskpulse.app.data.datastore.AppDataStore
import com.taskpulse.app.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val darkTheme: Boolean = true,
    val vibrateDefault: Boolean = true,
    val showOverlayDefault: Boolean = true,
    val autoRescheduleMissed: Boolean = false,
    val geminiApiKeyInput: String = "",
    val apiSaveMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appDataStore: AppDataStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appDataStore.darkThemeFlow.collect { isDark ->
                _state.update { it.copy(darkTheme = isDark) }
            }
        }
        viewModelScope.launch {
            appDataStore.geminiApiKeyFlow.collect { key ->
                _state.update { it.copy(geminiApiKeyInput = key) }
            }
        }
        viewModelScope.launch {
            appDataStore.vibrateDefaultFlow.collect { enabled ->
                _state.update { it.copy(vibrateDefault = enabled) }
            }
        }
        viewModelScope.launch {
            appDataStore.showOverlayDefaultFlow.collect { enabled ->
                _state.update { it.copy(showOverlayDefault = enabled) }
            }
        }
        viewModelScope.launch {
            appDataStore.autoRescheduleMissedFlow.collect { enabled ->
                _state.update { it.copy(autoRescheduleMissed = enabled) }
            }
        }
    }

    fun setDarkTheme(v: Boolean) {
        _state.update { it.copy(darkTheme = v) }
        viewModelScope.launch { appDataStore.setDarkTheme(v) }
    }
    fun setVibrateDefault(v: Boolean) {
        _state.update { it.copy(vibrateDefault = v) }
        viewModelScope.launch { appDataStore.setVibrateDefault(v) }
    }

    fun setShowOverlayDefault(v: Boolean) {
        _state.update { it.copy(showOverlayDefault = v) }
        viewModelScope.launch { appDataStore.setShowOverlayDefault(v) }
    }

    fun setAutoReschedule(v: Boolean) {
        _state.update { it.copy(autoRescheduleMissed = v) }
        viewModelScope.launch { appDataStore.setAutoRescheduleMissed(v) }
    }

    fun setGeminiApiKeyInput(v: String) = _state.update { it.copy(geminiApiKeyInput = v, apiSaveMessage = null) }

    fun saveGeminiApiKey() = viewModelScope.launch {
        appDataStore.setGeminiApiKey(_state.value.geminiApiKeyInput)
        _state.update { it.copy(apiSaveMessage = "Gemini API key saved") }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    val exactAlarmGranted = remember(refreshTick) { hasExactAlarmPermission(context) }
    val overlayGranted = remember(refreshTick) { hasOverlayPermission(context) }
    val fullScreenIntentGranted = remember(refreshTick) { hasFullScreenIntentPermission(context) }
    val notificationsGranted = remember(refreshTick) { hasNotificationPermission(context) }
    val batteryIgnored = remember(refreshTick) { isIgnoringBatteryOptimizations(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsSection("Permission Center") {
                PermissionStatusRow("Exact alarms", exactAlarmGranted)
                HorizontalDivider(color = BorderColor)
                PermissionStatusRow("Notifications", notificationsGranted)
                HorizontalDivider(color = BorderColor)
                PermissionStatusRow("Overlay / draw over apps", overlayGranted)
                HorizontalDivider(color = BorderColor)
                PermissionStatusRow("Full-screen alerts", fullScreenIntentGranted)
                HorizontalDivider(color = BorderColor)
                PermissionStatusRow("Battery unrestricted", batteryIgnored)
            }

            SettingsSection("Permission Actions") {
                SettingsActionButton("Open Alarms & reminders") {
                    openExactAlarmSettings(context)
                }
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("Open Overlay settings") {
                    openOverlaySettings(context)
                }
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("Open Full-screen alert settings") {
                    openFullScreenIntentSettings(context)
                }
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("Open Notification settings") {
                    openNotificationSettings(context)
                }
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("Open Battery settings") {
                    openBatterySettings(context)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { refreshTick++ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderColor),
                ) {
                    Text("Refresh permission status", color = TextPrimary)
                }
            }

            SettingsSection("Appearance") {
                SettingsToggle("Dark Theme", state.darkTheme, viewModel::setDarkTheme)
            }

            SettingsSection("Reminders") {
                SettingsToggle("Vibrate by default", state.vibrateDefault, viewModel::setVibrateDefault)
                HorizontalDivider(color = BorderColor)
                SettingsToggle("Show overlay by default", state.showOverlayDefault, viewModel::setShowOverlayDefault)
                HorizontalDivider(color = BorderColor)
                SettingsToggle("Auto-reschedule missed tasks", state.autoRescheduleMissed, viewModel::setAutoReschedule)
            }

            SettingsSection("AI (Gemini)") {
                Text(
                    "Paste your Gemini API key (stored locally on this device)",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.geminiApiKeyInput,
                    onValueChange = viewModel::setGeminiApiKeyInput,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("AIza...") },
                )
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("Save Gemini API key") {
                    viewModel.saveGeminiApiKey()
                }
                state.apiSaveMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, fontSize = 12.sp, color = Success)
                }
            }

            SettingsSection("About") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Version", fontSize = 14.sp, color = TextPrimary)
                    Text("1.0.0", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary)
        Text(
            text = if (granted) "Granted" else "Missing",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) Success else Danger,
        )
    }
}

@Composable
private fun SettingsActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
    ) {
        Text(label, color = androidx.compose.ui.graphics.Color.White)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple),
        )
    }
}

private fun hasOverlayPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
}

private fun hasExactAlarmPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    return alarmManager.canScheduleExactAlarms()
}

private fun hasNotificationPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun hasFullScreenIntentPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openOverlaySettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun openBatterySettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    } else {
        Intent(Settings.ACTION_SETTINGS)
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
