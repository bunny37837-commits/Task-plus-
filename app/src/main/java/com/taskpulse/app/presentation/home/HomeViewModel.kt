package com.taskpulse.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.domain.model.TaskStatus
import com.taskpulse.app.domain.usecase.*
import com.taskpulse.app.worker.ExactAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class HomeFilter { ALL, TODAY, UPCOMING, DONE }

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val filter: HomeFilter = HomeFilter.TODAY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val getTasksForDateUseCase: GetTasksForDateUseCase,
    private val getUpcomingTasksUseCase: GetUpcomingTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val alarmScheduler: ExactAlarmScheduler,
) : ViewModel() {

    private val _filter = MutableStateFlow(HomeFilter.TODAY)
    val uiState: StateFlow<HomeUiState> = _filter.flatMapLatest { filter ->
        val flow = when (filter) {
            HomeFilter.ALL -> getAllTasksUseCase()
            HomeFilter.TODAY -> getTasksForDateUseCase(LocalDate.now())
            HomeFilter.UPCOMING -> getUpcomingTasksUseCase()
            HomeFilter.DONE -> getAllTasksUseCase().map { it.filter { t -> t.status == TaskStatus.COMPLETED } }
        }
        flow.map { HomeUiState(tasks = it, filter = filter, isLoading = false) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setFilter(filter: HomeFilter) { _filter.value = filter }

    fun completeTask(task: Task) = viewModelScope.launch {
        completeTaskUseCase(task.id)
        alarmScheduler.cancel(task.id)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        deleteTaskUseCase(task)
        alarmScheduler.cancel(task.id)
    }
}
