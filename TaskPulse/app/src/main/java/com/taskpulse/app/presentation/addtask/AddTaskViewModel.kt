package com.taskpulse.app.presentation.addtask

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
    val date: LocalDate = LocalDate.now().plusDays(1),
    val time: LocalTime = LocalTime.of(9, 0),
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

    private val _state = MutableStateFlow(AddTaskUiState())
    val state: StateFlow<AddTaskUiState> = _state.asStateFlow()

    private var editingTaskId: Long? = null

    init {
        loadCategories()
    }

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

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setCategory(v: Category?) = _state.update { it.copy(selectedCategory = v) }
    fun setPriority(v: Priority) = _state.update { it.copy(priority = v) }
    fun setDate(v: LocalDate) = _state.update { it.copy(date = v) }
    fun setTime(v: LocalTime) = _state.update { it.copy(time = v) }
    fun setRecurrence(v: RecurrenceType) = _state.update { it.copy(recurrence = v) }
    fun setVibrate(v: Boolean) = _state.update { it.copy(vibrate = v) }
    fun setShowOverlay(v: Boolean) = _state.update { it.copy(showOverlay = v) }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return@launch
        }
        val dt = LocalDateTime.of(s.date, s.time)
        if (dt.isBefore(LocalDateTime.now())) {
            _state.update { it.copy(error = "Please select a future date and time") }
            return@launch
        }
        _state.update { it.copy(isLoading = true) }
        val task = Task(
            id = editingTaskId ?: 0,
            title = s.title.trim(),
            description = s.description.trim(),
            category = s.selectedCategory,
            priority = s.priority,
            scheduledDateTime = dt,
            recurrence = s.recurrence,
            vibrate = s.vibrate,
            showOverlay = s.showOverlay,
        )
        if (editingTaskId != null) {
            updateTaskUseCase(task)
            alarmScheduler.cancel(task.id)
        } else {
            val newId = createTaskUseCase(task)
            alarmScheduler.schedule(task.copy(id = newId))
        }
        if (editingTaskId == null) {
            // alarm scheduled above with new id
        } else {
            alarmScheduler.schedule(task)
        }
        _state.update { it.copy(isLoading = false, saved = true) }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
