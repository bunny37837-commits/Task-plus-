package com.taskpulse.app.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.taskpulse.app.presentation.ui.theme.TaskPulseTheme

class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val taskDescription = intent.getStringExtra("TASK_DESC") ?: ""

        setContent {
            TaskPulseTheme {
                OverlayScreen(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    taskDescription = taskDescription,
                    onComplete = { finish() },
                    onSnooze = { finish() },
                    onDismiss = { finish() }
                )
            }
        }
    }
}
