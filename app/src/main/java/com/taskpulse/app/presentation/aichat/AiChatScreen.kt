package com.taskpulse.app.presentation.aichat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskpulse.app.presentation.ui.theme.Background
import com.taskpulse.app.presentation.ui.theme.BorderColor
import com.taskpulse.app.presentation.ui.theme.Danger
import com.taskpulse.app.presentation.ui.theme.PrimaryPurple
import com.taskpulse.app.presentation.ui.theme.SurfaceCard
import com.taskpulse.app.presentation.ui.theme.TextSecondary
import java.io.File
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }

    fun stopRecording(send: Boolean) {
        val currentRecorder = recorder
        if (currentRecorder != null) {
            runCatching { currentRecorder.stop() }
            currentRecorder.release()
        }
        recorder = null
        isRecording = false

        val filePath = recordedFilePath
        val duration = (SystemClock.elapsedRealtime() - recordingStartedAt).coerceAtLeast(0L)
        if (send && !filePath.isNullOrBlank() && duration > 500) {
            viewModel.attachVoiceMessage(filePath = filePath, durationMs = duration)
        } else {
            if (!filePath.isNullOrBlank()) {
                runCatching { File(filePath).delete() }
            }
            if (send) {
                viewModel.attachVoicePlaceholder("Voice note was too short. Hold to record a little longer.")
            }
        }
        recordedFilePath = null
        recordingElapsedMs = 0L
    }

    fun startRecording() {
        val outputFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        runCatching {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            recordedFilePath = outputFile.absolutePath
            recordingStartedAt = SystemClock.elapsedRealtime()
            recordingElapsedMs = 0L
            isRecording = true
        }.onFailure {
            newRecorder.release()
            viewModel.attachVoicePlaceholder("Couldn't start recording on this device right now. Try again or type your request.")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            viewModel.attachVoicePlaceholder("Microphone permission is required for voice notes. Enable it in app settings and try again.")
            return@rememberLauncherForActivityResult
        }
        startRecording()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { stopRecording(send = false) }
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            recordingElapsedMs = (SystemClock.elapsedRealtime() - recordingStartedAt).coerceAtLeast(0L)
            kotlinx.coroutines.delay(250L)
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("AI Scheduler Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCard,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = TextSecondary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { Spacer(Modifier.height(10.dp)) }
                itemsIndexed(state.messages, key = { _, it -> it.id }) { index, msg ->
                    val previous = state.messages.getOrNull(index - 1)
                    val next = state.messages.getOrNull(index + 1)
                    ChatBubble(
                        message = msg,
                        isSending = state.isSending,
                        groupedWithPrevious = previous?.role == msg.role,
                        groupedWithNext = next?.role == msg.role,
                        onCreateTask = { viewModel.createTaskFromDraft(msg.id) },
                    )
                }
                if (state.isAssistantTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceCard)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("AI is typing…", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isRecording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Danger.copy(alpha = 0.08f))
                                .border(1.dp, Danger.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recording… ${formatDuration(recordingElapsedMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Danger,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Tap stop to attach",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = viewModel::setInputText,
                            modifier = Modifier.weight(1f),
                            minLines = 1,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            placeholder = { Text("Type a scheduling message") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )

                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    stopRecording(send = true)
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (granted) {
                                        startRecording()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            enabled = !state.isSending,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isRecording) Danger.copy(alpha = 0.22f) else Color.Transparent,
                                contentColor = if (isRecording) Danger else TextSecondary,
                                disabledContainerColor = BorderColor.copy(alpha = 0.2f),
                                disabledContentColor = TextSecondary,
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, if (isRecording) Danger else BorderColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Outlined.Stop else Icons.Outlined.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Record voice note"
                            )
                        }

                        val canSend = state.inputText.isNotBlank() && !state.isSending && !isRecording
                        IconButton(
                            onClick = viewModel::sendTextMessage,
                            enabled = canSend,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (canSend) PrimaryPurple else BorderColor,
                                contentColor = Color.White,
                                disabledContainerColor = BorderColor,
                                disabledContentColor = TextSecondary,
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSending: Boolean,
    groupedWithPrevious: Boolean,
    groupedWithNext: Boolean,
    onCreateTask: () -> Unit,
) {
    val isUser = message.role == ChatRole.USER
    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = if (groupedWithPrevious) 10.dp else 16.dp,
            bottomStart = 16.dp,
            bottomEnd = if (groupedWithNext) 10.dp else 6.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = if (groupedWithPrevious) 10.dp else 16.dp,
            topEnd = 16.dp,
            bottomStart = if (groupedWithNext) 10.dp else 6.dp,
            bottomEnd = 16.dp,
        )
    }
    val bubbleColor = if (isUser) PrimaryPurple.copy(alpha = 0.24f) else SurfaceCard

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (groupedWithPrevious) 2.dp else 10.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!groupedWithPrevious) {
            Text(
                text = if (isUser) "You" else "AI Assistant",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }

        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(bubbleColor)
                .border(1.dp, if (isUser) PrimaryPurple.copy(alpha = 0.45f) else BorderColor, bubbleShape)
                .padding(12.dp)
                .fillMaxWidth(0.9f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message.text?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurface)
                }

                message.voice?.let { voice ->
                    VoicePlayback(voice.filePath, voice.durationMs)
                }

                message.draft?.let { draft ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Draft ready", fontWeight = FontWeight.SemiBold)
                        Text("${draft.date} • ${draft.time}")
                        Text("${draft.recurrence.label} • ${draft.priority.label}")
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onCreateTask,
                            enabled = !isSending,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Create Task")
                        }
                    }
                }

                Text(
                    text = message.timestamp.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun VoicePlayback(filePath: String, durationMs: Long) {
    var isPlaying by remember(filePath) { mutableStateOf(false) }
    var player by remember(filePath) { mutableStateOf<MediaPlayer?>(null) }
    var playbackPositionMs by remember(filePath) { mutableLongStateOf(0L) }
    var playbackError by remember(filePath) { mutableStateOf<String?>(null) }

    DisposableEffect(filePath) {
        onDispose {
            player?.release()
            player = null
            isPlaying = false
        }
    }

    LaunchedEffect(isPlaying, player) {
        while (isPlaying && player != null) {
            playbackPositionMs = player?.currentPosition?.toLong() ?: 0L
            kotlinx.coroutines.delay(250L)
        }
    }

    val voiceFile = remember(filePath) { File(filePath) }
    if (!voiceFile.exists()) {
        Text(
            text = "Voice note unavailable on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    playbackError = null
                    val current = player
                    if (current != null && isPlaying) {
                        current.pause()
                        isPlaying = false
                    } else {
                        if (current == null) {
                            val newPlayer = MediaPlayer()
                            runCatching {
                                newPlayer.setDataSource(filePath)
                                newPlayer.prepare()
                                newPlayer.setOnCompletionListener {
                                    isPlaying = false
                                    playbackPositionMs = durationMs
                                }
                                newPlayer.start()
                                player = newPlayer
                                isPlaying = true
                            }.onFailure {
                                newPlayer.release()
                                player = null
                                playbackError = "Couldn't play this voice note on this device."
                            }
                        } else {
                            current.start()
                            isPlaying = true
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause voice note" else "Play voice note"
                )
            }
            Text(
                text = "Voice note • ${formatDuration(playbackPositionMs)} / ${formatDuration(durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LinearProgressIndicator(
            progress = {
                if (durationMs <= 0) 0f else (playbackPositionMs.coerceAtMost(durationMs).toFloat() / durationMs.toFloat())
            },
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryPurple,
            trackColor = BorderColor,
        )

        playbackError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = Danger,
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
