package com.taskpulse.app.presentation.addtask

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val categoryRepository: com.taskpulse.app.domain.repository.CategoryRepository,
    private val alarmScheduler: ExactAlarmScheduler,
) : ViewModel() {
    private val tag = "AddTaskViewModel"


    private val _state = MutableStateFlow(AddTaskUiState())
    val state: StateFlow<AddTaskUiState> = _state.asStateFlow()
    val uiState: StateFlow<AddTaskUiState> = state
    private var editingTaskId: Long? = null

    init { loadCategories() }

    private fun loadCategories() = viewModelScope.launch {
        categoryRepository.getAllCategories().collect { cats ->
            _state.update { it.copy(categories = cats) }
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

    fun setTitle(v: String) = _state.update { it.copy(title = v, error = null) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setCategory(v: Category?) = _state.update { it.copy(selectedCategory = v) }
    fun setPriority(v: Priority) = _state.update { it.copy(priority = v) }
    fun setDate(v: LocalDate) = _state.update { it.copy(date = v) }
    fun setTime(v: LocalTime) = _state.update { it.copy(time = v) }
    fun setRecurrence(v: RecurrenceType) = _state.update { it.copy(recurrence = v) }
    fun setVibrate(v: Boolean) = _state.update { it.copy(vibrate = v) }
    fun setShowOverlay(v: Boolean) = _state.update { it.copy(showOverlay = v) }
    fun clearError() = _state.update { it.copy(error = null) }

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
        _state.update { it.copy(isLoading = true) }
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

            if (alarmScheduler.hasExactAlarmPermission()) {
                Log.i(
                    tag,
                    "Scheduling reminder: taskId=${finalTask.id}, when=${finalTask.scheduledDateTime}"
                )
                alarmScheduler.schedule(finalTask)
                _state.update { it.copy(isLoading = false, saved = true) }
            } else {
                Log.w(
                    tag,
                    "Exact alarm permission missing, reminder not scheduled: " +
                        "taskId=${finalTask.id}, when=${finalTask.scheduledDateTime}"
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Reminder ke liye → Settings → Apps → Special app access → Alarms & reminders ON karo"
                    )
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
        }
    }
}
