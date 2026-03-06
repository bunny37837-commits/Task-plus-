package com.taskpulse.app.presentation.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsState(
    val darkTheme: Boolean = true,
    val vibrateDefault: Boolean = true,
    val showOverlayDefault: Boolean = true,
    val autoRescheduleMissed: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun setDarkTheme(v: Boolean) = _state.update { it.copy(darkTheme = v) }
    fun setVibrateDefault(v: Boolean) = _state.update { it.copy(vibrateDefault = v) }
    fun setShowOverlayDefault(v: Boolean) = _state.update { it.copy(showOverlayDefault = v) }
    fun setAutoReschedule(v: Boolean) = _state.update { it.copy(autoRescheduleMissed = v) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
            .statusBarsPadding().navigationBarsPadding(),
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

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            SettingsSection("About") {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version", fontSize = 14.sp, color = TextPrimary)
                    Text("1.0.0", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = SurfaceCard, border = BorderStroke(1.dp, BorderColor)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple))
    }
}
