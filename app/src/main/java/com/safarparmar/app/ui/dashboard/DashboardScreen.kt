package com.safarparmar.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.*
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.GlassDivider
import com.safarparmar.app.ui.glass.SafarGlassBackdrop
import com.safarparmar.app.ui.glass.SafarGlassButton
import com.safarparmar.app.ui.glass.SafarGlassCard
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.studyplanner.analytics.StudyPlannerAnalytics
import com.safarparmar.app.ui.studyplanner.screens.InsightsOverallProgressRedesign
import com.safarparmar.app.ui.theme.*

private fun glassTextPrimary(isDark: Boolean) =
    if (isDark) SafarGlassPalette.TextPrimary else SafarGlassPalette.LightTextPrimary

private fun glassTextSecondary(isDark: Boolean) =
    if (isDark) SafarGlassPalette.TextSecondary else SafarGlassPalette.LightTextSecondary

private fun glassAccent(isDark: Boolean) =
    if (isDark) SafarGlassPalette.Violet else SafarGlassPalette.LightViolet

private fun glassNestedBg(isDark: Boolean) =
    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

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

    SafarDrawerScaffold(
        title = stringResource(R.string.dashboard_title),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        containerColor = Color.Transparent,
        useGlassTopBar = true,
        useDetachedMenuGlass = true,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            SafarGlassBackdrop(modifier = Modifier.fillMaxSize(), isLight = !isDarkTheme)

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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                ) {
                    item { WelcomeBanner(uiState.userName, isDarkTheme) }
                    item { StudyPlanProgressCard(uiState.studyPlan, isDarkTheme, onNavigate) }
                    item {
                        ActiveTitleCard(
                            title = uiState.activeTitle,
                            titleId = uiState.activeTitleId,
                            isDark = isDarkTheme,
                            hasEarnedAchievements = uiState.earnedAchievements.isNotEmpty(),
                            onNavigateToAchievements = { onNavigate(Routes.ACHIEVEMENTS) }
                        )
                    }
                    item { MoodCard(uiState.todayMood, isDarkTheme, onNavigate) }
                    item { StreaksCard(uiState.streaks, isDarkTheme, onNavigate) }
                    if (uiState.allAchievements.isNotEmpty()) {
                        item { BadgesCard(uiState.earnedAchievements, uiState.allAchievements, isDarkTheme, onNavigate) }
                    }
                    item { TodayGoalsCard(uiState.todayGoals, isDarkTheme, onNavigate) }
                    uiState.monthlyReport?.let { item { MonthlyCard(it, isDarkTheme, onNavigate) } }
                    if (uiState.weeklyMoods.isNotEmpty()) {
                        item { WeeklyMoodChart(uiState.weeklyMoods, isDarkTheme, onNavigate) }
                    }

                }
                }
            }

            // ── Welcome note overlay ──────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.showWelcomeOverlay,
                enter   = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 3 },
                exit    = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 3 },
                modifier = Modifier.align(Alignment.Center),
            ) {
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
}

@Composable
private fun DashboardWelcomeOverlay(userName: String, isDark: Boolean, onDismiss: () -> Unit) {
    val isLight = !isDark
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safarFrostedPanel(
                    isLight = isLight,
                    shape = RoundedCornerShape(SafarGlassChromeRadius),
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_leaf),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = glassAccent(isDark),
            )
            Text(
                text = "Welcome back,\n${userName.replaceFirstChar { it.uppercase() }.ifEmpty { "Friend" }}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                color = glassTextPrimary(isDark),
            )
            Text(
                text = "Your journey continues here.\nEvery small step forward counts — today is a new opportunity to grow, reflect, and be present.",
                fontSize = 14.sp,
                color = glassTextSecondary(isDark),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            SafarGlassButton(
                text = "Let's begin",
                icon = Icons.Default.Star,
                onClick = onDismiss,
                isLight = isLight,
            )
        }
    }
}

@Composable
private fun WelcomeBanner(userName: String, isDark: Boolean) {
    val accent = glassAccent(isDark)
    DashCard(isDark) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row {
                Text("Welcome back, ", color = glassTextPrimary(isDark), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    userName.replaceFirstChar { it.uppercase() }.ifEmpty { "User" },
                    color = accent,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FormatQuote, contentDescription = null,
                        tint = accent, modifier = Modifier.size(16.dp))
                    Text("DAILY INSPIRATION", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = glassTextSecondary(isDark), letterSpacing = 1.sp)
                }
                Text(
                    "\"Your limit is mostly your imagination.\"",
                    color = glassTextSecondary(isDark),
                    fontSize = 13.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

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
        val accent = glassAccent(isDark)
        DashCard(isDark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Default.Today, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                CardTitle("Study Plan Progress", isDark)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Create a study plan to track your syllabus completion.",
                color = glassTextSecondary(isDark),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            SafarGlassButton(
                text = "Create Plan",
                icon = Icons.Default.Add,
                onClick = ::openPlanner,
                isLight = !isDark,
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

@Composable
private fun ActiveTitleCard(
    title: String,
    titleId: String,
    isDark: Boolean,
    hasEarnedAchievements: Boolean,
    onNavigateToAchievements: () -> Unit,
) {
    val imagePath = titleId.takeIf { it.isNotEmpty() }?.let { achievementImages[it] }
    val imageUrl = imagePath?.let { path ->
        val origin = BuildConfig.BASE_URL.trimEnd('/').let {
            val uri = android.net.Uri.parse(it)
            "${uri.scheme}://${uri.host}"
        }
        "$origin$path"
    }
    val isLight = !isDark
    val accent = glassAccent(isDark)
    val textPrimary = glassTextPrimary(isDark)
    val textSecondary = glassTextSecondary(isDark)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .safarFrostedPanel(
                isLight = isLight,
                shape = RoundedCornerShape(SafarGlassChromeRadius),
                tintAlpha = if (isDark) 0.12f else 0.42f,
            )
            .clickable { onNavigateToAchievements() }
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "CURRENT TITLE",
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = textSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (title.isNotEmpty()) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.width(180.dp).height(90.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLight) accent.copy(alpha = 0.16f)
                                else Color.White.copy(alpha = 0.16f),
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_zap),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (isLight) accent else Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            } else {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLight) accent.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.16f),
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_zap),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isLight) accent else Color.White,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (hasEarnedAchievements) "Tap to set achievement title" else "Unlock achievements to earn titles",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MoodCard(todayMood: Mood?, isDark: Boolean, onNavigate: (String) -> Unit) {
    val accent = glassAccent(isDark)
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null,
                tint = accent, modifier = Modifier.size(16.dp))
            CardTitle(stringResource(R.string.dashboard_today_mood), isDark)
        }
        Spacer(Modifier.height(8.dp))
        if (todayMood != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(moodEmoji(todayMood.mood), fontSize = 32.sp)
                Column {
                    Text(
                        todayMood.mood.replaceFirstChar { it.uppercase() },
                        color = glassTextPrimary(isDark),
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.dashboard_intensity, todayMood.intensity),
                        color = glassTextSecondary(isDark), fontSize = 12.sp
                    )
                }
            }
        } else {
            Text(stringResource(R.string.dashboard_no_checkin),
                color = glassTextSecondary(isDark), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            SafarGlassButton(
                text = "Check In Now",
                icon = Icons.Default.FavoriteBorder,
                onClick = { onNavigate(Routes.NISHTHA) },
                isLight = !isDark,
                customTint = if (isDark) SafarGlassPalette.Pink else SafarGlassPalette.LightPink,
            )
        }
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
private fun StreaksCard(streaks: Streaks, isDark: Boolean, onNavigate: (String) -> Unit) {
    val accent = glassAccent(isDark)
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Loop, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            CardTitle(stringResource(R.string.dashboard_streaks), isDark)
            Spacer(Modifier.weight(1f))
            Text(
                "View →",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigate(Routes.nishthaTab(3)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        StreakRow(stringResource(R.string.dashboard_streak_checkin), "${streaks.checkInStreak}", isDark)
        GlassDivider()
        StreakRow(stringResource(R.string.dashboard_streak_login), "${streaks.loginStreak}", isDark)
        GlassDivider()
        StreakRow(stringResource(R.string.dashboard_streak_goal), "${streaks.goalCompletionStreak}", isDark)
    }
}

@Composable
private fun StreakRow(label: String, value: String, isDark: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = glassTextPrimary(isDark), fontSize = 14.sp)
        Text(value, color = glassTextPrimary(isDark), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

private val achievementImages: Map<String, String> = mapOf(
    "G001" to "/Achievments/Badges/Badge (1).webp",
    "G002" to "/Achievments/Badges/Badge (2).webp",
    "G003" to "/Achievments/Badges/Badge (3).webp",
    "G004" to "/Achievments/Badges/Badge (4).webp",
    "F001" to "/Achievments/Badges/Special_Badge (2).webp",
    "F002" to "/Achievments/Badges/Special_Badge (5).webp",
    "F003" to "/Achievments/Badges/Special_Badge (4).webp",
    "F004" to "/Achievments/Badges/Badge (6).webp",
    "F005" to "/Achievments/Badges/Badge (7).webp",
    "S001" to "/Achievments/Badges/Badge (8).webp",
    "S002" to "/Achievments/Badges/Special_Badge (1).webp",
    "ET006" to "/Achievments/Badges/Special_Badge (3).webp",
    "SP001" to "/Achievments/Badges/Badge (4).webp",
    "SP002" to "/Achievments/Badges/Badge (8).webp",
    "D001" to "/Achievments/Badges/Special_Badge (1).webp",
    "D002" to "/Achievments/Badges/Special_Badge (3).webp",
    "K001" to "/Achievments/Badges/Badge (6).webp",
    "M001" to "/Achievments/Badges/Badge (7).webp",
    "T005" to "/Achievments/Titles/Title (5).webp",
    "T006" to "/Achievments/Titles/Title (3).webp",
    "T007" to "/Achievments/Titles/Title (7).webp",
    "T008" to "/Achievments/Titles/Title (6).webp",
    "T001" to "/Achievments/Titles/Title (8).webp",
    "T002" to "/Achievments/Titles/Title (2).webp",
    "T003" to "/Achievments/Titles/Title (1).webp",
    "T004" to "/Achievments/Titles/Title (4).webp",
    "ET001" to "/Achievments/Titles/Special_Title (2).webp",
    "ET002" to "/Achievments/Titles/Special_Title (1).webp",
    "ET003" to "/Achievments/Titles/Special_Title (5).webp",
    "ET004" to "/Achievments/Titles/Special_Title (3).webp",
    "ET005" to "/Achievments/Titles/Special_Title (4).webp",
    "T010" to "/Achievments/Titles/Special_Title (1).webp",
    "T011" to "/Achievments/Titles/Special_Title (2).webp",
    "T012" to "/Achievments/Titles/Special_Title (3).webp",
    "T013" to "/Achievments/Titles/Special_Title (4).webp",
    "T014" to "/Achievments/Titles/Special_Title (5).webp",
    "T009" to "/Achievments/svgviewer-output.svg",
)

@Composable
private fun BadgesCard(earned: List<Achievement>, all: List<Achievement>, isDark: Boolean, onNavigate: (String) -> Unit) {
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    val accent = glassAccent(isDark)

    selectedAchievement?.let { achievement ->
        AchievementDetailDialog(
            achievement = achievement,
            isDark = isDark,
            onDismiss = { selectedAchievement = null },
        )
    }

    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null,
                    tint = accent, modifier = Modifier.size(16.dp))
                CardTitle(stringResource(R.string.dashboard_achievements), isDark)
            }
            Text(
                "View All",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigate(Routes.ACHIEVEMENTS) },
            )
        }
        Spacer(Modifier.height(12.dp))
        val display = remember(earned, all) {
            (if (earned.isNotEmpty()) earned else all.take(3)).take(6)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(display, key = { it.id }) { achievement ->
                val onAchievementClick = remember(achievement) { { selectedAchievement = achievement } }
                Column(
                    modifier = Modifier
                        .width(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(glassNestedBg(isDark))
                        .clickable(onClick = onAchievementClick)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val imagePath = achievementImages[achievement.id]
                    if (imagePath != null) {
                        val imageUrl = remember(imagePath) {
                            val origin = BuildConfig.BASE_URL.trimEnd('/').let {
                                val uri = android.net.Uri.parse(it)
                                "${uri.scheme}://${uri.host}"
                            }
                            "$origin$imagePath"
                        }
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = achievement.name,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = if (achievement.type == "title") R.drawable.ic_crown else R.drawable.ic_medal
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = accent,
                        )
                    }
                    Text(
                        achievement.name,
                        color = glassTextPrimary(isDark),
                        fontSize = 9.sp, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    if (achievement.earned) {
                        Text("Active Badge", color = accent,
                            fontSize = 8.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementDetailDialog(achievement: Achievement, isDark: Boolean, onDismiss: () -> Unit) {
    val isLight = !isDark
    val imagePath = achievementImages[achievement.id]
    val imageUrl = imagePath?.let { path ->
        val origin = BuildConfig.BASE_URL.trimEnd('/').let {
            val uri = android.net.Uri.parse(it)
            "${uri.scheme}://${uri.host}"
        }
        "$origin$path"
    }
    val tierLabel = achievement.tier?.let { "Tier $it" } ?: ""
    val typeLabel = achievement.type.replaceFirstChar { it.uppercase() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        SafarGlassCard(
            isLight = isLight,
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = achievement.name,
                        modifier = Modifier.size(88.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(glassNestedBg(isDark)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = if (achievement.type == "title") R.drawable.ic_crown else R.drawable.ic_medal,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = glassAccent(isDark),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (typeLabel.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(glassAccent(isDark).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                typeLabel.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = glassAccent(isDark),
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                    if (tierLabel.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(glassNestedBg(isDark))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                tierLabel.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = glassTextSecondary(isDark),
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }

                Text(
                    achievement.name,
                    color = glassTextPrimary(isDark),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                if (!achievement.description.isNullOrBlank()) {
                    Text(
                        achievement.description,
                        fontSize = 13.sp,
                        color = glassTextSecondary(isDark),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }

                if (tierLabel.isNotEmpty()) {
                    Text(
                        "A $tierLabel achievement.",
                        fontSize = 13.sp,
                        color = glassTextSecondary(isDark),
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(4.dp))

                SafarGlassButton(
                    text = "Close",
                    icon = Icons.Default.Check,
                    onClick = onDismiss,
                    isLight = isLight,
                    customTint = Color(0xFF2E7D32),
                )
            }
        }
    }
}

@Composable
private fun TodayGoalsCard(goals: List<Goal>, isDark: Boolean, onNavigate: (String) -> Unit) {
    val visibleGoals = remember(goals) { goals.take(3) }
    val completed = remember(goals) { goals.count { it.completed } }
    val progress = remember(goals, completed) { if (goals.isNotEmpty()) completed.toFloat() / goals.size else 0f }
    val accent = glassAccent(isDark)
    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.TrackChanges, contentDescription = null,
                    tint = accent, modifier = Modifier.size(16.dp))
                CardTitle(stringResource(R.string.dashboard_today_goals), isDark)
            }
            Text(
                stringResource(R.string.dashboard_goals_count, completed, goals.size),
                color = glassTextSecondary(isDark),
                fontSize = 12.sp,
            )
        }
        if (goals.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Stay focused and consistent.", color = glassTextSecondary(isDark), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = glassNestedBg(isDark),
            )
            Spacer(Modifier.height(12.dp))
            visibleGoals.forEach { goal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(glassNestedBg(isDark))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(
                                1.5.dp,
                                if (goal.completed) accent else glassTextSecondary(isDark),
                                CircleShape,
                            )
                            .background(if (goal.completed) accent else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (goal.completed) {
                            Text(
                                "✓",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        goal.title,
                        color = glassTextPrimary(isDark),
                        fontSize = 13.sp, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.dashboard_no_goals),
                color = glassTextSecondary(isDark), fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        SafarGlassButton(
            text = "Manage Goals",
            icon = Icons.Default.TrackChanges,
            onClick = { onNavigate(Routes.nishthaTab(2)) },
            isLight = !isDark,
            customTint = if (isDark) SafarGlassPalette.Violet else SafarGlassPalette.LightViolet,
        )
    }
}

@Composable
private fun MonthlyCard(report: MonthlyReport, isDark: Boolean, onNavigate: (String) -> Unit) {
    val accent = glassAccent(isDark)
    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.BarChart, contentDescription = null,
                    tint = accent, modifier = Modifier.size(16.dp))
                CardTitle(stringResource(R.string.dashboard_monthly_snapshot), isDark)
            }
            Text(report.month, color = glassTextSecondary(isDark), fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "A quick look at your performance this month.",
            color = glassTextSecondary(isDark),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        StatRow(stringResource(R.string.dashboard_consistency), "${report.consistencyScore.toInt()}%", isDark)
        GlassDivider()
        StatRow(stringResource(R.string.dashboard_completion), "${report.completionRate.toInt()}%", isDark)
        GlassDivider()
        StatRow(stringResource(R.string.dashboard_focus), "${report.focusDepth.toInt()}m/day", isDark)
        Spacer(Modifier.height(10.dp))
        SafarGlassButton(
            text = "View Full Report",
            icon = Icons.Default.BarChart,
            onClick = { onNavigate(Routes.nishthaTab(4)) },
            isLight = !isDark,
            customTint = if (isDark) SafarGlassPalette.Lavender else SafarGlassPalette.LightLavender,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, isDark: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = glassTextPrimary(isDark), fontSize = 14.sp)
        Text(value, color = glassAccent(isDark), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeeklyMoodChart(moods: List<Mood>, isDark: Boolean, onNavigate: (String) -> Unit) {
    var showDetailDialog by remember { mutableStateOf(false) }
    if (showDetailDialog) {
        WeeklyMoodDetailDialog(moods = moods, isDark = isDark, onDismiss = { showDetailDialog = false })
    }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val accent = glassAccent(isDark)
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.ShowChart, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            CardTitle(stringResource(R.string.dashboard_weekly_mood), isDark)
            Spacer(Modifier.weight(1f))
            Text(
                "View →",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showDetailDialog = true },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Your emotional journey from Monday to Sunday.",
            color = glassTextSecondary(isDark),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))

        val intensityValues = remember(moods) { moods.take(7).map { it.intensity.toFloat() } }
        val primaryColor = accent
        val surfaceColor = glassNestedBg(isDark)

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val maxVal = 5f
            val pts = intensityValues.mapIndexed { i, v ->
                val x = if (intensityValues.size > 1) i * w / (intensityValues.size - 1) else w / 2
                val y = h - (v / maxVal) * h * 0.85f
                Offset(x, y)
            }
            // Grid lines
            for (i in 0..2) {
                val y = h - (i * h / 2f)
                drawLine(surfaceColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }
            // Fill path
            if (pts.size > 1) {
                val fillPath = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h)
                    close()
                }
                drawPath(fillPath, primaryColor.copy(alpha = 0.12f))
                // Line
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
                        Text("—", fontSize = 12.sp, color = glassTextSecondary(isDark))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(d, fontSize = 10.sp, color = glassTextSecondary(isDark), textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(primaryColor, "Intensity", isDark)
            LegendDot(primaryColor.copy(alpha = 0.5f), "Mood", isDark)
        }
    }
}

@Composable
private fun WeeklyMoodDetailDialog(moods: List<Mood>, isDark: Boolean, onDismiss: () -> Unit) {
    val isLight = !isDark
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        SafarGlassCard(
            isLight = isLight,
            contentPadding = PaddingValues(24.dp),
        ) {
            Text(
                "Weekly Mood Log",
                color = glassTextPrimary(isDark),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            if (moods.isEmpty()) {
                Text("No moods logged this week.", color = glassTextSecondary(isDark))
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.heightIn(max = 400.dp),
                ) {
                    items(moods) { mood ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(moodEmoji(mood.mood), fontSize = 28.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    mood.mood.replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = glassTextPrimary(isDark),
                                )
                                val dateText = try {
                                    val parser = java.text.SimpleDateFormat(
                                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                        java.util.Locale.getDefault(),
                                    ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                    val formatter = java.text.SimpleDateFormat(
                                        "EEE, MMM d",
                                        java.util.Locale.getDefault(),
                                    )
                                    val date = parser.parse(mood.timestamp)
                                    if (date != null) formatter.format(date) else mood.timestamp
                                } catch (_: Exception) {
                                    mood.timestamp
                                }
                                Text(dateText, fontSize = 12.sp, color = glassTextSecondary(isDark))
                                if (!mood.notes.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Why: ${mood.notes}",
                                        fontSize = 13.sp,
                                        color = glassTextPrimary(isDark),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SafarGlassButton(
                text = "Close",
                icon = Icons.Default.Check,
                onClick = onDismiss,
                isLight = isLight,
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = glassTextSecondary(isDark))
    }
}



@Composable
private fun DashCard(isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    SafarGlassCard(
        isLight = !isDark,
        content = content,
    )
}

@Composable
private fun CardTitle(text: String, isDark: Boolean) {
    Text(text, color = glassTextPrimary(isDark), fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
private fun ConfettiCelebration(
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    if (!isActive) return

    val colors = listOf(
        Color(0xFFFFC107), // Amber
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFF9C27B0), // Purple
        Color(0xFFE91E63)  // Pink
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
            val frame = ticks
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

@Composable
private fun CelebrationDialog(
    achievements: List<Achievement>,
    isDark: Boolean,
    onDismiss: () -> Unit,
) {
    val isLight = !isDark
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .safarFrostedPanel(
                    isLight = isLight,
                    shape = RoundedCornerShape(SafarGlassChromeRadius),
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            ConfettiCelebration(modifier = Modifier.matchParentSize())

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Congratulations! 🎉",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = glassAccent(isDark),
                    textAlign = TextAlign.Center,
                )

                Text(
                    if (achievements.size > 1) {
                        "You have unlocked new achievements!"
                    } else {
                        "You unlocked a new achievement!"
                    },
                    fontSize = 14.sp,
                    color = glassTextSecondary(isDark),
                    textAlign = TextAlign.Center,
                )

                achievements.forEach { achievement ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val imagePath = achievementImages[achievement.id]
                        if (imagePath != null) {
                            val imageUrl = remember(imagePath) {
                                val origin = BuildConfig.BASE_URL.trimEnd('/').let {
                                    val uri = android.net.Uri.parse(it)
                                    "${uri.scheme}://${uri.host}"
                                }
                                "$origin$imagePath"
                            }
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = achievement.name,
                                modifier = Modifier.size(96.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text(
                                if (achievement.type == "title") "👑" else "🏅",
                                fontSize = 48.sp,
                            )
                        }

                        Text(
                            achievement.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = glassTextPrimary(isDark),
                            textAlign = TextAlign.Center,
                        )

                        if (!achievement.description.isNullOrBlank()) {
                            Text(
                                achievement.description,
                                fontSize = 13.sp,
                                color = glassTextSecondary(isDark),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                SafarGlassButton(
                    text = "Awesome!",
                    icon = Icons.Default.Star,
                    onClick = onDismiss,
                    isLight = isLight,
                )
            }
        }
    }
}
