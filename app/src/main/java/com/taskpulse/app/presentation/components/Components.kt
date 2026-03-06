package com.taskpulse.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskpulse.app.domain.model.Priority
import com.taskpulse.app.domain.model.Task
import com.taskpulse.app.presentation.ui.theme.*
import java.time.format.DateTimeFormatter

// ─── GradientButton ──────────────────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryGlow)),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) gradient else Brush.horizontalGradient(listOf(TextMuted, TextMuted)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
    }
}

// ─── PriorityIndicator ───────────────────────────────────────────────────────

@Composable
fun PriorityChip(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = priority.toColor()
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.2f) else SurfaceElevated,
        border = if (selected) BorderStroke(1.5.dp, color) else BorderStroke(1.dp, BorderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = priority.label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) color else TextSecondary,
            )
        }
    }
}

fun Priority.toColor() = when (this) {
    Priority.LOW -> PriorityLow
    Priority.MEDIUM -> PriorityMedium
    Priority.HIGH -> PriorityHigh
    Priority.CRITICAL -> PriorityCritical
}

// ─── TaskCard ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priorityColor = task.priority.toColor()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                SwipeToDismissBoxValue.StartToEnd -> { onComplete(); true }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Success.copy(alpha = 0.2f)
                    SwipeToDismissBoxValue.EndToStart -> Danger.copy(alpha = 0.2f)
                    else -> SurfaceCard
                }, label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (direction == SwipeToDismissBoxValue.StartToEnd) Success else Danger,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        },
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left priority bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            color = priorityColor,
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                        ),
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .weight(1f),
                ) {
                    // Category chip
                    task.category?.let { cat ->
                        val catColor = Color(android.graphics.Color.parseColor(cat.colorHex))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${task.scheduledDateTime.format(timeFormatter)} · ${task.scheduledDateTime.format(dateFormatter)}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
            }
        }
    }
}

// ─── SectionHeader ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

// ─── EmptyState ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, color = TextSecondary, fontSize = 15.sp)
        }
    }
}
