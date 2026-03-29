package com.taskpulse.app.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.model.TaskStatus
import com.taskpulse.app.presentation.components.EmptyState
import com.taskpulse.app.presentation.components.TaskCard
import com.taskpulse.app.presentation.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTask: () -> Unit,
    onTaskDetail: (Long) -> Unit,
    onCalendar: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onAiChat: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNavBar(
                onHome = {},
                onCalendar = onCalendar,
                onAdd = onAddTask,
                onStats = onStats,
                onSettings = onSettings,
            )
        },
        floatingActionButton = {},
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                HomeHeader(onSettings = onSettings)
            }

            item {
                Spacer(Modifier.height(12.dp))
                SummaryCard(tasks = state.tasks)
            }

            item {
                Spacer(Modifier.height(8.dp))
                AiQuickAction(onAiChat = onAiChat)
            }

            item {
                FilterChips(
                    selected = state.filter,
                    onSelect = viewModel::setFilter,
                )
            }

            if (state.isLoading) {
                item { CircularProgressIndicator(modifier = Modifier.padding(32.dp), color = PrimaryPurple) }
            } else if (state.tasks.isEmpty()) {
                item {
                    EmptyState(
                        message = when (state.filter) {
                            HomeFilter.TODAY -> "No tasks today — enjoy your day!"
                            HomeFilter.DONE -> "No completed tasks yet"
                            else -> "No tasks found"
                        }
                    )
                }
            } else {
                items(items = state.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onComplete = { viewModel.completeTask(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onClick = { onTaskDetail(task.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "$greeting 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            Text(
                text = LocalDate.now().format(dayFormatter),
                fontSize = 13.sp,
                color = TextSecondary,
            )
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SurfaceCard)
                .border(1.dp, BorderColor, CircleShape),
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = TextSecondary)
        }
    }
}

@Composable
private fun SummaryCard(tasks: List<Task>) {
    val total = tasks.size
    val done = tasks.count { it.status == TaskStatus.COMPLETED }
    val missed = tasks.count { it.status == TaskStatus.MISSED }
    val pending = tasks.count { it.status == TaskStatus.PENDING }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(listOf(PrimaryPurple, Color(0xFF5A4FCF), AccentCyan.copy(alpha = 0.8f)))
            )
            .padding(20.dp),
    ) {
        Column {
            Text("TODAY'S OVERVIEW", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f), letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryItem("$total", "Total")
                SummaryItem("$done", "Done")
                SummaryItem("$pending", "Pending")
                if (missed > 0) SummaryItem("$missed", "Missed")
            }
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String) {
    Column {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun AiQuickAction(onAiChat: () -> Unit) {
    Surface(
        onClick = onAiChat,
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("✨ AI Chat Scheduler", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Type naturally and auto-create reminders", color = TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI", tint = PrimaryPurple)
        }
    }
}

@Composable
private fun FilterChips(selected: HomeFilter, onSelect: (HomeFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            Surface(
                modifier = Modifier.clickable { onSelect(filter) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryPurple else SurfaceCard,
                border = BorderStroke(1.dp, if (isSelected) PrimaryPurple else BorderColor),
            ) {
                Text(
                    text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    onHome: () -> Unit,
    onCalendar: () -> Unit,
    onAdd: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        color = SurfaceCard,
        shadowElevation = 0.dp,
        modifier = Modifier.border(width = 1.dp, color = BorderColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(icon = Icons.Outlined.Home, label = "Home", onClick = onHome)
            NavItem(icon = Icons.Outlined.CalendarMonth, label = "Calendar", onClick = onCalendar)

            // Center FAB
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryGlow)))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White, modifier = Modifier.size(26.dp))
            }

            NavItem(icon = Icons.Outlined.BarChart, label = "Stats", onClick = onStats)
            NavItem(icon = Icons.Outlined.Settings, label = "Settings", onClick = onSettings)
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = TextMuted)
    }
}
