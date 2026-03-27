package com.taskpulse.app.presentation.aichat

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                }
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
