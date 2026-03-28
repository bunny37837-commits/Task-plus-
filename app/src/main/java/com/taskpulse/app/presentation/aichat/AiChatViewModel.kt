package com.taskpulse.app.presentation.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpulse.app.ai.AiConfigurationException
import com.taskpulse.app.ai.AiTaskParser
import com.taskpulse.app.ai.TaskDraft
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.usecase.CreateTaskUseCase
import com.taskpulse.app.worker.ExactAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class AiChatUiState(
    val message: String = "",
    val isLoading: Boolean = false,
    val draft: TaskDraft? = null,
    val feedback: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val parser: AiTaskParser,
    private val createTaskUseCase: CreateTaskUseCase,
    private val alarmScheduler: ExactAlarmScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun setMessage(text: String) {
        _uiState.update { it.copy(message = text, error = null, feedback = null) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message, feedback = null) }
    }

    fun parseMessage() = viewModelScope.launch {
        val text = _uiState.value.message.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Type what you want to schedule first.") }
            return@launch
        }

        _uiState.update { it.copy(isLoading = true, error = null, feedback = null) }
        parser.parse(text)
            .onSuccess { draft ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        draft = draft,
                        feedback = "I parsed this task. Review and tap Create.",
                    )
                }
            }
            .onFailure { e ->
                val message = when (e) {
                    is AiConfigurationException -> e.message
                    else -> e.message ?: "Could not understand request"
                }
                _uiState.update {
                    it.copy(isLoading = false, error = message)
                }
            }
    }

    fun clearDraft() {
        _uiState.update { it.copy(draft = null, error = null, feedback = null) }
    }

    fun createTask() = viewModelScope.launch {
        val draft = _uiState.value.draft ?: return@launch
        val schedule = LocalDateTime.of(draft.date, draft.time)
        if (!schedule.isAfter(LocalDateTime.now())) {
            _uiState.update { it.copy(error = "Parsed time is in the past. Edit message with a future time.") }
            return@launch
        }

        _uiState.update { it.copy(isLoading = true, error = null, feedback = null) }

        runCatching {
            val id = createTaskUseCase(
                Task(
                    title = draft.title,
                    description = draft.description,
                    scheduledDateTime = schedule,
                    priority = draft.priority,
                    recurrence = draft.recurrence,
                )
            )
            val finalTask = Task(
                id = id,
                title = draft.title,
                description = draft.description,
                scheduledDateTime = schedule,
                priority = draft.priority,
                recurrence = draft.recurrence,
            )
            val scheduled = if (alarmScheduler.hasExactAlarmPermission()) {
                alarmScheduler.schedule(finalTask)
                true
            } else {
                false
            }
            scheduled
        }.onSuccess { scheduled ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    feedback = if (scheduled) {
                        "Task created and reminder scheduled."
                    } else {
                        "Task created, but reminder not scheduled. Enable Exact Alarms in Settings."
                    },
                    draft = null,
                    message = "",
                )
            }
        }.onFailure { e ->
            _uiState.update { it.copy(isLoading = false, error = "Failed: ${e.message}") }
        }
    }
}
