package com.taskpulse.app.presentation.addtask

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.RecurrenceType
import com.taskpulse.app.presentation.components.GradientButton
import com.taskpulse.app.presentation.components.PriorityChip
import com.taskpulse.app.presentation.ui.theme.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    taskId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) { taskId?.let { viewModel.loadTask(it) } }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    LaunchedEffect(state.error) {
        if (state.error != null) { /* show snackbar via state */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (taskId == null) "New Reminder" else "Edit Reminder",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Drag Handle
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Title
            FormSection(label = "Task Title") {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    placeholder = { Text("e.g., Take medication", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
            }

            // Date & Time
            FormSection(label = "📅 Date & Time") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Date Picker
                    Surface(
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, BorderColor),
                    ) {
                        Text(
                            text = state.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            modifier = Modifier.padding(14.dp),
                            color = TextPrimary,
                            fontSize = 14.sp,
                        )
                    }
                    // Time Picker
                    Surface(
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, BorderColor),
                    ) {
                        Text(
                            text = state.time.format(DateTimeFormatter.ofPattern("h:mm a")),
                            modifier = Modifier.padding(14.dp),
                            color = TextPrimary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // Recurrence
            FormSection(label = "🔁 Repeat") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RecurrenceType.entries
                        .filter { it != RecurrenceType.CUSTOM }
                        .forEach { recType ->
                        val selected = state.recurrence == recType
                        Surface(
                            modifier = Modifier.clickable { viewModel.setRecurrence(recType) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) PrimaryPurple.copy(0.2f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (selected) PrimaryPurple else BorderColor),
                        ) {
                            Text(
                                text = recType.label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) PrimaryPurple else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            // Priority
            FormSection(label = "🎯 Priority") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { p ->
                        PriorityChip(
                            priority = p,
                            selected = state.priority == p,
                            onClick = { viewModel.setPriority(p) },
                        )
                    }
                }
            }

            // Category
            if (state.categories.isNotEmpty()) {
                FormSection(label = "📁 Category") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // None option
                        Surface(
                            modifier = Modifier.clickable { viewModel.setCategory(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.selectedCategory == null) PrimaryPurple.copy(0.2f) else SurfaceElevated,
                            border = BorderStroke(1.dp, if (state.selectedCategory == null) PrimaryPurple else BorderColor),
                        ) {
                            Text("None", fontSize = 12.sp, color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                        state.categories.forEach { cat ->
                            val isSelected = state.selectedCategory?.id == cat.id
                            val catColor = Color(android.graphics.Color.parseColor(cat.colorHex))
                            Surface(
                                modifier = Modifier.clickable { viewModel.setCategory(cat) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) catColor.copy(0.2f) else SurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) catColor else BorderColor),
                            ) {
                                Text(cat.name, fontSize = 12.sp,
                                    color = if (isSelected) catColor else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                            }
                        }
                    }
                }
            }

            // Description
            FormSection(label = "📝 Description (Optional)") {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    placeholder = { Text("Add some details...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    colors = outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5,
                )
            }

            // Settings
            FormSection(label = "🔔 Reminder Settings") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, BorderColor),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToggleRow("Vibrate", state.vibrate, viewModel::setVibrate)
                        HorizontalDivider(color = BorderColor)
                        ToggleRow("Show overlay on screen", state.showOverlay, viewModel::setShowOverlay)
                    }
                }
            }

            // Error
            state.error?.let { message ->
                  Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Danger.copy(alpha = 0.12f),
                      border = BorderStroke(1.dp, Danger),
                      modifier = Modifier.fillMaxWidth()
                  ) {
                      Column(modifier = Modifier.padding(14.dp)) {
                          Text(
                              text = "Reminder setup incomplete",
                              color = Danger,
                              fontWeight = FontWeight.SemiBold,
                              fontSize = 14.sp
                          )
                          Spacer(Modifier.height(6.dp))
                          Text(
                              text = message,
                              color = TextPrimary,
                              fontSize = 13.sp
                          )
                      }
                  }
              }

            // Save Button
            GradientButton(
                text = if (taskId == null) "Set Reminder" else "Update Reminder",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * 86400000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK", color = PrimaryPurple) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextSecondary) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.time.hour,
            initialMinute = state.time.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = SurfaceCard,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setTime(java.time.LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) { Text("OK", color = PrimaryPurple) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = TextSecondary) } },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
private fun FormSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        content()
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryPurple),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = BorderColor,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PrimaryPurple,
    unfocusedContainerColor = SurfaceElevated,
    focusedContainerColor = SurfaceElevated,
)

// Extension
val AddTaskUiState.uiState get() = this
fun AddTaskViewModel.setUiState() {}
