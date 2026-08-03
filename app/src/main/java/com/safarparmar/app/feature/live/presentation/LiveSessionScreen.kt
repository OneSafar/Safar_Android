package com.safarparmar.app.feature.live.presentation

import androidx.compose.animation.AnimatedVisibility
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale

import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.ui.components.SafarErrorState

import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LiveSessionScreen(
    sessionId: String,
    onBack: () -> Unit,
    currentRoute: String = Routes.LIVE_SESSION,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: LiveSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.liveSessionState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.liveSessionsState.collectAsStateWithLifecycle()
    val chatState by viewModel.liveChatState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val isCompletedSession = uiState.session?.let {
        it.status == "ended" || it.status == "cancelled"
    } ?: false

    val isSessionLoaded = uiState.session != null

    // Load session data on entry
    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Load other completed sessions if current session is completed
    val session = uiState.session
    LaunchedEffect(session?.id, session?.courseId, isCompletedSession) {
        if (session != null && isCompletedSession) {
            viewModel.loadSessions(session.courseId, "ended")
        }
    }

    // Only join socket for non-completed sessions, AFTER session data is loaded
    LaunchedEffect(sessionId, isSessionLoaded, isCompletedSession) {
        if (isSessionLoaded && !isCompletedSession) {
            viewModel.joinLiveSession(sessionId)
        }
    }

    // Leave socket room when screen is removed from composition
    DisposableEffect(sessionId) {
        onDispose {
            viewModel.leaveLiveSession()
        }
    }

    // Show socket errors as snackbar (dismissible)
    LaunchedEffect(chatState.socketError) {
        val err = chatState.socketError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        viewModel.clearLiveChatError()
    }

    val currentDensity = LocalDensity.current
    val lockedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(LocalDensity provides lockedDensity) {
        SafarDrawerScaffold(
            title = uiState.session?.title ?: "Live Session",
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
        topBarActions = {
            IconButton(onClick = { viewModel.loadSession(sessionId) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // We use an inner Scaffold just for the Snackbar, overlaying the content.
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            ) { innerPadding ->
                // The inner content below
                when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> SafarErrorState(
                message = when (uiState.errorCode) {
                    401 -> "Your session has expired. Please sign in again."
                    403 -> "You don't have access to this live session."
                    404 -> "This live session could not be found."
                    in 500..599 -> "We're experiencing technical difficulties. Please try again shortly."
                    else -> uiState.errorMessage ?: "Failed to load session."
                },
                onRetry = { viewModel.loadSession(sessionId) },
                modifier = Modifier.padding(padding)
            )

            uiState.session == null -> SafarErrorState(
                message = "Session not found",
                onRetry = { viewModel.loadSession(sessionId) },
                modifier = Modifier.padding(padding)
            )

            else -> {
                val session = uiState.session!!
                if (isCompletedSession) {
                    CompletedSessionPlayback(
                        session = session,
                        completedSessions = sessionsState.sessions,
                        onSessionClick = { nextSessionId ->
                            onNavigate(Routes.liveSession(nextSessionId))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                } else {
                    LiveClassPlayerChat(
                        session = session,
                        chatState = chatState,
                        onSend = { text -> viewModel.sendLiveMessage(text) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
            }
        }
    }
    }
}
}

@Composable
private fun LiveClassPlayerChat(
    session: LiveSession,
    chatState: LiveChatUiState,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isPlaying by remember { mutableStateOf(false) }
    val embedUrl = remember(session.id) { resolveEmbedUrl(session) }

    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // ── Video Preview Cover (16:9 Black Screen / Thumbnail with Play Button) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                val videoId = extractVideoId(session)
                val thumbnailUrl = videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        // Straight to the dedicated fullscreen player. Setting
                        // isPlaying swapped in an INLINE 16:9 WebView, which
                        // rendered a blank white box in portrait and forced the
                        // student to find YouTube's own fullscreen button before
                        // they could see anything. Same behaviour as the sessions
                        // list, which already launches this activity directly.
                        .clickable {
                            val url = embedUrl
                            if (url != null) {
                                VideoPlayerActivity.start(
                                    context = context,
                                    embedUrl = url,
                                    videoTitle = session.title.orEmpty(),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(com.safarparmar.app.R.drawable.dhyan_session_placeholder),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }

                    // Center Play Button Overlay
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video in Landscape Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // LIVE badge
                    if (session.status == "live") {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    ),
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = session.title,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            // Session Details below video card
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = session.title.ifBlank { "Live Session" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!session.description.isNullOrBlank()) {
                    Text(
                        text = session.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Full-screen Video Player Overlay (Landscape Mode) ────────────────
        if (isPlaying && embedUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                YouTubePlayerWebView(
                    embedUrl = embedUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating back button — exits full-screen landscape back to portrait
                IconButton(
                    onClick = {
                        isPlaying = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Resolves the best embed URL for this session.
 * Prefers youtubeEmbedUrl; falls back to constructing one from recordingVideoId.
 */
private fun resolveEmbedUrl(session: LiveSession): String? {
    session.youtubeEmbedUrl?.takeIf { it.isNotBlank() }?.let { return it }
    session.recordingVideoId?.takeIf { it.isNotBlank() }?.let { videoId ->
        return "https://www.youtube.com/embed/$videoId"
    }
    return null
}

/**
 * Recording-only playback screen for completed sessions.
 * Shows the YouTube recording player, session title, date, and status — no chat.
 */
@Composable
private fun CompletedSessionPlayback(
    session: LiveSession,
    completedSessions: List<LiveSession>,
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isPlaying by remember { mutableStateOf(false) }
    // Resolve embed URL once — used by both the thumbnail clickable and the player overlay.
    val embedUrl = remember(session.id) { resolveEmbedUrl(session) }

    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WHY Box and NOT Dialog:
    // A Compose Dialog creates a separate Android Window. The WebView's internal
    // SurfaceView renders video by "punching a transparent hole" through the
    // current Window's surface and compositing video frames on a separate hardware
    // layer behind it. Inside a floating Dialog Window, that hole ends up revealing
    // the main app window behind the Dialog — not the video frames — making the
    // video area appear blank/white while audio and controls still work.
    //
    // By keeping the overlay as a Box sibling in the SAME Compose composition
    // (and therefore the same Android Window), the SurfaceView correctly punches
    // its hole through our single window surface and video becomes visible.
    // ─────────────────────────────────────────────────────────────────────────
    Box(modifier = modifier) {
        // ── Main Content (thumbnail + playlist) ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // ── Video Thumbnail / Play Button ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                val videoId = extractVideoId(session)
                val thumbnailUrl = videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        // Straight to the dedicated fullscreen player. Setting
                        // isPlaying swapped in an INLINE 16:9 WebView, which
                        // rendered a blank white box in portrait and forced the
                        // student to find YouTube's own fullscreen button before
                        // they could see anything. Same behaviour as the sessions
                        // list, which already launches this activity directly.
                        .clickable {
                            val url = embedUrl
                            if (url != null) {
                                VideoPlayerActivity.start(
                                    context = context,
                                    embedUrl = url,
                                    videoTitle = session.title.orEmpty(),
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Video Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Semi-transparent overlay to darken the thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                    }

                    // Center Play Button Overlay
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // ── Session Info + Other Completed Sessions List ───────────────────
            val playlistSessions = remember(completedSessions, session) {
                if (completedSessions.isEmpty()) {
                    listOf(session)
                } else {
                    if (completedSessions.any { it.id == session.id }) {
                        completedSessions
                    } else {
                        listOf(session) + completedSessions
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Current Session Info
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = session.title.ifBlank { "Untitled Session" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val statusLabel = when (session.status) {
                                "ended" -> "Completed"
                                "cancelled" -> "Cancelled"
                                else -> session.status.replaceFirstChar { it.uppercase() }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        text = statusLabel.uppercase(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (!session.description.isNullOrBlank()) {
                                Text(
                                    text = session.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Playlist / Completed Sessions
                item {
                    Text(
                        text = "Playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }

                items(playlistSessions, key = { "playlist_${it.id}" }) { otherSession ->
                    val isCurrent = otherSession.id == session.id
                    CompletedSessionCard(
                        session = otherSession,
                        isNowPlaying = isCurrent,
                        onClick = { onSessionClick(otherSession.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Full-screen Video Player Overlay ──────────────────────────────────
        // Rendered as a same-window Box sibling (not a Dialog) so the WebView's
        // internal SurfaceView can display video frames correctly.
        if (isPlaying && embedUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                YouTubePlayerWebView(
                    embedUrl = embedUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating back button — exits the player
                IconButton(
                    onClick = {
                        isPlaying = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun YouTubePlaceholder(status: String = "") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color(0xFF1F2937)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (status == "ended") Icons.Default.Warning else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (status) {
                    "ended" -> "This session has ended."
                    "scheduled" -> "This session hasn't started yet."
                    "cancelled" -> "This session was cancelled."
                    else -> "Waiting for the stream to begin…"
                },
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: LiveChatUiMessage,
) {
    val accent = if (message.isMine) MaterialTheme.colorScheme.primaryContainer
                 else MaterialTheme.colorScheme.secondaryContainer
    val initials = message.author.take(2).uppercase()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        if (!message.isMine) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp),
                )
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (message.isMine) "You" else message.author,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Extracts the 11-character YouTube video ID from the session's recording properties.
 */
private fun extractVideoId(session: LiveSession): String? {
    session.recordingVideoId?.takeIf { it.isNotBlank() }?.let { return it }
    val url = session.youtubeEmbedUrl ?: return null
    if (url.isBlank()) return null

    return try {
        val regex = "(?i)(?:youtube\\.com\\/(?:[^\\/]+\\/.*\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/ ]{11})".toRegex()
        regex.find(url)?.groupValues?.get(1)
    } catch (_: Exception) {
        null
    }
}
