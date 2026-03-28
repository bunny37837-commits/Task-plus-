package com.taskpulse.app.presentation.aichat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isListening by remember { mutableStateOf(false) }

    val latestSetMessage by rememberUpdatedState(newValue = viewModel::setMessage)
    val latestSetError by rememberUpdatedState(newValue = viewModel::setError)

    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder")
        }
    }

    val speechRecognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val recognizer = speechRecognizer
            if (recognizer == null) {
                latestSetError("Voice input is not available on this device.")
            } else {
                isListening = true
                recognizer.startListening(recognizerIntent)
            }
        } else {
            val shouldShowRationale = context.findActivity()?.let { activity ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
                } else true
            } ?: true

            val message = if (shouldShowRationale) {
                "Microphone permission is needed to use voice input."
            } else {
                "Microphone permission is off. Enable it from app settings to use voice input."
            }
            latestSetError(message)
        }
    }

    DisposableEffect(speechRecognizer) {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            onDispose { }
        } else {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) = Unit

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()

                    if (text.isNullOrBlank()) {
                        latestSetError("I couldn't catch that. Please try again.")
                    } else {
                        latestSetMessage(text)
                    }
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio capture failed. Please try again."
                        SpeechRecognizer.ERROR_CLIENT -> "Voice input was cancelled."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue while recognizing speech. Check your connection and retry."
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try speaking a little closer to the microphone."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy. Please wait a moment and retry."
                        SpeechRecognizer.ERROR_SERVER -> "Voice service is unavailable right now. Please try again shortly."
                        else -> "Voice input failed. Please try again."
                    }
                    latestSetError(message)
                }
            })

            onDispose {
                isListening = false
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            }
        }
    }

    fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context) || speechRecognizer == null) {
            viewModel.setError("Voice input is not available on this device.")
            return
        }

        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            isListening = true
            speechRecognizer.startListening(recognizerIntent)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat to Schedule") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Tell me naturally, e.g. 'Remind me to pay rent every month on 1/4 at 9am high priority'",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::setMessage,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("Your message") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::parseMessage, enabled = !state.isLoading) {
                    Text("Parse with AI")
                }

                IconButton(onClick = ::startVoiceInput, enabled = !state.isLoading && !isListening) {
                    if (isListening) {
                        Icon(Icons.Outlined.MicOff, contentDescription = "Listening")
                    } else {
                        Icon(Icons.Outlined.Mic, contentDescription = "Use voice input")
                    }
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                }
            }

            if (isListening) {
                Text(
                    text = "Listening…",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.feedback?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            state.draft?.let { draft ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Parsed task", style = MaterialTheme.typography.titleMedium)
                        Text("Title: ${draft.title}")
                        Text("Date: ${draft.date.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                        Text("Time: ${draft.time}")
                        Text("Repeat: ${draft.recurrence.label}")
                        Text("Priority: ${draft.priority.label}")
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::createTask, enabled = !state.isLoading) {
                                Text("Create Task")
                            }
                            TextButton(onClick = {
                                viewModel.setMessage("")
                                viewModel.clearDraft()
                            }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
