package com.taskpulse.app.alert

import android.app.Activity
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Reminder"
        val desc = intent.getStringExtra("TASK_DESC") ?: ""

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
