package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.habits.analytics.HabitAnalyticsCalculator
import com.safarparmar.app.feature.habits.data.HabitRepository
import com.safarparmar.app.feature.habits.export.ExportDateRangeOption
import com.safarparmar.app.feature.habits.export.HabitExportManager
import com.safarparmar.app.feature.habits.premium.PremiumFeature
import com.safarparmar.app.feature.habits.ui.HabitColors
import com.safarparmar.app.feature.habits.ui.LocalHabitDarkTheme
import com.safarparmar.app.feature.habits.viewmodel.HabitInsightsViewModel
import com.safarparmar.app.performance.LocalMotionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitInsightsScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    viewModel: HabitInsightsViewModel = hiltViewModel(),
    exportManager: HabitExportManager = remember { HabitExportManager() },
    calculator: HabitAnalyticsCalculator = remember { HabitAnalyticsCalculator() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalHabitDarkTheme provides isDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Habit Insights",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabitColors.TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = HabitColors.TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HabitColors.Background
                    )
                )
            },
            containerColor = HabitColors.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HabitColors.RoyalPurple)
                }
            } else {
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 920.dp)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        item(key = "overview") {
                            MonthOverviewCard(
                                selectedMonth = state.selectedMonth,
                                completionRate = state.monthlyStats?.completionRate ?: 0f,
                                activeDays = state.monthlyStats?.activeDays ?: 0,
                                perfectDays = state.monthlyStats?.perfectDays ?: 0,
                                currentStreak = state.currentStreak,
                                bestStreak = state.bestStreak,
                                canNavigateNextMonth = state.canNavigateNextMonth,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onCurrentMonth = viewModel::currentMonth
                            )
                        }
                        item(key = "monthly-progress") {
                            ProgressChart(
                                dailyStats = state.monthlyDailyStats,
                                today = LocalDate.now()
                            )
                        }
                        item(key = "weekly-rhythm") {
                            WeekdayRhythmChart(
                                dailyStats = state.monthlyDailyStats,
                                today = LocalDate.now(),
                            )
                        }
                        item(key = "year-heatmap") {
                            YearHeatmap(
                                selectedYear = state.selectedYear,
                                dailyStats = state.yearlyDailyStats,
                                availableHabits = state.availableHabits,
                                selectedHabitId = state.selectedHabitId,
                                onSelectHabit = viewModel::selectHabit,
                                onPreviousYear = viewModel::previousYear,
                                onNextYear = viewModel::nextYear,
                                onCurrentYear = viewModel::currentYear,
                                canNavigateNextYear = state.canNavigateNextYear,
                                today = LocalDate.now()
                            )
                        }
                        item(key = "habit-performance") {
                            HabitPerformanceList(performanceList = state.habitPerformance)
                        }
                        item(key = "export") {
                            ExportSection(
                                hasExportAccess = viewModel.featureAccessManager.hasAccess(PremiumFeature.HABIT_EXPORT),
                                isExporting = isExporting,
                                onOpenExport = { showExportDialog = true }
                            )
                        }
                        item(key = "bottom-space") { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }

        if (showExportDialog) {
            HabitExportDialog(
                availableHabits = state.availableHabits,
                onDismiss = { showExportDialog = false },
                onExport = { rangeOption, customStart, customEnd, selectedHabitIds ->
                    scope.launch {
                        isExporting = true
                        try {
                            val csvContent = viewModel.generateExportCsv(
                                exportManager = exportManager,
                                rangeOption = rangeOption,
                                customStart = customStart,
                                customEnd = customEnd,
                                selectedHabitIds = selectedHabitIds
                            )
                            exportManager.exportAndShare(context, csvContent)
                            snackbarHostState.showSnackbar("Export ready to save or share")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage ?: "Unknown error"}")
                        } finally {
                            isExporting = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MonthOverviewCard(
    selectedMonth: YearMonth,
    completionRate: Float,
    activeDays: Int,
    perfectDays: Int,
    currentStreak: Int,
    bestStreak: Int,
    canNavigateNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit
) {
    val outlineColor = HabitColors.Outline
    val surfaceColor = HabitColors.Surface
    val textPrimary = HabitColors.TextPrimary
    val textSecondary = HabitColors.TextSecondary
    val purple = HabitColors.RoyalPurple
    val checkDone = HabitColors.CheckDone

    val monthFmt = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val isCurrentMonth = selectedMonth == YearMonth.now()
    val targetRatePercentage = (completionRate * 100f).toInt()
    val motion = LocalMotionPolicy.current
    val ratePercentage by animateIntAsState(
        targetValue = targetRatePercentage,
        animationSpec = tween(if (motion.animationsEnabled) 650 else 0),
        label = "monthlyRate",
    )
    val animatedRate by animateFloatAsState(
        targetValue = completionRate.coerceIn(0f, 1f),
        animationSpec = tween(if (motion.animationsEnabled) 750 else 0),
        label = "monthlyRateBar",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        // Month Selector: ‹ September 2026 › [This Month]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = textPrimary
                    )
                }

                Text(
                    text = monthFmt,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = onNextMonth,
                    enabled = canNavigateNextMonth,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = if (canNavigateNextMonth) textPrimary else textSecondary.copy(alpha = 0.3f)
                    )
                }
            }

            if (!isCurrentMonth) {
                TextButton(
                    onClick = onCurrentMonth,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Current Month",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = purple
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Large Completion Rate headline
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$ratePercentage%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (ratePercentage >= 80) checkDone else purple,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Monthly Completion",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            }

            // Streak Badges
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDayCount(currentStreak),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDayCount(bestStreak),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = purple
                    )
                    Text(
                        text = "Best Streak",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(HabitColors.Parchment),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRate)
                    .clip(RoundedCornerShape(999.dp))
                    .background(HabitColors.RoyalPurple),
            )
        }

        Spacer(Modifier.height(18.dp))

        // Secondary Stats: Active Days and Perfect Days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatPill(
                title = "Active Days",
                value = formatDayCount(activeDays),
                modifier = Modifier.weight(1f)
            )
            StatPill(
                title = "Perfect Days",
                value = formatDayCount(perfectDays),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatDayCount(count: Int): String = if (count == 1) "1 day" else "$count days"

@Composable
private fun StatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HabitColors.Parchment)
            .border(1.dp, HabitColors.Outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = HabitColors.TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HabitColors.TextPrimary
        )
    }
}

@Composable
private fun ExportSection(
    hasExportAccess: Boolean,
    isExporting: Boolean,
    onOpenExport: () -> Unit
) {
    val outlineColor = HabitColors.Outline
    val surfaceColor = HabitColors.Surface
    val textPrimary = HabitColors.TextPrimary
    val textSecondary = HabitColors.TextSecondary
    val purple = HabitColors.RoyalPurple

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Your Data",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            text = "Export your habit consistency data to CSV format for use in Excel, Google Sheets, or personal archiving.",
            fontSize = 12.sp,
            color = textSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )

        Button(
            onClick = onOpenExport,
            enabled = !isExporting && hasExportAccess,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HabitColors.Parchment,
                contentColor = purple
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isExporting) "Generating..." else "Export Spreadsheet →",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
