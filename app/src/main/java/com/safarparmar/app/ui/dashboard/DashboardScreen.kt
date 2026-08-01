package com.safarparmar.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.*
import com.safarparmar.app.ui.achievements.AchievementImages
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.studyplanner.analytics.StudyPlannerAnalytics
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.glassSurface
import com.safarparmar.app.ui.studyplanner.components.SafarGlassDialogHost
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.studyplanner.screens.InsightsOverallProgressRedesign
import com.safarparmar.app.ui.theme.LoraFontFamily

// ── macOS Control Center Design Helpers ─────────────────────────────────────

@Composable
private fun MacOSControlCard(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val bodyColor = DashboardFlatColors.glassBody(isDarkTheme)
    val borderBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }
    val shadowElevation = if (isDarkTheme) 12.dp else 4.dp
    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .clip(shape)
            .background(bodyColor)
            .border(
                width = 0.5.dp,
                brush = borderBrush,
                shape = shape
            )
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun MacOSControlButton(
    iconVector: ImageVector? = null,
    iconLetter: String? = null,
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = Color(0xFF0A84FF),
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(20.dp)
    val bodyColor = DashboardFlatColors.glassBody(isDarkTheme)
    val textColor = DashboardFlatColors.onGlassText(isDarkTheme)
    val subtitleColor = DashboardFlatColors.onGlassMuted(isDarkTheme)
    val borderBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }
    val shadowElevation = if (isDarkTheme) 12.dp else 4.dp
    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .height(68.dp)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .clip(shape)
            .background(bodyColor)
            .border(
                width = 0.5.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconLetter != null) {
                    Text(
                        text = iconLetter,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MacOSButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    buttonColor: Color = Color(0xFF0A84FF),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(buttonColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        )
    }
}

private fun macTextColor(isDark: Boolean) = DashboardFlatColors.onGlassText(isDark)
private fun macSubtitleColor(isDark: Boolean) = DashboardFlatColors.onGlassMuted(isDark)
private fun macAccentBlue() = Color(0xFF0A84FF)

private enum class DashboardSheetType {
    NONE, MOOD, GOALS, STREAKS, BADGES
}

// ── Main Dashboard Screen ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentRoute: String = Routes.DASHBOARD,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeSheet by remember { mutableStateOf(DashboardSheetType.NONE) }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
    SafarDrawerScaffold(
        title = stringResource(R.string.dashboard_title),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        containerColor = DashboardFlatColors.Bg,
        useGlassTopBar = false,
        useDetachedMenuGlass = false,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(DashboardFlatColors.Bg)) {

            if (uiState.error != null && uiState.userName.isEmpty() && !uiState.isLoading) {
                SafarErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.onEvent(DashboardEvent.Refresh) },
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (uiState.isLoading && uiState.userName.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(4) { StatCardSkeleton() }
                }
            } else {
                SafarPullRefreshBox(
                    isRefreshing = uiState.isLoading && uiState.userName.isNotEmpty(),
                    onRefresh = { viewModel.onEvent(DashboardEvent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                    ) {
                        // 1. Hero Welcome Header
                        item { WelcomeBanner(uiState.userName, isDarkTheme) }

                        // 2. Active Title Card
                        item {
                            ActiveTitleCard(
                                title = uiState.activeTitle,
                                titleImageUrl = uiState.activeTitleImageUrl,
                                isDark = isDarkTheme,
                                hasEarnedAchievements = uiState.earnedAchievements.isNotEmpty(),
                                onNavigateToAchievements = { onNavigate(Routes.ACHIEVEMENTS) }
                            )
                        }

                        // 3. Study Plan Progress
                        item { StudyPlanProgressCard(uiState.studyPlan, isDarkTheme, onNavigate) }

                        // 4. macOS Quick Control Grid (Interactive Hub)
                        item {
                            MacOSQuickControlGrid(
                                uiState = uiState,
                                isDark = isDarkTheme,
                                onOpenSheet = { sheet -> activeSheet = sheet }
                            )
                        }

                        // 5. Analytics & Monthly Snapshot
                        item { MonthlyCard(uiState.monthlyReport, isDarkTheme, onNavigate) }

                        // 6. Weekly Mood Graph
                        if (uiState.weeklyMoods.isNotEmpty()) {
                            item { WeeklyMoodChart(uiState.weeklyMoods, isDarkTheme) }
                        }
                    }
                }
            }

            // ── Interactive Detail Bottom Sheets ─────────────────────────────
            if (activeSheet != DashboardSheetType.NONE) {
                ModalBottomSheet(
                    onDismissRequest = { activeSheet = DashboardSheetType.NONE },
                    containerColor = DashboardFlatColors.Bg,
                    contentColor = DashboardFlatColors.Text,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = DashboardFlatColors.Hairline) },
                ) {
                    Box(modifier = Modifier.padding(bottom = 32.dp)) {
                        when (activeSheet) {
                            DashboardSheetType.MOOD -> MoodSheetContent(uiState.todayMood, isDarkTheme, onNavigate, onDismiss = { activeSheet = DashboardSheetType.NONE })
                            DashboardSheetType.GOALS -> TodayGoalsSheetContent(uiState.todayGoals, isDarkTheme, onNavigate, onDismiss = { activeSheet = DashboardSheetType.NONE })
                            DashboardSheetType.STREAKS -> StreaksSheetContent(uiState.streaks, isDarkTheme, onNavigate, onDismiss = { activeSheet = DashboardSheetType.NONE })
                            DashboardSheetType.BADGES -> BadgesSheetContent(uiState.earnedAchievements, uiState.allAchievements, isDarkTheme, onNavigate, onDismiss = { activeSheet = DashboardSheetType.NONE })
                            else -> Unit
                        }
                    }
                }
            }

            // ── Welcome Overlay (macOS glass + backdrop blur) ──────────────
            if (uiState.showWelcomeOverlay) {
                DashboardWelcomeOverlay(
                    userName = uiState.userName,
                    isDark = isDarkTheme,
                    onDismiss = { viewModel.dismissWelcome() },
                )
            }

            val celebrationAchievements = uiState.celebrationAchievements
            if (celebrationAchievements.isNotEmpty()) {
                CelebrationDialog(
                    achievements = celebrationAchievements,
                    isDark = isDarkTheme,
                    onDismiss = { viewModel.dismissCelebration() }
                )
            }
        }
    }
    } // CompositionLocalProvider
}

// ── macOS Quick Control Grid ────────────────────────────────────────────────

@Composable
private fun MacOSQuickControlGrid(
    uiState: DashboardUiState,
    isDark: Boolean,
    onOpenSheet: (DashboardSheetType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacOSControlButton(
                iconVector = Icons.Default.Favorite,
                title = "Today Mood",
                subtitle = uiState.todayMood?.mood?.replaceFirstChar { it.uppercase() } ?: "Check In",
                isDarkTheme = isDark,
                iconBackgroundColor = Color(0xFFFF2D55), // macOS Pink
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(DashboardSheetType.MOOD) }
            )

            MacOSControlButton(
                iconVector = Icons.Default.TrackChanges,
                title = "Today Goals",
                subtitle = "${uiState.todayGoals.count { it.completed }}/${uiState.todayGoals.size} Done",
                isDarkTheme = isDark,
                iconBackgroundColor = Color(0xFF0A84FF), // macOS Blue
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(DashboardSheetType.GOALS) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MacOSControlButton(
                iconVector = Icons.Default.LocalFireDepartment,
                title = "Streak",
                subtitle = "${uiState.streaks.checkInStreak} Days Active",
                isDarkTheme = isDark,
                iconBackgroundColor = Color(0xFFFF9500), // macOS Orange
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(DashboardSheetType.STREAKS) }
            )

            MacOSControlButton(
                iconVector = Icons.Default.EmojiEvents,
                title = "Badges",
                subtitle = "${uiState.earnedAchievements.size} Unlocked",
                isDarkTheme = isDark,
                iconBackgroundColor = Color(0xAF5856D6), // macOS Purple
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(DashboardSheetType.BADGES) }
            )
        }
    }
}

// ── Welcome Banner Section ──────────────────────────────────────────────────

@Composable
private fun WelcomeBanner(userName: String, isDark: Boolean) {
    MacOSControlCard(isDarkTheme = isDark) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(macAccentBlue()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase().ifEmpty { "S" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Welcome back,",
                        color = macSubtitleColor(isDark),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = userName.replaceFirstChar { it.uppercase() }.ifEmpty { "User" },
                        color = macTextColor(isDark),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = macAccentBlue(),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DAILY INSPIRATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = macSubtitleColor(isDark),
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "\"Your limit is mostly your imagination.\"",
                    color = macSubtitleColor(isDark),
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Study Plan Progress Card ────────────────────────────────────────────────

@Composable
private fun StudyPlanProgressCard(
    state: DashboardStudyPlanState,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
) {
    fun openPlanner() {
        StudyPlannerAnalytics.track(
            StudyPlannerAnalytics.DASHBOARD_TODAY_CARD_CLICKED,
            mapOf("state" to state.status.name.lowercase(), "plan_id" to state.planId.orEmpty()),
        )
        val route = if (!state.planId.isNullOrBlank()) {
            "${Routes.STUDY_PLANNER}?planId=${state.planId}"
        } else {
            Routes.STUDY_PLANNER
        }
        onNavigate(route)
    }

    if (state.status == DashboardStudyPlanStatus.NO_ACTIVE_PLAN) {
        MacOSControlCard(isDarkTheme = isDark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(macAccentBlue()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Today, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "Study Plan Progress",
                    color = macTextColor(isDark),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Create a study plan to track your syllabus completion.",
                color = macSubtitleColor(isDark),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            MacOSButton(
                text = "Create Plan",
                icon = Icons.Default.Add,
                onClick = ::openPlanner,
                isDarkTheme = isDark,
                buttonColor = macAccentBlue()
            )
        }
    } else {
        Box(modifier = Modifier.clickable { openPlanner() }) {
            InsightsOverallProgressRedesign(
                overallProgressPercent = state.overallCompletionPercent,
                dailyTodoProgressPercent = state.dailyTodoProgressPercent,
                doneTopics = state.overallDoneTopics,
                totalTopics = state.overallTotalTopics,
                isLight = !isDark,
            )
        }
    }
}

// ── Active Title Card ───────────────────────────────────────────────────────

@Composable
private fun ActiveTitleCard(
    title: String,
    titleImageUrl: String?,
    isDark: Boolean,
    hasEarnedAchievements: Boolean,
    onNavigateToAchievements: () -> Unit,
) {
    MacOSControlCard(
        isDarkTheme = isDark,
        modifier = Modifier.clickable { onNavigateToAchievements() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "CURRENT TITLE",
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = macSubtitleColor(isDark),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (title.isNotEmpty()) {
                if (titleImageUrl != null) {
                    AsyncImage(
                        model = titleImageUrl,
                        contentDescription = title,
                        modifier = Modifier.width(180.dp).height(90.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(macAccentBlue()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_zap),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = macTextColor(isDark), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(macAccentBlue()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_zap),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (hasEarnedAchievements) "Tap to set achievement title" else "Unlock achievements to earn titles",
                    color = macTextColor(isDark),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Interactive Detail Sheets ──────────────────────────────────────────────

@Composable
private fun MoodSheetContent(
    todayMood: Mood?,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF2D55)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text("Today's Mood Check-In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = macTextColor(isDark))
        }

        if (todayMood != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                    .padding(16.dp)
            ) {
                Text(moodEmoji(todayMood.mood), fontSize = 40.sp)
                Column {
                    Text(
                        todayMood.mood.replaceFirstChar { it.uppercase() },
                        color = macTextColor(isDark),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.dashboard_intensity, todayMood.intensity),
                        color = macSubtitleColor(isDark),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Text("You haven't checked in your mood today. Reflecting on your state helps build self-awareness.", color = macSubtitleColor(isDark), fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            MacOSButton(
                text = "Check In Now",
                icon = Icons.Default.FavoriteBorder,
                onClick = {
                    onDismiss()
                    onNavigate(Routes.NISHTHA)
                },
                isDarkTheme = isDark,
                buttonColor = Color(0xFFFF2D55)
            )
        }
    }
}

@Composable
private fun TodayGoalsSheetContent(
    goals: List<Goal>,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val completed = goals.count { it.completed }
    val progress = if (goals.isNotEmpty()) completed.toFloat() / goals.size else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(macAccentBlue()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrackChanges, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text("Today's Goals", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = macTextColor(isDark))
            }
            Text("$completed / ${goals.size} Done", fontSize = 13.sp, color = macSubtitleColor(isDark), fontWeight = FontWeight.SemiBold)
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = macAccentBlue(),
            trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
        )

        if (goals.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { goal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (goal.completed) macAccentBlue() else macSubtitleColor(isDark), CircleShape)
                                .background(if (goal.completed) macAccentBlue() else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (goal.completed) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(goal.title, color = macTextColor(isDark), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text("No goals scheduled for today.", color = macSubtitleColor(isDark), fontSize = 14.sp)
        }

        Spacer(Modifier.height(4.dp))
        MacOSButton(
            text = "Manage Goals",
            icon = Icons.Default.TrackChanges,
            onClick = {
                onDismiss()
                onNavigate(Routes.nishthaTab(2))
            },
            isDarkTheme = isDark,
            buttonColor = macAccentBlue()
        )
    }
}

@Composable
private fun StreaksSheetContent(
    streaks: Streaks,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9500)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text("Activity Streaks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = macTextColor(isDark))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            StreakRow(stringResource(R.string.dashboard_streak_checkin), "${streaks.checkInStreak}", isDark)
            PlanHairline()
            StreakRow(stringResource(R.string.dashboard_streak_login), "${streaks.loginStreak}", isDark)
            PlanHairline()
            StreakRow(stringResource(R.string.dashboard_streak_goal), "${streaks.goalCompletionStreak}", isDark)
        }

        MacOSButton(
            text = "View Streak History",
            icon = Icons.Default.Loop,
            onClick = {
                onDismiss()
                onNavigate(Routes.nishthaTab(3))
            },
            isDarkTheme = isDark,
            buttonColor = Color(0xFFFF9500)
        )
    }
}

@Composable
private fun BadgesSheetContent(
    earned: List<Achievement>,
    all: List<Achievement>,
    isDark: Boolean,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xAF5856D6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text("Unlocked Badges", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = macTextColor(isDark))
            }
            Text("${earned.size} Unlocked", fontSize = 13.sp, color = macSubtitleColor(isDark), fontWeight = FontWeight.SemiBold)
        }

        val display = remember(earned, all) { (if (earned.isNotEmpty()) earned else all).take(6) }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(display, key = { it.id }) { achievement ->
                val imageUrl = remember(achievement.id) {
                    AchievementImages.urlFor(achievement.id)
                }
                Column(
                    modifier = Modifier
                        .width(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xAF5856D6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = achievement.name,
                                modifier = Modifier.size(36.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text(
                                if (achievement.type == "title") "👑" else "🏅",
                                fontSize = 20.sp,
                            )
                        }
                    }
                    Text(
                        achievement.name,
                        color = macTextColor(isDark),
                        fontSize = 10.sp, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        MacOSButton(
            text = "View All Achievements",
            icon = Icons.Default.EmojiEvents,
            onClick = {
                onDismiss()
                onNavigate(Routes.ACHIEVEMENTS)
            },
            isDarkTheme = isDark,
            buttonColor = Color(0xAF5856D6)
        )
    }
}

private fun moodEmoji(mood: String) = when (mood.lowercase()) {
    "happy", "joyful"  -> "😄"
    "calm", "peaceful" -> "😌"
    "neutral"          -> "😐"
    "sad"              -> "😢"
    "anxious"          -> "😰"
    "angry"            -> "😠"
    "tired"            -> "🥱"
    "excited"          -> "🤩"
    else               -> "😊"
}

@Composable
private fun StreakRow(label: String, value: String, isDark: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = macTextColor(isDark), fontSize = 14.sp)
        Text(value, color = macTextColor(isDark), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Monthly Report Section ──────────────────────────────────────────────────

@Composable
private fun MonthlyCard(report: MonthlyReport?, isDark: Boolean, onNavigate: (String) -> Unit) {
    if (report == null) return
    MacOSControlCard(isDarkTheme = isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759)), // macOS Green
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(stringResource(R.string.dashboard_monthly_snapshot), color = macTextColor(isDark), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(report.month, color = macSubtitleColor(isDark), fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "A quick look at your performance this month.",
            color = macSubtitleColor(isDark),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        StatRow(stringResource(R.string.dashboard_consistency), "${report.consistencyScore.toInt()}%", isDark)
        PlanHairline()
        StatRow(stringResource(R.string.dashboard_completion), "${report.completionRate.toInt()}%", isDark)
        PlanHairline()
        StatRow(stringResource(R.string.dashboard_focus), "${report.focusDepth.toInt()}m/day", isDark)
        Spacer(Modifier.height(10.dp))
        MacOSButton(
            text = "View Full Report",
            icon = Icons.Default.BarChart,
            onClick = { onNavigate(Routes.nishthaTab(4)) },
            isDarkTheme = isDark,
            buttonColor = Color(0xFF34C759)
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, isDark: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = macTextColor(isDark), fontSize = 14.sp)
        Text(value, color = macAccentBlue(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Weekly Mood Chart Section ───────────────────────────────────────────────

@Composable
private fun WeeklyMoodChart(moods: List<Mood>, isDark: Boolean) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    MacOSControlCard(isDarkTheme = isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(macAccentBlue()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(stringResource(R.string.dashboard_weekly_mood), color = macTextColor(isDark), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Your emotional journey from Monday to Sunday.",
            color = macSubtitleColor(isDark),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))

        val intensityValues = remember(moods) { moods.take(7).map { it.intensity.toFloat() } }
        val primaryColor = macAccentBlue()
        val surfaceColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val maxVal = 5f
            val pts = intensityValues.mapIndexed { i, v ->
                val x = if (intensityValues.size > 1) i * w / (intensityValues.size - 1) else w / 2
                val y = h - (v / maxVal) * h * 0.85f
                Offset(x, y)
            }
            for (i in 0..2) {
                val y = h - (i * h / 2f)
                drawLine(surfaceColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            if (pts.size > 1) {
                val fillPath = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h)
                    close()
                }
                drawPath(fillPath, primaryColor.copy(alpha = 0.12f))
                for (i in 0 until pts.size - 1) {
                    drawLine(primaryColor, pts[i], pts[i + 1], strokeWidth = 2.5f, cap = StrokeCap.Round)
                }
                pts.forEach { drawCircle(primaryColor, radius = 4f, center = it) }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    val moodItem = moods.getOrNull(i)
                    if (moodItem != null && moodItem.mood.isNotBlank()) {
                        Text(moodEmoji(moodItem.mood), fontSize = 12.sp)
                    } else {
                        Text("—", fontSize = 12.sp, color = macSubtitleColor(isDark))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(d, fontSize = 10.sp, color = macSubtitleColor(isDark), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ── Overlays & Dialogs ──────────────────────────────────────────────────────

@Composable
private fun DashboardWelcomeOverlay(userName: String, isDark: Boolean, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SafarGlassDialogHost(isDarkTheme = isDark) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .glassSurface(shape = RoundedCornerShape(22.dp), isDarkTheme = isDark)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlanEyebrow("Safar")
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_leaf),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = macAccentBlue(),
                )
                Text(
                    text = "Welcome back,\n${userName.replaceFirstChar { it.uppercase() }.ifEmpty { "Friend" }}",
                    fontFamily = LoraFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    color = macTextColor(isDark),
                )
                Text(
                    text = "Your journey continues here.\nEvery small step forward counts — today is a new opportunity to grow, reflect, and be present.",
                    fontSize = 14.sp,
                    color = macSubtitleColor(isDark),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                GlassButton(
                    onClick = onDismiss,
                    accentColor = macAccentBlue(),
                    modifier = Modifier.fillMaxWidth(),
                    isDarkTheme = isDark,
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Let's begin",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfettiCelebration(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    if (!isActive) return

    val colors = listOf(
        Color(0xFFFFC107),
        Color(0xFFFF5722),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFF9C27B0),
        Color(0xFFE91E63)
    )

    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                ticks++
                kotlinx.coroutines.delay(16)
            }
        }
    }

    val particles = remember { mutableStateListOf<ConfettiParticle>() }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (particles.isEmpty() && width > 0 && height > 0) {
            val random = java.util.Random()
            repeat(45) {
                particles.add(
                    ConfettiParticle(
                        x = random.nextFloat() * width,
                        y = -random.nextFloat() * 100f - 20f,
                        vx = (random.nextFloat() - 0.5f) * 4f,
                        vy = random.nextFloat() * 6f + 3f,
                        color = colors[random.nextInt(colors.size)],
                        size = random.nextFloat() * 12f + 8f,
                        isCircle = random.nextBoolean(),
                        rotation = random.nextFloat() * 360f,
                        rotationSpeed = (random.nextFloat() - 0.5f) * 8f
                    )
                )
            }
        }

        if (particles.isNotEmpty()) {
            particles.forEach { p ->
                p.x += p.vx
                p.y += p.vy
                p.rotation += p.rotationSpeed

                if (p.x < -20f) p.x = width + 20f
                if (p.x > width + 20f) p.x = -20f

                if (p.y > height + 20f) {
                    p.y = -20f
                    p.vy = (0.1f + Math.random().toFloat() * 0.9f) * 6f + 3f
                }

                rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.size / 2,
                            center = Offset(p.x, p.y)
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(p.x - p.size / 2, p.y - p.size / 4),
                            size = androidx.compose.ui.geometry.Size(p.size, p.size / 2)
                        )
                    }
                }
            }
        }
    }
}

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    val isCircle: Boolean,
    var rotation: Float,
    val rotationSpeed: Float
)

@Composable
private fun CelebrationDialog(
    achievements: List<Achievement>,
    isDark: Boolean,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SafarGlassDialogHost(isDarkTheme = isDark) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .glassSurface(shape = RoundedCornerShape(22.dp), isDarkTheme = isDark)
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                ConfettiCelebration(modifier = Modifier.matchParentSize())

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PlanEyebrow("Safar")
                    Text(
                        "Congratulations! 🎉",
                        fontFamily = LoraFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = macTextColor(isDark),
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        if (achievements.size > 1) {
                            "You have unlocked new achievements!"
                        } else {
                            "You unlocked a new achievement!"
                        },
                        fontSize = 14.sp,
                        color = macSubtitleColor(isDark),
                        textAlign = TextAlign.Center,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        achievements.forEach { achievement ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    if (achievement.type == "title") "👑" else "🏅",
                                    fontSize = 48.sp,
                                )

                                Text(
                                    achievement.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = macTextColor(isDark),
                                    textAlign = TextAlign.Center,
                                )

                                if (!achievement.description.isNullOrBlank()) {
                                    Text(
                                        achievement.description,
                                        fontSize = 13.sp,
                                        color = macSubtitleColor(isDark),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    GlassButton(
                        onClick = onDismiss,
                        accentColor = macAccentBlue(),
                        modifier = Modifier.fillMaxWidth(),
                        isDarkTheme = isDark,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Awesome!",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                        )
                    }
                }
            }
        }
    }
}
