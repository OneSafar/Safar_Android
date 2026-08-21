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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                                    // Passing the session in gives the player its own
                                    // comments pane, so a student no longer has to leave
                                    // the video to say anything.
                                    sessionId = session.id,
                                    sessionStatus = session.status,
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

            // ── Live comments ─────────────────────────────────────────────────
            // Only present while the session is actually broadcasting. Nothing is
            // stored server-side, so once the host ends the session the transcript
            // is gone for everyone — the UI must not imply otherwise.
            LiveChatPanel(
                chatState = chatState,
                sessionStatus = session.status,
                onSend = onSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
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
 * How many people are watching right now, counted per person rather than per
 * connection. Shown to the host and to students alike.
 */
@Composable
private fun LiveViewerBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
        )
        Text(
            text = if (count == 1) "1 watching" else "$count watching",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The live comments panel: transcript plus composer.
 *
 * Chat is a property of the broadcast, not of the session record — it appears when
 * the host goes live and disappears when they end it. Because nothing is persisted,
 * a student joining late sees only what arrives from now on, which is why the empty
 * state says so rather than pretending history is still loading.
 */
@Composable
private fun LiveChatPanel(
    chatState: LiveChatUiState,
    sessionStatus: String,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Follow the conversation as it arrives.
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    // The composer is cleared when chat closes so a half-typed comment doesn't
    // linger and get sent into a session that already ended.
    LaunchedEffect(chatState.isChatOpen) {
        if (!chatState.isChatOpen) draft = ""
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Live comments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (chatState.isConnecting) {
                Text(
                    text = "Connecting…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (sessionStatus == "live") {
                LiveViewerBadge(count = chatState.viewerCount)
            }
        }

        if (!chatState.isChatOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (sessionStatus) {
                        "scheduled" -> "Comments open when the session goes live."
                        "ended" -> "This session has ended, so live comments are closed."
                        "cancelled" -> "This session was cancelled."
                        else -> "Comments are turned off for this session."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        if (chatState.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No comments yet. Say hello!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(chatState.messages) { message ->
                    ChatBubble(message = message)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        val waiting = chatState.cooldownRemainingSeconds > 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= MAX_LIVE_MESSAGE_LENGTH) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        // The wait is stated plainly, so a disabled Send button never
                        // reads as the app being broken.
                        text = if (waiting) {
                            "Wait ${chatState.cooldownRemainingSeconds}s before commenting again"
                        } else {
                            "Add a comment…"
                        },
                    )
                },
                enabled = !waiting,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (draft.isNotBlank() && chatState.canSend) {
                            onSend(draft)
                            draft = ""
                            keyboardController?.hide()
                        }
                    },
                ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (draft.isNotBlank() && chatState.canSend) {
                        onSend(draft)
                        draft = ""
                        keyboardController?.hide()
                    }
                },
                enabled = draft.isNotBlank() && chatState.canSend,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (draft.isNotBlank() && chatState.canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            ) {
                if (waiting) {
                    Text(
                        text = "${chatState.cooldownRemainingSeconds}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (draft.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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

/**
 * One line of live chat, laid out the way a live stream's chat is: a single
 * shared lane, avatar then name then message, all flowing inline.
 *
 * Explicitly not a messaging bubble. Alternating sides is a two-person
 * convention — in a room of a hundred students, "mine on the right, everyone
 * else's on the left" tells a reader nothing and makes a class discussion look
 * like a private conversation.
 */
@Composable
private fun ChatBubble(
    message: LiveChatUiMessage,
) {
    val hostColor = MaterialTheme.colorScheme.primary
    val nameColor = when {
        message.isHost -> MaterialTheme.colorScheme.onPrimary
        message.isMine -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (message.isHost) hostColor
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message.author.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (message.isHost) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(Modifier.width(8.dp))

        // Name and message share one flowing paragraph so long comments wrap
        // under the name instead of being pushed into a narrow column.
        Text(
            text = buildAnnotatedString {
                if (message.isHost) {
                    // The presenter's name is highlighted rather than badged with
                    // a word, so it stays readable at chat density.
                    withStyle(
                        SpanStyle(
                            background = hostColor,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(" ${message.author} ")
                    }
                } else {
                    withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                        append(message.author)
                    }
                }
                append("  ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(message.text)
                }
            },
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
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
