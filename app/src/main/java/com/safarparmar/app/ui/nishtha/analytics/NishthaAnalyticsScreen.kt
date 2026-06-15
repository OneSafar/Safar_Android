package com.safarparmar.app.ui.nishtha.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.MonthlyReport
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.theme.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NishthaAnalyticsScreen(
    viewModel: NishthaViewModel = hiltViewModel(),
    premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    initialSection: String = "overview",
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
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
                "sessions" -> "sessions"
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Analytics",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "One home for progress patterns and monthly reflection.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
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
            AnalyticsSectionChip("Overview", selectedSection == "overview") { selectedSection = "overview" }
            AnalyticsSectionChip("Goals", selectedSection == "goals") { selectedSection = "goals" }
            AnalyticsSectionChip("Ekagra", selectedSection == "ekagra") { selectedSection = "ekagra" }
            AnalyticsSectionChip("Sessions", selectedSection == "sessions") { selectedSection = "sessions" }
            AnalyticsSectionChip("Monthly Review", selectedSection == "monthly") { selectedSection = "monthly" }
        }

        Box(Modifier.fillMaxSize()) {
            // Content rendered at full height, faded and unscrollable by the overlay
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedSection) {
                    "goals" -> GoalInsightsSection(uiState.goals)
                    "ekagra" -> FocusInsightsSection(uiState.ekagraAnalytics)
                    "sessions" -> SessionHistorySection(uiState.ekagraAnalytics)
                    "monthly" -> MonthlyReviewSection(
                        selectedMonthLabel = months.first { it.first == selectedMonth }.second,
                        onMonthClick = { showMonthPicker = true },
                        isLoading = uiState.isLoadingReport,
                        report = report,
                        achievements = achievements,
                        onNavigate = onNavigate,
                        onGenerate = { viewModel.onEvent(NishthaEvent.LoadMonthlyReport) },
                    )
                    else -> AnalyticsOverviewSection(uiState.goals, uiState.ekagraAnalytics, report)
                }
            }
            if (!premiumStatus.canUseNishthaAnalytics) {
                // Gradient fade + premium lock gate
                AnalyticsPremiumLockOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onUpgradeClick = { onNavigate(com.safarparmar.app.ui.navigation.Routes.PREMIUM) }
                )
            }
        }
    }
}

@Composable
private fun AnalyticsSectionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onNavigate: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onMonthClick() },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    selectedMonthLabel,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(selectedMonthLabel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            stringResource(R.string.analytics_no_report_hint),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onGenerate,
                            shape = ButtonDefaults.shape
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.analytics_generate))
                        }
                    }
                }
            }
            else -> ReportContent(report, achievements, onNavigate)
        }
    }
}

@Composable
private fun ReportContent(report: MonthlyReport, achievements: List<com.safarparmar.app.domain.model.Achievement> = emptyList(), onNavigate: (String) -> Unit = {}) {
    ScoreCard(R.drawable.ic_zap, stringResource(R.string.analytics_consistency_score), "${report.consistencyScore.toInt()}%", report.consistencyMessage, MaterialTheme.colorScheme.secondary)
    ScoreCard(R.drawable.ic_circle_check, stringResource(R.string.analytics_completion_rate), "${report.completionRate.toInt()}%", report.completionMessage, MaterialTheme.colorScheme.tertiary)
    ScoreCard(R.drawable.ic_target, stringResource(R.string.analytics_focus_depth), "${report.totalFocusMinutes}m/day", report.focusMessage, MaterialTheme.colorScheme.primary)

    // Skill Radar — rendered as horizontal progress bars
    if (report.radar.isNotEmpty()) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.analytics_skill_radar),
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.analytics_multidimensional),
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                report.radar.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(item.subject, modifier = Modifier.width(88.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { (item.score / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.weight(1f).height(7.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        Text(
                            "${item.score.toInt()}",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (report.heatmap.isNotEmpty()) {
        val days = report.heatmap.takeLast(30)
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.analytics_activity_heatmap),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.analytics_30_day),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                val dotColor = MaterialTheme.colorScheme.primary
                val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
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
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.analytics_less_active),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    listOf(emptyColor, dotColor.copy(alpha = 0.30f), dotColor.copy(alpha = 0.65f), dotColor).forEach { c ->
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(14.dp).background(c, RoundedCornerShape(3.dp)))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(R.string.analytics_power_mode),
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
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
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(R.string.analytics_self_discovery),
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                )
                if (report.powerHourMessage.isNotEmpty()) {
                    InsightRow(R.drawable.ic_zap, stringResource(R.string.analytics_power_hour_title), report.powerHourMessage)
                }
                if (report.moodConnectionMessage.isNotEmpty()) {
                    InsightRow(R.drawable.ic_brain, stringResource(R.string.analytics_mood_connection_title), report.moodConnectionMessage)
                }
                if (report.sundayScariesMessage.isNotEmpty()) {
                    InsightRow(R.drawable.ic_calendar_dots, stringResource(R.string.analytics_sunday_scaries_title), report.sundayScariesMessage)
                }
            }
        }
    }

    // ── Achievements ──────────────────────────────────────────────────────────
    AchievementsSection(achievements, onNavigate)
}

@Composable
private fun AchievementsSection(achievements: List<com.safarparmar.app.domain.model.Achievement>, onNavigate: (String) -> Unit = {}) {
    if (achievements.isEmpty()) return
    val earned = achievements.filter { it.earned }

    val achievementImages: Map<String, String> = remember {
        mapOf(
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
    }

    val baseUrl = remember {
        com.safarparmar.app.BuildConfig.BASE_URL.trimEnd('/').let {
            val uri = android.net.Uri.parse(it)
            "${uri.scheme}://${uri.host}"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_trophy), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                Text("Achievements", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(
                    "See All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
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
                    Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_lock), contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No achievements earned yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Keep up your streaks to earn badges!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                // Only show earned achievements
                earned.forEach { ach ->
                    val imagePath = achievementImages[ach.id]
                    val imageUrl = imagePath?.let { "$baseUrl$it" }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl != null) {
                                coil.compose.AsyncImage(
                                    model = imageUrl,
                                    contentDescription = ach.name,
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(painter = androidx.compose.ui.res.painterResource(id = if (ach.type == "title") R.drawable.ic_crown else R.drawable.ic_medal), contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(ach.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)) {
                                    Text("Earned", fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (!ach.description.isNullOrBlank()) {
                                Text(ach.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(iconRes: Int, label: String, value: String, message: String, accentColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(painter = androidx.compose.ui.res.painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp), tint = accentColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label.uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = accentColor))
                if (message.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InsightRow(iconRes: Int, title: String, message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painter = androidx.compose.ui.res.painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(18.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title.uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
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

        // Draw dots
        pts.forEach { pt ->
            drawCircle(color = lineColor,  radius = 6f, center = pt)
            drawCircle(color = Color.White, radius = 3f, center = pt)
        }
    }
}

@Composable
private fun AnalyticsPremiumLockOverlay(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.background
    // Gradient: transparent at top -> background color at ~30% -> fully opaque background for lock card
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,
            0.25f to bg.copy(alpha = 0.85f),
            0.45f to bg,
        )
    )
    Box(
        modifier = modifier
            .background(gradient)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, _ -> }
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onUpgradeClick
            )
    ) {
        // Lock card centered in lower portion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Lock icon in a glowing circle
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                scheme.primary.copy(alpha = 0.25f),
                                scheme.primary.copy(alpha = 0.08f),
                                Color.Transparent,
                            )
                        ),
                        androidx.compose.foundation.shape.CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(scheme.primary.copy(alpha = 0.6f), scheme.secondary.copy(alpha = 0.3f))
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium feature",
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = "Premium Feature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onBackground,
            )
            Text(
                text = "See goal progress, ekagra history,\nsession history and monthly review.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = scheme.onSurfaceVariant,
                lineHeight = 19.sp,
            )
            Button(
                onClick = onUpgradeClick,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(0.75f),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Upgrade to Premium", fontWeight = FontWeight.Bold)
            }
        }
    }
}
