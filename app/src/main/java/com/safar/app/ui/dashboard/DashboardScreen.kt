package com.safar.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.safar.app.BuildConfig
import com.safar.app.R
import com.safar.app.domain.model.*
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.theme.*

@Composable
fun DashboardScreen(
    currentRoute: String = Routes.DASHBOARD,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SafarDrawerScaffold(
        title = stringResource(R.string.dashboard_title),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        topBarActions = {
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.userName.firstOrNull()?.uppercase() ?: "U",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                ) {
                    item { WelcomeBanner(uiState, isDarkTheme) }
                    item { InspirationCard(isDarkTheme) }
                    if (uiState.activeTitle.isNotEmpty()) {
                        item { ActiveTitleCard(uiState.activeTitle, uiState.activeTitleId, isDarkTheme) }
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
                    if (uiState.completedGoals.isNotEmpty()) {
                        item { GoalHistoryCard(uiState.completedGoals, isDarkTheme, onNavigate) }
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
                    onDismiss = { viewModel.dismissWelcome() },
                )
            }
        }
    }
}

@Composable
private fun DashboardWelcomeOverlay(userName: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🌿", fontSize = 48.sp, textAlign = TextAlign.Center)
                Text(
                    text = "Welcome back,\n${userName.replaceFirstChar { it.uppercase() }.ifEmpty { "Friend" }} 🙏",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                )
                Text(
                    text = "Your journey continues here.\nEvery small step forward counts — today is a new opportunity to grow, reflect, and be present.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Let's begin ✨", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WelcomeBanner(uiState: DashboardUiState, isDark: Boolean) {
    DashCard(isDark) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Green500))
                    Text("ONLINE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = Green500, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("Welcome back, ", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        uiState.userName.replaceFirstChar { it.uppercase() }.ifEmpty { "User" },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InspirationCard(isDark: Boolean) {
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FormatQuote, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text("DAILY INSPIRATION", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "\"Your limit is mostly your imagination.\"",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 13.sp,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ActiveTitleCard(title: String, titleId: String, isDark: Boolean) {
    val imagePath = achievementImages[titleId]
    val imageUrl = imagePath?.let { path ->
        val origin = BuildConfig.BASE_URL.trimEnd('/').let {
            val uri = android.net.Uri.parse(it)
            "${uri.scheme}://${uri.host}"
        }
        "$origin$path"
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(BgDark)
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("CURRENT TITLE", fontSize = 10.sp, letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    Modifier.size(72.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(BrandTeal, BgDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚡", fontSize = 28.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MoodCard(todayMood: Mood?, isDark: Boolean, onNavigate: (String) -> Unit) {
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            CardTitle(stringResource(R.string.dashboard_today_mood), isDark)
        }
        Spacer(Modifier.height(8.dp))
        if (todayMood != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(moodEmoji(todayMood.mood), fontSize = 32.sp)
                Column {
                    Text(
                        todayMood.mood.replaceFirstChar { it.uppercase() },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.dashboard_intensity, todayMood.intensity),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp
                    )
                }
            }
        } else {
            Text(stringResource(R.string.dashboard_no_checkin),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onNavigate(Routes.NISHTHA) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Check In Now →", fontWeight = FontWeight.SemiBold)
            }
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
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Loop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            CardTitle(stringResource(R.string.dashboard_streaks), isDark)
            Spacer(Modifier.weight(1f))
            Text("View →", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onNavigate(Routes.NISHTHA_STREAKS) })
        }
        Spacer(Modifier.height(12.dp))
        // Table style like screenshot
        StreakRow(stringResource(R.string.dashboard_streak_checkin), "${streaks.checkInStreak}", isDark)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        StreakRow(stringResource(R.string.dashboard_streak_login), "${streaks.loginStreak}", isDark)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
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
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
    "T009" to "/Achievments/svgviewer-output.svg",
)

@Composable
private fun BadgesCard(earned: List<Achievement>, all: List<Achievement>, isDark: Boolean, onNavigate: (String) -> Unit) {
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    selectedAchievement?.let { achievement ->
        AchievementDetailDialog(achievement = achievement, onDismiss = { selectedAchievement = null })
    }

    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                CardTitle(stringResource(R.string.dashboard_achievements), isDark)
            }
            Text("View All", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigate(Routes.ACHIEVEMENTS) })
        }
        Spacer(Modifier.height(12.dp))
        val display = if (earned.isNotEmpty()) earned else all.take(3)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(display.take(6)) { achievement ->
                Column(
                    modifier = Modifier.width(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { selectedAchievement = achievement }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val imagePath = achievementImages[achievement.id]
                    if (imagePath != null) {
                        AsyncImage(
                            model = run {
                                val origin = BuildConfig.BASE_URL.trimEnd('/').let {
                                    val uri = android.net.Uri.parse(it)
                                    "${uri.scheme}://${uri.host}"
                                }
                                "$origin$imagePath"
                            },
                            contentDescription = achievement.name,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text(if (achievement.type == "title") "👑" else "🏅", fontSize = 26.sp)
                    }
                    Text(
                        achievement.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    if (achievement.earned) {
                        Text("Active Badge", color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementDetailDialog(achievement: Achievement, onDismiss: () -> Unit) {
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
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Badge image
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = achievement.name,
                        modifier = Modifier.size(88.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (achievement.type == "title") "👑" else "🏅", fontSize = 36.sp)
                    }
                }

                // Type + Tier chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (typeLabel.isNotEmpty()) {
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(typeLabel.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                        }
                    }
                    if (tierLabel.isNotEmpty()) {
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(tierLabel.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary, letterSpacing = 0.5.sp)
                        }
                    }
                }

                // Achievement name
                Text(
                    achievement.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                )

                // Description
                if (!achievement.description.isNullOrBlank()) {
                    Text(
                        achievement.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                    )
                }

                // Holder count
                if (achievement.holderCount > 0) {
                    Text(
                        "Held by ${achievement.holderCount} people.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                // Tier description
                if (tierLabel.isNotEmpty()) {
                    Text(
                        "A $tierLabel achievement.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TodayGoalsCard(goals: List<Goal>, isDark: Boolean, onNavigate: (String) -> Unit) {
    val completed = goals.count { it.completed }
    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.TrackChanges, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                CardTitle(stringResource(R.string.dashboard_today_goals), isDark)
            }
            Text(stringResource(R.string.dashboard_goals_count, completed, goals.size),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        if (goals.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Stay focused and consistent.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (goals.isNotEmpty()) completed.toFloat() / goals.size else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(12.dp))
            goals.take(3).forEach { goal ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier.size(18.dp).clip(CircleShape)
                            .border(1.5.dp, if (goal.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape)
                            .background(if (goal.completed) MaterialTheme.colorScheme.primary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (goal.completed) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        goal.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.dashboard_no_goals),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onNavigate(Routes.NISHTHA_GOALS) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Manage Goals →", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthlyCard(report: MonthlyReport, isDark: Boolean, onNavigate: (String) -> Unit) {
    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.BarChart, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                CardTitle(stringResource(R.string.dashboard_monthly_snapshot), isDark)
            }
            Text(report.month, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("A quick look at your performance this month.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        // Table style like screenshot
        StatRow(stringResource(R.string.dashboard_consistency), "${report.consistencyScore.toInt()}%", isDark)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        StatRow(stringResource(R.string.dashboard_completion), "${report.completionRate.toInt()}%", isDark)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        StatRow(stringResource(R.string.dashboard_focus), "${report.totalFocusMinutes}", isDark)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { onNavigate(Routes.NISHTHA_ANALYTICS) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("View Full Report →", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, isDark: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeeklyMoodChart(moods: List<Mood>, isDark: Boolean, onNavigate: (String) -> Unit) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    DashCard(isDark) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            CardTitle(stringResource(R.string.dashboard_weekly_mood), isDark)
            Spacer(Modifier.weight(1f))
            Text("View →", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onNavigate(Routes.NISHTHA_STREAKS) })
        }
        Spacer(Modifier.height(4.dp))
        Text("Your emotional journey from Monday to Sunday.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        val intensityValues = moods.take(7).map { it.intensity.toFloat() }
        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

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
            days.forEach { d ->
                Text(d, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(primaryColor, "Intensity")
            LegendDot(primaryColor.copy(alpha = 0.5f), "Mood")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun GoalHistoryCard(goals: List<Goal>, isDark: Boolean, onNavigate: (String) -> Unit) {
    DashCard(isDark) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.History, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                CardTitle("Goal History", isDark)
            }
            Text("View Full History", color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigate(Routes.NISHTHA_GOALS) })
        }
        Spacer(Modifier.height(4.dp))
        Text("Review your past accomplishments.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("Showing the last ${goals.size} completed goals.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        goals.forEach { goal ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(goal.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("Completed", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        }
    }
}

@Composable
private fun DashCard(isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun CardTitle(text: String, isDark: Boolean) {
    Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}
