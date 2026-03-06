// Extension to fix uiState reference
package com.taskpulse.app.presentation.addtask
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
// uiState alias for AddTaskViewModel
val AddTaskViewModel.uiState: StateFlow<AddTaskUiState> get() = state
