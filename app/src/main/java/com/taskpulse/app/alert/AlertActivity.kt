package com.taskpulse.app.alert

import android.app.Activity
import android.app.KeyguardManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlertActivity : ComponentActivity() {
    private val tag = "AlertActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        enableEdgeToEdge()

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""

        Log.i(tag, "Alert activity launched: taskId=$taskId")

        setContent {
            MaterialTheme {
                AlertScreen(
                    title = title,
                    desc = desc,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun AlertScreen(
    title: String,
    desc: String,
    onDismiss: () -> Unit,
) {
    val bg = Color(0xFFFF3C38)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⏰ REMINDER",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = bg
                )
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        }
    }
}
