package com.taskpulse.app.presentation.taskdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.usecase.GetTaskByIdUseCase
import com.taskpulse.app.presentation.components.GradientButton
import com.taskpulse.app.presentation.components.toColor
import com.taskpulse.app.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
) : ViewModel() {
    private val _task = MutableStateFlow<Task?>(null)
    val task = _task.asStateFlow()

    fun load(id: Long) = viewModelScope.launch {
        _task.value = getTaskByIdUseCase(id)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val task by viewModel.task.collectAsStateWithLifecycle()
    LaunchedEffect(taskId) { viewModel.load(taskId) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Text("Task Detail", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = PrimaryPurple)
            }
        }

        task?.let { t ->
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Priority bar
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(t.priority.toColor())
                )

                Text(t.title, style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp), color = TextPrimary)

                if (t.description.isNotBlank()) {
                    Text(t.description, color = TextSecondary, lineHeight = 22.sp)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderColor),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Priority", t.priority.label)
                        DetailRow("Status", t.status.name.lowercase().replaceFirstChar { it.uppercase() })
                        DetailRow("Scheduled", t.scheduledDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")))
                        DetailRow("Recurrence", t.recurrence.label)
                        t.category?.let { DetailRow("Category", it.name) }
                    }
                }
            }
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
