package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.kavachanalytics.domain.AppCategory
import com.safarparmar.app.feature.kavachanalytics.domain.AppUsageRow
import com.safarparmar.app.feature.kavachanalytics.domain.CategoryTotals
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.feature.kavachanalytics.domain.KavachAnalyticsReport
import com.safarparmar.app.feature.kavachanalytics.domain.KavachCategoryFilter
import com.safarparmar.app.feature.kavachanalytics.domain.KavachGranularity
import com.safarparmar.app.feature.kavachanalytics.domain.secondsFor
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Category colours. Only ever used as meaning — a dot, a bar, a number — never as
 * a selection highlight, so a colour on this screen always says "distracting" or
 * "productive" rather than "you tapped this".
 */
internal object KavachCategoryColors {
    fun productive(isLight: Boolean) = if (isLight) Color(0xFF047857) else Color(0xFF4ADE80)
    fun distracting(isLight: Boolean) = if (isLight) Color(0xFFB91C1C) else Color(0xFFF87171)
    fun neutral(isLight: Boolean) = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    fun unclassified(isLight: Boolean) = if (isLight) Color(0xFFB45309) else Color(0xFFFBBF24)

    fun of(category: AppCategory, isLight: Boolean) = when (category) {
        AppCategory.PRODUCTIVE -> productive(isLight)
        AppCategory.DISTRACTING -> distracting(isLight)
        AppCategory.NEUTRAL -> neutral(isLight)
        AppCategory.UNCLASSIFIED -> unclassified(isLight)
    }

    fun of(filter: KavachCategoryFilter, isLight: Boolean) = when (filter) {
        KavachCategoryFilter.ALL -> if (isLight) SafarGlassPalette.LightTextPrimary else Color.White
        KavachCategoryFilter.DISTRACTING -> distracting(isLight)
        KavachCategoryFilter.PRODUCTIVE -> productive(isLight)
        KavachCategoryFilter.OTHERS -> neutral(isLight)
    }
}

/**
 * The Kavach tab of Nishtha Analytics. Free for every signed-in student.
 *
 * Laid out flat rather than as a stack of cards: the numbers are the content, and
 * a border around each one only adds edges to count. One control row is a tab
 * strip, one is chips, and everything below is data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachAnalyticsSection(
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
    viewModel: KavachAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()

    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    state.appDetail?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAppDetail,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AppDetailSheet(
                detail = detail,
                scopeLabel = state.scope.label,
                seconds = state.secondsOf(detail.row),
                isLight = isLight,
                onSetCategory = { viewModel.setCategory(detail.row.packageName, it, detail.row.appLabel) },
                onToggleBlocked = { viewModel.toggleBlocked(detail.row.packageName) },
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GranularityTabs(state.granularity, isLight, viewModel::selectGranularity)

        val report = state.report
        when {
            state.isLoading && report == null -> LoadingRow(isLight)

            !state.hasUsageAccess && report == null -> EmptyMessage(
                title = "Usage access is off",
                body = "Kavach can't measure app time without it. Turn it on in Kavach settings and " +
                    "your usage will start showing here.",
                isLight = isLight,
            )

            report == null -> EmptyMessage(
                title = "No Kavach data yet",
                body = state.error ?: "Start a Kavach session and your app time will show up here.",
                isLight = isLight,
            )

            else -> {
                Spacer(Modifier.height(20.dp))
                PeriodHeader(state, isLight, viewModel::page)
                Spacer(Modifier.height(14.dp))
                ScopeSwitch(state.scope, isLight, viewModel::selectScope)
                Spacer(Modifier.height(18.dp))
                CategoryChips(state.categoryFilter, isLight, viewModel::selectCategory)
                Spacer(Modifier.height(22.dp))
                TrendChart(state, isLight)
                Spacer(Modifier.height(18.dp))
                Legend(state, isLight)

                CoverageLine(report, isLight)

                Spacer(Modifier.height(20.dp))
                HeadlineCounters(report, isLight)

                if (state.unclassifiedPrompts.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    UncategorisedSection(state.unclassifiedPrompts, isLight) { pkg, category, label ->
                        viewModel.setCategory(pkg, category, label)
                    }
                }

                Spacer(Modifier.height(24.dp))
                AppList(state, isLight, viewModel::setSearchQuery, viewModel::openAppDetail)

                Spacer(Modifier.height(24.dp))
                SessionHistory(report, isLight)

                Spacer(Modifier.height(24.dp))
                FooterLinks(isLight, onNavigate)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Controls ─────────────────────────────────────────────────────────────────

/** Text tabs with an underline. A filled pill per period read as three buttons competing. */
@Composable
private fun GranularityTabs(
    selected: KavachGranularity,
    isLight: Boolean,
    onSelect: (KavachGranularity) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        KavachGranularity.entries.forEach { granularity ->
            val active = granularity == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(granularity) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    granularity.label,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) primaryText(isLight) else secondaryText(isLight),
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            if (active) primaryText(isLight)
                            else secondaryText(isLight).copy(alpha = 0.12f),
                        ),
                )
            }
        }
    }
}

/** The hero number, on the background rather than boxed in a card. */
@Composable
private fun PeriodHeader(
    state: KavachAnalyticsUiState,
    isLight: Boolean,
    onPage: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onPage(-1) }, enabled = state.canPageBack) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous period",
                tint = if (state.canPageBack) secondaryText(isLight) else secondaryText(isLight).copy(alpha = 0.3f),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                KavachAnalyticsFormat.duration(state.filteredSeconds),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                KavachAnalyticsFormat.periodLabel(state.granularity, state.startDate, state.endDate),
                fontSize = 13.sp,
                color = secondaryText(isLight),
            )
            state.deltaPercent?.let { delta ->
                Spacer(Modifier.height(6.dp))
                val down = delta < 0
                Text(
                    "${if (down) "↓" else "↑"} ${kotlin.math.abs(delta)}% vs previous",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Less distracting time is good news; less productive time is not.
                    color = when (state.categoryFilter) {
                        KavachCategoryFilter.DISTRACTING ->
                            if (down) KavachCategoryColors.productive(isLight)
                            else KavachCategoryColors.distracting(isLight)
                        KavachCategoryFilter.PRODUCTIVE ->
                            if (down) KavachCategoryColors.distracting(isLight)
                            else KavachCategoryColors.productive(isLight)
                        else -> secondaryText(isLight)
                    },
                )
            }
        }
        IconButton(onClick = { onPage(1) }, enabled = state.canPageForward) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next period",
                tint = if (state.canPageForward) secondaryText(isLight) else secondaryText(isLight).copy(alpha = 0.3f),
            )
        }
    }
}

/**
 * All day / During Kavach as one quiet line rather than a third row of buttons.
 * It is a lens on the same number, not a separate destination.
 */
@Composable
private fun ScopeSwitch(selected: KavachScope, isLight: Boolean, onSelect: (KavachScope) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KavachScope.entries.forEachIndexed { index, scope ->
            if (index > 0) {
                Text(
                    "·",
                    fontSize = 13.sp,
                    color = secondaryText(isLight).copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
            val active = scope == selected
            Text(
                scope.label,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) primaryText(isLight) else secondaryText(isLight),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(scope) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Outline chips. The selected one fills with its own category colour, so the chip says what it means. */
@Composable
private fun CategoryChips(
    selected: KavachCategoryFilter,
    isLight: Boolean,
    onSelect: (KavachCategoryFilter) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KavachCategoryFilter.entries.forEach { filter ->
            val active = filter == selected
            val accent = KavachCategoryColors.of(filter, isLight)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (active) Modifier.background(accent.copy(alpha = if (isLight) 0.14f else 0.20f))
                        else Modifier,
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) accent.copy(alpha = 0.7f)
                        else secondaryText(isLight).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    filter.label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) accent else secondaryText(isLight),
                )
            }
        }
    }
}

// ── Chart ────────────────────────────────────────────────────────────────────

/**
 * Flat bars against a rounded value axis.
 *
 * When "All apps" is selected each bar is stacked by category rather than drawn in
 * one colour — there is no honest single colour for "all", and the split is the
 * interesting part anyway. A specific chip draws that category alone.
 */
@Composable
private fun TrendChart(state: KavachAnalyticsUiState, isLight: Boolean) {
    val bars = state.chartBars.takeLast(31)
    if (bars.isEmpty()) return

    val filter = state.categoryFilter
    val dataMax = bars.maxOf { it.totals.secondsFor(filter) }
    // Snap the top of the axis to a round duration so the ticks read as numbers a
    // person already thinks in, and a 41-minute day stops filling the frame.
    val axisMax = KavachAnalyticsFormat.niceAxisMax(dataMax, state.granularity)
    val gridColor = secondaryText(isLight).copy(alpha = 0.15f)

    val segments = if (filter == KavachCategoryFilter.ALL) {
        listOf(
            KavachCategoryColors.distracting(isLight) to { t: CategoryTotals -> t.distractingSeconds },
            KavachCategoryColors.productive(isLight) to { t: CategoryTotals -> t.productiveSeconds },
            KavachCategoryColors.neutral(isLight) to { t: CategoryTotals -> t.neutralSeconds + t.unclassifiedSeconds },
        )
    } else {
        listOf(KavachCategoryColors.of(filter, isLight) to { t: CategoryTotals -> t.secondsFor(filter) })
    }

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            Modifier.height(136.dp).width(34.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            listOf(axisMax, axisMax / 2, 0).forEach { seconds ->
                Text(
                    KavachAnalyticsFormat.axisValue(seconds),
                    fontSize = 9.sp,
                    color = secondaryText(isLight),
                )
            }
        }
        Spacer(Modifier.width(6.dp))

        Box(Modifier.weight(1f)) {
            Column(
                Modifier.fillMaxWidth().height(136.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(3) { Box(Modifier.fillMaxWidth().height(1.dp).background(gridColor)) }
            }

            Row(
                Modifier.fillMaxWidth().height(136.dp),
                horizontalArrangement = Arrangement.spacedBy(if (bars.size > 12) 2.dp else 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    val unmeasured = bar.coverage == DataCoverage.UNAVAILABLE
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (unmeasured) {
                            // A day we could not measure gets a hairline stub, never a
                            // zero-height bar that would read as real idle time.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(gridColor),
                            )
                        } else {
                            // Context days in the Daily view are dimmed so the day the
                            // headline actually describes is obvious.
                            val alpha = if (bar.isFocused) 1f else 0.35f
                            segments.forEach { (color, extract) ->
                                val seconds = extract(bar.totals)
                                if (seconds <= 0) return@forEach
                                val fraction = (seconds.toFloat() / axisMax).coerceIn(0f, 1f)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((120 * fraction).dp.coerceAtLeast(2.dp))
                                        .background(color.copy(alpha = alpha)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    Row(
        Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (bars.size > 12) 2.dp else 6.dp),
    ) {
        bars.forEachIndexed { index, bar ->
            // A month of day numbers is unreadable at this width; label every fifth.
            val showLabel = bars.size <= 12 || index % 5 == 0 || index == bars.lastIndex
            Text(
                if (showLabel) KavachAnalyticsFormat.axisLabel(bar.localDate, state.granularity) else "",
                fontSize = 9.sp,
                fontWeight = if (bar.isFocused && bars.size <= 12) FontWeight.Bold else FontWeight.Normal,
                color = if (bar.isFocused) primaryText(isLight) else secondaryText(isLight),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Legend(state: KavachAnalyticsUiState, isLight: Boolean) {
    val totals = (if (state.scope == KavachScope.ALL_DAY) state.report?.allDay else state.report?.duringKavach)
        ?: CategoryTotals()
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendStat("Distracting", totals.distractingSeconds, KavachCategoryColors.distracting(isLight), isLight, Modifier.weight(1f))
        LegendStat("Productive", totals.productiveSeconds, KavachCategoryColors.productive(isLight), isLight, Modifier.weight(1f))
        LegendStat("Others", totals.neutralSeconds + totals.unclassifiedSeconds, KavachCategoryColors.neutral(isLight), isLight, Modifier.weight(1f))
    }
}

@Composable
private fun LegendStat(
    label: String,
    seconds: Int,
    color: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Text(label, fontSize = 11.sp, color = secondaryText(isLight))
        }
        Text(
            KavachAnalyticsFormat.duration(seconds),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText(isLight),
        )
    }
}

/**
 * Missing days are stated in one line. It still has to be said — a blank day is
 * not a day of no phone use — but it is a footnote, not an alarm.
 */
@Composable
private fun CoverageLine(report: KavachAnalyticsReport, isLight: Boolean) {
    if (report.coverage == DataCoverage.COMPLETE) return
    Spacer(Modifier.height(12.dp))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = secondaryText(isLight),
            modifier = Modifier.size(13.dp),
        )
        Text(
            if (report.coverage == DataCoverage.UNAVAILABLE) {
                "No days in this range could be measured."
            } else {
                "${report.daysMissingCoverage.size} day(s) couldn't be measured — shown blank, not zero."
            },
            fontSize = 11.sp,
            color = secondaryText(isLight),
        )
    }
}

/** Three numbers on one line instead of three bordered tiles. */
@Composable
private fun HeadlineCounters(report: KavachAnalyticsReport, isLight: Boolean) {
    val sessions = report.completedSessions + report.endedEarlySessions + report.interruptedSessions
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CounterStat("Blocked opens", report.blockedAttempts.toString(), isLight, Modifier.weight(1f))
        CounterStat("Quick unlocks", report.quickUnlockCount.toString(), isLight, Modifier.weight(1f))
        CounterStat(
            "Sessions done",
            if (sessions == 0) "—" else "${KavachAnalyticsFormat.percent(report.completedSessions, sessions)}%",
            isLight,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun CounterStat(label: String, value: String, isLight: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        Text(label, fontSize = 11.sp, color = secondaryText(isLight))
    }
}

// ── Lists ────────────────────────────────────────────────────────────────────

@Composable
private fun AppIcon(
    packageName: String,
    appLabel: String,
    category: AppCategory,
    isLight: Boolean,
    size: Dp = 38.dp,
    showCategoryDot: Boolean = true,
) {
    val context = LocalContext.current
    val iconDrawable = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    }

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (iconDrawable != null) {
            AsyncImage(
                model = iconDrawable,
                contentDescription = appLabel,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(secondaryText(isLight).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Apps,
                    contentDescription = null,
                    tint = secondaryText(isLight),
                    modifier = Modifier.size(size * 0.55f),
                )
            }
        }

        if (showCategoryDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(KavachCategoryColors.of(category, isLight))
                    .border(1.5.dp, if (isLight) Color.White else Color.Black, CircleShape),
            )
        }
    }
}

@Composable
private fun AppList(
    state: KavachAnalyticsUiState,
    isLight: Boolean,
    onSearch: (String) -> Unit,
    onOpen: (AppUsageRow) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            singleLine = true,
            placeholder = { Text("Search apps", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        val rows = state.filteredApps.take(40)
        if (rows.isEmpty()) {
            Text(
                "No apps in this category for this period.",
                fontSize = 13.sp,
                color = secondaryText(isLight),
                modifier = Modifier.padding(vertical = 20.dp),
            )
            return@Column
        }

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(row) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    packageName = row.packageName,
                    appLabel = row.appLabel,
                    category = row.category,
                    isLight = isLight,
                    size = 38.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(row.appLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryText(isLight))
                    Text(
                        row.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = KavachCategoryColors.of(row.category, isLight),
                    )
                }
                Text(
                    KavachAnalyticsFormat.duration(state.secondsOf(row)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText(isLight),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = secondaryText(isLight).copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(secondaryText(isLight).copy(alpha = 0.10f)))
        }
    }
}

@Composable
private fun UncategorisedSection(
    rows: List<AppUsageRow>,
    isLight: Boolean,
    onClassify: (String, AppCategory, String?) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Uncategorised time", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        Text(
            "SAFAR doesn't guess. Tell it what these are and this time joins your split — " +
                "including the days already shown above.",
            fontSize = 12.sp,
            color = secondaryText(isLight),
        )
        rows.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(
                        packageName = row.packageName,
                        appLabel = row.appLabel,
                        category = row.category,
                        isLight = isLight,
                        size = 30.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(row.appLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = primaryText(isLight), modifier = Modifier.weight(1f))
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
                        OutlineChip(label, KavachCategoryColors.of(category, isLight), isLight) {
                            onClassify(row.packageName, category, row.appLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistory(report: KavachAnalyticsReport, isLight: Boolean) {
    val sessions = report.sessions.take(10)
    if (sessions.isEmpty()) return

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recent sessions", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        sessions.forEach { session ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        KavachAnalyticsFormat.sessionLabel(session.startedAtMs),
                        fontSize = 13.sp,
                        color = primaryText(isLight),
                    )
                    Text(
                        "${KavachAnalyticsFormat.duration(session.actualSeconds)} · " +
                            "${session.blockedAttempts} blocked · ${session.quickUnlockCount} unlocks",
                        fontSize = 11.sp,
                        color = secondaryText(isLight),
                    )
                }
                Text(
                    KavachAnalyticsFormat.outcomeLabel(session.outcome),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (session.outcome == KavachSessionOutcome.COMPLETED) {
                        KavachCategoryColors.productive(isLight)
                    } else {
                        secondaryText(isLight)
                    },
                )
            }
        }
    }
}

@Composable
private fun FooterLinks(isLight: Boolean, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.KAVACH_APP_CATEGORIES) },
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = secondaryText(isLight), modifier = Modifier.size(18.dp))
            Text(
                "Edit app categories",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight),
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secondaryText(isLight),
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            "The record of which app you opened and when stays on this phone. Only daily totals " +
                "and session summaries sync to your account.",
            fontSize = 11.sp,
            color = secondaryText(isLight).copy(alpha = 0.8f),
        )
    }
}

// ── App detail ───────────────────────────────────────────────────────────────

@Composable
private fun AppDetailSheet(
    detail: KavachAppDetail,
    scopeLabel: String,
    seconds: Int,
    isLight: Boolean,
    onSetCategory: (AppCategory) -> Unit,
    onToggleBlocked: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            AppIcon(
                packageName = detail.row.packageName,
                appLabel = detail.row.appLabel,
                category = detail.row.category,
                isLight = isLight,
                size = 52.dp,
            )
            Spacer(Modifier.height(10.dp))
            Text(detail.row.appLabel, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
            Spacer(Modifier.height(6.dp))
            Text(
                KavachAnalyticsFormat.duration(seconds),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            Text(scopeLabel, fontSize = 11.sp, color = secondaryText(isLight))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CounterStat("Blocked opens", detail.row.blockedAttempts.toString(), isLight, Modifier.weight(1f))
            CounterStat("Quick unlocks", detail.row.quickUnlockCount.toString(), isLight, Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Usage category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AppCategory.PRODUCTIVE to "Productive",
                    AppCategory.DISTRACTING to "Distracting",
                    AppCategory.NEUTRAL to "Neutral",
                ).forEach { (category, label) ->
                    OutlineChip(
                        label = label,
                        accent = KavachCategoryColors.of(category, isLight),
                        isLight = isLight,
                        selected = detail.row.category == category,
                    ) { onSetCategory(category) }
                }
            }
        }

        // The reference app can only offer "add a limit" here. Kavach already blocks
        // apps, so the analytics row and the block list are one decision.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Block during Kavach", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText(isLight))
                Text(
                    "Hides this app while a Kavach session is running.",
                    fontSize = 11.sp,
                    color = secondaryText(isLight),
                )
            }
            Switch(checked = detail.isBlockedInKavach, onCheckedChange = { onToggleBlocked() })
        }
    }
}

// ── Shared bits ──────────────────────────────────────────────────────────────

@Composable
internal fun OutlineChip(
    label: String,
    accent: Color,
    isLight: Boolean,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (selected) Modifier.background(accent.copy(alpha = if (isLight) 0.14f else 0.20f)) else Modifier)
            .border(
                1.dp,
                if (selected) accent.copy(alpha = 0.7f) else secondaryText(isLight).copy(alpha = 0.25f),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else secondaryText(isLight),
        )
    }
}

@Composable
private fun LoadingRow(isLight: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text("Reading your app time…", fontSize = 13.sp, color = secondaryText(isLight))
    }
}

@Composable
private fun EmptyMessage(title: String, body: String, isLight: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        Text(body, fontSize = 13.sp, color = secondaryText(isLight), textAlign = TextAlign.Center)
    }
}

internal fun primaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary

internal fun secondaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary
