package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.safarparmar.app.feature.habits.export.HabitExportManager
import com.safarparmar.app.feature.habits.premium.PremiumFeature
import com.safarparmar.app.feature.habits.ui.HabitColors
import com.safarparmar.app.feature.habits.ui.LocalHabitDarkTheme
import com.safarparmar.app.feature.habits.viewmodel.HabitInsightsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitInsightsScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: () -> Unit = {},
    viewModel: HabitInsightsViewModel = hiltViewModel(),
    exportManager: HabitExportManager = remember { HabitExportManager() }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalHabitDarkTheme provides isDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_insights),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabitColors.TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_back),
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
            HabitInsightsContent(
                viewModel = viewModel,
                exportManager = exportManager,
                onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
fun HabitInsightsContent(
    modifier: Modifier = Modifier,
    viewModel: HabitInsightsViewModel = hiltViewModel(),
    exportManager: HabitExportManager = remember { HabitExportManager() },
    onShowSnackbar: ((String) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = HabitColors.RoyalPurple)
        }
    } else {
        Box(modifier = modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 920.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Bento Stat Grid (Streak, Started, Completions, Missed)
                item(key = "bento-stats") {
                    BentoStatGrid(
                        currentStreak = state.currentStreak,
                        bestStreak = state.bestStreak,
                        startedDate = state.startedDate,
                        daysSinceStarted = state.daysSinceStarted,
                        totalCompleted = state.totalCompleted,
                        completionRate = state.completionRate,
                        totalMissed = state.totalMissed,
                        missedRate = state.missedRate
                    )
                }

                // 2. 7-Row Continuous GitHub/HabitBox Heatmap Matrix (Segregated by Month)
                item(key = "heatmap-matrix") {
                    ContributionHeatmapMatrix(
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

                // 3. Weekly Rhythm Card (Green card with vertical pill tracks, using Daily Progress design)
                item(key = "weekly-rhythm-card") {
                    val selectedHabit = state.availableHabits.firstOrNull { it.id == state.selectedHabitId }
                    val title = if (selectedHabit != null) "${selectedHabit.name} Rhythm" else "Weekly Rhythm"

                    HabitWeeklyRhythmCard(
                        title = title,
                        dailyStats = state.yearlyDailyStats,
                        today = LocalDate.now()
                    )
                }

                // 4. Line Sparkline Card (Blue, matching reference)
                item(key = "graph-line-sparkline") {
                    val selectedHabit = state.availableHabits.firstOrNull { it.id == state.selectedHabitId }
                    val title = if (selectedHabit != null) "${selectedHabit.name} Trend" else "Consistency Trend"
                    val progressLabel = "${(state.completionRate * 100).toInt()}% avg"

                    HabitLineSparklineCard(
                        title = title,
                        progressLabel = progressLabel,
                        dailyStats = state.yearlyDailyStats,
                        today = LocalDate.now()
                    )
                }

                // 4. Habit Performance List
                item(key = "habit-performance") {
                    HabitPerformanceList(performanceList = state.habitPerformance)
                }

                // 5. Data Export
                item(key = "export") {
                    ExportSection(
                        hasExportAccess = viewModel.featureAccessManager.hasAccess(PremiumFeature.HABIT_EXPORT),
                        isExporting = isExporting,
                        onOpenExport = { showExportDialog = true }
                    )
                }

                item(key = "bottom-space") {
                    Spacer(Modifier.height(32.dp))
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
                        onShowSnackbar?.invoke("Export ready to save or share")
                    } catch (e: Exception) {
                        onShowSnackbar?.invoke("Export failed: ${e.localizedMessage ?: "Unknown error"}")
                    } finally {
                        isExporting = false
                    }
                }
            }
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
            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_your_data),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_export_explanation),
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
