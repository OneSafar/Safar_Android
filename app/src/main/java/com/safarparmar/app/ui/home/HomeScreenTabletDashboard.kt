package com.safarparmar.app.ui.home

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.IconButton as ComposablesIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.ui.dashboard.DashboardUiState
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.util.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreenTabletDashboard(
    dashboardUiState: DashboardUiState,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onToggleGoal: (Goal) -> Unit,
    onRefresh: () -> Unit,
    userName: String?,
    userAvatar: String?,
    unreadNotifications: Int,
    onOpenAnnouncements: () -> Unit,
    dataStore: SafarDataStore?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val canvasBg = if (isDarkTheme) Color(0xFF131316) else Color(0xFFFAF7F2)

    val dateFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // ── 1. Top Bar: Date & Actions (Butterfly, Video, Bell, Avatar) ──
        TabletTopBar(
            dateText = dateFormatted,
            isDarkTheme = isDarkTheme,
            userName = userName,
            userAvatar = userAvatar,
            unreadNotifications = unreadNotifications,
            onOpenAnnouncements = onOpenAnnouncements,
            onNavigate = onNavigate,
            dataStore = dataStore,
        )

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // ── 2. Hero Banner: 2-Page Expanded Carousel (Habit Tracker & YouTube Focus) ──
            TabletHeroBannersCarousel(
                isDarkTheme = isDarkTheme,
                onNavigate = onNavigate,
            )

            // Keep both cards readable as the sidebar and window change width.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 800.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TabletTodaysFocusCard(
                            goals = dashboardUiState.todayGoals, isDarkTheme = isDarkTheme,
                            onToggleGoal = onToggleGoal, onViewPlan = { onNavigate(Routes.STUDY_PLANNER) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TabletStudyStreakCard(
                            uiState = dashboardUiState, isDarkTheme = isDarkTheme,
                            onViewHabitTracker = { onNavigate(Routes.HABIT_TRACKER) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        TabletTodaysFocusCard(
                            goals = dashboardUiState.todayGoals, isDarkTheme = isDarkTheme,
                            onToggleGoal = onToggleGoal, onViewPlan = { onNavigate(Routes.STUDY_PLANNER) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        TabletStudyStreakCard(
                            uiState = dashboardUiState, isDarkTheme = isDarkTheme,
                            onViewHabitTracker = { onNavigate(Routes.HABIT_TRACKER) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }

            // ── 4. Modules & Tools: 8 Rounded App Cards in 4x2 Grid ───────
            TabletModulesAndToolsSection(
                isDarkTheme = isDarkTheme,
                onNavigate = onNavigate,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Top Bar Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabletTopBar(
    dateText: String,
    isDarkTheme: Boolean,
    userName: String?,
    userAvatar: String?,
    unreadNotifications: Int,
    onOpenAnnouncements: () -> Unit,
    onNavigate: (String) -> Unit,
    dataStore: SafarDataStore?,
) {
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF10143F)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date (e.g. Mon, Oct 26)
        androidx.compose.material3.Text(
            text = dateText,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )

        // Right actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Share the exact butterfly geometry, colors and tilt used by other screens.
            ComposablesIconButton(
                onClick = { onNavigate(Routes.TITLI_QUERIES) },
                style = ButtonStyle.Ghost, shape = CircleShape,
                modifier = Modifier.size(48.dp), contentColor = textColor,
            ) {
                com.safarparmar.app.ui.tutorial.TitliIcon(
                    modifier = Modifier.size(36.dp).semantics { contentDescription = "Titli Guide" },
                )
            }

            // Video Player Icon
            VideoPlaylistEntryPoint(
                dataStore = dataStore,
                tint = textColor,
                isDarkTheme = isDarkTheme,
                showTooltip = true,
                modifier = Modifier.size(48.dp),
            )

            // Keep the badge outside the icon so neither the count nor bell gets clipped.
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                ComposablesIconButton(
                    onClick = onOpenAnnouncements, style = ButtonStyle.Ghost,
                    shape = CircleShape, modifier = Modifier.size(48.dp), contentColor = textColor,
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = if (unreadNotifications > 0) {
                            "Notifications, $unreadNotifications unread"
                        } else "Notifications",
                        tint = textColor, modifier = Modifier.size(26.dp),
                    )
                }
                if (unreadNotifications > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                            .background(Color(0xFFFF3446), CircleShape)
                            .border(1.dp, if (isDarkTheme) Color(0xFF131316) else Color(0xFFFAF7F2), CircleShape)
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Text(
                            text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                            color = Color.White, fontSize = 9.sp, lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            ComposablesIconButton(
                onClick = { onNavigate(Routes.PROFILE) },
                style = ButtonStyle.Ghost, shape = CircleShape,
                modifier = Modifier.size(48.dp), contentColor = textColor,
            ) {
                Box(
                    Modifier.size(44.dp).background(
                        if (isDarkTheme) Color(0xFF302D3D) else Color(0xFFECEBF0), CircleShape,
                    ).clip(CircleShape), contentAlignment = Alignment.Center,
                ) {
                    if (!userAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = userAvatar, contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        )
                    } else {
                        androidx.compose.material3.Text(
                            text = userName?.trim()?.firstOrNull()?.uppercase() ?: "S",
                            modifier = Modifier.semantics { contentDescription = "Profile" },
                            color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Scenic Mountain & Study Desk Hero Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabletStudyStreakHeroBanner(
    isDarkTheme: Boolean,
    onOpenHabitTracker: () -> Unit,
    onViewProgress: () -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)
    val cardBg = if (isDarkTheme) Color(0xFF1E1E24) else Color(0xFFE8F1FC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 210.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.8f),
                shape = cardShape,
            ),
    ) {
        // Base horizontal sky/peach gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF181B28), Color(0xFF1F2536), Color(0xFF1B2330))
                        } else {
                            listOf(Color(0xFFDCE8FA), Color(0xFFEAF1FC), Color(0xFFFBEFE8))
                        }
                    )
                )
        )

        // Right Scenic Artwork (Desk with calendar, glowing flame leaf, mountains)
        Image(
            painter = painterResource(R.drawable.tablet_banner_scenic_faded),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .matchParentSize(),
            alignment = Alignment.CenterEnd,
        )

        // Left Content (native interactive text and buttons)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .heightIn(min = 210.dp)
                .padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Eyebrow
                androidx.compose.material3.Text(
                    text = "SAFAR - YOUR LEARNING COMPANION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                )

                // Headline
                Column {
                    androidx.compose.material3.Text(
                        text = "Keep your study",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                        lineHeight = 34.sp,
                    )
                    androidx.compose.material3.Text(
                        text = "streak going.",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0D9488),
                        lineHeight = 34.sp,
                    )
                }

                // Subtitle
                androidx.compose.material3.Text(
                    text = "Make a little progress, every day.",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                )
            }

            // Let actions wrap instead of clipping on narrow windows or larger text sizes.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Open Habit Tracker button (Royal Purple)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6B21A8))
                        .bounceClick(scaleDown = 0.95f, onClick = onOpenHabitTracker)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = "Open Habit Tracker",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                }

                // View My Progress button (Frosted White / Slate)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkTheme) Color(0xFF2C2C32) else Color.White)
                        .border(
                            1.dp,
                            if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .bounceClick(scaleDown = 0.95f, onClick = onViewProgress)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(16.dp),
                    )
                    androidx.compose.material3.Text(
                        text = "View My Progress",
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2b. YouTube Focus Hero Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabletYouTubeFocusHeroBanner(
    isDarkTheme: Boolean,
    onTryYouTubeFocus: () -> Unit,
) {
    val cardShape = RoundedCornerShape(22.dp)
    val cardBg = if (isDarkTheme) Color(0xFF141D2B) else Color(0xFF0F1D2E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 210.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                shape = cardShape,
            ),
    ) {
        // Deep obsidian navy horizontal gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0A131F),
                            Color(0xFF0D1B2A),
                            Color(0xFF102235),
                            Color(0xFF13273D),
                        )
                    )
                )
        )

        // Right Scenic 3D Artwork (Tablet playing study video, glowing shield, study notes)
        Image(
            painter = painterResource(R.drawable.tablet_banner_youtube_art_faded),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.matchParentSize(),
            alignment = Alignment.CenterEnd,
        )

        // Left Content (native interactive text and emerald button)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.60f)
                .heightIn(min = 210.dp)
                .padding(horizontal = 26.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Eyebrow badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF0000))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                    androidx.compose.material3.Text(
                        text = "SAFAR · YOUTUBE FOCUS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF94A3B8),
                    )
                }

                // Headline
                Column {
                    androidx.compose.material3.Text(
                        text = "YouTube.",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 34.sp,
                    )
                    androidx.compose.material3.Text(
                        text = "Just for study.",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00D2A0),
                        lineHeight = 34.sp,
                    )
                }

                // Subtitle
                androidx.compose.material3.Text(
                    text = "Keep your study channels. Block distractions.",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                )
            }

            // Emerald CTA Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF00D2A0))
                    .bounceClick(scaleDown = 0.95f, onClick = onTryYouTubeFocus)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "Try YouTube Focus",
                    color = Color(0xFF07121E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF07121E),
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2c. Tablet Hero Banners Carousel (2 full-width pages)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabletHeroBannersCarousel(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(pagerState, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(6_000L)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            ScaledTabletHomeBanner(habitTracker = page == 0, isDarkTheme = isDarkTheme, onNavigate = onNavigate)
        }

        // Carousel Indicator Controls: < . . >
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ComposablesIconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1 + pagerState.pageCount) % pagerState.pageCount)
                    }
                },
                style = ButtonStyle.Ghost,
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous banner",
                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp),
                )
            }

            repeat(2) { pageIndex ->
                val isSelected = pagerState.currentPage == pageIndex
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 8.dp,
                    label = "banner_dot_width",
                )
                val dotColor = if (isSelected) {
                    if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF2563EB)
                } else {
                    if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dotColor)
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(pageIndex) }
                        }
                )
            }

            ComposablesIconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
                    }
                },
                style = ButtonStyle.Ghost,
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next banner",
                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Today's Focus Card
// ─────────────────────────────────────────────────────────────────────────────

private data class FocusDisplayItem(
    val id: String,
    val title: String,
    val completed: Boolean,
    val rawGoal: Goal? = null,
)

@Composable
private fun TabletTodaysFocusCard(
    goals: List<Goal>,
    isDarkTheme: Boolean,
    onToggleGoal: (Goal) -> Unit,
    onViewPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(20.dp)
    val cardBg = if (isDarkTheme) Color(0xFF1E1E24) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val displayItems = remember(goals) {
        goals.take(5).map {
            FocusDisplayItem(
                id = it.id,
                title = it.text.ifBlank { it.title },
                completed = it.completed,
                rawGoal = it,
            )
        }
    }

    val totalCount = goals.size
    val doneCount = goals.count { it.completed }
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) doneCount.toFloat() / totalCount.toFloat() else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "focusProgress"
    )

    Box(
        modifier = modifier
            .heightIn(min = 270.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                shape = cardShape,
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Target bullseye circular badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0xFF382352) else Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        androidx.compose.material3.Text(
                            text = "Today's Focus",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textColor,
                        )
                        androidx.compose.material3.Text(
                            text = "Stay consistent with your goals",
                            fontSize = 13.sp,
                            color = mutedColor,
                        )
                    }
                }

                // View Plan link
                Row(
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.95f, onClick = onViewPlan)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = "View Plan",
                        color = Color(0xFF7C3AED),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Body: Circular Progress Ring + Task List
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // Circular Progress Ring
                Box(
                    modifier = Modifier.size(126.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val trackColor = if (isDarkTheme) Color(0xFF2C243B) else Color(0xFFF3E8FF)
                    val activeColor = Color(0xFF8B5CF6)

                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val strokeW = 10.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeW),
                        )
                        drawArc(
                            color = activeColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        androidx.compose.material3.Text(
                            text = "$doneCount/$totalCount",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = textColor,
                        )
                        androidx.compose.material3.Text(
                            text = "Tasks today",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = mutedColor,
                        )
                    }
                }

                if (displayItems.isEmpty()) {
                    androidx.compose.material3.Text(
                        text = "No tasks planned for today. Open your plan to add tasks.",
                        modifier = Modifier.weight(1f),
                        color = mutedColor,
                        fontSize = 14.sp,
                    )
                }
                // Tasks list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    displayItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    item.rawGoal?.let(onToggleGoal)
                                }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (item.completed) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7C3AED)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(
                                            1.8.dp,
                                            if (isDarkTheme) Color(0xFF64748B) else Color(0xFFCBD5E1),
                                            CircleShape
                                        ),
                                )
                            }
                            androidx.compose.material3.Text(
                                text = item.title,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (item.completed) mutedColor else textColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Study Streak Card
// ─────────────────────────────────────────────────────────────────────────────

private data class WeekDayStatus(
    val name: String,
    val completed: Boolean,
)

@Composable
private fun TabletStudyStreakCard(
    uiState: DashboardUiState,
    isDarkTheme: Boolean,
    onViewHabitTracker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(20.dp)
    val cardBg = if (isDarkTheme) Color(0xFF1E1E24) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Calculate the 7 days of the current week (Monday to Sunday)
    val weekDays = remember(uiState.loginHistory, uiState.streaks) {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val streak = uiState.streaks.checkInStreak.coerceAtLeast(0)
        (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val dateKey = date.toString()
            val hasLogin = uiState.loginHistory.any { it.timestamp.startsWith(dateKey) }
            val completed = hasLogin || (date <= today && (today.dayOfWeek.value - 1 - offset) < streak)
            val name = when (offset) {
                0 -> "Mon"
                1 -> "Tue"
                2 -> "Wed"
                3 -> "Thu"
                4 -> "Fri"
                5 -> "Sat"
                else -> "Sun"
            }
            WeekDayStatus(name, completed)
        }
    }

    val streakCount = uiState.streaks.checkInStreak.coerceAtLeast(0)

    Box(
        modifier = modifier
            .heightIn(min = 270.dp)
            .shadow(
                elevation = if (isDarkTheme) 6.dp else 2.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                shape = cardShape,
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Teal Flame badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0xFF133633) else Color(0xFFCCFBF1)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        androidx.compose.material3.Text(
                            text = "Study Streak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textColor,
                        )
                        androidx.compose.material3.Text(
                            text = "Small steps build big results",
                            fontSize = 13.sp,
                            color = mutedColor,
                        )
                    }
                }

                // View Habit Tracker link
                Row(
                    modifier = Modifier
                        .bounceClick(scaleDown = 0.95f, onClick = onViewHabitTracker)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = "View Habit Tracker",
                        color = Color(0xFF7C3AED),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Streak Metric + 7 Days Circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Big Metric
                Column(
                    modifier = Modifier.width(116.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = streakCount.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 38.sp,
                        color = textColor,
                        lineHeight = 40.sp,
                    )
                    androidx.compose.material3.Text(
                        text = "day streak",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = mutedColor,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Keep going pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) Color(0xFF133829) else Color(0xFFECFDF5))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(13.dp),
                        )
                        androidx.compose.material3.Text(
                            text = "Keep going!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                        )
                    }
                }

                // 7 Weekly Circles
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    weekDays.forEach { day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (day.completed) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D9488)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .border(
                                            1.8.dp,
                                            if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                            CircleShape
                                        ),
                                )
                            }
                            androidx.compose.material3.Text(
                                text = day.name,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = mutedColor,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Quote Pill Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkTheme) Color(0xFF281C38) else Color(0xFFF5F3FF))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "“",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED),
                )
                androidx.compose.material3.Text(
                    text = "Discipline today creates the freedom you want tomorrow.",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF4B5563),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Modules & Tools Section: 8 App Cards in 4x2 Grid
// ─────────────────────────────────────────────────────────────────────────────

private data class TabletToolItem(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val route: String,
)

@Composable
private fun TabletModulesAndToolsSection(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    val row1Tools = remember {
        listOf(
            TabletToolItem("Ekagra", "Focus better.\nStudy deeper.", R.drawable.tool_ekagra, Routes.EKAGRA),
            TabletToolItem("Nishtha", "Build discipline.\nStay consistent.", R.drawable.home_nishtha_book, Routes.nishthaRoot()),
            TabletToolItem("Mehfil", "Learn together.\nGrow together.", R.drawable.tool_mehfil, Routes.MEHFIL),
            TabletToolItem("Exam Planner", "Plan smarter.\nAchieve more.", R.drawable.tool_study_planner, Routes.STUDY_PLANNER),
        )
    }

    val row2Tools = remember {
        listOf(
            TabletToolItem("Dhyan", "A calmer mind.\nA sharper you.", R.drawable.tool_dhyan, Routes.DHYAN),
            TabletToolItem("Study Circle", "Discuss. Share.\nLearn together.", R.drawable.tool_circle, Routes.STUDY_CIRCLES),
            TabletToolItem("KAVACH", "Your safety.\nOur priority.", R.drawable.tool_kavach, Routes.FOCUS_SHIELD),
            TabletToolItem("Leaderboard", "Compete. Improve.\nMove ahead.", R.drawable.tool_leaderboard, Routes.LEADERBOARD),
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    androidx.compose.material3.Text(
                        text = "Modules & Tools",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor,
                    )
                    androidx.compose.material3.Text(
                        text = "Everything you need for your SAFAR",
                        fontSize = 13.sp,
                        color = mutedColor,
                    )
                }
            }

            // Explore All link
            Row(
                modifier = Modifier.bounceClick(scaleDown = 0.95f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigate(Routes.COURSES)
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "Explore All",
                    color = Color(0xFF7C3AED),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 1000.dp -> 4
                maxWidth >= 720.dp -> 3
                else -> 2
            }
            val tools = row1Tools + row2Tools
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tools.chunked(columns).forEach { rowTools ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowTools.forEach { tool ->
                            TabletToolCard(
                                tool = tool,
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigate(tool.route)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowTools.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletToolCard(
    tool: TabletToolItem,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)
    val cardBg = if (isDarkTheme) Color(0xFF1E1E24) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val mutedColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = modifier
            .heightIn(min = 98.4.dp)
            .shadow(
                elevation = if (isDarkTheme) 4.dp else 1.5.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            )
            .clip(cardShape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                shape = cardShape,
            )
            .bounceClick(scaleDown = 0.96f, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Thumbnail Image
        Image(
            painter = painterResource(tool.iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(67.2.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            androidx.compose.material3.Text(
                text = tool.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            androidx.compose.material3.Text(
                text = tool.subtitle,
                fontSize = 13.sp,
                color = mutedColor,
                lineHeight = 17.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The same finished WebP banners are used on phone and tablet. */
@Composable
internal fun ScaledTabletHomeBanner(
    habitTracker: Boolean,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
) {
    PlumHomePromo(habitTracker = habitTracker, onNavigate = onNavigate,
        modifier = Modifier.fillMaxWidth().aspectRatio(805f / 355f))
}
