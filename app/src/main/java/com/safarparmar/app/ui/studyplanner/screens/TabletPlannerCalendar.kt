package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.safarparmar.app.ui.tutorial.tutorialTarget
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.logic.*
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.PlannerActions
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle

/** Bounded calendar and independently scrolling day details for a landscape tablet. */
@Composable
internal fun TabletPlannerCalendar(
    plan: StudyPlan,
    calendar: Map<String, List<CalendarTopicItem>>,
    visibleMonth: YearMonth,
    selectedDay: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDay: (String) -> Unit,
    onToday: () -> Unit,
    actions: PlannerActions,
    onMissedTopics: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = shape,
            color = scheme.surface,
            border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.planner_calendar_target_exam),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Text(
                        plan.title.ifBlank { stringResource(R.string.planner_tablet_title) },
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                    )
                    plan.examDate?.takeIf { it.isNotBlank() }?.let {
                        Text(readableDate(it), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onToday) { Text(stringResource(R.string.planner_warm_today)) }
                daysUntil(plan.examDate)?.takeIf { it >= 0 }?.let { days ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$days", style = MaterialTheme.typography.headlineLarge, color = PlannerFlatColors.PrimaryAccent)
                        Text(
                            stringResource(if (days == 1L) R.string.planner_calendar_day_left else R.string.planner_calendar_days_left),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val detailsWidth = (maxWidth * 0.36f).coerceIn(340.dp, 480.dp)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = shape,
                    color = scheme.surface,
                    border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                ) {
                    TabletMonthGrid(plan, calendar, visibleMonth, selectedDay, onPrevious, onNext, onDay)
                }
                Surface(
                    modifier = Modifier.width(detailsWidth).fillMaxHeight(),
                    shape = shape,
                    color = scheme.surface,
                    border = BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        if (selectedDay == null) {
                            Column(
                                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(Icons.Default.CalendarMonth, null, Modifier.size(48.dp), tint = scheme.primary.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.planner_calendar_select_date),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = scheme.onSurface,
                                )
                            }
                        } else key(plan.id, selectedDay) {
                            SelectedDayTargets(
                                dateIso = selectedDay,
                                plan = plan,
                                items = calendar[selectedDay].orEmpty(),
                                actions = actions,
                                embedded = true,
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
                            )
                        }
                        HorizontalDivider(color = PlannerFlatColors.BorderSoft)
                        // Keep both existing calendar actions reachable without scrolling the month.
                        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                            TextButton(onClick = actions::openRevisionTopics, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.planner_view_revision_topics))
                            }
                            TextButton(onClick = onMissedTopics, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.planner_view_missed_topics), color = scheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletMonthGrid(
    plan: StudyPlan,
    calendar: Map<String, List<CalendarTopicItem>>,
    month: YearMonth,
    selectedDay: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDay: (String) -> Unit,
) {
    val weeks = remember(month) { monthCalendarSlots(month).chunked(7) }
    val locale = LocalLocale.current.platformLocale
    val today = todayKey()
    val scheme = MaterialTheme.colorScheme
    val offDays = remember(plan.offDays) { plan.offDays.toSet() }
    BoxWithConstraints(Modifier.fillMaxSize().tutorialTarget("planner.calendar")) {
        // Height is independent of width; even six-week months fit a typical tablet.
        // Small windows and large accessibility fonts can scroll this pane alone.
        val rowHeight = ((maxHeight - 104.dp) / weeks.size).coerceIn(52.dp, 100.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.ChevronLeft, stringResource(R.string.planner_previous_month))
                }
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.planner_next_month))
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
            weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(
                            Modifier.weight(1f).height(rowHeight)
                                .border(0.5.dp, PlannerFlatColors.BorderSoft),
                        ) {
                            if (date != null) {
                                val iso = date.toString()
                                CalendarDayChip(
                                    dateIso = iso,
                                    items = calendar[iso].orEmpty(),
                                    selected = iso == selectedDay,
                                    isToday = iso == today,
                                    isOff = jsDayOfWeek(date) in offDays,
                                    isExamDay = iso == plan.examDate?.take(10),
                                    isWeekend = date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                                    isLight = scheme.background.isLightBackground(),
                                    dense = true,
                                    onClick = { onDay(iso) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("● " + stringResource(R.string.planner_warm_planned), style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                Text("● " + stringResource(R.string.planner_warm_done), style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                Text("☾ " + stringResource(R.string.planner_warm_rest), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }

        }
    }
}
