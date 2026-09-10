package com.safarparmar.app.feature.live.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.ui.theme.LoraFontFamily
import coil.compose.AsyncImage
import kotlinx.coroutines.launch


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LiveSessionsScreen(
    courseId: String,
    initialView: String = "live",
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onOpenCourses: () -> Unit = onBack,
    showTopBar: Boolean = true,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    viewModel: LiveSessionViewModel = hiltViewModel(),
) {
    val isDark = isDarkTheme

    var selectedFilter by rememberSaveable(initialView) {
        mutableStateOf(
            when (initialView.lowercase()) {
                "recordings" -> LiveSessionFilter.COMPLETED
                "join" -> LiveSessionFilter.JOIN
                else -> LiveSessionFilter.LIVE
            },
        )
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isReminderSet by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val uiState by viewModel.liveSessionsState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val openTelegramCommunity: () -> Unit = {
        val telegramUrl = uiState.telegramCommunityUrl
        if (telegramUrl.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar("The Telegram group link is unavailable right now.")
            }
        } else {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)))
            }.onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar("Could not open Telegram on this device.")
                }
            }
            Unit
        }
    }

    // Load all sessions so tab switching between Live and Completed is instantaneous
    LaunchedEffect(courseId) {
        viewModel.loadSessions(courseId, status = null)
    }

    val allSessions = uiState.sessions

    val activeLiveSessions = remember(allSessions) {
        allSessions.filter { it.status.equals("live", ignoreCase = true) }
    }
    val scheduledSessions = remember(allSessions) {
        allSessions.filter { it.status.equals("scheduled", ignoreCase = true) }
    }
    val completedSessions = remember(allSessions) {
        allSessions.filter {
            it.status.equals("ended", ignoreCase = true) || it.status.equals("cancelled", ignoreCase = true)
        }
    }

    val liveSession = activeLiveSessions.firstOrNull()
    val nextScheduledSession = scheduledSessions.firstOrNull()

    val filteredCompletedSessions = remember(completedSessions, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) completedSessions
        else completedSessions.filter { it.title.lowercase().contains(q) }
    }

    val onSessionClick: (LiveSession) -> Unit = { session ->
        val embedUrl = session.youtubeEmbedUrl?.takeIf { it.isNotBlank() }
            ?: session.recordingVideoId?.takeIf { it.isNotBlank() }?.let { "https://www.youtube.com/embed/$it" }
            ?: session.youtubeVideoId?.takeIf { it.isNotBlank() }?.let { "https://www.youtube.com/embed/$it" }

        if (embedUrl != null) {
            VideoPlayerActivity.start(
                context = context,
                embedUrl = embedUrl,
                videoTitle = session.title,
            )
        } else {
            onOpenSession(session.id)
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                LiveClassroomTopBar(onBack = onBack)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LiveThemeColors.background(isDark),
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "dhyan_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = LiveThemeColors.cardBorder(isDark),
                            shape = RoundedCornerShape(22.dp),
                        ),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveThemeColors.card(isDark)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.dhyan_live_hero),
                            contentDescription = "Dhyan Live",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Dhyan Live",
                                fontFamily = LoraFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiveThemeColors.textPrimary(isDark),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Join Parmar sir live for yoga and meditation.",
                                fontSize = 13.sp,
                                color = LiveThemeColors.textSecondary(isDark),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // 1. Search Bar ("Search sessions")
            item(key = "search") {
                LiveSessionSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    isDarkTheme = isDark,
                )
            }

            // 2. Segmented Tabs: [ Live ] [ Completed ]
            item(key = "tabs") {
                LiveSessionSegmentedTabs(
                    selected = selectedFilter,
                    onSelected = { selectedFilter = it },
                    isDarkTheme = isDark,
                )
            }

            // Loading state
            if (uiState.isLoading && allSessions.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = LiveThemeColors.primary(isDark),
                        )
                    }
                }
            }

            // 3. Tab: LIVE
            if (selectedFilter == LiveSessionFilter.LIVE) {
                // If a session is actively broadcasting
                if (liveSession != null) {
                    item(key = "live_hero_${liveSession.id}") {
                        LiveHeroSessionCard(
                            session = liveSession,
                            onPlay = { onOpenSession(liveSession.id) },
                            onJoinChat = { onOpenSession(liveSession.id) },
                            isDarkTheme = isDark,
                        )
                    }
                } else if (!uiState.isLoading) {
                    // Parmar Sir isn't live right now card
                    item(key = "not_live_card") {
                        TeacherNotLiveCard(
                            nextSession = nextScheduledSession,
                            isReminderSet = isReminderSet,
                            onToggleReminder = {
                                isReminderSet = !isReminderSet
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (isReminderSet) {
                                            "Reminder set! We'll notify you when Parmar sir goes live."
                                        } else {
                                            "Reminder cancelled."
                                        },
                                    )
                                }
                            },
                            isDarkTheme = isDark,
                        )
                    }
                }

                // 4. Section: "Missed a session? Catch up below"
                if (filteredCompletedSessions.isNotEmpty()) {
                    item(key = "missed_header") {
                        Text(
                            text = "Missed a session? Catch up below",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiveThemeColors.textPrimary(isDark),
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        )
                    }

                    items(filteredCompletedSessions, key = { "catch_up_${it.id}" }) { session ->
                        CompletedSessionCatchUpCard(
                            session = session,
                            onClick = { onSessionClick(session) },
                            isDarkTheme = isDark,
                        )
                    }
                }
            } else if (selectedFilter == LiveSessionFilter.COMPLETED) {
                // 5. Tab: COMPLETED
                item(key = "completed_header") {
                    Text(
                        text = "Dhyan recordings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiveThemeColors.textPrimary(isDark),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }

                if (filteredCompletedSessions.isEmpty() && !uiState.isLoading) {
                    item(key = "empty_completed") {
                        LiveClassroomEmptyState(
                            title = if (searchQuery.isNotBlank()) "No sessions found" else "No completed sessions yet",
                            subtitle = if (searchQuery.isNotBlank()) "Try a different search keyword" else "Sessions will appear here once ended.",
                            showClearFilters = searchQuery.isNotBlank(),
                            onClearFilters = { searchQuery = "" },
                            isDarkTheme = isDark,
                        )
                    }
                } else {
                    items(filteredCompletedSessions, key = { "completed_${it.id}" }) { session ->
                        CompletedSessionCatchUpCard(
                            session = session,
                            onClick = { onSessionClick(session) },
                            isDarkTheme = isDark,
                        )
                    }
                }
            } else {
                item(key = "join_us") {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveThemeColors.card(isDark)),
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Join the Safar Dhyan community", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LiveThemeColors.textPrimary(isDark))
                            Text(
                                "Join our community to receive friendly reminders and all the latest updates about Safar's yoga and meditation courses.",
                                fontSize = 14.sp,
                                color = LiveThemeColors.textSecondary(isDark),
                            )
                            Button(onClick = openTelegramCommunity) {
                                Text("Join our Telegram group")
                            }
                        }
                    }
                }
            }
        }
    }
}
