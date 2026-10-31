package com.safarparmar.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.*
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.DashboardMonthlySummary
import com.safarparmar.app.ui.components.SafarCircularProgressIndicator
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.analytics.StudyPlannerAnalytics
import com.safarparmar.app.ui.theme.*

internal val DashboardPlum = Color(0xFF491438)
internal val DashboardPeach = Color(0xFFFFDFD5)
internal val DashboardLilac = Color(0xFFEDE0F4)

@Composable
internal fun DashboardWarmTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme.copy(background = DashboardFlatColors.Bg,
        surface = DashboardFlatColors.Paper, surfaceContainerHigh = DashboardFlatColors.Paper,
        onBackground = DashboardFlatColors.Text, onSurface = DashboardFlatColors.Text,
        onSurfaceVariant = DashboardFlatColors.Muted, primary = DashboardFlatColors.Accent,
        secondaryContainer = DashboardFlatColors.Soft, onSecondaryContainer = DashboardFlatColors.Text,
        outlineVariant = DashboardFlatColors.Hairline)
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography.copy(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = PoppinsFontFamily))) {
        CompositionLocalProvider(LocalSafarComposablesAccent provides DashboardFlatColors.Accent) {
            SafarComposablesTheme { content() }
        }
    }
}

@Composable
internal fun DashboardPaper(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(DashboardFlatColors.Paper)
        .border(1.dp, DashboardFlatColors.Hairline, RoundedCornerShape(22.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun DashboardGreeting(name: String, avatar: String?, premium: Boolean, onProfile: () -> Unit) {
    val displayName = name.ifBlank { stringResource(R.string.dashboard_default_user) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // Keep the badge outside the circular button's clipping layer.
        Box(Modifier.size(72.dp)) {
            Button(onClick = onProfile, style = ButtonStyle.Ghost,
                contentColor = DashboardFlatColors.Text, shape = CircleShape,
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(0.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(DashboardFlatColors.Soft), contentAlignment = Alignment.Center) {
                    Text(displayName.take(1).uppercase(), color = DashboardFlatColors.Accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    if (!avatar.isNullOrBlank()) AsyncImage(model = avatar, contentDescription = stringResource(R.string.nav_profile),
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            if (premium) Box(Modifier.size(25.dp).align(Alignment.BottomEnd)
                .background(Color(0xFFD5A63C), CircleShape).border(2.dp, DashboardFlatColors.Bg, CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(ImageVector.vectorResource(R.drawable.ic_sidebar_premium_crown),
                    contentDescription = stringResource(R.string.sidebar_premium_active), tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dashboard_warm_welcome, displayName), color = DashboardFlatColors.Text,
                fontFamily = PoppinsFontFamily, fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.dashboard_warm_small_progress), color = DashboardFlatColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
internal fun DashboardAchievementPreview(title: String, imageUrl: String?, hasEarnedAchievements: Boolean, onOpen: () -> Unit) {
    var artworkLoaded by remember(imageUrl) { mutableStateOf(false) }
    Button(onClick = onOpen, style = ButtonStyle.Ghost, contentColor = DashboardFlatColors.Text, shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().background(DashboardFlatColors.Paper, RoundedCornerShape(22.dp))
            .border(1.dp, DashboardFlatColors.Hairline, RoundedCornerShape(22.dp)), contentPadding = PaddingValues(18.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.dashboard_current_title), color = DashboardFlatColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                // Artwork is loaded without tint or cropping; the trophy remains a useful loading/error fallback.
                if (!artworkLoaded) Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = DashboardFlatColors.Accent,
                    modifier = Modifier.size(64.dp).background(DashboardFlatColors.Soft, CircleShape).padding(14.dp))
                if (!imageUrl.isNullOrBlank()) AsyncImage(model = imageUrl, contentDescription = title,
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                    onSuccess = { artworkLoaded = true }, onError = { artworkLoaded = false })
            }
            Text(title.ifBlank { stringResource(if (hasEarnedAchievements) R.string.dashboard_set_title else R.string.dashboard_unlock_titles) },
                color = DashboardFlatColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DashboardFlatColors.Accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun DashboardPlanHero(state: DashboardStudyPlanState, onNavigate: (String) -> Unit) {
    val hasPlan = state.status != DashboardStudyPlanStatus.NO_ACTIVE_PLAN
    val percent = state.overallCompletionPercent.coerceIn(0, 100)
    val open = {
        StudyPlannerAnalytics.track(StudyPlannerAnalytics.DASHBOARD_TODAY_CARD_CLICKED,
            mapOf("state" to state.status.name.lowercase(), "plan_id" to state.planId.orEmpty()))
        onNavigate(if (state.planId.isNullOrBlank()) Routes.STUDY_PLANNER else "${Routes.STUDY_PLANNER}?planId=${state.planId}")
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(DashboardPlum, Color(0xFF35102C), Color(0xFF65315B))))) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DashboardPeach,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).size(25.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().padding(20.dp)) {
            val wide = maxWidth > 480.dp
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dashboard_warm_study_plan), color = Color.White, fontSize = if (wide) 28.sp else 21.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.dashboard_warm_forward), color = DashboardPeach, fontSize = if (wide) 22.sp else 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(if (hasPlan) R.string.dashboard_warm_habits else R.string.dashboard_create_plan_help),
                        color = Color.White.copy(alpha = .85f), fontSize = 11.sp, lineHeight = 17.sp)
                    Button(onClick = open, style = ButtonStyle.Ghost, shape = CircleShape,
                        contentColor = DashboardPlum, modifier = Modifier.heightIn(min = 48.dp).background(Color(0xFFC9EFE0), CircleShape),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(stringResource(if (hasPlan) R.string.dashboard_warm_view_plan else R.string.dashboard_create_plan), color = DashboardPlum, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DashboardPlum, modifier = Modifier.size(16.dp))
                    }
                }
                if (hasPlan) Column(Modifier.width(if (wide) 170.dp else 112.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(if (wide) 145.dp else 106.dp), contentAlignment = Alignment.Center) {
                        SafarCircularProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxSize(),
                            color = Color(0xFFD6B5F0), trackColor = Color.White.copy(alpha = .14f),
                            strokeWidth = 9.dp, strokeCap = StrokeCap.Round, gapSize = 0.dp)
                        Text("$percent%", color = Color.White, fontSize = if (wide) 32.sp else 25.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.dashboard_warm_topics, state.overallDoneTopics, state.overallTotalTopics),
                        color = Color.White, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
internal fun DashboardTodayTiles(state: DashboardUiState, onMood: () -> Unit, onGoals: () -> Unit, onStreak: () -> Unit, onBadges: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.common_today), color = DashboardFlatColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardTodayTile(Icons.Default.Mood, stringResource(R.string.dashboard_warm_mood),
                state.todayMood?.mood?.replaceFirstChar { it.uppercase() } ?: stringResource(R.string.dashboard_check_in), DashboardPeach, onMood, Modifier.weight(1f))
            DashboardTodayTile(Icons.Default.CheckBox, stringResource(R.string.dashboard_warm_goals),
                stringResource(R.string.dashboard_goals_done, state.todayGoals.count { it.completed }, state.todayGoals.size), DashboardLilac, onGoals, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardTodayTile(Icons.Default.LocalFireDepartment, stringResource(R.string.dashboard_streak),
                stringResource(R.string.dashboard_days_active, state.streaks.checkInStreak), DashboardPeach, onStreak, Modifier.weight(1f))
            DashboardTodayTile(Icons.Default.EmojiEvents, stringResource(R.string.dashboard_badges),
                stringResource(R.string.dashboard_unlocked_count, state.earnedAchievements.size), DashboardLilac, onBadges, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardTodayTile(icon: ImageVector, title: String, value: String, tile: Color, onClick: () -> Unit, modifier: Modifier) {
    Button(onClick = onClick, style = ButtonStyle.Ghost, contentColor = DashboardFlatColors.Text, modifier = modifier.heightIn(min = 90.dp)
        .background(DashboardFlatColors.Paper, RoundedCornerShape(20.dp))
        .border(1.dp, DashboardFlatColors.Hairline, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = DashboardPlum,
                modifier = Modifier.size(40.dp).background(tile, RoundedCornerShape(12.dp)).padding(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = DashboardFlatColors.Muted, fontSize = 11.sp)
                Text(value, color = DashboardFlatColors.Text, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun DashboardGoalPreview(goals: List<Goal>, onOpen: () -> Unit) {
    DashboardPaper {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dashboard_today_goals), modifier = Modifier.weight(1f), color = DashboardFlatColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onOpen, style = ButtonStyle.Ghost, contentColor = DashboardFlatColors.Accent, contentPadding = PaddingValues(6.dp)) {
                Text(stringResource(R.string.dashboard_warm_view_all), color = DashboardFlatColors.Accent, fontSize = 11.sp)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DashboardFlatColors.Accent, modifier = Modifier.size(16.dp))
            }
        }
        if (goals.isEmpty()) Text(stringResource(R.string.dashboard_no_goals), color = DashboardFlatColors.Muted, fontSize = 13.sp)
        goals.take(3).forEachIndexed { index, goal ->
            if (index > 0) HorizontalSeparator(color = DashboardFlatColors.Hairline)
            val target = (goal.targetValue ?: 1).coerceAtLeast(1)
            val achieved = if (goal.completed) target else goal.achievedValue.coerceIn(0, target)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (goal.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = stringResource(if (goal.completed) R.string.goals_completed else R.string.dashboard_warm_incomplete),
                    tint = DashboardFlatColors.Accent, modifier = Modifier.size(24.dp))
                Text(goal.title.ifBlank { goal.text }, modifier = Modifier.weight(1f), color = DashboardFlatColors.Text, fontSize = 13.sp)
                Column(Modifier.width(80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProgressIndicator(progress = achieved.toFloat() / target, height = 5.dp,
                        modifier = Modifier.fillMaxWidth(), indicatorColor = Color(0xFFCBABE9), trackColor = DashboardFlatColors.Soft)
                    Text("$achieved/$target", fontSize = 10.sp, color = DashboardFlatColors.Muted)
                }
            }
        }
        if (goals.isEmpty()) Button(onClick = onOpen, style = ButtonStyle.Outlined, contentColor = DashboardFlatColors.Text) {
            Text(stringResource(R.string.dashboard_manage_goals), color = DashboardFlatColors.Text)
        }
    }
}

@Composable
internal fun DashboardMonthlyPreview(report: DashboardMonthlySummary?, onOpen: () -> Unit) {
    if (report == null) return
    DashboardPaper {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dashboard_monthly_snapshot), color = DashboardFlatColors.Text, fontSize = 18.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(report.month, color = DashboardFlatColors.Muted, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("${report.consistencyScore.toInt()}%" to R.string.dashboard_consistency,
                "${report.completionRate.toInt()}%" to R.string.dashboard_completion,
                "${report.focusDepth.toInt()}m" to R.string.dashboard_focus).forEach { (value, label) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, color = DashboardFlatColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(label), color = DashboardFlatColors.Muted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Button(onClick = onOpen, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp), shape = CircleShape) {
            Text(stringResource(R.string.dashboard_warm_view_report), color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
        }
    }
}
