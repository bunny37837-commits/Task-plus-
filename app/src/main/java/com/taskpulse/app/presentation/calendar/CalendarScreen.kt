package com.taskpulse.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.usecase.GetTasksForDateUseCase
import com.taskpulse.app.presentation.components.TaskCard
import com.taskpulse.app.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── ViewModel ─────────────────────────────────────────────────────────────

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getTasksForDateUseCase: GetTasksForDateUseCase,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _currentMonth = MutableStateFlow(YearMonth.now())

    val selectedDate = _selectedDate.asStateFlow()
    val currentMonth = _currentMonth.asStateFlow()

    val tasksForDay: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        getTasksForDateUseCase(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: LocalDate) { _selectedDate.value = date }
    fun nextMonth() { _currentMonth.update { it.plusMonths(1) } }
    fun prevMonth() { _currentMonth.update { it.minusMonths(1) } }
}

// ─── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onTaskDetail: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksForDay.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Calendar", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            IconButton(
                onClick = onAddTask,
                modifier = Modifier.clip(CircleShape).background(PrimaryPurple),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }

        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::prevMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = TextSecondary)
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            listOf("Mo","Tu","We","Th","Fr","Sa","Su").forEach { day ->
                Text(
                    day, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Calendar grid
        val firstDay = currentMonth.atDay(1)
        val startOffset = (firstDay.dayOfWeek.value - 1) // Mon=0
        val daysInMonth = currentMonth.lengthOfMonth()

        val weeks = ((startOffset + daysInMonth + 6) / 7)
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            for (week in 0 until weeks) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dow in 0..6) {
                        val dayNum = week * 7 + dow - startOffset + 1
                        val date = if (dayNum in 1..daysInMonth) currentMonth.atDay(dayNum) else null
                        DayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            modifier = Modifier.weight(1f),
                            onClick = { date?.let { viewModel.selectDate(it) } },
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

        // Task list for selected day
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No tasks this day", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(task = task, onComplete = {}, onDelete = {}, onClick = { onTaskDetail(task.id) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> PrimaryPurple
                    isToday -> PrimaryPurple.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> PrimaryPurple
                    else -> TextSecondary
                }
            )
        }
    }
}
