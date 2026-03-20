package com.safar.app.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.domain.model.*
import com.safar.app.ui.navigation.Screen
import com.safar.app.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = !MaterialTheme.colorScheme.background.isLight()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = { HomeTopBar(isDark, navController, uiState) },
        bottomBar = { SafarBottomNav(navController, isDark) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(BrandMidnight, BgDark, BrandPurpleDeep)
                        else listOf(BgLight, Color.White, BrandMint)
                    )
                )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryDark)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                ) {
                    item { WelcomeBanner(uiState, isDark) }
                    item { DailyQuoteCard(isDark) }
                    item { MoodCheckInCard(uiState.todayMood, isDark) }
                    item { StreaksCard(uiState.streaks, isDark) }
                    if (uiState.earnedAchievements.isNotEmpty() || uiState.allAchievements.isNotEmpty()) {
                        item { BadgesCard(uiState.earnedAchievements, uiState.allAchievements, isDark) }
                    }
                    item { TodayGoalsCard(uiState.todayGoals, isDark) }
                    uiState.monthlyReport?.let { item { MonthlySnapshotCard(it, isDark) } }
                    item { WeeklyMoodChart(uiState.weeklyMoods, isDark) }
                    if (uiState.completedGoals.isNotEmpty()) {
                        item { GoalHistoryCard(uiState.completedGoals, isDark) }
                    }
                    item { ResourcesCard(isDark, uriHandler) }
                    item { FooterCard(isDark, uriHandler) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(isDark: Boolean, navController: NavController, uiState: HomeUiState) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryDark))
                Column {
                    Text("SAFAR", color = if (isDark) BrandMint else BrandMidnight, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(stringResource(R.string.home_online), color = PrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                if (!uiState.userAvatar.isNullOrEmpty()) {
                    AsyncImage(
                        model = uiState.userAvatar, contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(32.dp).clip(CircleShape).border(1.5.dp, PrimaryDark, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Brush.radialGradient(listOf(PrimaryDark, PrimaryLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(uiState.userName.firstOrNull()?.uppercase() ?: "U", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun WelcomeBanner(uiState: HomeUiState, isDark: Boolean) {
    HomeCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_welcome_back), color = if (isDark) FieldHint else FieldHintLight, fontSize = 13.sp)
                Text(
                    uiState.userName.replaceFirstChar { it.uppercase() } + " 👋",
                    color = if (isDark) BrandMint else BrandMidnight,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                if (uiState.activeTitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = if (isDark) BrandPlumDark else BrandMint) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("✨ ${uiState.activeTitle}", color = if (isDark) BrandTeal else PrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.home_tap_achievement), color = if (isDark) FieldHint else FieldHintLight, fontSize = 10.sp)
                        }
                    }
                }
            }
            if (!uiState.userAvatar.isNullOrEmpty()) {
                AsyncImage(
                    model = uiState.userAvatar, contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape).border(2.dp, PrimaryDark, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.radialGradient(listOf(PrimaryDark, PrimaryLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.userName.firstOrNull()?.uppercase() ?: "U", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DailyQuoteCard(isDark: Boolean) {
    HomeCard(isDark) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("💬", fontSize = 20.sp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CardSectionTitle(stringResource(R.string.home_daily_inspiration), isDark)
                Text(
                    stringResource(R.string.home_quote),
                    color = if (isDark) BrandMint else BrandMidnight,
                    fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MoodCheckInCard(todayMood: Mood?, isDark: Boolean) {
    val moods = listOf("😄" to "Joyful", "😊" to "Happy", "😐" to "Neutral", "😢" to "Sad", "😰" to "Anxious")

    HomeCard(isDark) {
        CardSectionTitle(stringResource(R.string.home_today_mood), isDark)
        Spacer(Modifier.height(4.dp))
        if (todayMood != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(moodEmoji(todayMood.mood), fontSize = 28.sp)
                Column {
                    Text(todayMood.mood.replaceFirstChar { it.uppercase() }, color = if (isDark) BrandMint else BrandMidnight, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.home_intensity_label, todayMood.intensity), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
                    todayMood.notes?.let { Text(it.take(60), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        } else {
            Text(stringResource(R.string.home_mood_question), color = if (isDark) FieldHint else FieldHintLight, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.home_no_checkin), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                moods.forEach { (emoji, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (isDark) BrandPlumDark else BrandMint).clickable {}.padding(vertical = 8.dp)
                    ) {
                        Text(emoji, fontSize = 20.sp)
                        Text(label, fontSize = 9.sp, color = if (isDark) BrandTeal else PrimaryLightDim, textAlign = TextAlign.Center)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientActionButton(stringResource(R.string.home_check_in_now), isDark)
        }
    }
}

private fun moodEmoji(mood: String) = when (mood.lowercase()) {
    "joyful" -> "😄"; "happy" -> "😊"; "neutral" -> "😐"
    "sad" -> "😢"; "anxious" -> "😰"; "angry" -> "😠"
    else -> "😊"
}

@Composable
private fun StreaksCard(streaks: Streaks, isDark: Boolean) {
    HomeCard(isDark) {
        CardSectionTitle(stringResource(R.string.home_current_streaks), isDark)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakItem("🔥", stringResource(R.string.home_streak_checkin), "${streaks.checkInStreak}", isDark, Modifier.weight(1f))
            StreakItem("⚡", stringResource(R.string.home_streak_login),   "${streaks.loginStreak}",   isDark, Modifier.weight(1f))
            StreakItem("🎯", stringResource(R.string.home_streak_goal),    "${streaks.goalCompletionStreak}", isDark, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StreakItem(emoji: String, label: String, value: String, isDark: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(if (isDark) BrandPlumDark else BrandMint).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = if (isDark) BrandMint else BrandMidnight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (isDark) FieldHint else FieldHintLight, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BadgesCard(earned: List<Achievement>, all: List<Achievement>, isDark: Boolean) {
    HomeCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CardSectionTitle(stringResource(R.string.home_achievements), isDark)
            Text(stringResource(R.string.home_view_all), color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {})
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.home_achievements_count, earned.size, all.size), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        val display = if (earned.isNotEmpty()) earned else all.take(3)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(display.take(6)) { BadgeItem(it, isDark) }
        }
    }
}

@Composable
private fun BadgeItem(achievement: Achievement, isDark: Boolean) {
    Column(
        modifier = Modifier.width(90.dp).clip(RoundedCornerShape(14.dp)).background(if (isDark) BrandPlumDark else BrandMint).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(if (achievement.type == "title") "👑" else "🏅", fontSize = 26.sp)
        Text(achievement.name, color = if (isDark) BrandMint else BrandMidnight, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (achievement.earned) {
            Surface(shape = RoundedCornerShape(6.dp), color = PrimaryDark) {
                Text(stringResource(R.string.home_active_badge), color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        } else {
            LinearProgressIndicator(
                progress = { achievement.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = PrimaryDark,
                trackColor = if (isDark) BrandMidnight else DividerLight
            )
        }
    }
}

@Composable
private fun TodayGoalsCard(goals: List<Goal>, isDark: Boolean) {
    val completed = goals.count { it.completed }
    val total = goals.size

    HomeCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CardSectionTitle(stringResource(R.string.home_today_goals), isDark)
            Text(stringResource(R.string.home_goals_count, completed, total), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.home_goals_subtitle), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        if (total > 0) {
            LinearProgressIndicator(
                progress = { if (total > 0) completed.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = PrimaryDark,
                trackColor = if (isDark) BrandMidnight else DividerLight
            )
            Spacer(Modifier.height(12.dp))
            goals.forEach { goal -> GoalItem(goal, isDark); Spacer(Modifier.height(8.dp)) }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isDark) BrandPlumDark else BrandMint).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📋", fontSize = 24.sp)
                    Text(stringResource(R.string.home_no_goals), color = if (isDark) BrandMint else BrandMidnight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.home_ready_to_plan), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientActionButton(stringResource(R.string.home_set_goals), isDark)
        }
    }
}

@Composable
private fun GoalItem(goal: Goal, isDark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isDark) BrandPlumDark else BrandMint).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape)
                .border(1.5.dp, if (goal.completed) PrimaryDark else if (isDark) FieldHint else FieldHintLight, CircleShape)
                .background(if (goal.completed) PrimaryDark else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (goal.completed) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(goal.title, color = if (isDark) BrandMint else BrandMidnight, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!goal.description.isNullOrEmpty()) {
                Text(goal.description, color = if (isDark) FieldHint else FieldHintLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Surface(shape = RoundedCornerShape(6.dp), color = priorityColor(goal.priority)) {
            Text(goal.priority.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

private fun priorityColor(priority: String) = when (priority.lowercase()) {
    "high"   -> Color(0xFFD85A30)
    "medium" -> Color(0xFFBA7517)
    else     -> Color(0xFF1D9E75)
}

@Composable
private fun MonthlySnapshotCard(report: MonthlyReport, isDark: Boolean) {
    HomeCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CardSectionTitle(stringResource(R.string.home_monthly_snapshot), isDark)
            Text(report.month, color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.home_monthly_subtitle), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatItem(stringResource(R.string.home_stat_consistency),    "${report.consistencyScore.toInt()}%", isDark, Modifier.weight(1f))
            StatItem(stringResource(R.string.home_stat_completion_rate),"${report.completionRate.toInt()}%",   isDark, Modifier.weight(1f))
            StatItem(stringResource(R.string.home_stat_focus_depth),    "${report.focusDepth}",                isDark, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(report.consistencyMessage, color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text("⚡ ${report.powerHourMessage}", color = if (isDark) BrandTeal else PrimaryLight, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedActionButton(stringResource(R.string.home_view_full_report), isDark)
    }
}

@Composable
private fun StatItem(label: String, value: String, isDark: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(if (isDark) BrandPlumDark else BrandMint).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = if (isDark) BrandMint else BrandMidnight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (isDark) FieldHint else FieldHintLight, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WeeklyMoodChart(moods: List<Mood>, isDark: Boolean) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val yLabels = listOf("10", "6", "3", "0")
    val maxIntensity = 10

    val dayValues = days.map { day ->
        moods.firstOrNull { mood ->
            runCatching {
                val date = java.time.LocalDateTime.parse(mood.timestamp.take(19))
                date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() } == day
            }.getOrDefault(false)
        }?.intensity ?: 0
    }

    HomeCard(isDark) {
        CardSectionTitle(stringResource(R.string.home_weekly_mood), isDark)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.home_weekly_subtitle), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            Column(modifier = Modifier.width(20.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                yLabels.forEach { label ->
                    Text(label, color = if (isDark) FieldHint else FieldHintLight, fontSize = 9.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.width(6.dp))
            Row(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                days.zip(dayValues).forEach { (day, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(((value.toFloat() / maxIntensity) * 90).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(Brush.verticalGradient(listOf(PrimaryDark, PrimaryLight)))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(day, color = if (isDark) FieldHint else FieldHintLight, fontSize = 9.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(stringResource(R.string.home_legend_intensity), PrimaryDark)
            LegendDot(stringResource(R.string.home_legend_mood), PrimaryLight)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text("* $label", fontSize = 11.sp, color = color)
    }
}

@Composable
private fun GoalHistoryCard(goals: List<Goal>, isDark: Boolean) {
    HomeCard(isDark) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CardSectionTitle(stringResource(R.string.home_goal_history), isDark)
            Text(stringResource(R.string.home_view_full_history), color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {})
        }
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.home_goal_history_subtitle), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        goals.forEach { goal ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isDark) BrandPlumDark else BrandMint).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, color = if (isDark) BrandMint else BrandMidnight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.home_completed_on, formatDate(goal.completedAt)), color = if (isDark) FieldHint else FieldHintLight, fontSize = 11.sp)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = PrimaryDark) {
                    Text(stringResource(R.string.home_completed), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(stringResource(R.string.home_showing_last_5), color = if (isDark) FieldHint else FieldHintLight, fontSize = 11.sp)
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return "Invalid Date"
    return runCatching {
        val date = java.time.LocalDateTime.parse(dateString.take(19))
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }.getOrDefault(dateString.take(10))
}

@Composable
private fun ResourcesCard(isDark: Boolean, uriHandler: androidx.compose.ui.platform.UriHandler) {
    HomeCard(isDark) {
        CardSectionTitle(stringResource(R.string.home_resources), isDark)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.home_resources_subtitle), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        ResourceItem("▶", stringResource(R.string.home_youtube_title), stringResource(R.string.home_youtube_tag), stringResource(R.string.home_youtube_subtitle), isDark) { uriHandler.openUri("https://www.youtube.com/@SafarParmar") }
        Spacer(Modifier.height(8.dp))
        ResourceItem("📚", stringResource(R.string.home_course_title), stringResource(R.string.home_course_tag), stringResource(R.string.home_course_subtitle), isDark) { uriHandler.openUri("https://parmarssc.in") }
        Spacer(Modifier.height(8.dp))
        ResourceItem("🧘", stringResource(R.string.home_meditation_title), stringResource(R.string.home_meditation_tag), stringResource(R.string.home_meditation_subtitle), isDark) { uriHandler.openUri("https://parmarssc.in") }
    }
}

@Composable
private fun ResourceItem(emoji: String, title: String, tag: String, subtitle: String, isDark: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (isDark) BrandPlumDark else BrandMint).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(emoji, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (isDark) BrandMint else BrandMidnight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = if (isDark) FieldHint else FieldHintLight, fontSize = 11.sp)
        }
        Surface(shape = RoundedCornerShape(8.dp), color = if (isDark) BrandMidnight else Color.White) {
            Text(tag, color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun FooterCard(isDark: Boolean, uriHandler: androidx.compose.ui.platform.UriHandler) {
    HomeCard(isDark) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.home_footer_title), color = if (isDark) BrandMint else BrandMidnight, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(stringResource(R.string.home_footer_tagline), color = if (isDark) FieldHint else FieldHintLight, fontSize = 12.sp, textAlign = TextAlign.Center)
            HorizontalDivider(color = if (isDark) DividerDark else DividerLight)
            Text(stringResource(R.string.home_footer_query), color = if (isDark) FieldHint else FieldHintLight, fontSize = 11.sp, textAlign = TextAlign.Center)
            FooterLink("onesaffar@gmail.com", isDark) { uriHandler.openUri("mailto:onesaffar@gmail.com") }
            Text(stringResource(R.string.home_footer_write), color = if (isDark) FieldHint else FieldHintLight, fontSize = 11.sp)
            FooterLink("safarparmar0@gmail.com", isDark) { uriHandler.openUri("mailto:safarparmar0@gmail.com") }
            FooterLink(stringResource(R.string.home_footer_instagram), isDark) { uriHandler.openUri("https://instagram.com/safar_parmar") }
            FooterLink(stringResource(R.string.home_footer_playstore), isDark) { uriHandler.openUri("https://play.google.com") }
            HorizontalDivider(color = if (isDark) DividerDark else DividerLight)
            Text(stringResource(R.string.home_footer_copyright), color = if (isDark) FooterDark else FooterLight, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FooterLink(text: String, isDark: Boolean, onClick: () -> Unit) {
    Text(text, color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onClick))
}

@Composable
private fun HomeCard(isDark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (isDark) BrandMidnightLight else Color.White)
            .border(0.5.dp, if (isDark) DividerDark else DividerLight, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun CardSectionTitle(text: String, isDark: Boolean) {
    Text(text, color = if (isDark) BrandMint else BrandMidnight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun GradientActionButton(text: String, isDark: Boolean, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(if (isDark) PrimaryDark else PrimaryLight, GradientMidDark)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OutlinedActionButton(text: String, isDark: Boolean, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (isDark) PrimaryDark else PrimaryLight, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun Color.isLight(): Boolean {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return luminance > 0.5f
}