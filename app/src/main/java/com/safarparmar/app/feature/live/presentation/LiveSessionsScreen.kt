package com.safarparmar.app.feature.live.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.live.model.LiveSession
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LiveSessionsScreen(
    courseId: String,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: LiveSessionViewModel = hiltViewModel(),
) {
    var selectedFilter by remember { mutableStateOf(LiveSessionFilter.LIVE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.liveSessionsState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val backendStatus = selectedFilter.backendStatus

    LaunchedEffect(courseId, selectedFilter) {
        viewModel.loadSessions(courseId, backendStatus)
    }

    LaunchedEffect(actionState.message) {
        if (actionState.message == "Live class started.") {
            selectedFilter = LiveSessionFilter.LIVE
        }
    }

    val filteredSessions = remember(uiState.sessions, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) uiState.sessions
        else uiState.sessions.filter { it.title.lowercase().contains(q) }
    }

    val featuredSession = remember(filteredSessions, selectedSessionId) {
        selectedSessionId?.let { id -> filteredSessions.find { it.id == id } }
            ?: filteredSessions.firstOrNull { it.status == "live" }
            ?: filteredSessions.firstOrNull()
    }

    val upNextSessions = remember(filteredSessions, featuredSession) {
        featuredSession?.let { featured ->
            filteredSessions.filter { it.id != featured.id }
        } ?: filteredSessions
    }

    val showTeacherFab by remember(uiState.sessions) {
        derivedStateOf {
            uiState.sessions.any { it.canManage } &&
                selectedFilter != LiveSessionFilter.COMPLETED
        }
    }

    Scaffold(
        topBar = { LiveClassroomTopBar(onBack = onBack) },
        floatingActionButton = {
            if (showTeacherFab) {
                LiveClassroomTeacherFab(
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val errorText = uiState.errorMessage?.let {
            liveSessionsErrorMessage(it, uiState.errorCode)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "teacher_controls") {
                TeacherLiveControlCard(
                    sessions = uiState.sessions,
                    selectedFilter = selectedFilter,
                    actionState = actionState,
                    onStartLive = { sessionId, youtubeUrl ->
                        viewModel.startLiveSession(sessionId, youtubeUrl, courseId, backendStatus)
                    },
                    onEndLive = { sessionId ->
                        viewModel.endLiveSession(sessionId, courseId, backendStatus)
                    },
                    onDismissMessage = viewModel::clearActionMessage,
                )
            }

            item(key = "search") {
                LiveClassroomSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                )
            }

            item(key = "filters") {
                LiveClassroomFilterChips(
                    selected = selectedFilter,
                    onSelected = { selectedFilter = it },
                )
            }

            if (uiState.isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (errorText != null && !uiState.isLoading) {
                item(key = "error_banner") {
                    LiveClassroomErrorBanner(
                        message = errorText,
                        onRetry = { viewModel.loadSessions(courseId, backendStatus) },
                    )
                }
            }

            if (!uiState.isLoading && featuredSession != null) {
                item(key = "hero_${featuredSession.id}") {
                    LiveHeroSessionCard(
                        session = featuredSession,
                        onPlay = { onOpenSession(featuredSession.id) },
                        onJoinChat = { onOpenSession(featuredSession.id) },
                        onShare = { /* share intent can be wired later */ },
                    )
                }
            }

            if (!uiState.isLoading && filteredSessions.isEmpty()) {
                item(key = "empty") {
                    LiveClassroomEmptyState(
                        onClearFilters = {
                            searchQuery = ""
                            selectedFilter = LiveSessionFilter.ALL
                        },
                    )
                }
            } else if (!uiState.isLoading && upNextSessions.isNotEmpty()) {
                item(key = "up_next_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                item(key = "up_next_divider") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                items(upNextSessions, key = { it.id }) { session ->
                    LiveUpNextListItem(
                        session = session,
                        selected = session.id == featuredSession?.id,
                        onClick = { selectedSessionId = session.id },
                    )
                }
            }
        }
    }
}

/**
 * Teacher controls — elevated card + error filled button per M3 cards/buttons assets.
 */
@Composable
private fun TeacherLiveControlCard(
    sessions: List<LiveSession>,
    selectedFilter: LiveSessionFilter,
    actionState: LiveSessionActionState,
    onStartLive: (sessionId: String, youtubeUrl: String) -> Unit,
    onEndLive: (sessionId: String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    val manageableSessions = sessions.filter { it.canManage }
    if (manageableSessions.isEmpty()) return

    val startableSessions = manageableSessions.filter { it.status == "scheduled" }
    val liveSessions = manageableSessions.filter { it.status == "live" }

    val showForFilter = when (selectedFilter) {
        LiveSessionFilter.LIVE -> liveSessions.isNotEmpty() || startableSessions.isNotEmpty()
        LiveSessionFilter.ALL -> startableSessions.isNotEmpty() || liveSessions.isNotEmpty()
        LiveSessionFilter.COMPLETED -> false
    }
    if (!showForFilter) return

    val activeSessions = when {
        selectedFilter == LiveSessionFilter.LIVE && liveSessions.isNotEmpty() -> liveSessions
        liveSessions.isNotEmpty() -> liveSessions
        else -> startableSessions
    }
    if (activeSessions.isEmpty()) return

    var selectedSessionId by remember(activeSessions) {
        mutableStateOf(activeSessions.first().id)
    }
    var youtubeUrl by remember { mutableStateOf("") }
    val selectedSession = activeSessions.firstOrNull { it.id == selectedSessionId } ?: activeSessions.first()
    val isLiveControl = selectedSession.status == "live"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Teacher Controls",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Manage your current session",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (activeSessions.size > 1) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        activeSessions.forEach { session ->
                            FilterChip(
                                selected = session.id == selectedSession.id,
                                onClick = {
                                    selectedSessionId = session.id
                                    onDismissMessage()
                                },
                                label = { Text(session.title, maxLines = 1) },
                            )
                        }
                    }
                }
            }

            if (isLiveControl) {
                Button(
                    onClick = { onEndLive(selectedSession.id) },
                    enabled = !actionState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(50),
                ) {
                    Text("End Live", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (!isLiveControl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) {
                    if (actionState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Start Live", fontWeight = FontWeight.SemiBold)
                }
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        actionState.message?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
