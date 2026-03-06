package com.taskpulse.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskpulse.app.presentation.addtask.AddTaskScreen
import com.taskpulse.app.presentation.calendar.CalendarScreen
import com.taskpulse.app.presentation.home.HomeScreen
import com.taskpulse.app.presentation.settings.SettingsScreen
import com.taskpulse.app.presentation.stats.StatsScreen
import com.taskpulse.app.presentation.taskdetail.TaskDetailScreen

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Calendar   : Screen("calendar")
    object AddTask    : Screen("add_task?taskId={taskId}") {
        fun withTask(id: Long) = "add_task?taskId=$id"
        val args = listOf(navArgument("taskId") {
            type = NavType.LongType
            defaultValue = -1L
        })
    }
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun withId(id: Long) = "task_detail/$id"
    }
    object Stats      : Screen("stats")
    object Settings   : Screen("settings")
}

@Composable
fun TaskPulseNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onEditTask = { id -> navController.navigate(Screen.AddTask.withTask(id)) },
                onTaskDetail = { id -> navController.navigate(Screen.TaskDetail.withId(id)) },
                onCalendar = { navController.navigate(Screen.Calendar.route) },
                onStats = { navController.navigate(Screen.Stats.route) },
                onSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onTaskDetail = { id -> navController.navigate(Screen.TaskDetail.withId(id)) },
            )
        }

        composable(
            route = Screen.AddTask.route,
            arguments = Screen.AddTask.args,
        ) { backStack ->
            val taskId = backStack.arguments?.getLong("taskId") ?: -1L
            AddTaskScreen(
                taskId = if (taskId == -1L) null else taskId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
        ) { backStack ->
            val taskId = backStack.arguments!!.getLong("taskId")
            TaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.AddTask.withTask(taskId)) },
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
