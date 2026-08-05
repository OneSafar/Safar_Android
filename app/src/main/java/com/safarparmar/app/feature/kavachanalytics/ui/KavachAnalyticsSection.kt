package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.feature.kavachanalytics.domain.KavachAnalyticsReport
import com.safarparmar.app.ui.glass.GlassDivider
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.macOSControlPanel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import java.time.LocalDate
import java.time.YearMonth

/** Category colours, shared by the summary bar, the trend and the per-app table. */
internal object KavachCategoryColors {
    fun productive(isLight: Boolean) = if (isLight) Color(0xFF047857) else Color(0xFF6EE7B7)
    fun distracting(isLight: Boolean) = if (isLight) Color(0xFFB91C1C) else Color(0xFFFCA5A5)
    fun neutral(isLight: Boolean) = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8)
    fun unclassified(isLight: Boolean) = if (isLight) Color(0xFF92400E) else Color(0xFFFCD34D)

    fun of(category: AppCategory, isLight: Boolean) = when (category) {
        AppCategory.PRODUCTIVE -> productive(isLight)
        AppCategory.DISTRACTING -> distracting(isLight)
        AppCategory.NEUTRAL -> neutral(isLight)
        AppCategory.UNCLASSIFIED -> unclassified(isLight)
    }
}

/**
 * The Kavach tab of Nishtha Analytics. Free for every signed-in student — this
 * section is deliberately outside the Nishtha premium entitlement.
 */
@Composable
fun KavachAnalyticsSection(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
    viewModel: KavachAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showCustomStartPicker by remember { mutableStateOf(false) }

    if (showMonthPicker) {
        MonthPickerDialog(
            initial = runCatching { YearMonth.parse(state.selectedMonth) }
                .getOrDefault(YearMonth.now()),
            earliest = YearMonth.from(state.earliestDate),
            onPicked = { viewModel.selectMonth(it.toString()); showMonthPicker = false },
            onDismiss = { showMonthPicker = false },
        )
    }

    if (showCustomStartPicker) {
        CustomRangeDialog(
            earliest = state.earliestDate,
            onPicked = { start, end -> viewModel.selectCustomRange(start, end); showCustomStartPicker = false },
            onDismiss = { showCustomStartPicker = false },
        )
    }

    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RangeChips(
            selected = state.range,
            isLight = isLight,
            onSelect = { range ->
                when (range) {
                    KavachRange.MONTH -> showMonthPicker = true
                    KavachRange.CUSTOM -> showCustomStartPicker = true
                    else -> viewModel.selectRange(range)
                }
            },
        )

        Text(
            "${KavachAnalyticsFormat.dayLabel(state.startDate)} – ${KavachAnalyticsFormat.dayLabel(state.endDate)}",
            fontSize = 12.sp,
            color = secondaryText(isLight),
        )

        if (!state.hasUsageAccess) {
            NoticeCard(
                icon = Icons.Default.Info,
                title = "Usage access is off",
                body = "Kavach can't measure app time without Usage access, so days without it are shown as " +
                    "\"no data\" rather than zero. Turn it back on from Kavach settings.",
                accent = KavachCategoryColors.unclassified(isLight),
                isLight = isLight,
            )
        }

        val report = state.report
        when {
            state.isLoading && report == null -> LoadingCard(isLight)
            report == null -> NoticeCard(
                icon = Icons.Default.Info,
                title = "No Kavach data yet",
                body = state.error ?: "Start a Kavach session and your app time will show up here.",
                accent = SafarGlassPalette.LightViolet,
                isLight = isLight,
            )
            else -> {
                if (report.coverage != DataCoverage.COMPLETE) {
                    NoticeCard(
                        icon = Icons.Default.Info,
                        title = if (report.coverage == DataCoverage.UNAVAILABLE) {
                            "No measurements for this range"
                        } else {
                            "Some days are incomplete"
                        },
                        body = "SAFAR couldn't measure ${report.daysMissingCoverage.size} day(s) in this " +
                            "range. Those days are left blank instead of counted as zero.",
                        accent = KavachCategoryColors.unclassified(isLight),
                        isLight = isLight,
                    )
                }

                ScopeToggle(
                    selected = state.scope,
                    isLight = isLight,
                    onSelect = viewModel::selectScope,
                )

                val totals = if (state.scope == KavachScope.ALL_DAY) report.allDay else report.duringKavach
                FocusSplitCard(totals = totals, isLight = isLight)
                DailyTrendCard(report = report, scope = state.scope, isLight = isLight)
                ShieldCountersRow(report = report, isLight = isLight)
                SessionOutcomeCard(report = report, isLight = isLight)
                MostAttemptedCard(report = report, isLight = isLight)

                if (state.unclassifiedPrompts.isNotEmpty()) {
                    UnclassifiedPromptCard(
                        rows = state.unclassifiedPrompts,
                        isLight = isLight,
                        onClassify = { pkg, category, label ->
                            viewModel.setCategory(pkg, category, label)
                        },
                    )
                }

                PerAppTableCard(report = report, scope = state.scope, isLight = isLight)
                SessionHistoryCard(report = report, isLight = isLight)

                GlassCard(isLight) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Routes.KAVACH_APP_CATEGORIES) },
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = SafarGlassPalette.LightViolet,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Edit app categories",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText(isLight),
                            )
                            Text(
                                "Decide which apps count as productive, distracting or neutral.",
                                fontSize = 12.sp,
                                color = secondaryText(isLight),
                            )
                        }
                    }
                }

                PrivacyNote(isLight)
            }
        }
    }
}

// ── Cards ────────────────────────────────────────────────────────────────────

@Composable
private fun FocusSplitCard(totals: CategoryTotals, isLight: Boolean) {
    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Productive vs distracting",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            if (totals.totalSeconds == 0) {
                Text("Nothing measured in this range yet.", fontSize = 13.sp, color = secondaryText(isLight))
                return@Column
            }
            StackedBar(totals = totals, isLight = isLight)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendStat("Productive", totals.productiveSeconds, totals.totalSeconds, KavachCategoryColors.productive(isLight), isLight, Modifier.weight(1f))
                LegendStat("Distracting", totals.distractingSeconds, totals.totalSeconds, KavachCategoryColors.distracting(isLight), isLight, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendStat("Neutral", totals.neutralSeconds, totals.totalSeconds, KavachCategoryColors.neutral(isLight), isLight, Modifier.weight(1f))
                LegendStat("Unclassified", totals.unclassifiedSeconds, totals.totalSeconds, KavachCategoryColors.unclassified(isLight), isLight, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StackedBar(totals: CategoryTotals, isLight: Boolean, height: Int = 14) {
    val total = totals.totalSeconds.coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp)),
    ) {
        listOf(
            totals.productiveSeconds to KavachCategoryColors.productive(isLight),
            totals.distractingSeconds to KavachCategoryColors.distracting(isLight),
            totals.neutralSeconds to KavachCategoryColors.neutral(isLight),
            totals.unclassifiedSeconds to KavachCategoryColors.unclassified(isLight),
        ).forEach { (seconds, color) ->
            if (seconds > 0) {
                Box(
                    Modifier
                        .weight(seconds.toFloat() / total)
                        .fillMaxSize()
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun DailyTrendCard(report: KavachAnalyticsReport, scope: KavachScope, isLight: Boolean) {
    val points = report.trend
    if (points.isEmpty()) return
    val maxSeconds = points.maxOf {
        (if (scope == KavachScope.ALL_DAY) it.allDay else it.duringKavach).totalSeconds
    }.coerceAtLeast(1)

    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Daily breakdown",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            points.takeLast(31).forEach { point ->
                val totals = if (scope == KavachScope.ALL_DAY) point.allDay else point.duringKavach
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        KavachAnalyticsFormat.dayLabel(point.localDate),
                        fontSize = 11.sp,
                        color = secondaryText(isLight),
                        modifier = Modifier.width(52.dp),
                    )
                    Box(Modifier.weight(1f)) {
                        if (point.coverage == DataCoverage.UNAVAILABLE) {
                            Text(
                                "No data",
                                fontSize = 11.sp,
                                color = secondaryText(isLight),
                            )
                        } else {
                            Box(
                                Modifier.fillMaxWidth(
                                    (totals.totalSeconds.toFloat() / maxSeconds).coerceIn(0.02f, 1f),
                                ),
                            ) {
                                StackedBar(totals = totals, isLight = isLight, height = 10)
                            }
                        }
                    }
                    Text(
                        if (point.coverage == DataCoverage.UNAVAILABLE) "—"
                        else KavachAnalyticsFormat.duration(totals.totalSeconds),
                        fontSize = 11.sp,
                        color = primaryText(isLight),
                        modifier = Modifier.width(52.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShieldCountersRow(report: KavachAnalyticsReport, isLight: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            icon = Icons.Default.Block,
            label = "Blocked opens",
            value = report.blockedAttempts.toString(),
            hint = "Times a blocked app was opened",
            accent = KavachCategoryColors.distracting(isLight),
            isLight = isLight,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Default.LockOpen,
            label = "Quick unlocks",
            value = report.quickUnlockCount.toString(),
            hint = KavachAnalyticsFormat.duration(report.quickUnlockSeconds) + " unlocked",
            accent = KavachCategoryColors.unclassified(isLight),
            isLight = isLight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SessionOutcomeCard(report: KavachAnalyticsReport, isLight: Boolean) {
    val total = report.completedSessions + report.endedEarlySessions + report.interruptedSessions
    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = KavachCategoryColors.productive(isLight),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Kavach sessions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText(isLight),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${KavachAnalyticsFormat.percent(report.completedSessions, total)}% completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachCategoryColors.productive(isLight),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Completed", report.completedSessions, KavachCategoryColors.productive(isLight), isLight, Modifier.weight(1f))
                MiniStat("Ended early", report.endedEarlySessions, KavachCategoryColors.unclassified(isLight), isLight, Modifier.weight(1f))
                MiniStat("Interrupted", report.interruptedSessions, KavachCategoryColors.neutral(isLight), isLight, Modifier.weight(1f))
            }
            Text(
                "Interrupted means the app or phone stopped the session — not you.",
                fontSize = 11.sp,
                color = secondaryText(isLight),
            )
        }
    }
}

@Composable
private fun MostAttemptedCard(report: KavachAnalyticsReport, isLight: Boolean) {
    val rows = report.apps
        .filter { it.blockedAttempts > 0 }
        .sortedByDescending { it.blockedAttempts }
        .take(5)
    if (rows.isEmpty()) return

    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Most attempted while blocked",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(KavachCategoryColors.of(row.category, isLight)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(row.appLabel, fontSize = 13.sp, color = primaryText(isLight), modifier = Modifier.weight(1f))
                    Text(
                        "${row.blockedAttempts}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KavachCategoryColors.distracting(isLight),
                    )
                }
            }
        }
    }
}

@Composable
private fun UnclassifiedPromptCard(
    rows: List<com.safarparmar.app.feature.kavachanalytics.domain.AppUsageRow>,
    isLight: Boolean,
    onClassify: (String, AppCategory, String?) -> Unit,
) {
    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Uncategorised time",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KavachCategoryColors.unclassified(isLight),
            )
            Text(
                "SAFAR doesn't guess. Tell it what these are and this time joins your split — " +
                    "including for the days already shown above.",
                fontSize = 12.sp,
                color = secondaryText(isLight),
            )
            rows.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(row.appLabel, fontSize = 13.sp, color = primaryText(isLight), modifier = Modifier.weight(1f))
                        Text(
                            KavachAnalyticsFormat.duration(row.allDaySeconds),
                            fontSize = 12.sp,
                            color = secondaryText(isLight),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AppCategory.PRODUCTIVE to "Productive",
                            AppCategory.DISTRACTING to "Distracting",
                            AppCategory.NEUTRAL to "Neutral",
                        ).forEach { (category, label) ->
                            CategoryPill(
                                label = label,
                                color = KavachCategoryColors.of(category, isLight),
                                selected = false,
                                isLight = isLight,
                            ) { onClassify(row.packageName, category, row.appLabel) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerAppTableCard(report: KavachAnalyticsReport, scope: KavachScope, isLight: Boolean) {
    val rows = report.apps.filter { it.allDaySeconds > 0 || it.kavachSeconds > 0 }.take(25)
    if (rows.isEmpty()) return

    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "App breakdown",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            GlassDivider()
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(KavachCategoryColors.of(row.category, isLight)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.appLabel, fontSize = 13.sp, color = primaryText(isLight))
                        Text(
                            row.category.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            color = secondaryText(isLight),
                        )
                    }
                    Text(
                        KavachAnalyticsFormat.duration(
                            if (scope == KavachScope.ALL_DAY) row.allDaySeconds else row.kavachSeconds,
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText(isLight),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(report: KavachAnalyticsReport, isLight: Boolean) {
    val sessions = report.sessions.take(15)
    if (sessions.isEmpty()) return

    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Recent sessions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            sessions.forEach { session ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            KavachAnalyticsFormat.sessionLabel(session.startedAtMs),
                            fontSize = 13.sp,
                            color = primaryText(isLight),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            KavachAnalyticsFormat.outcomeLabel(session.outcome),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (session.outcome) {
                                com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome.COMPLETED ->
                                    KavachCategoryColors.productive(isLight)
                                else -> KavachCategoryColors.neutral(isLight)
                            },
                        )
                    }
                    Text(
                        "${KavachAnalyticsFormat.duration(session.actualSeconds)} focus · " +
                            "${session.blockedAttempts} blocked · ${session.quickUnlockCount} unlocks",
                        fontSize = 11.sp,
                        color = secondaryText(isLight),
                    )
                    if (session.permissionLost || session.dataGap) {
                        Text(
                            if (session.permissionLost) {
                                "A Kavach permission was off during this session."
                            } else {
                                "Some app time in this session couldn't be measured."
                            },
                            fontSize = 10.sp,
                            color = KavachCategoryColors.unclassified(isLight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyNote(isLight: Boolean) {
    GlassCard(isLight) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "How this is measured",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            Text(
                "The detailed record of which app you opened and when stays on this phone. Only daily " +
                    "per-app totals and Kavach session summaries sync to your SAFAR account.",
                fontSize = 11.sp,
                color = secondaryText(isLight),
            )
        }
    }
}

// ── Small pieces ─────────────────────────────────────────────────────────────

@Composable
private fun RangeChips(selected: KavachRange, isLight: Boolean, onSelect: (KavachRange) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KavachRange.entries.forEach { range ->
            CategoryPill(
                label = range.label,
                color = SafarGlassPalette.LightViolet,
                selected = selected == range,
                isLight = isLight,
            ) { onSelect(range) }
        }
    }
}

@Composable
private fun ScopeToggle(selected: KavachScope, isLight: Boolean, onSelect: (KavachScope) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KavachScope.entries.forEach { scope ->
            CategoryPill(
                label = scope.label,
                color = SafarGlassPalette.LightViolet,
                selected = selected == scope,
                isLight = isLight,
            ) { onSelect(scope) }
        }
    }
}

@Composable
internal fun CategoryPill(
    label: String,
    color: Color,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .then(
                if (selected) Modifier.clip(shape).background(color)
                else Modifier.macOSControlPanel(isLight = isLight, shape = shape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else secondaryText(isLight),
        )
    }
}

@Composable
private fun LegendStat(
    label: String,
    seconds: Int,
    total: Int,
    color: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, fontSize = 11.sp, color = secondaryText(isLight))
        }
        Text(
            KavachAnalyticsFormat.duration(seconds),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
        )
        Text("${KavachAnalyticsFormat.percent(seconds, total)}%", fontSize = 10.sp, color = secondaryText(isLight))
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color, isLight: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 10.sp, color = secondaryText(isLight))
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    hint: String,
    accent: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(16.dp)).padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = primaryText(isLight))
            Text(hint, fontSize = 10.sp, color = secondaryText(isLight))
        }
    }
}

@Composable
private fun NoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    accent: Color,
    isLight: Boolean,
) {
    GlassCard(isLight) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
                Text(body, fontSize = 12.sp, color = secondaryText(isLight))
            }
        }
    }
}

@Composable
private fun LoadingCard(isLight: Boolean) {
    GlassCard(isLight) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Reading your app time…", fontSize = 13.sp, color = secondaryText(isLight))
        }
    }
}

@Composable
internal fun GlassCard(isLight: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .macOSControlPanel(isLight = isLight, shape = RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) { content() }
}

internal fun primaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary

internal fun secondaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary

// ── Date pickers ─────────────────────────────────────────────────────────────

@Composable
private fun MonthPickerDialog(
    initial: YearMonth,
    earliest: YearMonth,
    onPicked: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(initial) {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, initial.year)
            set(java.util.Calendar.MONTH, initial.monthValue - 1)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val dialog = android.app.DatePickerDialog(
            context,
            { _, year, month, _ -> onPicked(YearMonth.of(year, month + 1)) },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            1,
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.datePicker.minDate = earliest.atDay(1)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialog.setOnDismissListener { onDismiss() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

/** Two chained native pickers: start date, then end date. */
@Composable
private fun CustomRangeDialog(
    earliest: LocalDate,
    onPicked: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val today = LocalDate.now()
        val minMs = earliest.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var startDialog: android.app.DatePickerDialog? = null
        var endDialog: android.app.DatePickerDialog? = null

        startDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val start = LocalDate.of(year, month + 1, day)
                endDialog = android.app.DatePickerDialog(
                    context,
                    { _, endYear, endMonth, endDay ->
                        val end = LocalDate.of(endYear, endMonth + 1, endDay)
                        onPicked(minOf(start, end), maxOf(start, end))
                    },
                    today.year,
                    today.monthValue - 1,
                    today.dayOfMonth,
                ).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                    datePicker.minDate = minMs
                    setOnCancelListener { onDismiss() }
                    show()
                }
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth,
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            datePicker.minDate = minMs
            setOnCancelListener { onDismiss() }
            show()
        }

        onDispose {
            startDialog?.dismiss()
            endDialog?.dismiss()
        }
    }
}
