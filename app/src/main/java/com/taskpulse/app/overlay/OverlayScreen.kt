package com.taskpulse.app.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpulse.app.presentation.ui.theme.*

@Composable
fun OverlayScreen(
    taskId: Long,
    taskTitle: String,
    taskDescription: String,
    onComplete: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var showSnoozeOptions by remember { mutableStateOf(false) }

    // Pulse animation for the bell icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        ) + fadeIn(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)), // Dim the background
            contentAlignment = Alignment.Center,
        ) {
            // Main card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Background),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryGlow)),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        )
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Default.Check, // Bell icon
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "Task Reminder",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f),
                        ) {
                            Text(
                                "NOW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Pulsing bell icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔔", fontSize = 36.sp)
                }

                Spacer(Modifier.height(20.dp))

                // Task title
                Text(
                    text = taskTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                if (taskDescription.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = taskDescription,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Snooze options
                AnimatedVisibility(visible = showSnoozeOptions) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Snooze for...", fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            listOf(5, 10, 15, 30).forEach { min ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { onSnooze(min) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceElevated,
                                    border = BorderStroke(1.dp, BorderColor),
                                ) {
                                    Text("${min}m", fontSize = 13.sp, color = TextPrimary,
                                        textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(60, 120).forEach { min ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { onSnooze(min) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceElevated,
                                    border = BorderStroke(1.dp, BorderColor),
                                ) {
                                    Text(if (min == 60) "1 hr" else "2 hrs", fontSize = 13.sp, color = TextPrimary,
                                        textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Snooze button
                    Surface(
                        modifier = Modifier.weight(1f).clickable { showSnoozeOptions = !showSnoozeOptions },
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, BorderColor),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Snooze, "Snooze", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Snooze", fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }

                    // Complete button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryGlow)))
                            .clickable(onClick = onComplete)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Check, "Complete", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Complete", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
