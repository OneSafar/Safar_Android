package com.safarparmar.app.ui.nishtha.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.alpha
import coil.compose.AsyncImage
import com.safarparmar.app.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.MonthlyReport
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.theme.*
import androidx.compose.foundation.shape.CircleShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Liquid Glass design system ──────────────────────────────────────────────
import com.safarparmar.app.ui.glass.LiquidGlassBackdrop
import com.safarparmar.app.ui.glass.macOSControlPanel
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.GlassDivider
import com.safarparmar.app.ui.glass.SafarGlassButton

@Composable
fun NishthaAnalyticsScreen(
    viewModel: NishthaViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    initialSection: String = "overview",
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val report = uiState.monthlyReport
    val achievements = uiState.achievements

    val today = remember { LocalDate.now() }
    val months = remember {
        (0..5).map { offset ->
            today.minusMonths(offset.toLong()).let { d ->
                Pair(
                    d.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    d.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
                )
            }
        }
    }
    var selectedMonth by remember { mutableStateOf(months[0].first) }
    var selectedSection by remember(initialSection) {
        mutableStateOf(
            when (initialSection.lowercase(Locale.US)) {
                "goals" -> "goals"
                "ekagra" -> "ekagra"
                "sessions" -> "ekagra"
                "monthly" -> "monthly"
                else -> "overview"
            }
        )
    }

    var showMonthPicker by remember { mutableStateOf(false) }
    val androidContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedMonth) {
        viewModel.onEvent(NishthaEvent.LoadReportForMonth(selectedMonth))
    }

    // Native month-year picker via DatePickerDialog (capped to current month, no future)
    if (showMonthPicker) {
        val parts = selectedMonth.split("-")
        val initYear = parts[0].toIntOrNull() ?: today.year
        val initMonth = (parts.getOrNull(1)?.toIntOrNull() ?: today.monthValue) - 1 // 0-based
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, initYear)
            set(java.util.Calendar.MONTH, initMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val maxCal = java.util.Calendar.getInstance() // today = max
        val dialog = android.app.DatePickerDialog(
            androidContext,
            { _, year, month, _ ->
                selectedMonth = "$year-${(month + 1).toString().padStart(2, '0')}"
                showMonthPicker = false
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            1
        )
        dialog.datePicker.maxDate = maxCal.timeInMillis
        // Show only month+year by hiding day spinner
        dialog.datePicker.apply {
            try {
                val f = javaClass.getDeclaredField("mDaySpinner")
                f.isAccessible = true
                (f.get(this) as? android.view.View)?.visibility = android.view.View.GONE
            } catch (_: Exception) {}
        }
        DisposableEffect(Unit) {
            dialog.show()
            dialog.setOnDismissListener { showMonthPicker = false }
            onDispose { dialog.dismiss() }
        }
    }

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize(), isLight = isLight)

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
                    )
                    Text(
                        "One home for progress patterns and monthly reflection.",
                        fontSize = 13.sp,
                        color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsSectionChip("Overview", selectedSection == "overview", Color(0xFF1E3A8A), isLight) { selectedSection = "overview" }
                AnalyticsSectionChip("Goals", selectedSection == "goals", Color(0xFF065F46), isLight) { selectedSection = "goals" }
                AnalyticsSectionChip("Ekagra", selectedSection == "ekagra", Color(0xFF9A3412), isLight) { selectedSection = "ekagra" }
                AnalyticsSectionChip("Monthly Review", selectedSection == "monthly", Color(0xFF5B21B6), isLight) { selectedSection = "monthly" }
            }

            Box(Modifier.fillMaxSize()) {
                when (selectedSection) {
                    "goals" -> GoalInsightsSection(uiState.goals)
                    "ekagra" -> FocusInsightsSection(uiState.ekagraAnalytics)
                    "monthly" -> MonthlyReviewSection(
                        selectedMonthLabel = months.firstOrNull { it.first == selectedMonth }?.second ?: "",
                        onMonthClick = { showMonthPicker = true },
                        isLoading = uiState.isLoadingReport,
                        report = report,
                        achievements = achievements,
                        isLight = isLight,
                        onNavigate = onNavigate,
                        onGenerate = { viewModel.onEvent(NishthaEvent.LoadReportForMonth(selectedMonth)) },
                    )
                    else -> AnalyticsOverviewSection(uiState.goals, uiState.ekagraAnalytics, report)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsSectionChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    isLight: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val textColor = if (selected) {
        Color.White
    } else {
        if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
    }

    Box(
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier
                        .clip(shape)
                        .background(selectedColor)
                        .border(
                            width = 0.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                            ),
                            shape = shape
                        )
                } else {
                    Modifier.macOSControlPanel(isLight = isLight, shape = shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MonthlyReviewSection(
    selectedMonthLabel: String,
    onMonthClick: () -> Unit,
    isLoading: Boolean,
    report: MonthlyReport?,
    achievements: List<com.safarparmar.app.domain.model.Achievement>,
    isLight: Boolean,
    onNavigate: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(16.dp))
                .clickable { onMonthClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    selectedMonthLabel,
                    color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                )
            }
        }

        when {
            isLoading -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.safarparmar.app.ui.components.StatCardSkeleton()
                    com.safarparmar.app.ui.components.StatCardSkeleton()
                    com.safarparmar.app.ui.components.StatCardSkeleton()
                }
            }
            report == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                ) {
                    Column(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            selectedMonthLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
                        )
                        Text(
                            stringResource(R.string.analytics_no_report_hint),
                            fontSize = 13.sp,
                            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        val btnAccent = SafarSemanticColors.brandPurple(isDarkTheme = !isLight)
                        SafarGlassButton(
                            text = stringResource(R.string.analytics_generate),
                            icon = Icons.Default.Refresh,
                            onClick = onGenerate,
                            isLight = isLight,
                            customTint = btnAccent
                        )
                    }
                }
            }
            else -> ReportContent(report, achievements, isLight, onNavigate)
        }
    }
}

@Composable
private fun ReportContent(
    report: MonthlyReport,
    achievements: List<com.safarparmar.app.domain.model.Achievement> = emptyList(),
    isLight: Boolean,
    onNavigate: (String) -> Unit = {}
) {
    val scoreAccent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
    val completionAccent = if (isLight) Color(0xFF065F46) else Color(0xFF81C784)
    val focusAccent = if (isLight) Color(0xFF9A3412) else Color(0xFFFF8A65)

    ScoreCard(R.drawable.ic_zap, stringResource(R.string.analytics_consistency_score), "${report.consistencyScore.toInt()}%", report.consistencyMessage, scoreAccent, isLight)
    ScoreCard(R.drawable.ic_circle_check, stringResource(R.string.analytics_completion_rate), "${report.completionRate.toInt()}%", report.completionMessage, completionAccent, isLight)
    ScoreCard(R.drawable.ic_target, stringResource(R.string.analytics_focus_depth), "${report.focusDepth.toInt()}m/day", report.focusMessage, focusAccent, isLight)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GoalsCountCard("Goals Set", report.goalsCreated.toString(), completionAccent, isLight, Modifier.weight(1f))
        GoalsCountCard("Goals Completed", report.goalsCompleted.toString(), completionAccent, isLight, Modifier.weight(1f))
    }

    StreakReviewCard(report, isLight)

    // Skill Radar — rendered as horizontal progress bars
    if (report.radar.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.analytics_skill_radar),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.analytics_multidimensional),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                    )
                }
                report.radar.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            item.subject,
                            modifier = Modifier.width(88.dp),
                            fontSize = 12.sp,
                            color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                        )
                        LinearProgressIndicator(
                            progress = { (item.score / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                            color = scoreAccent,
                            trackColor = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)
                        )
                        Text(
                            "${item.score.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (report.heatmap.isNotEmpty()) {
        val days = report.heatmap.takeLast(30)
        val dotColor = scoreAccent
        val emptyColor = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.analytics_activity_heatmap),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.analytics_30_day),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = dotColor
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.chunked(14).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { day ->
                                val intensity = (day.intensity ?: 0).coerceIn(0, 3)
                                val color = when (intensity) {
                                    0 -> emptyColor
                                    1 -> dotColor.copy(alpha = 0.30f)
                                    2 -> dotColor.copy(alpha = 0.65f)
                                    else -> dotColor
                                }
                                val size = when (intensity) {
                                    2 -> 20.dp
                                    3 -> 22.dp
                                    else -> 18.dp
                                }
                                Box(modifier = Modifier.size(size).background(color, CircleShape))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                GlassDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.analytics_less_active),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    listOf(emptyColor, dotColor.copy(alpha = 0.30f), dotColor.copy(alpha = 0.65f), dotColor).forEach { c ->
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(14.dp).background(c, RoundedCornerShape(3.dp)))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.analytics_power_mode),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = dotColor
                    )
                }
            }
        }
    }

    // Self-Discovery Insights
    val hasInsights = report.powerHourMessage.isNotEmpty() ||
            report.moodConnectionMessage.isNotEmpty() ||
            report.sundayScariesMessage.isNotEmpty()

    if (hasInsights) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(R.string.analytics_self_discovery),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
                )
                if (report.powerHourMessage.isNotEmpty()) {
                    InsightRow(R.drawable.ic_zap, stringResource(R.string.analytics_power_hour_title), report.powerHourMessage, isLight)
                }
                if (report.moodConnectionMessage.isNotEmpty()) {
                    InsightRow(R.drawable.ic_brain, stringResource(R.string.analytics_mood_connection_title), report.moodConnectionMessage, isLight)
                }
                if (report.sundayScariesMessage.isNotEmpty()) {
                    InsightRow(R.drawable.ic_calendar_dots, stringResource(R.string.analytics_sunday_scaries_title), report.sundayScariesMessage, isLight)
                }
            }
        }
    }

    AchievementsSection(achievements, isLight, onNavigate)
}

@Composable
private fun AchievementsSection(
    achievements: List<com.safarparmar.app.domain.model.Achievement>,
    isLight: Boolean,
    onNavigate: (String) -> Unit = {}
) {
    if (achievements.isEmpty()) return
    val earned = achievements.filter { it.earned }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink
                )
                Text(
                    "Achievements",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "See All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
                    modifier = Modifier.clickable { onNavigate(com.safarparmar.app.ui.navigation.Routes.ACHIEVEMENTS) }
                )
            }

            if (earned.isEmpty()) {
                // No earned achievements yet
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                    )
                    Text(
                        "No achievements earned yet",
                        fontSize = 13.sp,
                        color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                    )
                    Text(
                        "Keep up your streaks to earn badges!",
                        fontSize = 11.sp,
                        color = (if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary).copy(alpha = 0.7f)
                    )
                }
            } else {
                GlassDivider()
                // Only show earned achievements
                earned.forEach { ach ->
                    val imageUrl = com.safarparmar.app.ui.achievements.AchievementImages.urlFor(ach.id)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    (if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl != null) {
                                coil.compose.AsyncImage(
                                    model = imageUrl,
                                    contentDescription = ach.name,
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = if (ach.type == "title") R.drawable.ic_crown else R.drawable.ic_medal),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    ach.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = (if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "Earned",
                                        fontSize = 9.sp,
                                        color = if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!ach.description.isNullOrBlank()) {
                                Text(
                                    ach.description,
                                    fontSize = 11.sp,
                                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                                    lineHeight = 15.sp
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
private fun ScoreCard(
    iconRes: Int,
    label: String,
    value: String,
    message: String,
    accentColor: Color,
    isLight: Boolean
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight, shape = shape)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isLight) 0.12f else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = accentColor
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label.uppercase(Locale.US),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        message,
                        fontSize = 12.sp,
                        color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsCountCard(
    label: String,
    value: String,
    accentColor: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
            )
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun StreakReviewCard(report: MonthlyReport, isLight: Boolean) {
    val streakThemeColor = if (isLight) Color(0xFFC2410C) else Color(0xFFFF8A65)
    val breakDayFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Streak Review",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = streakThemeColor
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalsCountCard("Longest Streak", "${report.longestStreakDays}d", streakThemeColor, isLight, Modifier.weight(1f))
                GoalsCountCard("Streak Breaks", report.streakBreaksCount.toString(), streakThemeColor, isLight, Modifier.weight(1f))
            }
            if (report.streakMessage.isNotEmpty()) {
                Text(
                    report.streakMessage,
                    fontSize = 12.sp,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                )
            }
            if (report.streakBreakDates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                GlassDivider()
                Text(
                    "Broken on",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
                )
                Text(
                    report.streakBreakDates.joinToString("  •  ") { dateKey ->
                        runCatching { LocalDate.parse(dateKey).format(breakDayFormatter) }.getOrDefault(dateKey)
                    },
                    fontSize = 13.sp,
                    color = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun InsightRow(iconRes: Int, title: String, message: String, isLight: Boolean) {
    val accent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
            tint = accent
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 0.8.sp
            )
            Text(
                message,
                fontSize = 13.sp,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun LineChart(values: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor  = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val w       = size.width
        val h       = size.height
        val maxVal  = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val stepX   = w / (values.size - 1).toFloat()
        val padding = h * 0.12f

        fun yOf(v: Float) = h - padding - (v / maxVal) * (h - 2 * padding)

        val pts = values.mapIndexed { i, v -> Offset(i * stepX, yOf(v)) }

        // Subtle baseline
        drawLine(
            color       = lineColor.copy(alpha = 0.15f),
            start       = Offset(0f, h - padding),
            end         = Offset(w, h - padding),
            strokeWidth = 1.5f
        )

        // Fill area under line
        if (pts.isNotEmpty()) {
            val path = Path().apply {
                moveTo(pts.first().x, h - padding)
                pts.forEach { lineTo(it.x, it.y) }
                lineTo(pts.last().x, h - padding)
                close()
            }
            drawPath(path, fillColor)

            // Draw line segments
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color       = lineColor,
                    start       = pts[i],
                    end         = pts[i + 1],
                    strokeWidth = 3f,
                    cap         = StrokeCap.Round
                )
            }
        }

        // Draw dots
        pts.forEach { pt ->
            drawCircle(color = lineColor,  radius = 6f, center = pt)
            drawCircle(color = Color.White, radius = 3f, center = pt)
        }
    }
}
