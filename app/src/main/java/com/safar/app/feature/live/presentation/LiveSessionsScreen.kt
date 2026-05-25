package com.safar.app.feature.live.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.feature.live.model.LiveSession
import com.safar.app.ui.components.SafarEmptyState
import com.safar.app.ui.components.SafarErrorState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LiveSessionsScreen(
    courseId: String,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: LiveSessionViewModel = hiltViewModel(),
) {
    var selectedStatus by remember { mutableStateOf("upcoming") }
    val uiState by viewModel.liveSessionsState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    val backendStatus = when (selectedStatus) {
        "live" -> "live"
        "completed" -> "ended"
        else -> "scheduled"
    }

    LaunchedEffect(courseId, selectedStatus) {
        viewModel.loadSessions(courseId, backendStatus)
    }

    LaunchedEffect(actionState.message) {
        if (actionState.message == "Live class started.") selectedStatus = "live"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Classes") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("upcoming" to "Upcoming", "live" to "Live", "completed" to "Completed").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedStatus == key,
                        onClick = { selectedStatus = key },
                        label = { Text(label) }
                    )
                }
            }

            when {
                uiState.isLoading -> Text("Loading live sessions...", modifier = Modifier.padding(top = 20.dp))
                uiState.errorMessage != null -> SafarErrorState(
                    message = when (uiState.errorCode) {
                        401 -> "Session expired. Please login again."
                        403 -> "You are not allowed to view this course's live sessions."
                        else -> uiState.errorMessage ?: "Could not load live sessions"
                    },
                    onRetry = { viewModel.loadSessions(courseId, backendStatus) }
                )
                else -> {
                    TeacherLiveControlCard(
                        sessions = uiState.sessions,
                        selectedStatus = selectedStatus,
                        actionState = actionState,
                        onStartLive = { sessionId, youtubeUrl ->
                            viewModel.startLiveSession(sessionId, youtubeUrl, courseId, backendStatus)
                        },
                        onEndLive = { sessionId ->
                            viewModel.endLiveSession(sessionId, courseId, backendStatus)
                        },
                        onDismissMessage = viewModel::clearActionMessage,
                    )

                    if (uiState.sessions.isEmpty()) {
                        SafarEmptyState(
                            title = "No live sessions",
                            message = "No sessions found for this filter.",
                        )
                    } else {
                        LiveSessionsList(
                            sessions = uiState.sessions,
                            onOpenSession = onOpenSession
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherLiveControlCard(
    sessions: List<LiveSession>,
    selectedStatus: String,
    actionState: LiveSessionActionState,
    onStartLive: (sessionId: String, youtubeUrl: String) -> Unit,
    onEndLive: (sessionId: String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    val manageableSessions = sessions.filter { it.canManage }
    if (manageableSessions.isEmpty()) return

    val startableSessions = manageableSessions.filter { it.status == "scheduled" }
    val liveSessions = manageableSessions.filter { it.status == "live" }
    if (selectedStatus != "upcoming" && selectedStatus != "live") return
    if (selectedStatus == "upcoming" && startableSessions.isEmpty()) return
    if (selectedStatus == "live" && liveSessions.isEmpty()) return

    var selectedSessionId by remember(manageableSessions) {
        mutableStateOf(manageableSessions.firstOrNull()?.id.orEmpty())
    }
    var youtubeUrl by remember { mutableStateOf("") }
    val activeSessions = if (selectedStatus == "live") liveSessions else startableSessions
    val selectedSession = activeSessions.firstOrNull { it.id == selectedSessionId } ?: activeSessions.first()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (selectedStatus == "live") "Teacher live controls" else "Start today's live class",
                style = MaterialTheme.typography.titleMedium,
            )

            if (activeSessions.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeSessions.forEach { session ->
                        FilterChip(
                            selected = selectedSession.id == session.id,
                            onClick = {
                                selectedSessionId = session.id
                                onDismissMessage()
                            },
                            label = { Text(session.title, maxLines = 1) },
                        )
                    }
                }
            } else {
                Text(selectedSession.title, style = MaterialTheme.typography.bodyMedium)
            }

            if (selectedStatus == "live") {
                OutlinedButton(
                    onClick = { onEndLive(selectedSession.id) },
                    enabled = !actionState.isLoading,
                ) {
                    Text("End Live")
                }
            } else {
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = {
                        youtubeUrl = it
                        if (actionState.errorMessage != null || actionState.message != null) {
                            onDismissMessage()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("YouTube Live URL") },
                    singleLine = true,
                    enabled = !actionState.isLoading,
                )
                Button(
                    onClick = { onStartLive(selectedSession.id, youtubeUrl) },
                    enabled = !actionState.isLoading,
                ) {
                    if (actionState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Start Live")
                }
            }

            actionState.errorMessage?.let { message ->
                Text(
                    text = when (actionState.errorCode) {
                        400 -> "Invalid YouTube Live URL."
                        401 -> "Session expired. Please login again."
                        403 -> "You are not allowed to manage this live class."
                        else -> message
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            actionState.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LiveSessionsList(
    sessions: List<LiveSession>,
    onOpenSession: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenSession(session.id) }) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(session.title, style = MaterialTheme.typography.titleMedium)
                    session.teacherId?.takeIf { it.isNotBlank() }?.let { Text("Teacher: $it") }
                    Text("Scheduled: ${session.scheduledStartAt ?: "-"}")
                    AssistChip(onClick = {}, label = { Text(session.status.uppercase()) })
                    OutlinedButton(onClick = { onOpenSession(session.id) }) {
                        val cta = when {
                            session.status == "live" -> "Join Live"
                            session.status == "ended" && session.isRecordingAvailable -> "View Replay"
                            else -> "View Details"
                        }
                        Text(cta)
                    }
                }
            }
        }
    }
}
