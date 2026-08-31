package com.safarparmar.app.ui.studycircle

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safarparmar.app.data.remote.dto.PublicStudyCircleDto
import com.safarparmar.app.data.remote.dto.StudyCircleDetailDto
import com.safarparmar.app.data.remote.dto.StudyCircleLeaderboardEntryDto
import com.safarparmar.app.data.remote.dto.StudyCircleMemberDto
import com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MAX_CIRCLE_NAME_LENGTH = 40

// ── Unified Design Tokens ───────────────────────────────────────────────────

/** Primary color: Deep Royal Purple/Blue */
private val CirclePrimary: Color
    @Composable get() = if (isPlannerDark) Color(0xFFC084FC) else Color(0xFF581C87)

private val CirclePrimaryButton: Color
    @Composable get() = if (isPlannerDark) Color(0xFF7C3AED) else Color(0xFF581C87)

/** Secondary color: Deep Green for Live & Active indicators */
private val DeepGreen: Color
    @Composable get() = if (isPlannerDark) Color(0xFF4ADE80) else Color(0xFF15803D)

/** Dedicated SAFAR Official accent: deep green in light mode, legible mint in dark mode. */
private val OfficialGreen: Color
    @Composable get() = if (isPlannerDark) Color(0xFF34D399) else Color(0xFF0B5C3A)

private val OfficialSurface: Color
    @Composable get() = if (isPlannerDark) Color(0xFF10261D) else Color(0xFFF0F7F3)

/** Text-only owner highlight in Deep Orange */
private val DeepOrange: Color
    @Composable get() = if (isPlannerDark) Color(0xFFFB923C) else Color(0xFFEA580C)

// ── Live Animation Components ───────────────────────────────────────────────

@Composable
fun LivePulsingAvatarRing(
    isLive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!isLive) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
        return
    }

    val liveColor = DeepGreen
    val infiniteTransition = rememberInfiniteTransition(label = "avatarLiveRing")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "avatarPulseProgress",
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.padding(3.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseRadius = size.minDimension / 2f
            val extraRadius = 4.dp.toPx() * pulseProgress
            val strokeWidth = (2.dp.toPx() * (1f - pulseProgress)).coerceAtLeast(0f)

            if (strokeWidth > 0.1f) {
                drawCircle(
                    color = liveColor,
                    radius = baseRadius + extraRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                    alpha = ((1f - pulseProgress) * 0.7f).coerceIn(0f, 1f),
                )
            }
            // Inner crisp accent ring
            drawCircle(
                color = liveColor,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
                alpha = 0.9f,
            )
        }
        content()
    }
}

@Composable
fun LivePulseDot(modifier: Modifier = Modifier, size: Int = 8) {
    val liveColor = DeepGreen
    val infiniteTransition = rememberInfiniteTransition(label = "liveDotPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dotPulseProgress",
    )

    Box(
        modifier = modifier.size((size + 6).dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val baseRadius = (size.dp.toPx() / 2f)
            val extraRadius = 3.dp.toPx() * pulseProgress
            val strokeWidth = (1.5.dp.toPx() * (1f - pulseProgress)).coerceAtLeast(0f)

            if (strokeWidth > 0.1f) {
                drawCircle(
                    color = liveColor,
                    radius = baseRadius + extraRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                    alpha = ((1f - pulseProgress) * 0.7f).coerceIn(0f, 1f),
                )
            }
            drawCircle(
                color = liveColor,
                radius = baseRadius,
                center = center,
            )
        }
    }
}

@Composable
fun LiveFocusingBadge(modifier: Modifier = Modifier) {
    val liveColor = DeepGreen
    Surface(
        modifier = modifier,
        color = liveColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, liveColor.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            LivePulseDot(size = 6)
            Text(
                "Focusing",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = liveColor,
            )
        }
    }
}

// ── Hub Screen Root ─────────────────────────────────────────────────────────

@Composable
fun StudyCircleScreen(
    currentRoute: String = Routes.STUDY_CIRCLES,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: StudyCircleViewModel = hiltViewModel(),
) {
    val state by viewModel.hub.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CircleDialog?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(CircleListTab.All) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadHub(refresh = true)
    }

    val pendingDmRequests by viewModel.pendingDmRequests.collectAsStateWithLifecycle()
    var showConnectRequestsSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    if (showConnectRequestsSheet) {
        StudyCircleConnectRequestsSheet(
            pendingRequests = pendingDmRequests,
            onAccept = { fromUserId, userName ->
                viewModel.acceptDmRequest(fromUserId) { uid, uname ->
                    showConnectRequestsSheet = false
                    onNavigate(
                        Routes.dmChatDirect(
                            targetUserId = uid,
                            targetUserName = uname,
                            contextPreview = "Study Circle connection",
                            autoRequest = false,
                        )
                    )
                }
            },
            onDecline = { fromUserId ->
                viewModel.declineDmRequest(fromUserId)
            },
            onOpenDmChat = {
                onNavigate(Routes.DM_CHAT)
            },
            onDismiss = { showConnectRequestsSheet = false },
        )
    }

    SafarDrawerScaffold(
        title = "Study Circle",
        subtitle = null,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        topBarActions = {
            val pendingCount = pendingDmRequests.size
            IconButton(onClick = { showConnectRequestsSheet = true }) {
                BadgedBox(
                    badge = {
                        if (pendingCount > 0) {
                            Badge(
                                containerColor = Color(0xFFF97316),
                                contentColor = Color.White,
                            ) {
                                Text("$pendingCount", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (pendingCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = "Connection requests",
                        tint = if (pendingCount > 0) CirclePrimaryButton else PlannerFlatColors.TextDark,
                    )
                }
            }
            IconButton(onClick = { viewModel.loadHub(refresh = true) }, enabled = !state.refreshing) {
                if (state.refreshing) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = CirclePrimary)
                else Icon(Icons.Default.Refresh, "Refresh circles", tint = PlannerFlatColors.TextDark)
            }
        },
        useGlassTopBar = false,
        containerColor = PlannerFlatColors.BgCream,
    ) { padding ->
        val atLimit = state.circles.size >= 5
        val creationAllowed = state.creationEligibility.allowed
        val joinedIds = remember(state.circles) { state.circles.mapTo(mutableSetOf()) { it.id } }
        val availablePublicCircles = remember(state.publicCircles, joinedIds) {
            state.publicCircles.filterNot { it.id in joinedIds }
        }
        var searchQuery by rememberSaveable { mutableStateOf("") }
        var visiblePublicLimit by rememberSaveable { mutableIntStateOf(15) }
        val filteredCircles = remember(state.circles, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) state.circles
            else state.circles.filter { it.name.lowercase().contains(q) }
        }
        val filteredPublicCircles = remember(availablePublicCircles, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isBlank()) availablePublicCircles
            else availablePublicCircles.filter { it.name.lowercase().contains(q) }
        }
        val displayedPublicCircles = remember(filteredPublicCircles, visiblePublicLimit, searchQuery) {
            if (searchQuery.isNotBlank()) filteredPublicCircles
            else filteredPublicCircles.take(visiblePublicLimit)
        }
        val allCount = state.circles.size + (if (state.publicTotal > 0) state.publicTotal else availablePublicCircles.size)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PlannerFlatColors.BgCream),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StudyCircleTopButtons(
                    atLimit = atLimit,
                    creationAllowed = creationAllowed,
                    onJoin = { dialog = CircleDialog.Join },
                    onCreate = { dialog = CircleDialog.Create },
                )
            }
            item { CircleLimitInfoRow(atLimit, state.creationEligibility.requiredStreak, creationAllowed) }
            if (state.pinnedCircles.isNotEmpty()) {
                item {
                    OfficialCirclesSection(
                        pinnedCircles = state.pinnedCircles,
                        joinedIds = joinedIds,
                        busyId = state.busyId,
                        onJoin = { circle ->
                            viewModel.joinPublic(circle) { onNavigate(Routes.studyCircleDetail(it)) }
                        },
                        onOpen = { circleId ->
                            onNavigate(Routes.studyCircleDetail(circleId))
                        },
                    )
                }
            }
            item {
                CircleTabsHeader(
                    selectedTab = selectedTab,
                    allCount = allCount,
                    yoursCount = state.circles.size,
                    onSelectTab = { selectedTab = it },
                )
            }
            if (selectedTab == CircleListTab.All) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search public circles by name", fontSize = 14.sp, color = PlannerFlatColors.TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PlannerFlatColors.TextMuted) },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = PlannerFlatColors.TextMuted)
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PlannerFlatColors.CardWhite,
                            unfocusedContainerColor = PlannerFlatColors.CardWhite,
                            unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                            focusedBorderColor = CirclePrimary,
                            cursorColor = CirclePrimary,
                        ),
                    )
                }
            }
            if (state.loading) {
                item { CircleLoadingState() }
            } else if (state.error != null) {
                item { CircleErrorState(state.error.orEmpty(), viewModel::loadHub) }
            } else {
                if (selectedTab == CircleListTab.All) {
                    if (allCount == 0 && state.publicCircles.isEmpty()) {
                        item { DashedStyleEmptyCard("No study circles are available yet.") }
                    } else if (searchQuery.isNotBlank() && filteredCircles.isEmpty() && filteredPublicCircles.isEmpty()) {
                        item { DashedStyleEmptyCard("No circles match \"$searchQuery\".") }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                                border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                                elevation = CardDefaults.cardElevation(0.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    var isFirst = true
                                    filteredCircles.forEach { circle ->
                                        if (!isFirst) {
                                            HorizontalDivider(
                                                color = PlannerFlatColors.BorderSoft.copy(alpha = 0.6f),
                                                thickness = 1.dp,
                                            )
                                        }
                                        isFirst = false
                                        MyCircleRow(circle) { onNavigate(Routes.studyCircleDetail(circle.id)) }
                                    }
                                    displayedPublicCircles.forEachIndexed { index, circle ->
                                        if (!isFirst) {
                                            HorizontalDivider(
                                                color = PlannerFlatColors.BorderSoft.copy(alpha = 0.6f),
                                                thickness = 1.dp,
                                            )
                                        }
                                        isFirst = false
                                        PublicCircleRow(
                                            circle = circle,
                                            rank = index + 1,
                                            enabled = !atLimit,
                                            busy = state.busyId == circle.id,
                                            onJoin = { viewModel.joinPublic(circle) { onNavigate(Routes.studyCircleDetail(it)) } },
                                            onOpen = { onNavigate(Routes.studyCircleDetail(circle.id)) },
                                        )
                                    }
                                    if (searchQuery.isBlank() && filteredPublicCircles.size > displayedPublicCircles.size) {
                                        HorizontalDivider(
                                            color = PlannerFlatColors.BorderSoft.copy(alpha = 0.6f),
                                            thickness = 1.dp,
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { visiblePublicLimit += 15 }
                                                .padding(vertical = 13.dp, horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = CirclePrimary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Show more circles",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CirclePrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (state.circles.isEmpty()) {
                        item { DashedStyleEmptyCard("You have not joined a circle yet.") }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                                border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                                elevation = CardDefaults.cardElevation(0.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    state.circles.forEachIndexed { index, circle ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = PlannerFlatColors.BorderSoft.copy(alpha = 0.6f),
                                                thickness = 1.dp,
                                            )
                                        }
                                        MyCircleRow(circle) { onNavigate(Routes.studyCircleDetail(circle.id)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    dialog?.let { mode ->
        StudyCircleInputDialog(
            mode = mode,
            busy = state.busyId != null,
            onDismiss = { dialog = null },
            onSubmit = { value, visibility ->
                val open: (String) -> Unit = { id -> dialog = null; onNavigate(Routes.studyCircleDetail(id)) }
                if (mode == CircleDialog.Create) viewModel.createCircle(value, visibility, open)
                else viewModel.joinWithCode(value, open)
            },
        )
    }
}

private enum class CircleDialog { Create, Join }
private enum class CircleListTab { All, Yours }

// ── Action Buttons & Header Controls ────────────────────────────────────────

@Composable
private fun StudyCircleTopButtons(
    atLimit: Boolean,
    creationAllowed: Boolean,
    onJoin: () -> Unit,
    onCreate: () -> Unit,
) {
    val primary = CirclePrimary
    val primaryBtn = CirclePrimaryButton

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onJoin,
            enabled = !atLimit,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = primary,
                disabledContentColor = primary.copy(alpha = 0.38f),
            ),
        ) {
            Icon(Icons.Default.Key, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Join with code", maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Button(
            onClick = onCreate,
            enabled = !atLimit && creationAllowed,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBtn,
                contentColor = Color.White,
                disabledContainerColor = primaryBtn.copy(alpha = 0.38f),
                disabledContentColor = Color.White.copy(alpha = 0.38f),
            ),
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Create circle", maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CircleLimitInfoRow(
    atLimit: Boolean,
    requiredStreak: Int,
    creationAllowed: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = PlannerFlatColors.TextMuted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                atLimit -> "5-circle limit reached. Leave one to add another."
                !creationAllowed -> "$requiredStreak-day check-in streak needed to create circles."
                else -> "Join up to 5 circles; private circles allow 100 members."
            },
            color = PlannerFlatColors.TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CircleTabsHeader(
    selectedTab: CircleListTab,
    allCount: Int,
    yoursCount: Int,
    onSelectTab: (CircleListTab) -> Unit,
) {
    val primary = CirclePrimary
    val textMuted = PlannerFlatColors.TextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isAll = selectedTab == CircleListTab.All
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelectTab(CircleListTab.All) },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "All ($allCount)",
                    fontSize = 14.5.sp,
                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAll) primary else textMuted,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(36.dp)
                        .height(2.5.dp)
                        .background(if (isAll) primary else Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }

        val isYours = selectedTab == CircleListTab.Yours
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSelectTab(CircleListTab.Yours) },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Yours ($yoursCount)",
                    fontSize = 14.5.sp,
                    fontWeight = if (isYours) FontWeight.Bold else FontWeight.Medium,
                    color = if (isYours) primary else textMuted,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(36.dp)
                        .height(2.5.dp)
                        .background(if (isYours) primary else Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

// ── Official & Pinned Circles Components ─────────────────────────────────────

@Composable
fun OfficialBadge(modifier: Modifier = Modifier) {
    val official = OfficialGreen
    Surface(
        modifier = modifier,
        color = official.copy(alpha = 0.10f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, official.copy(alpha = 0.30f)),
    ) {
        Text(
            text = "Official",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = official,
        )
    }
}

@Composable
private fun OfficialCirclesSection(
    pinnedCircles: List<PublicStudyCircleDto>,
    joinedIds: Set<String>,
    busyId: String?,
    onJoin: (PublicStudyCircleDto) -> Unit,
    onOpen: (String) -> Unit,
) {
    val official = OfficialGreen

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(official.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = official,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Text(
                    text = "Our Official Circles",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = official,
                )
            }
            Text(
                text = "${pinnedCircles.size}/5 Pinned",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = official,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OfficialSurface),
            border = BorderStroke(1.dp, official.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                pinnedCircles.forEachIndexed { index, circle ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = official.copy(alpha = 0.18f),
                            thickness = 1.dp,
                        )
                    }
                    OfficialCircleRow(
                        circle = circle,
                        isJoined = circle.id in joinedIds,
                        busy = busyId == circle.id,
                        onJoin = { onJoin(circle) },
                        onOpen = { onOpen(circle.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficialCircleRow(
    circle: PublicStudyCircleDto,
    isJoined: Boolean,
    busy: Boolean,
    onJoin: () -> Unit,
    onOpen: () -> Unit,
) {
    val official = OfficialGreen

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isJoined) onOpen() else onJoin() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(official.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Official Circle",
                tint = official,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = circle.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = official,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                OfficialBadge()
            }
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${circle.memberCount} members · Official",
                    color = PlannerFlatColors.TextMuted,
                    fontSize = 12.sp,
                )
                if (circle.focusingCount > 0) {
                    Text(" · ", color = PlannerFlatColors.TextMuted, fontSize = 12.sp)
                    LivePulseDot(size = 6)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${circle.focusingCount} live",
                        color = DeepGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        if (isJoined) {
            OutlinedButton(
                onClick = onOpen,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, official.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(
                    "Open",
                    color = official,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Button(
                onClick = onJoin,
                enabled = !busy,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = official,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(
                        "Join",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ── Circle List Cards ───────────────────────────────────────────────────────

@Composable
private fun MyCircleRow(circle: StudyCircleSummaryDto, onClick: () -> Unit) {
    val primary = CirclePrimary
    val official = OfficialGreen
    val isPublic = circle.visibility == "public"
    val accent = if (circle.isPinned) official else primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (circle.isPinned) official.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (circle.isPinned) Icons.Default.Verified else if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                contentDescription = if (circle.isPinned) "Official Circle" else if (isPublic) "Public Circle" else "Private Circle",
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = circle.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (circle.isPinned) official else PlannerFlatColors.TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (circle.isPinned) {
                    OfficialBadge()
                }
            }
            Spacer(Modifier.height(3.dp))
            val memberText = if (circle.isPinned) {
                "${circle.memberCount} members · Official"
            } else if (isPublic) {
                "${circle.memberCount} members · Public"
            } else {
                "${circle.memberCount} of ${circle.maxMembers ?: 100} members · Private"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = memberText,
                    color = PlannerFlatColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (circle.focusingCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Text(" · ", color = PlannerFlatColors.TextMuted, fontSize = 12.sp)
                        LivePulseDot(size = 6)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${circle.focusingCount} live",
                            color = DeepGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        if (circle.role == "owner") {
            OwnerTextBadge()
            Spacer(Modifier.width(6.dp))
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Open circle",
            tint = PlannerFlatColors.TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PublicCircleRow(
    circle: PublicStudyCircleDto,
    rank: Int,
    enabled: Boolean,
    busy: Boolean,
    onJoin: () -> Unit,
    onOpen: (() -> Unit)? = null,
) {
    val primary = CirclePrimary
    val official = OfficialGreen
    val accent = if (circle.isPinned) official else primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }
            .background(if (circle.isPinned) official.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (circle.isPinned) Icons.Default.Verified else Icons.Default.Public,
                contentDescription = if (circle.isPinned) "Official Circle" else "Public Circle",
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = circle.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (circle.isPinned) official else PlannerFlatColors.TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (circle.isPinned) {
                    OfficialBadge()
                } else if (rank in 1..10) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp),
                    ) {
                        Text(
                            text = "#$rank Rank",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            val subText = if (circle.isPinned) {
                "${circle.memberCount} members · Official"
            } else if (circle.joined) {
                "${circle.memberCount} members · Public"
            } else {
                "By ${circle.ownerName} · ${circle.memberCount} ${if (circle.memberCount == 1) "member" else "members"}"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (circle.focusingCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Text(" · ", color = PlannerFlatColors.TextMuted, fontSize = 12.sp)
                        LivePulseDot(size = 6)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${circle.focusingCount} live",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreen,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        if (circle.joined) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Joined",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlannerFlatColors.TextMuted,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open circle",
                    tint = PlannerFlatColors.TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            OutlinedButton(
                onClick = onJoin,
                enabled = enabled && !busy,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
                border = BorderStroke(1.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp, color = accent)
                } else {
                    Text("Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Pure Deep Orange text badge without any yellow background container */
@Composable
private fun OwnerTextBadge(modifier: Modifier = Modifier) {
    val orange = DeepOrange
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = orange,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "Owner",
            color = orange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FlatCircleCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color = PlannerFlatColors.CardWhite,
    border: BorderStroke? = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

@Composable
private fun CircleIcon(public: Boolean, size: Int = 44) {
    val primary = CirclePrimary
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (public) Icons.Default.Public else Icons.Default.Lock,
            null,
            tint = primary,
            modifier = Modifier.size((size * .44f).dp),
        )
    }
}

@Composable
private fun CircleLoadingState() = com.safarparmar.app.ui.components.StudyCircleSkeleton()

@Composable
private fun CircleErrorState(message: String, retry: () -> Unit) = FlatCircleCard {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(10.dp))
        Text(message, textAlign = TextAlign.Center, color = PlannerFlatColors.TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = retry, shape = RoundedCornerShape(10.dp)) {
            Text("Try again")
        }
    }
}

@Composable
private fun DashedStyleEmptyCard(text: String) = Surface(
    modifier = Modifier.fillMaxWidth(),
    color = PlannerFlatColors.CardWhite,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
) {
    Text(
        text,
        Modifier.padding(28.dp),
        textAlign = TextAlign.Center,
        color = PlannerFlatColors.TextMuted,
        fontSize = 13.5.sp,
    )
}

// ── Anti-Slop Input Dialog (Create / Join) ───────────────────────────────────

@Composable
private fun StudyCircleInputDialog(
    mode: CircleDialog,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    var value by remember(mode) { mutableStateOf("") }
    var visibility by remember(mode) { mutableStateOf("private") }
    val isCreate = mode == CircleDialog.Create
    val primary = CirclePrimary
    val primaryBtn = CirclePrimaryButton

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PlannerFlatColors.CardWhite)
                    .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(18.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Title
                Text(
                    text = if (isCreate) "Create a Study Circle" else "Join with Code",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlannerFlatColors.TextDark,
                )

                // Subtitle
                Text(
                    text = if (isCreate) "Choose a name and decide who can join your circle."
                    else "Enter the 6-character code shared by the circle organizer.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = PlannerFlatColors.TextMuted,
                )

                // Input Field
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = if (isCreate) it.take(MAX_CIRCLE_NAME_LENGTH)
                        else it.uppercase().filter(Char::isLetterOrDigit).take(6)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !busy,
                    placeholder = {
                        Text(
                            if (isCreate) "e.g. JEE Morning Circle" else "e.g. ABC234",
                            fontSize = 13.5.sp,
                            color = PlannerFlatColors.TextMuted.copy(alpha = 0.7f),
                        )
                    },
                    supportingText = if (isCreate) {
                        {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("${value.trim().length}/$MAX_CIRCLE_NAME_LENGTH", fontSize = 11.sp, color = PlannerFlatColors.TextMuted)
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (isCreate) KeyboardCapitalization.Sentences else KeyboardCapitalization.Characters,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                        cursorColor = primary,
                    ),
                )

                // Visibility Choice (when creating)
                if (isCreate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VisibilityChoice("private", visibility, Modifier.weight(1f)) { visibility = it }
                        VisibilityChoice("public", visibility, Modifier.weight(1f)) { visibility = it }
                    }
                    Text(
                        if (visibility == "public") "Anyone signed in to Safar can find and join."
                        else "Only people with the invite code can join.",
                        fontSize = 12.sp,
                        color = PlannerFlatColors.TextMuted,
                    )
                }

                // Action Buttons
                val canSubmit = !busy && value.trim().length >= (if (isCreate) 3 else 6)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(10.dp))
                            .clickable(enabled = !busy) { onDismiss() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextDark,
                        )
                    }

                    // Confirm CTA
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canSubmit) primaryBtn else primaryBtn.copy(alpha = 0.4f))
                            .clickable(enabled = canSubmit) { onSubmit(value.trim(), visibility) }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (busy) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                Text(
                                    text = if (isCreate) "Creating…" else "Joining…",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        } else {
                            Text(
                                text = if (isCreate) "Create" else "Join",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityChoice(value: String, selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
    val isSelected = value == selected
    val primary = CirclePrimary
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) primary.copy(alpha = 0.12f) else PlannerFlatColors.CardWhite,
        border = BorderStroke(1.dp, if (isSelected) primary else PlannerFlatColors.BorderSoft),
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                if (value == "public") Icons.Default.Public else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isSelected) primary else PlannerFlatColors.TextMuted,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) primary else PlannerFlatColors.TextDark,
            )
        }
    }
}

// ── Detail Screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCircleDetailScreen(
    circleId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: StudyCircleViewModel = hiltViewModel(),
) {
    val state by viewModel.detail.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val mehfilDm by viewModel.mehfilDm.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<ConfirmAction?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var renameDialogOpen by remember { mutableStateOf(false) }
    val pendingDmRequests by viewModel.pendingDmRequests.collectAsStateWithLifecycle()
    var showConnectRequestsSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(circleId) { viewModel.loadDetail(circleId) }
    LaunchedEffect(circleId) { while (true) { delay(30_000); viewModel.loadDetail(circleId, refresh = true) } }
    LaunchedEffect(message) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.consumeMessage() }
    }

    if (showConnectRequestsSheet) {
        StudyCircleConnectRequestsSheet(
            pendingRequests = pendingDmRequests,
            onAccept = { fromUserId, userName ->
                viewModel.acceptDmRequest(fromUserId) { uid, uname ->
                    showConnectRequestsSheet = false
                    onNavigate(
                        Routes.dmChatDirect(
                            targetUserId = uid,
                            targetUserName = uname,
                            contextPreview = "Study Circle connection",
                            autoRequest = false,
                        )
                    )
                }
            },
            onDecline = { fromUserId ->
                viewModel.declineDmRequest(fromUserId)
            },
            onOpenDmChat = {
                onNavigate(Routes.DM_CHAT)
            },
            onDismiss = { showConnectRequestsSheet = false },
        )
    }

    Scaffold(
        containerColor = PlannerFlatColors.BgCream,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Study Circle", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PlannerFlatColors.TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PlannerFlatColors.TextDark)
                    }
                },
                actions = {
                    val pendingCount = pendingDmRequests.size
                    IconButton(onClick = { showConnectRequestsSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFF97316),
                                        contentColor = Color.White,
                                    ) {
                                        Text("$pendingCount", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (pendingCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Connection requests",
                                tint = if (pendingCount > 0) CirclePrimaryButton else PlannerFlatColors.TextDark,
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.loadDetail(circleId, refresh = true) }, enabled = !state.refreshing) {
                        if (state.refreshing) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = CirclePrimary)
                        else Icon(Icons.Default.Refresh, "Refresh", tint = PlannerFlatColors.TextDark)
                    }
                    if (state.circle?.role == "owner" || isAdmin) {
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(Icons.Default.MoreVert, "More options", tint = PlannerFlatColors.TextDark)
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false },
                            ) {
                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (state.circle?.isPinned == true) "Unpin from Official Circles"
                                                else "Pin as Official Circle"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (state.circle?.isPinned == true) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = null,
                                                tint = CirclePrimary,
                                            )
                                        },
                                        onClick = {
                                            overflowExpanded = false
                                            state.circle?.let { c ->
                                                viewModel.togglePinCircle(c.id, c.isPinned)
                                            }
                                        },
                                        enabled = !state.actionInProgress,
                                    )
                                }
                                if (state.circle?.role == "owner") {
                                    DropdownMenuItem(
                                        text = { Text("Edit circle name") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = CirclePrimary)
                                        },
                                        onClick = {
                                            overflowExpanded = false
                                            renameDialogOpen = true
                                        },
                                        enabled = !state.actionInProgress,
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete circle", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            overflowExpanded = false
                                            confirm = ConfirmAction.Delete
                                        },
                                        enabled = !state.actionInProgress,
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Leave circle", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            overflowExpanded = false
                                            confirm = ConfirmAction.Leave
                                        },
                                        enabled = !state.actionInProgress,
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlannerFlatColors.BgCream),
            )
        },
    ) { padding ->
        when {
            state.loading -> com.safarparmar.app.ui.components.StudyCircleSkeleton(Modifier.padding(padding))
            state.error != null || state.circle == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(PlannerFlatColors.BgCream),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Circle not found", textAlign = TextAlign.Center, color = PlannerFlatColors.TextMuted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.loadDetail(circleId) }) { Text("Try again") }
                }
            }
            else -> DetailContent(
                circle = state.circle!!,
                entries = state.leaderboard?.entries.orEmpty(),
                padding = padding,
                busy = state.actionInProgress,
                currentUserId = currentUserId.orEmpty(),
                isAdmin = isAdmin,
                hasDmAccess = mehfilDm || isPremium,
                onEditName = { renameDialogOpen = true },
                onToggleVisibility = viewModel::toggleVisibility,
                onTogglePin = { state.circle?.let { c -> viewModel.togglePinCircle(c.id, c.isPinned) } },
                onJoinCircle = { viewModel.joinDetailCircle { viewModel.loadDetail(circleId, refresh = true) } },
                onConnectMember = { member ->
                    val circleName = state.circle?.name.orEmpty()
                    onNavigate(
                        Routes.dmChatDirect(
                            targetUserId = member.userId,
                            targetUserName = member.name,
                            contextPreview = "Study Circle: $circleName",
                        )
                    )
                },
                onRequirePremium = {
                    onNavigate(Routes.PREMIUM)
                },
                onRemove = { userId, name -> confirm = ConfirmAction.Remove(userId, name) },
                onLeave = { confirm = ConfirmAction.Leave },
            )
        }
    }

    if (renameDialogOpen && state.circle != null) {
        val originalName = state.circle!!.name
        var newName by rememberSaveable(originalName) { mutableStateOf(originalName) }
        val canSave = newName.trim().length in 3..MAX_CIRCLE_NAME_LENGTH && newName.trim() != originalName
        val canUndo = newName != originalName

        AlertDialog(
            onDismissRequest = { if (!state.actionInProgress) renameDialogOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Edit Circle Name", fontWeight = FontWeight.Bold)
                    if (canUndo) {
                        TextButton(
                            onClick = { newName = originalName },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter a new name for your study circle (3–$MAX_CIRCLE_NAME_LENGTH characters).",
                        fontSize = 13.sp,
                        color = PlannerFlatColors.TextMuted,
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { if (it.length <= MAX_CIRCLE_NAME_LENGTH) newName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. UPSC Champions 2026") },
                        trailingIcon = {
                            if (newName.isNotBlank()) {
                                IconButton(onClick = { newName = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        },
                        supportingText = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("${newName.trim().length}/$MAX_CIRCLE_NAME_LENGTH", fontSize = 11.sp, color = PlannerFlatColors.TextMuted)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val savedName = newName.trim()
                        renameDialogOpen = false
                        viewModel.renameCircle(savedName) { previousName ->
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Renamed to \"$savedName\"",
                                    actionLabel = "UNDO",
                                    duration = SnackbarDuration.Short,
                                    withDismissAction = true,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.renameCircle(previousName)
                                }
                            }
                        }
                    },
                    enabled = canSave && !state.actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = CirclePrimaryButton),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { renameDialogOpen = false },
                    enabled = !state.actionInProgress,
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(18.dp),
        )
    }

    confirm?.let { action ->
        val circle = state.circle ?: return@let
        val title = when (action) {
            ConfirmAction.Delete -> "Delete Study Circle?"
            is ConfirmAction.Remove -> "Remove ${action.name}?"
            ConfirmAction.Leave -> when {
                circle.memberCount == 1 -> "Leave and archive circle?"
                circle.role == "owner" -> "Leave and transfer ownership?"
                else -> "Leave this circle?"
            }
        }
        val description = when (action) {
            ConfirmAction.Delete -> "Are you sure you want to delete \"${circle.name}\"? All members will be removed and this action cannot be undone."
            is ConfirmAction.Remove -> "This person will lose access to the member list and leaderboard."
            ConfirmAction.Leave -> when {
                circle.memberCount == 1 -> "You are the last member, so the circle will be archived."
                circle.role == "owner" -> "The oldest active member will become the new owner."
                else -> "You will lose access to this circle and its leaderboard."
            }
        }
        AlertDialog(
            onDismissRequest = { if (!state.actionInProgress) confirm = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(description, color = PlannerFlatColors.TextMuted, fontSize = 13.5.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        when (action) {
                            ConfirmAction.Delete -> viewModel.deleteCircle { confirm = null; onBack() }
                            is ConfirmAction.Remove -> { viewModel.removeMember(action.userId, action.name); confirm = null }
                            ConfirmAction.Leave -> viewModel.leaveCircle { confirm = null; onBack() }
                        }
                    },
                    enabled = !state.actionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        when (action) {
                            ConfirmAction.Delete -> "Delete circle"
                            is ConfirmAction.Remove -> "Remove member"
                            ConfirmAction.Leave -> "Leave circle"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }, enabled = !state.actionInProgress) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(18.dp),
        )
    }
}

private sealed interface ConfirmAction {
    data object Leave : ConfirmAction
    data object Delete : ConfirmAction
    data class Remove(val userId: String, val name: String) : ConfirmAction
}

@Composable
private fun DetailContent(
    circle: StudyCircleDetailDto,
    entries: List<StudyCircleLeaderboardEntryDto>,
    padding: PaddingValues,
    busy: Boolean,
    currentUserId: String,
    isAdmin: Boolean = false,
    hasDmAccess: Boolean,
    onEditName: () -> Unit,
    onToggleVisibility: () -> Unit,
    onTogglePin: () -> Unit = {},
    onJoinCircle: () -> Unit,
    onConnectMember: (StudyCircleMemberDto) -> Unit,
    onRequirePremium: () -> Unit,
    onRemove: (String, String) -> Unit,
    onLeave: () -> Unit,
) {
    val entryByUser = remember(entries) { entries.associateBy { it.userId } }
    val sortedMembers = remember(circle.members, entryByUser, currentUserId) {
        val self = circle.members.filter { it.userId == currentUserId }
        val others = circle.members.filterNot { it.userId == currentUserId }.sortedBy { member ->
            entryByUser[member.userId]?.rank?.takeIf { it > 0 } ?: 999
        }
        self + others
    }
    val ownerName = remember(circle) { circle.members.firstOrNull { it.role == "owner" }?.name ?: "Organizer" }
    val clipboard = LocalClipboardManager.current
    val owner = circle.role == "owner"
    val isMember = circle.role == "owner" || circle.role == "member" || circle.members.any { it.userId == currentUserId }
    val joinCode = circle.joinCode
    val primary = CirclePrimary
    val primaryBtn = CirclePrimaryButton
    val official = OfficialGreen
    val circleAccent = if (circle.isPinned) official else primary
    var isCodeCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCodeCopied) {
        if (isCodeCopied) {
            delay(1500)
            isCodeCopied = false
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .background(PlannerFlatColors.BgCream),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 0. Public Preview Banner (if viewing without being a member)
        if (!isMember && circle.visibility == "public") {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.25f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Previewing Public Circle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = primary,
                            )
                            Text(
                                "Join to log focus sessions and compete on the leaderboard.",
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = onJoinCircle,
                            enabled = !busy,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryBtn,
                                contentColor = Color.White,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                            else Text("Join Circle", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Overview Card
        item {
            FlatCircleCard(contentPadding = PaddingValues(18.dp)) {
                // Top row with Initial Avatar, Title, Rename, and Owner Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(circleAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (circle.isPinned) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Official Circle",
                                tint = official,
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Text(
                                text = circle.name.take(1).uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = circleAccent,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = circle.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (circle.isPinned) official else PlannerFlatColors.TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        if (owner) {
                            Text(
                                text = "Tap to rename",
                                fontSize = 12.sp,
                                color = primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable(enabled = !busy, onClick = onEditName),
                            )
                        } else {
                            Text(
                                text = "Created by $ownerName",
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }
                    }
                    if (circle.isPinned) {
                        OfficialBadge()
                        Spacer(Modifier.width(6.dp))
                    }
                    if (owner) {
                        OwnerTextBadge()
                        Spacer(Modifier.width(6.dp))
                    }
                    if (isAdmin) {
                        IconButton(
                            onClick = onTogglePin,
                            enabled = !busy,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                if (circle.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (circle.isPinned) "Unpin official circle" else "Pin as official circle",
                                tint = if (circle.isPinned) official else PlannerFlatColors.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = PlannerFlatColors.BorderSoft)
                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Group, null, Modifier.size(16.dp), tint = PlannerFlatColors.TextMuted)
                        Spacer(Modifier.width(6.dp))
                        val memberCountDisplay = if (circle.isPinned || circle.visibility == "public") {
                            "${circle.memberCount} members"
                        } else {
                            "${circle.memberCount}/${circle.maxMembers ?: 100} members"
                        }
                        Text(memberCountDisplay, color = PlannerFlatColors.TextMuted, fontSize = 13.sp)
                    }

                    Box(
                        Modifier
                            .height(16.dp)
                            .width(1.dp)
                            .background(PlannerFlatColors.BorderSoft)
                    )
                    Spacer(Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1.2f),
                    ) {
                        if (circle.isPinned) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Official circle",
                                modifier = Modifier.size(15.dp),
                                tint = official,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Official circle",
                                color = official,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Icon(
                                if (circle.visibility == "public") Icons.Default.Public else Icons.Default.Lock,
                                null,
                                Modifier.size(14.dp),
                                tint = PlannerFlatColors.TextMuted,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (circle.visibility == "public") "Public circle" else "Private circle",
                                color = PlannerFlatColors.TextMuted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                // Live status row (if members are focusing)
                if (circle.focusingCount > 0) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = PlannerFlatColors.BorderSoft)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePulseDot(size = 8)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (circle.focusingCount == 1) "1 member focusing right now" else "${circle.focusingCount} members focusing right now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepGreen,
                        )
                    }
                }

                // Toggle visibility button (if owner and not pinned)
                if (owner && !circle.isPinned) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = onToggleVisibility,
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PlannerFlatColors.TextDark),
                    ) {
                        Icon(if (circle.visibility == "public") Icons.Default.Lock else Icons.Default.Public, null, Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (circle.visibility == "public") "Make circle private" else "Make circle public",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // 2. Dedicated Top-Level Invite Code Card
        if (circle.visibility == "private" && !joinCode.isNullOrBlank()) {
            item {
                FlatCircleCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Invite Code",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PlannerFlatColors.TextDark,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                joinCode.chunked(1).joinToString(" "),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = primary,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Share this code with friends to join this circle.",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }

                        // Tactile silent copy button
                        Surface(
                            onClick = {
                                clipboard.setText(AnnotatedString(joinCode))
                                isCodeCopied = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCodeCopied) DeepGreen.copy(alpha = 0.12f) else primary.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, if (isCodeCopied) DeepGreen else primary.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (isCodeCopied) DeepGreen else primary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isCodeCopied) "Copied!" else "Copy",
                                    color = if (isCodeCopied) DeepGreen else primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Circle Rankings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark,
                )
                Text(
                    "Focus Time",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlannerFlatColors.TextMuted,
                )
            }
        }

        // 4. Ranking Members List (Variation 3: Modern Minimalist Typography List)
        itemsIndexed(sortedMembers, key = { _, it -> it.userId }) { index, member ->
            if (index > 0) {
                HorizontalDivider(
                    color = PlannerFlatColors.BorderSoft.copy(alpha = 0.55f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            LeaderboardMemberRow(
                member = member,
                entry = entryByUser[member.userId],
                currentUserId = currentUserId,
                hasDmAccess = hasDmAccess,
                owner = owner,
                busy = busy,
                onConnect = onConnectMember,
                onRequirePremium = onRequirePremium,
                onRemove = onRemove,
                onLeave = onLeave,
            )
        }
    }
}

// ── Animated Leaderboard Rank Badges ─────────────────────────────────────────

@Composable
fun Rank1GoldCrownBadge(modifier: Modifier = Modifier) {
    com.safarparmar.app.ui.leaderboard.ImperialGoldCrownBadge(modifier = modifier)
}

@Composable
fun Rank2SilverLaurelBadge(modifier: Modifier = Modifier) {
    com.safarparmar.app.ui.leaderboard.HeraldicSilverLaurelShieldBadge(modifier = modifier)
}

@Composable
fun Rank3BronzeFlameBadge(modifier: Modifier = Modifier) {
    com.safarparmar.app.ui.leaderboard.RadiantBronzeFlameMedallionBadge(modifier = modifier)
}


@Composable
fun LeaderboardRankBadge(
    rank: Int?,
    isLive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val rankVal = rank ?: 0
    val isDark = isPlannerDark

    val rankBg = when (rankVal) {
        1 -> if (isDark) Color(0xFF854D0E).copy(alpha = 0.35f) else Color(0xFFFEF9C3) // Golden
        2 -> if (isDark) Color(0xFF334155).copy(alpha = 0.50f) else Color(0xFFF1F5F9) // Silver
        3 -> if (isDark) Color(0xFF7C2D12).copy(alpha = 0.35f) else Color(0xFFFFEDD5) // Bronze
        else -> PlannerFlatColors.BorderSoft
    }
    val rankBorder = when (rankVal) {
        1 -> BorderStroke(1.dp, if (isDark) Color(0xFFFACC15).copy(alpha = 0.8f) else Color(0xFFEAB308))
        2 -> BorderStroke(1.dp, if (isDark) Color(0xFFCBD5E1).copy(alpha = 0.7f) else Color(0xFF94A3B8))
        3 -> BorderStroke(1.dp, if (isDark) Color(0xFFFB923C).copy(alpha = 0.8f) else Color(0xFFD97706))
        else -> null
    }
    val rankTextColor = when (rankVal) {
        1 -> if (isDark) Color(0xFFFEF08A) else Color(0xFF854D0E)
        2 -> if (isDark) Color(0xFFF8FAFC) else Color(0xFF334155)
        3 -> if (isDark) Color(0xFFFDBA74) else Color(0xFF7C2D12)
        else -> PlannerFlatColors.TextMuted
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = rankBg,
        border = rankBorder,
        modifier = modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (rankVal > 0) rankVal.toString() else "—",
                fontSize = 12.sp,
                fontWeight = if (rankVal in 1..3) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = rankTextColor,
            )
        }
    }
}

// ── Member Row (Variation 3 - Modern Minimalist Typography List) ─────────────

@Composable
private fun LeaderboardMemberRow(
    member: StudyCircleMemberDto,
    entry: StudyCircleLeaderboardEntryDto?,
    currentUserId: String,
    hasDmAccess: Boolean,
    owner: Boolean,
    busy: Boolean,
    onConnect: (StudyCircleMemberDto) -> Unit,
    onRequirePremium: () -> Unit,
    onRemove: (String, String) -> Unit,
    onLeave: () -> Unit,
) {
    val isLive = member.isFocusing || (entry?.isFocusing == true)
    val rank = entry?.rank?.takeIf { it > 0 }
    val isSelf = member.userId == currentUserId
    val canConnect = member.userId.isNotBlank() && !isSelf
    var selfMenuExpanded by remember { mutableStateOf(false) }

    val primary = CirclePrimary
    val primaryBtn = CirclePrimaryButton
    val liveGreen = DeepGreen

    val rowBg = when {
        isSelf -> primary.copy(alpha = 0.08f)
        isLive -> liveGreen.copy(alpha = 0.04f)
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .padding(horizontal = 10.dp, vertical = 11.dp),
    ) {
        // Rank badge: Custom animated badge for ranks 1-3, sleek typography for others
        LeaderboardRankBadge(
            rank = rank,
            isLive = isLive,
        )

        Spacer(Modifier.width(8.dp))

        // Avatar with live indicator
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!member.avatar.isNullOrBlank()) {
                AsyncImage(
                    model = member.avatar,
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isLive) liveGreen.copy(alpha = 0.12f) else PlannerFlatColors.BorderSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = member.name.take(2).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) liveGreen else PlannerFlatColors.TextDark,
                    )
                }
            }

            if (isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .background(PlannerFlatColors.BgCream, CircleShape)
                        .padding(1.5.dp)
                        .background(liveGreen, CircleShape),
                )
            }
        }

        Spacer(Modifier.width(11.dp))

        // Member Info
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSelf) "You" else member.name,
                    fontWeight = if (isSelf) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = PlannerFlatColors.TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isSelf) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(primary.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "YOU",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                        )
                    }
                }
                if (member.role == "owner") {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Star, contentDescription = "Owner", tint = DeepOrange, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(2.5.dp))
            if (isLive) {
                val focusMins = entry?.totalFocusMinutes ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .background(liveGreen, CircleShape),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (focusMins > 0) "Live · ${formatFocusMinutes(focusMins)}" else "Focusing now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = liveGreen,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else {
                val focusMins = entry?.totalFocusMinutes ?: 0
                val sessionCount = entry?.sessionCount ?: 0
                val subtext = if (focusMins > 0) {
                    "${formatFocusMinutes(focusMins)} · $sessionCount ${if (sessionCount == 1) "session" else "sessions"}"
                } else {
                    "0m · Not focused today"
                }
                Text(
                    text = subtext,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted,
                    maxLines = 1,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isSelf) {
                Box {
                    IconButton(
                        onClick = { selfMenuExpanded = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "More options",
                            tint = PlannerFlatColors.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = selfMenuExpanded,
                        onDismissRequest = { selfMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Leave circle", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                selfMenuExpanded = false
                                onLeave()
                            },
                            enabled = !busy,
                        )
                    }
                }
            } else if (canConnect) {
                // Distinction: Premium users get active Primary CTA; Free users get locked grayed pill
                if (hasDmAccess) {
                    // User HAS Safar Premium -> Active Primary CTA
                    Surface(
                        onClick = { onConnect(member) },
                        shape = RoundedCornerShape(8.dp),
                        color = primaryBtn,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Connect",
                                modifier = Modifier.size(13.dp),
                                tint = Color.White,
                            )
                            Text(
                                text = "Connect",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                } else {
                    // User does NOT have Safar Premium -> Grayed-out Locked Pill
                    Surface(
                        onClick = onRequirePremium,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Connect (Premium)",
                                modifier = Modifier.size(12.dp),
                                tint = PlannerFlatColors.TextMuted,
                            )
                            Text(
                                text = "Connect",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }
                    }
                }
            }
            if (owner && member.role != "owner") {
                IconButton(
                    onClick = { onRemove(member.userId, member.name) },
                    enabled = !busy,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        "Remove ${member.name}",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Formatting Helpers ───────────────────────────────────────────────────────

private fun formatFocusMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCircleConnectRequestsSheet(
    pendingRequests: List<PendingStudyCircleDmRequest>,
    onAccept: (fromUserId: String, userName: String) -> Unit,
    onDecline: (fromUserId: String) -> Unit,
    onOpenDmChat: () -> Unit,
    onDismiss: () -> Unit,
) {
    var acceptingUserId by remember { mutableStateOf<String?>(null) }

    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.85f, 1.05f),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlannerFlatColors.CardWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlannerFlatColors.TextMuted.copy(alpha = 0.4f)) },
    ) {
        CompositionLocalProvider(LocalDensity provides clampedDensity) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CirclePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = CirclePrimaryButton,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Connection Requests",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PlannerFlatColors.TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (pendingRequests.isNotEmpty()) "${pendingRequests.size} student${if (pendingRequests.size > 1) "s" else ""} want to connect"
                                else "Direct student messaging",
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenDmChat()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Open Chats",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CirclePrimaryButton,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

            if (pendingRequests.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.BgCream),
                    border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = PlannerFlatColors.TextMuted,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "No pending requests",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = PlannerFlatColors.TextDark,
                        )
                        Text(
                            text = "When students in your Study Circles or Mehfil send you a chat request, you can accept and chat directly here.",
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            color = PlannerFlatColors.TextMuted,
                            lineHeight = 17.sp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(pendingRequests, key = { it.userId }) { request ->
                        val isAccepting = acceptingUserId == request.userId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.BgCream),
                            border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(CirclePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!request.userAvatar.isNullOrBlank()) {
                                        AsyncImage(
                                            model = request.userAvatar,
                                            contentDescription = request.userName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        )
                                    } else {
                                        Text(
                                            text = request.userName.firstOrNull()?.uppercase() ?: "S",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = CirclePrimaryButton,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = request.userName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PlannerFlatColors.TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Wants to connect",
                                        fontSize = 12.sp,
                                        color = PlannerFlatColors.TextMuted,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            acceptingUserId = request.userId
                                            onAccept(request.userId, request.userName)
                                        },
                                        enabled = acceptingUserId == null,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CirclePrimaryButton),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp),
                                    ) {
                                        if (isAccepting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White,
                                            )
                                        } else {
                                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onDecline(request.userId) },
                                        enabled = acceptingUserId == null,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp),
                                    ) {
                                        Text("Decline", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
