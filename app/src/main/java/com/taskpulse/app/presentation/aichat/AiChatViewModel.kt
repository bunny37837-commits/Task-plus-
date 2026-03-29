package com.taskpulse.app.presentation.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

enum class ChatRole { USER, ASSISTANT }
enum class MessageStatus { SENDING, SENT, FAILED }

data class VoiceAttachment(
    val filePath: String,
    val durationMs: Long,
)

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: MessageStatus = MessageStatus.SENT,
    val draft: TaskDraft? = null,
    val voice: VoiceAttachment? = null,
)

data class AiChatUiState(
    val inputText: String = "",
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome",
            role = ChatRole.ASSISTANT,
            text = "Hi! Tell me what to schedule in plain language. Example: 'Remind me to pay rent every month on April 1st at 9am, high priority.'"
        )
    ),
    val isSending: Boolean = false,
    val isAssistantTyping: Boolean = false,
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

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendTextMessage() = viewModelScope.launch {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Type a message first.") }
            return@launch
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = text,
            status = MessageStatus.SENT,
        )

        _uiState.update {
            it.copy(
                inputText = "",
                error = null,
                isSending = true,
                isAssistantTyping = true,
                messages = it.messages + userMessage,
            )
        }

        parser.parse(text)
            .onSuccess { draft ->
                val schedule = LocalDateTime.of(draft.date, draft.time)
                val summary = buildString {
                    append("Got it. I parsed this task draft:\n")
                    append("• ${draft.title}\n")
                    append("• ${schedule.format(DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a"))}\n")
                    append("• Repeat: ${draft.recurrence.label}\n")
                    append("• Priority: ${draft.priority.label}\n\n")
                    append("Tap Create Task below this message to save it.")
                }

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.ASSISTANT,
                    text = summary,
                    draft = draft,
                )
                _uiState.update {
                    it.copy(
                        isSending = false,
                        isAssistantTyping = false,
                        messages = it.messages + assistantMessage,
                    )
                }
            }
            .onFailure { e ->
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.ASSISTANT,
                    text = e.message ?: "I couldn't understand that request. Try adding date and time.",
                    status = MessageStatus.FAILED,
                )
                _uiState.update {
                    it.copy(
                        isSending = false,
                        isAssistantTyping = false,
                        messages = it.messages + assistantMessage,
                    )
                }
            }
    }

    fun createTaskFromDraft(messageId: String) = viewModelScope.launch {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId }
        val draft = message?.draft ?: return@launch
        val schedule = LocalDateTime.of(draft.date, draft.time)

        if (!schedule.isAfter(LocalDateTime.now())) {
            appendAssistantMessage("That time is in the past. Please send an updated message with a future time.")
            return@launch
        }

        _uiState.update { it.copy(isSending = true, isAssistantTyping = true, error = null) }

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
            Task(
                id = id,
                title = draft.title,
                description = draft.description,
                scheduledDateTime = schedule,
                priority = draft.priority,
                recurrence = draft.recurrence,
            )
        }.onSuccess { task ->
            val hasAlarmPermission = alarmScheduler.hasExactAlarmPermission()
            if (!hasAlarmPermission) {
                appendAssistantMessage("Task created. Reminder is not scheduled yet because Alarms & reminders permission is off. Enable it in Settings to receive alerts on time.")
            } else {
                val scheduled = alarmScheduler.schedule(task)
                if (scheduled) {
                    appendAssistantMessage("Task created and reminder scheduled.")
                } else {
                    appendAssistantMessage("Task created, but reminder scheduling failed on this device right now. Please verify Alarms & reminders and notification settings in Settings.")
                }
            }
        }.onFailure { e ->
            appendAssistantMessage("I couldn't create the task: ${e.message}")
        }

        _uiState.update { it.copy(isSending = false, isAssistantTyping = false) }
    }

    fun attachVoiceMessage(filePath: String, durationMs: Long) {
        val userVoiceMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            voice = VoiceAttachment(filePath = filePath, durationMs = durationMs),
            text = "Voice note",
        )

        val assistantFallback = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.ASSISTANT,
            text = "Voice note received. Automatic transcription is not enabled yet, so please type a short summary and I’ll schedule it.",
        )

        _uiState.update {
            it.copy(messages = it.messages + userVoiceMessage + assistantFallback)
        }
    }

    fun attachVoicePlaceholder(reason: String) {
        appendAssistantMessage(reason)
    }

    private fun appendAssistantMessage(text: String) {
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatRole.ASSISTANT,
                    text = text,
                )
            )
        }
    }
}
