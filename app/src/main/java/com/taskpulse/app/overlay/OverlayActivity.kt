package com.taskpulse.app.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.taskpulse.app.presentation.ui.theme.TaskPulseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""
        setContent {
            TaskPulseTheme {
                OverlayScreen(
                    taskTitle = title,
                    taskDesc = desc,
                    onSnooze = { finish() },
                    onComplete = { finish() },
                )
            }
        }
    }
}
