package com.safarparmar.app.ui.nishtha.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.composables.ui.components.ButtonStyle as UiButtonStyle
import com.composables.ui.components.HorizontalSeparator as UiHorizontalSeparator
import com.composables.ui.components.Icon as UiIcon
import com.composables.ui.components.IconButton as UiIconButton
import com.composables.ui.components.Text as UiText
import com.composables.ui.theme.ComposablesTheme
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.MonthlyReport
import com.safarparmar.app.ui.glass.LiquidGlassBackdrop
import com.safarparmar.app.ui.glass.SafarGlassButton
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.nishtha.NishthaEvent
import com.safarparmar.app.ui.nishtha.NishthaTab
import com.safarparmar.app.ui.nishtha.NishthaViewModel
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.theme.isLightBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NishthaAnalyticsScreen(
    viewModel: NishthaViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    initialSection: String = "overview",
) {
    LaunchedEffect(viewModel) { viewModel.loadTab(NishthaTab.ANALYTICS) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val report = uiState.monthlyReport
    val achievements = uiState.achievements

    val today = remember { LocalDate.now(com.safarparmar.app.util.IstDateUtils.zone) }
    val months = remember(today) {
        (0..11).map { offset ->
            val d = today.minusMonths(offset.toLong())
            val key = d.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val fullLabel = when (offset) {
                0 -> "${d.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))} (Current Month)"
                1 -> "${d.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))} (Last Month)"
                else -> d.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            }
            val displayLabel = d.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            Triple(key, fullLabel, displayLabel)
        }
    }
    var selectedMonth by remember { mutableStateOf(months[0].first) }
    val currentMonthIndex = months.indexOfFirst { it.first == selectedMonth }.coerceAtLeast(0)
    val canGoPrev = currentMonthIndex < months.size - 1
    val canGoNext = currentMonthIndex > 0

    var selectedSection by remember(initialSection) {
        mutableStateOf(
            when (initialSection.lowercase(Locale.US)) {
                "goals" -> "goals"
                "ekagra" -> "ekagra"
                "sessions" -> "ekagra"
                "monthly" -> "monthly"
                "kavach" -> "kavach"
                else -> "overview"
            }
        )
    }

    var showMonthPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMonth) {
        viewModel.onEvent(NishthaEvent.LoadReportForMonth(selectedMonth))
    }

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val isLight = !isDark

    ComposablesTheme {
        if (showMonthPicker) {
            MonthSelectionDialog(
                months = months,
                selectedMonth = selectedMonth,
                onSelectMonth = { selectedMonth = it },
                onDismiss = { showMonthPicker = false },
                isLight = isLight,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LiquidGlassBackdrop(modifier = Modifier.fillMaxSize(), isLight = isLight)

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnalyticsSectionChip(
                        stringResource(R.string.nishtha_analytics_overview),
                        selectedSection == "overview",
                        Color(0xFF1E3A8A),
                        isLight
                    ) { selectedSection = "overview" }
                    AnalyticsSectionChip("Goals", selectedSection == "goals", Color(0xFF065F46), isLight) { selectedSection = "goals" }
                    AnalyticsSectionChip("Ekagra", selectedSection == "ekagra", Color(0xFF9A3412), isLight) { selectedSection = "ekagra" }
                    AnalyticsSectionChip("Kavach", selectedSection == "kavach", Color(0xFF0F766E), isLight) { selectedSection = "kavach" }
                    AnalyticsSectionChip("Monthly Review", selectedSection == "monthly", Color(0xFF5B21B6), isLight) { selectedSection = "monthly" }
                }

                Box(Modifier.fillMaxSize()) {
                    when (selectedSection) {
                        "goals" -> GoalInsightsSection(uiState.goals)
                        "ekagra" -> FocusInsightsSection(uiState.ekagraAnalytics)
                        "kavach" -> com.safarparmar.app.feature.kavachanalytics.ui.KavachAnalyticsSection(
                            onNavigate = onNavigate,
                        )
                        "monthly" -> MonthlyReviewSection(
                            selectedMonthLabel = months.firstOrNull { it.first == selectedMonth }?.third ?: "",
                            canGoPrev = canGoPrev,
                            canGoNext = canGoNext,
                            onPrevMonth = { if (canGoPrev) selectedMonth = months[currentMonthIndex + 1].first },
                            onNextMonth = { if (canGoNext) selectedMonth = months[currentMonthIndex - 1].first },
                            onMonthClick = { showMonthPicker = true },
                            isLoading = uiState.isLoadingReport,
                            report = report,
                            reportError = uiState.reportError,
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
}

@Composable
private fun AnalyticsSectionChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    isLight: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val textColor = if (selected) {
        if (isLight) Color.White else Color.Black
    } else {
        secondaryText(isLight)
    }
    val activeBg = if (isLight) SafarGlassPalette.LightTextPrimary else Color.White

    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) activeBg
                else secondaryText(isLight).copy(alpha = 0.08f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        UiText(
            label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun MonthlyReviewSection(
    selectedMonthLabel: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit,
    isLoading: Boolean,
    report: MonthlyReport?,
    reportError: String? = null,
    achievements: List<com.safarparmar.app.domain.model.Achievement>,
    isLight: Boolean,
    onNavigate: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Bar (Composables UI IconButton + Dropdown Pill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UiIconButton(
                onClick = onPrevMonth,
                enabled = canGoPrev,
                style = UiButtonStyle.Ghost,
                modifier = Modifier.size(44.dp)
            ) {
                UiIcon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nishtha_previous_month),
                    tint = if (canGoPrev) primaryText(isLight) else secondaryText(isLight).copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(secondaryText(isLight).copy(alpha = 0.08f))
                    .clickable { onMonthClick() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    UiIcon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isLight) Color(0xFF5B21B6) else Color(0xFFC084FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    UiText(
                        selectedMonthLabel,
                        color = primaryText(isLight),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    UiIcon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.nishtha_select_month),
                        tint = secondaryText(isLight),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            UiIconButton(
                onClick = onNextMonth,
                enabled = canGoNext,
                style = UiButtonStyle.Ghost,
                modifier = Modifier.size(44.dp)
            ) {
                UiIcon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.nishtha_next_month),
                    tint = if (canGoNext) primaryText(isLight) else secondaryText(isLight).copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        when {
            isLoading -> {
                com.safarparmar.app.ui.components.NishthaAnalyticsSkeleton()
            }
            report == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(secondaryText(isLight).copy(alpha = 0.06f))
                ) {
                    Column(
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UiText(
                            selectedMonthLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryText(isLight)
                        )
                        UiText(
                            if (reportError != null) reportError else stringResource(R.string.analytics_no_report_hint),
                            fontSize = 13.sp,
                            color = if (reportError != null) MaterialTheme.colorScheme.error else secondaryText(isLight),
                            textAlign = TextAlign.Center
                        )
                        val btnAccent = SafarSemanticColors.brandPurple(isDarkTheme = !isLight)
                        SafarGlassButton(
                            text = if (reportError != null) "Retry Loading" else stringResource(R.string.analytics_generate),
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
    val scoreAccent = if (isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)
    val completionAccent = if (isLight) Color(0xFF10B981) else Color(0xFF34D399)
    val focusAccent = if (isLight) Color(0xFF6366F1) else Color(0xFF818CF8)

    // Hero Row: Nirantarta Score & Poornata Dar side-by-side circular progress cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularMetricCard(
            title = "Nirantarta Score",
            percentage = report.consistencyScore.toInt().coerceIn(0, 100),
            statusText = if (report.consistencyScore >= 50) "High consistency" else "Building consistency",
            dateRange = "Active Period",
            color = scoreAccent,
            isLight = isLight,
            modifier = Modifier.weight(1f)
        )
        CircularMetricCard(
            title = "Poornata Dar",
            percentage = report.completionRate.toInt().coerceIn(0, 100),
            statusText = "Daily tasks complete",
            dateRange = "${report.goalsCompleted} / ${report.goalsCreated} Goals",
            color = completionAccent,
            isLight = isLight,
            modifier = Modifier.weight(1f)
        )
    }

    // Focus Depth Area Chart Card with real data
    val focusValues = remember(report) {
        val daily = report.heatmap.map { it.value.toFloat() }
        if (daily.any { it > 0f }) daily.takeLast(14) else emptyList()
    }
    FocusDepthChartCard(
        focusMinutes = report.focusDepth.toInt(),
        focusMessage = report.focusMessage,
        focusValues = focusValues,
        isLight = isLight
    )

    // Streak Review Card with real streak data & breakdown
    StreakReviewSection(report = report, isLight = isLight)

    // Goals Set vs Completed
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GoalsCountCard("Goals Set", report.goalsCreated.toString(), R.drawable.ic_target, Color(0xFF0284C7), isLight, Modifier.weight(1f))
        GoalsCountCard("Goals Completed", report.goalsCompleted.toString(), R.drawable.ic_circle_check, completionAccent, isLight, Modifier.weight(1f))
    }

    // Kaushal Radar Chart Section with skill breakdown
    if (report.radar.isNotEmpty()) {
        RadarChartCard(radarItems = report.radar, isLight = isLight)
    }

    // Activity Heatmap
    if (report.heatmap.isNotEmpty()) {
        val days = report.heatmap.takeLast(28).ifEmpty { report.heatmap.takeLast(30) }
        val dotColor = scoreAccent
        val emptyColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UiText(
                        stringResource(R.string.analytics_activity_heatmap),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryText(isLight),
                        modifier = Modifier.weight(1f)
                    )
                    UiText(
                        stringResource(R.string.analytics_30_day),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = dotColor
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.chunked(7).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { day ->
                                val intensity = day.intensity.coerceIn(0, 3)
                                val color = when (intensity) {
                                    0 -> emptyColor
                                    1 -> dotColor.copy(alpha = 0.30f)
                                    2 -> dotColor.copy(alpha = 0.65f)
                                    else -> dotColor
                                }
                                val dotSize = when (intensity) {
                                    2 -> 18.dp
                                    3 -> 20.dp
                                    else -> 16.dp
                                }
                                Box(
                                    modifier = Modifier.size(22.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(dotSize)
                                            .background(color, CircleShape)
                                    )
                                }
                            }
                            repeat(7 - row.size) {
                                Spacer(modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                UiHorizontalSeparator()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    UiText(
                        stringResource(R.string.analytics_less_active),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText(isLight)
                    )
                    Spacer(Modifier.weight(1f))
                    listOf(emptyColor, dotColor.copy(alpha = 0.30f), dotColor.copy(alpha = 0.65f), dotColor).forEach { c ->
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(14.dp).background(c, RoundedCornerShape(3.dp)))
                    }
                    Spacer(Modifier.weight(1f))
                    UiText(
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UiText(
                    stringResource(R.string.analytics_self_discovery),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryText(isLight)
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
private fun CircularMetricCard(
    title: String,
    percentage: Int,
    statusText: String,
    dateRange: String,
    color: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UiText(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val stroke = 9.dp.toPx()
                    drawArc(
                        color = color.copy(alpha = 0.15f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = (percentage / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UiText(
                        text = "$percentage%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText(isLight)
                    )
                    UiText(
                        text = "$percentage / 100",
                        fontSize = 9.sp,
                        color = secondaryText(isLight)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            UiText(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight),
                textAlign = TextAlign.Center
            )
            UiText(
                text = dateRange,
                fontSize = 10.sp,
                color = secondaryText(isLight),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FocusDepthChartCard(
    focusMinutes: Int,
    focusMessage: String,
    focusValues: List<Float>,
    isLight: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            UiText(
                text = "Focus Depth",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight)
            )
            if (focusMessage.isNotEmpty()) {
                UiText(
                    text = focusMessage,
                    fontSize = 11.sp,
                    color = secondaryText(isLight)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (focusValues.size >= 2) {
                LineAreaGradientChart(
                    values = focusValues,
                    isLight = isLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        UiText(
                            text = "${focusMinutes}m/day",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText(isLight)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        UiText(
                            text = "Log more Ekagra sessions to see daily trends",
                            fontSize = 12.sp,
                            color = secondaryText(isLight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineAreaGradientChart(
    values: List<Float>,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFF0284C7)
    val gridColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)

    Canvas(modifier = modifier) {
        val cleanValues = values.map { if (it.isFinite() && it >= 0f) it else 0f }
        if (cleanValues.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val maxVal = (cleanValues.maxOrNull() ?: 100f).coerceAtLeast(100f)
        val stepX = w / (cleanValues.size - 1)
        val paddingY = h * 0.1f

        fun yPos(v: Float) = h - paddingY - (v / maxVal) * (h - 2 * paddingY)

        val points = cleanValues.mapIndexed { idx, valY ->
            Offset(idx * stepX, yPos(valY))
        }

        // Draw horizontal grid lines
        listOf(0.2f, 0.5f, 0.8f).forEach { frac ->
            drawLine(
                color = gridColor,
                start = Offset(0f, h * frac),
                end = Offset(w, h * frac),
                strokeWidth = 1f
            )
        }

        // Fill area under line
        val fillPath = Path().apply {
            moveTo(points.first().x, h - paddingY)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h - paddingY)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f))
            )
        )

        // Draw smooth gradient stroke line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = primaryColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw node circles
        points.forEach { pt ->
            drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = pt)
            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun RadarChartCard(
    radarItems: List<com.safarparmar.app.domain.model.RadarItem>,
    isLight: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UiText(
                    text = "Kaushal Radar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText(isLight)
                )
                UiText(
                    text = "Multi-dimensional",
                    fontSize = 11.sp,
                    color = secondaryText(isLight)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (radarItems.size >= 3) {
                PolygonRadarChart(
                    items = radarItems,
                    isLight = isLight,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            radarItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UiText(
                        text = item.subject,
                        fontSize = 12.sp,
                        color = secondaryText(isLight),
                        modifier = Modifier.weight(1f)
                    )
                    UiText(
                        text = "${item.score.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText(isLight)
                    )
                }
            }
        }
    }
}

@Composable
private fun PolygonRadarChart(
    items: List<com.safarparmar.app.domain.model.RadarItem>,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val radarColor = Color(0xFF10B981)
    val gridColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)

    Canvas(modifier = modifier) {
        if (items.size < 3) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = (size.width / 2f) * 0.75f
        val count = items.size
        val angleStep = (2.0 * Math.PI / count).toFloat()

        // Draw concentric web rings
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ringFrac ->
            val ringRadius = radius * ringFrac
            val webPath = Path()
            for (i in 0 until count) {
                val angle = i * (2.0 * Math.PI / count) - (Math.PI / 2.0)
                val x = (center.x + ringRadius * Math.cos(angle)).toFloat()
                val y = (center.y + ringRadius * Math.sin(angle)).toFloat()
                if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
            }
            webPath.close()
            drawPath(webPath, color = gridColor, style = Stroke(width = 1f))
        }

        // Draw radial spoke lines
        for (i in 0 until count) {
            val angle = i * (2.0 * Math.PI / count) - (Math.PI / 2.0)
            val endX = (center.x + radius * Math.cos(angle)).toFloat()
            val endY = (center.y + radius * Math.sin(angle)).toFloat()
            drawLine(color = gridColor, start = center, end = Offset(endX, endY), strokeWidth = 1f)
        }

        // Polygon path for radar data
        val dataPath = Path()
        items.forEachIndexed { i, item ->
            val scoreNorm = (item.score / 100.0).coerceIn(0.0, 1.0).toFloat()
            val angle = i * (2.0 * Math.PI / count) - (Math.PI / 2.0)
            val r = radius * scoreNorm
            val x = (center.x + r * Math.cos(angle)).toFloat()
            val y = (center.y + r * Math.sin(angle)).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(dataPath, color = radarColor.copy(alpha = 0.35f))
        drawPath(dataPath, color = radarColor, style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable
private fun StreakReviewSection(report: MonthlyReport, isLight: Boolean) {
    val breakDayFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UiText(
                text = "Streak Review",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = primaryText(isLight)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreakStatCard(
                    title = "Longest Streak",
                    days = "${report.longestStreakDays}d 🔥",
                    iconRes = R.drawable.ic_zap,
                    accentColor = Color(0xFFF97316),
                    isLight = isLight,
                    modifier = Modifier.weight(1f)
                )
                StreakStatCard(
                    title = "Streak Breaks",
                    days = "${report.streakBreaksCount}",
                    iconRes = R.drawable.ic_circle_check,
                    accentColor = if (report.streakBreaksCount == 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    isLight = isLight,
                    modifier = Modifier.weight(1f)
                )
            }
            if (report.streakMessage.isNotEmpty()) {
                UiText(
                    text = report.streakMessage,
                    fontSize = 12.sp,
                    color = secondaryText(isLight)
                )
            }
            if (report.streakBreakDates.isNotEmpty()) {
                UiHorizontalSeparator()
                UiText(
                    text = "Broken on",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryText(isLight)
                )
                UiText(
                    text = report.streakBreakDates.joinToString("  •  ") { dateKey ->
                        runCatching { LocalDate.parse(dateKey).format(breakDayFormatter) }.getOrDefault(dateKey)
                    },
                    fontSize = 12.sp,
                    color = primaryText(isLight),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StreakStatCard(
    title: String,
    days: String,
    iconRes: Int,
    accentColor: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(secondaryText(isLight).copy(alpha = 0.06f))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                M3Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                UiText(
                    text = title,
                    fontSize = 11.sp,
                    color = secondaryText(isLight)
                )
                UiText(
                    text = days,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText(isLight)
                )
            }
        }
    }
}

@Composable
private fun GoalsCountCard(
    label: String,
    value: String,
    iconRes: Int,
    accentColor: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                M3Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                UiText(
                    text = label.uppercase(Locale.US),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryText(isLight)
                )
                UiText(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun InsightRow(iconRes: Int, title: String, message: String, isLight: Boolean) {
    val accent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        M3Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp),
            tint = accent
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            UiText(
                title.uppercase(Locale.US),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 0.8.sp
            )
            UiText(
                message,
                fontSize = 12.sp,
                color = secondaryText(isLight),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun AchievementsSection(
    achievements: List<com.safarparmar.app.domain.model.Achievement>,
    isLight: Boolean,
    onNavigate: (String) -> Unit = {}
) {
    if (achievements.isEmpty()) return
    val earned = achievements.filter { it.earned }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                M3Icon(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink
                )
                UiText(
                    "Achievements",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryText(isLight),
                    modifier = Modifier.weight(1f)
                )
                UiText(
                    "See All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
                    modifier = Modifier.clickable { onNavigate(com.safarparmar.app.ui.navigation.Routes.ACHIEVEMENTS) }
                )
            }

            if (earned.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    M3Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = secondaryText(isLight)
                    )
                    UiText(
                        "No achievements earned yet",
                        fontSize = 13.sp,
                        color = secondaryText(isLight)
                    )
                    UiText(
                        "Keep up your streaks to earn badges!",
                        fontSize = 11.sp,
                        color = secondaryText(isLight).copy(alpha = 0.7f)
                    )
                }
            } else {
                UiHorizontalSeparator()
                earned.forEach { ach ->
                    val imageUrl = com.safarparmar.app.ui.achievements.AchievementImages.urlFor(ach.id)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    (if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl != null) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = ach.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                M3Icon(
                                    painter = painterResource(id = if (ach.type == "title") R.drawable.ic_crown else R.drawable.ic_medal),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                UiText(
                                    ach.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText(isLight)
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = (if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink).copy(alpha = 0.15f)
                                ) {
                                    UiText(
                                        "Earned",
                                        fontSize = 9.sp,
                                        color = if (isLight) SafarGlassPalette.LightPink else SafarGlassPalette.Pink,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!ach.description.isNullOrBlank()) {
                                UiText(
                                    ach.description,
                                    fontSize = 11.sp,
                                    color = secondaryText(isLight),
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

private fun primaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary

private fun secondaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary

@Composable
private fun MonthSelectionDialog(
    months: List<Triple<String, String, String>>,
    selectedMonth: String,
    onSelectMonth: (String) -> Unit,
    onDismiss: () -> Unit,
    isLight: Boolean,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            UiText(
                text = stringResource(R.string.nishtha_select_month),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = primaryText(isLight)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                months.forEach { (key, fullLabel, _) ->
                    val isSelected = key == selectedMonth
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectMonth(key)
                                onDismiss()
                            },
                        color = if (isSelected) {
                            (if (isLight) Color(0xFF5B21B6) else Color(0xFFC084FC)).copy(alpha = 0.12f)
                        } else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            UiText(
                                text = fullLabel,
                                fontSize = 14.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    if (isLight) Color(0xFF5B21B6) else Color(0xFFC084FC)
                                } else primaryText(isLight),
                            )
                            if (isSelected) {
                                UiIcon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.common_selected),
                                    tint = if (isLight) Color(0xFF5B21B6) else Color(0xFFC084FC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                UiText(stringResource(R.string.common_close), fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = if (isLight) Color.White else Color(0xFF1E1E2E),
    )
}
