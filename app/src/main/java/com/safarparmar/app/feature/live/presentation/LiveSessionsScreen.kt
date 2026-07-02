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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
    showTopBar: Boolean = true,
    viewModel: LiveSessionViewModel = hiltViewModel(),
) {
    var selectedFilter by remember { mutableStateOf(LiveSessionFilter.LIVE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val uiState by viewModel.liveSessionsState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val backendStatus = selectedFilter.backendStatus

    LaunchedEffect(courseId, selectedFilter) {
        viewModel.loadSessions(courseId, backendStatus)
    }


    val filteredSessions = remember(uiState.sessions, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) uiState.sessions
        else uiState.sessions.filter { it.title.lowercase().contains(q) }
    }

    val featuredSession = remember(filteredSessions, selectedSessionId) {
        val activeOrScheduled = filteredSessions.filter { it.status == "live" || it.status == "scheduled" }
        selectedSessionId?.let { id -> activeOrScheduled.find { it.id == id } }
            ?: activeOrScheduled.firstOrNull { it.status == "live" }
            ?: activeOrScheduled.firstOrNull()
    }

    val upNextSessions = remember(filteredSessions, featuredSession) {
        featuredSession?.let { featured ->
            filteredSessions.filter { it.id != featured.id }
        } ?: filteredSessions
    }


    val currentDensity = LocalDensity.current
    val lockedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(LocalDensity provides lockedDensity) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    LiveClassroomTopBar(onBack = onBack)
                }
            },

            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {


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
                        title = if (selectedFilter == LiveSessionFilter.COMPLETED) {
                            "No sessions currently."
                        } else {
                            "Parmar sir is not live currently."
                        },
                        subtitle = if (selectedFilter == LiveSessionFilter.COMPLETED) {
                            null
                        } else {
                            "Please check back later."
                        },
                        showClearFilters = searchQuery.isNotBlank(),
                        onClearFilters = {
                            searchQuery = ""
                            selectedFilter = LiveSessionFilter.LIVE
                        },
                    )
                }
            } else if (!uiState.isLoading && upNextSessions.isNotEmpty()) {
                item(key = "up_next_header") {
                    val headerText = if (selectedFilter == LiveSessionFilter.COMPLETED) "Completed Sessions" else "Up Next"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (selectedFilter != LiveSessionFilter.COMPLETED) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                item(key = "up_next_divider") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                items(upNextSessions, key = { "up_next_${it.id}" }) { session ->
                    val isCompleted = session.status == "ended" || session.status == "cancelled"
                    if (isCompleted) {
                        CompletedSessionCard(
                            session = session,
                            onClick = {
                                // Resolve the best YouTube embed URL from data already in the list.
                                // Priority: youtubeEmbedUrl > recordingVideoId > youtubeVideoId
                                val embedUrl = session.youtubeEmbedUrl?.takeIf { it.isNotBlank() }
                                    ?: session.recordingVideoId?.takeIf { it.isNotBlank() }
                                        ?.let { "https://www.youtube.com/embed/$it" }
                                    ?: session.youtubeVideoId?.takeIf { it.isNotBlank() }
                                        ?.let { "https://www.youtube.com/embed/$it" }

                                if (embedUrl != null) {
                                    // Open the in-app fullscreen WebView player directly.
                                    // Skips the intermediate CompletedSessionPlayback screen entirely.
                                    VideoPlayerActivity.start(
                                        context = context,
                                        embedUrl = embedUrl,
                                        videoTitle = session.title,
                                    )
                                } else {
                                    // No video URL — fall back to the session screen
                                    onOpenSession(session.id)
                                }
                            },
                        )
                    } else {
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
}
}
