package com.taskpulse.app.presentation.stats

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.model.TaskStatus
import com.taskpulse.app.domain.usecase.GetAllTasksUseCase
import com.taskpulse.app.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StatsUiState(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val missedTasks: Int = 0,
    val completionRate: Float = 0f,
    val recentCompleted: List<Task> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    getAllTasksUseCase: GetAllTasksUseCase,
) : ViewModel() {
    val state: StateFlow<StatsUiState> = getAllTasksUseCase().map { tasks ->
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val missed = tasks.count { it.status == TaskStatus.MISSED }
        val rate = if (tasks.isNotEmpty()) completed.toFloat() / tasks.size else 0f
        StatsUiState(
            totalTasks = tasks.size,
            completedTasks = completed,
            missedTasks = missed,
            completionRate = rate,
            recentCompleted = tasks.filter { it.status == TaskStatus.COMPLETED }
                .sortedByDescending { it.completedAt }.take(10),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
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
            Text("Your Progress", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Stats cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    value = "${(state.completionRate * 100).toInt()}%",
                    label = "Completion",
                    gradient = Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryGlow)),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "${state.completedTasks}",
                    label = "Completed",
                    gradient = Brush.horizontalGradient(listOf(Success, Color(0xFF00A885))),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = "${state.missedTasks}",
                    label = "Missed",
                    gradient = Brush.horizontalGradient(listOf(Danger, Critical)),
                    modifier = Modifier.weight(1f),
                )
            }

            // Progress bar
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceCard, border = BorderStroke(1.dp, BorderColor)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overall Progress", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    LinearProgressIndicator(
                        progress = { state.completionRate },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = PrimaryPurple,
                        trackColor = SurfaceElevated,
                    )
                    Text("${state.completedTasks} of ${state.totalTasks} tasks completed",
                        fontSize = 12.sp, color = TextSecondary)
                }
            }

            // Recent completions
            if (state.recentCompleted.isNotEmpty()) {
                Text("Recent Completions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                state.recentCompleted.take(5).forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, BorderColor),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(task.title, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("✓", color = Success, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, gradient: Brush, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}
