package com.taskpulse.app.presentation.addtask

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpulse.app.data.datastore.AppDataStore
import com.taskpulse.app.domain.model.*
import com.taskpulse.app.domain.usecase.*
import com.taskpulse.app.worker.ExactAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class AddTaskUiState(
    val title: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val priority: Priority = Priority.MEDIUM,
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now().plusMinutes(5),
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val vibrate: Boolean = true,
    val showOverlay: Boolean = true,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val reminderPermissionWarning: String? = null,
)

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val categoryRepository: com.taskpulse.app.domain.repository.CategoryRepository,
    private val alarmScheduler: ExactAlarmScheduler,
    private val appDataStore: AppDataStore,
) : ViewModel() {
    private val tag = "AddTaskViewModel"


    private val _state = MutableStateFlow(AddTaskUiState())
    val state: StateFlow<AddTaskUiState> = _state.asStateFlow()
    val uiState: StateFlow<AddTaskUiState> = state
    private var editingTaskId: Long? = null

    init {
        loadCategories()
        loadReminderDefaults()
    }

    private fun loadCategories() = viewModelScope.launch {
        categoryRepository.getAllCategories().collect { cats ->
            _state.update { it.copy(categories = cats) }
        }
    }

    private fun loadReminderDefaults() = viewModelScope.launch {
        combine(
            appDataStore.vibrateDefaultFlow,
            appDataStore.showOverlayDefaultFlow,
        ) { vibrateDefault, showOverlayDefault ->
            vibrateDefault to showOverlayDefault
        }.collect { (vibrateDefault, showOverlayDefault) ->
            if (editingTaskId == null) {
                _state.update {
                    it.copy(
                        vibrate = vibrateDefault,
                        showOverlay = showOverlayDefault,
                    )
                }
            }
        }
    }

    fun loadTask(id: Long) = viewModelScope.launch {
        val task = getTaskByIdUseCase(id) ?: return@launch
        editingTaskId = id
        _state.update {
            it.copy(
                title = task.title,
                description = task.description,
                selectedCategory = task.category,
                priority = task.priority,
                date = task.scheduledDateTime.toLocalDate(),
                time = task.scheduledDateTime.toLocalTime(),
                recurrence = task.recurrence,
                vibrate = task.vibrate,
                showOverlay = task.showOverlay,
            )
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v, error = null, reminderPermissionWarning = null) }
    fun setDescription(v: String) = _state.update { it.copy(description = v, reminderPermissionWarning = null) }
    fun setCategory(v: Category?) = _state.update { it.copy(selectedCategory = v, reminderPermissionWarning = null) }
    fun setPriority(v: Priority) = _state.update { it.copy(priority = v, reminderPermissionWarning = null) }
    fun setDate(v: LocalDate) = _state.update { it.copy(date = v, reminderPermissionWarning = null) }
    fun setTime(v: LocalTime) = _state.update { it.copy(time = v, reminderPermissionWarning = null) }
    fun setRecurrence(v: RecurrenceType) = _state.update { it.copy(recurrence = v, reminderPermissionWarning = null) }
    fun setVibrate(v: Boolean) = _state.update { it.copy(vibrate = v, reminderPermissionWarning = null) }
    fun setShowOverlay(v: Boolean) = _state.update { it.copy(showOverlay = v, reminderPermissionWarning = null) }
    fun clearError() = _state.update { it.copy(error = null, reminderPermissionWarning = null) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return@launch
        }
        val scheduledDt = LocalDateTime.of(s.date, s.time)
        if (scheduledDt.isBefore(LocalDateTime.now())) {
            _state.update { it.copy(error = "Please select a future date and time") }
            return@launch
        }
        _state.update { it.copy(isLoading = true, error = null, reminderPermissionWarning = null) }
        try {
            val task = Task(
                id = editingTaskId ?: 0L,
                title = s.title.trim(),
                description = s.description.trim(),
                category = s.selectedCategory,
                priority = s.priority,
                scheduledDateTime = scheduledDt,
                recurrence = s.recurrence,
                vibrate = s.vibrate,
                showOverlay = s.showOverlay,
            )
            val finalTask = if (editingTaskId != null) {
                updateTaskUseCase(task)
                alarmScheduler.cancel(editingTaskId!!)
                task
            } else {
                val newId = createTaskUseCase(task)
                task.copy(id = newId)
            }

            if (!alarmScheduler.hasExactAlarmPermission()) {
                Log.w(
                    tag,
                    "Exact alarm permission missing, reminder not scheduled: " +
                        "taskId=${finalTask.id}, when=${finalTask.scheduledDateTime}"
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        reminderPermissionWarning = "Task saved, but reminder is not scheduled yet. Enable Alarms & reminders permission in Settings to get alerts on time."
                    )
                }
                return@launch
            }

            Log.i(
                tag,
                "Scheduling reminder: taskId=${finalTask.id}, when=${finalTask.scheduledDateTime}"
            )
            val scheduled = alarmScheduler.schedule(finalTask)
            if (scheduled) {
                _state.update { it.copy(isLoading = false, saved = true) }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        reminderPermissionWarning = "Task saved, but reminder could not be scheduled on this device right now. Please check Alarms & reminders and notification settings."
                    )
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
        }
    }
}
