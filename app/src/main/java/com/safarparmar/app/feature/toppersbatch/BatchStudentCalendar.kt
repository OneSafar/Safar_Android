/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4
 * Component: calendar · tone: simple and practical · theme: SAFAR Material tokens
 * Hierarchy: month → grouped day agenda → next classes → study dates. No decorative motion.
 */
package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.composables.ui.components.DropdownMenu as UiDropdownMenu
import com.composables.ui.components.DropdownMenuItem as UiDropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.safarparmar.app.feature.toppersbatch.BatchAlertDialog as AlertDialog
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.Text as UiText
import com.composables.ui.components.ButtonStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun calendarDateLabel(date: String?, strings: BatchStrings, pattern: String = "d MMM yyyy") = strings.date(date, pattern)
private fun BatchLecture.calendarTime(strings: BatchStrings): String = classStartsAt?.let {
    runCatching { Instant.parse(it).atZone(batchCalendarZone).format(DateTimeFormatter.ofPattern("h:mm a", strings.locale)) }.getOrNull()
} ?: classTime?.let { value -> runCatching { java.time.LocalTime.parse(value).format(DateTimeFormatter.ofPattern("h:mm a", strings.locale)) }.getOrDefault(value) } ?: strings.text(R.string.toppers_batch_time_not_set)
private fun BatchEventKind.label(strings: BatchStrings) = strings.text(titleRes)
private fun milestoneLabel(event: BatchCalendarEvent, strings: BatchStrings) = event.milestone?.let { strings.text(it.labelRes) } ?: event.title

@Composable
internal fun BatchStudentCalendar(state: BatchUiState, vm: ToppersBatchViewModel) {
    var detailId by remember { mutableStateOf<String?>(null) }
    // Keep the calendar compact even when the device uses a large text setting.
    val density = LocalDensity.current
    val fontScale = LocalConfiguration.current.fontScale.coerceIn(1f, 1.15f)
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        BatchStudentCalendarContent(state, vm::selectDay, vm::changeMonth, vm::currentMonth,
            vm::goToCalendarDay, { detailId = it.id })
    }
    val overview = state.overview ?: return
    val detail = overview.lectures.firstOrNull { it.id == detailId }
    if (detail != null) LectureDetailsDialog(detail, overview.subjects.firstOrNull { it.id == detail.subjectId }, state, vm) { detailId = null }
}

/** Presentation is independent of the ViewModel so real layouts can be previewed and captured. */
@Composable
internal fun BatchStudentCalendarContent(state: BatchUiState, onDay: (String) -> Unit,
    onMonth: (Long) -> Unit, onToday: () -> Unit, onJump: (String) -> Unit,
    onLecture: (BatchLecture) -> Unit, today: String = ToppersBatchViewModel.indiaDay()) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceIn(1f, 1.15f))) {
        CalendarLayout(state, onDay, onMonth, onToday, onJump, onLecture, today)
    }
}

@Composable
private fun CalendarLayout(state: BatchUiState, onDay: (String) -> Unit,
    onMonth: (Long) -> Unit, onToday: () -> Unit, onJump: (String) -> Unit,
    onLecture: (BatchLecture) -> Unit, today: String) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val month = runCatching { YearMonth.parse(state.month) }.getOrNull() ?: return
    val events = remember(overview, state.studyMode) { overview.calendarEvents(state.studyMode) }
    val dates = remember(events) { events.groupBy { it.date } }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (true) { now = Instant.now(); kotlinx.coroutines.delay(1000) } }
    val subjectDates = remember(overview, now, state.studyMode) {
        overview.remainingClasses(now).map { info -> info.copy(nextPersonal =
            if (state.studyMode == BatchStudyMode.PERSONAL) overview.nextStudyLecture(info.subject.id, state.studyMode, today)
            else null) }
    }
    var listMode by rememberSaveable { mutableStateOf(false) }
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
        // These are sections in one view, not separate navigation panes.
        val sideBySide = maxWidth >= 720.dp && scale <= 1.3f
        val columns = if (maxWidth >= 560.dp && scale <= 1.3f) 2 else 1
        val cardWidth = (maxWidth - 12.dp * (columns - 1)) / columns
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (sideBySide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                CalendarMonth(month, state.selectedDay, today, dates, listMode, { listMode = it }, onDay, onMonth, onToday, Modifier.weight(1f), onLecture)
                CalendarDay(overview, state.selectedDay, today, dates[state.selectedDay].orEmpty(), events, onJump, onLecture, Modifier.weight(1f))
            } else {
                CalendarMonth(month, state.selectedDay, today, dates, listMode, { listMode = it }, onDay, onMonth, onToday, onLecture = onLecture)
                CalendarDay(overview, state.selectedDay, today, dates[state.selectedDay].orEmpty(), events, onJump, onLecture)
            }
            PlanDisclosure(strings.text(R.string.toppers_batch_next_classes), subjectDates.size.toString()) {
                if (subjectDates.isEmpty()) Text(strings.text(R.string.toppers_batch_no_more_classes_scheduled_today), color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = columns) {
                    subjectDates.forEach { info -> NextClassCard(info, today, now, onJump, Modifier.width(cardWidth)) }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonth(month: YearMonth, selectedDay: String, today: String,
    dates: Map<String, List<BatchCalendarEvent>>, listMode: Boolean, onList: (Boolean) -> Unit,
    onDay: (String) -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit, modifier: Modifier = Modifier, onLecture: (BatchLecture) -> Unit) {
    val strings = rememberBatchStrings()
    val colors = MaterialTheme.colorScheme
    val scale = LocalDensity.current.fontScale
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Surface(Modifier.widthIn(max = 400.dp).fillMaxWidth().testTag("calendar-month"), shape = RoundedCornerShape(18.dp),
        color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 200.dp * scale) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CalendarMonthTitle(month)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CalendarMonthArrow(-1, onMonth)
                            CalendarMonthArrow(1, onMonth)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        CalendarMonthArrow(-1, onMonth)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CalendarMonthTitle(month) }
                        CalendarMonthArrow(1, onMonth)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = !listMode, onClick = batchFeatureAction({ onList(false) }), label = { Text(strings.text(R.string.toppers_batch_month)) },
                    modifier = Modifier.heightIn(min = 48.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = colors.onPrimary))
                FilterChip(selected = listMode, onClick = batchFeatureAction({ onList(true) }), label = { Text(strings.text(R.string.toppers_batch_list)) },
                    modifier = Modifier.heightIn(min = 48.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = colors.onPrimary))
                TextButton(onClick = batchFeatureAction(onToday), modifier = Modifier.heightIn(min = 48.dp).testTag("calendar-today")) { Text(strings.text(R.string.toppers_batch_today)) }
            }
            if (listMode) {
                val own = dates.filterKeys { it.startsWith(month.toString()) }
                if (own.isEmpty()) Text(strings.text(R.string.toppers_batch_no_classes_or_activity_this_month), color = colors.onSurfaceVariant)
                own.forEach { (date, rows) ->
                    Surface(onClick = batchFeatureAction({ onDay(date) }), shape = RoundedCornerShape(10.dp),
                        color = if (date == selectedDay) colors.primaryContainer else colors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(calendarDateLabel(date, strings, "EEE, d MMM"), fontWeight = FontWeight.SemiBold)
                            Text(activitySummary(rows, strings), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            } else {
                BoxWithConstraints {
                    val short = maxWidth < 360.dp
                    Column(Modifier.widthIn(max = 376.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            strings.weekdays().forEach { label ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(if (short) label.take(1) else label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                        val leading = month.atDay(1).dayOfWeek.value - 1
                        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it).toString() }
                        val padded = cells + List((7 - cells.size % 7) % 7) { null }
                        // Compact at the default text size; allow room for larger accessibility text.
                        val height = 56.dp + 16.dp * (scale - 1f).coerceAtLeast(0f)
                        padded.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                week.forEach { date ->
                                    if (date == null) Spacer(Modifier.weight(1f).height(height))
                                    else {
                                        val rows = dates[date].orEmpty()
                                        val completions = rows.filter { it.kind == BatchEventKind.DONE }.mapNotNull { it.subject }.distinctBy { it.id }
                                        val revisions = pendingCalendarRevisions(rows)
                                        val isSelected = date == selectedDay
                                        val interaction = remember { MutableInteractionSource() }
                                        val pressed by interaction.collectIsPressedAsState()
                                        val motionEnabled = com.safarparmar.app.performance.LocalMotionPolicy.current.animationsEnabled
                                        val pressScale by animateFloatAsState(if (pressed && motionEnabled) .97f else 1f,
                                            spring(dampingRatio = .8f, stiffness = 500f), label = "Calendar date press")
                                        Surface(onClick = batchFeatureAction({ onDay(date) }), shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) colors.primaryContainer else if (completions.isNotEmpty()) colors.primaryContainer else colors.surface,
                                            contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                                            border = BorderStroke(1.dp, if (date == today || isSelected) colors.primary else colors.surface),
                                            interactionSource = interaction,
                                            modifier = Modifier.weight(1f).testTag("calendar-date-$date").height(height).graphicsLayer { scaleX = pressScale; scaleY = pressScale }.semantics {
                                                selected = isSelected
                                                contentDescription = "${strings.date(date)}${if (date == today) strings.text(R.string.toppers_batch_today_2) else ""}, ${activitySummary(rows, strings)}"
                                            }) {
                                            Column(Modifier.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(date.takeLast(2).toInt().toString(), style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 16.sp), fontWeight = if (date == today || isSelected) FontWeight.Bold else FontWeight.Normal)
                                                BoxWithConstraints(Modifier.fillMaxWidth().height(5.dp).testTag("calendar-markers-$date")) {
                                                    // Subject markers stay inside the cell; the agenda contains every lecture.
                                                    val markerLimit = ((maxWidth.value + 2f) / 7f).toInt().coerceIn(0, 4)
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)) {
                                                        completions.take(markerLimit).forEach { subject ->
                                                            Box(Modifier.size(5.dp).testTag("calendar-marker-$date-${subject.id}")
                                                                .background(subjectProgressColor(subject), CircleShape))
                                                        }
                                                    }
                                                }
                                                if (revisions.isNotEmpty()) CalendarRevisionMenu(date, today, revisions, onDay, onLecture)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    }
}

@Composable
private fun CalendarRevisionMenu(date: String, today: String, revisions: List<Pair<BatchCalendarEvent, Int>>,
    onDay: (String) -> Unit, onLecture: (BatchLecture) -> Unit) {
    val strings = rememberBatchStrings()
    var expanded by remember(date, revisions) { mutableStateOf(false) }
    UiDropdownMenu(expanded = expanded, onExpandedChange = { expanded = it }, panel = {
        DropdownMenuPanel {
            Column(Modifier.widthIn(max = 300.dp).heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                UiText(strings.text(R.string.toppers_batch_revision_count_count, calendarDateLabel(date, strings), if (date < today) strings.text(R.string.toppers_batch_overdue) else ""), modifier = Modifier.padding(12.dp))
                revisions.forEach { (event, sessions) ->
                    UiDropdownMenuItem(onClick = batchFeatureAction({ expanded = false; onLecture(event.lecture!!) })) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            UiText(strings.text(R.string.toppers_batch_count_lecture_count_2, event.subject?.let(strings::subject).orEmpty(), event.lecture!!.lectureNumber))
                            UiText(event.title)
                            if (sessions > 1) UiText(strings.quantity(R.plurals.toppers_batch_count_revision_sessions, sessions, sessions))
                        }
                    }
                }
            }
        }
    }) {
        UiButton(onClick = batchFeatureAction({ onDay(date); expanded = !expanded }), style = ButtonStyle.Ghost,
            modifier = Modifier.fillMaxWidth().height(24.dp).testTag("calendar-revisions-$date")) {
            Icon(Icons.Default.Refresh, strings.quantity(R.plurals.toppers_batch_count_lectures_to_revise_on_count, revisions.size, revisions.size, calendarDateLabel(date, strings)), Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CalendarMonthTitle(month: YearMonth) {
    val strings = rememberBatchStrings()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(month.month.getDisplayName(java.time.format.TextStyle.FULL, strings.locale),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(month.year.toString(), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarMonthArrow(delta: Long, onMonth: (Long) -> Unit) {
    val strings = rememberBatchStrings()
    IconButton(onClick = batchFeatureAction({ onMonth(delta) }), Modifier.size(48.dp).semantics {
        contentDescription = if (delta < 0) strings.text(R.string.toppers_batch_previous_month) else strings.text(R.string.toppers_batch_next_month)
    }) { Icon(if (delta < 0) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, null) }
}

private fun activitySummary(rows: List<BatchCalendarEvent>, strings: BatchStrings): String = listOfNotNull(
    rows.count { it.kind == BatchEventKind.CLASS }.takeIf { it > 0 }?.let { strings.quantity(R.plurals.toppers_batch_classes_count, it, it) },
    rows.count { it.kind == BatchEventKind.PLANNED }.takeIf { it > 0 }?.let { strings.quantity(R.plurals.toppers_batch_count_planned_to_study, it, it) },
    rows.count { it.kind == BatchEventKind.DONE }.takeIf { it > 0 }?.let { strings.quantity(R.plurals.toppers_batch_count_done, it, it) },
    rows.count { it.kind == BatchEventKind.REVISION }.takeIf { it > 0 }?.let { strings.quantity(R.plurals.toppers_batch_count_revisions, it, it) },
    rows.count { it.kind == BatchEventKind.MILESTONE }.takeIf { it > 0 }?.let { strings.quantity(R.plurals.toppers_batch_study_dates_count, it, it) }
).joinToString(" · ").ifEmpty { strings.text(R.string.toppers_batch_no_activity) }

@Composable
private fun CalendarDay(overview: BatchOverview, day: String, today: String, rows: List<BatchCalendarEvent>, events: List<BatchCalendarEvent>,
    onJump: (String) -> Unit, onLecture: (BatchLecture) -> Unit, modifier: Modifier = Modifier) {
    val strings = rememberBatchStrings()
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().testTag("calendar-agenda"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (day == today) strings.text(R.string.toppers_batch_today_count, calendarDateLabel(day, strings, "d MMMM")) else calendarDateLabel(day, strings, "EEE, d MMM yyyy"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (rows.isEmpty()) {
            Surface(color = colors.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.EventAvailable, null, tint = colors.onSurfaceVariant)
                    Text(strings.text(R.string.toppers_batch_nothing_planned_or_done_this_day), color = colors.onSurfaceVariant)
                    events.firstOrNull { it.date > day && it.kind == BatchEventKind.CLASS }?.let { next ->
                        Text(calendarDateLabel(next.date, strings), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = batchFeatureAction({ onJump(next.date) })) { Text(strings.text(R.string.toppers_batch_see_next_class)) }
                    }
                }
            }
        }
        listOf(BatchEventKind.DONE, BatchEventKind.CLASS, BatchEventKind.PLANNED, BatchEventKind.REVISION, BatchEventKind.MILESTONE).forEach { kind ->
            val own = rows.filter { it.kind == kind }
            if (own.isNotEmpty() || kind == BatchEventKind.DONE || kind == BatchEventKind.CLASS) key(day, kind) {
                PlanDisclosure(
                    strings.copy(overview, when (kind) { BatchEventKind.DONE -> "calendarCompleted"; BatchEventKind.CLASS -> "calendarScheduled"; BatchEventKind.PLANNED -> "calendarPlanned"; BatchEventKind.REVISION -> "calendarRevision"; else -> "calendarMilestones" }, kind.label(strings)),
                    own.size.toString(),
                ) {
                    if (own.isEmpty()) Text(if (kind == BatchEventKind.DONE) strings.copy(overview, "calendarCompletedEmpty", strings.text(R.string.toppers_batch_no_lectures_completed)) else strings.copy(overview, "calendarScheduledEmpty", strings.text(R.string.toppers_batch_no_classes_scheduled)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    own.forEach { event ->
                        Surface(onClick = batchFeatureAction({ event.lecture?.let(onLecture) }), enabled = event.lecture != null,
                            color = event.subject?.let { androidx.compose.ui.graphics.lerp(colors.surface, subjectProgressColor(it), 0.08f) } ?: colors.surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, event.subject?.let { subjectProgressColor(it).copy(alpha = 0.35f) } ?: colors.outlineVariant),
                            modifier = Modifier.fillMaxWidth().testTag("calendar-event-${kind.name}-${event.lecture?.id ?: event.title}")) {
                            Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        event.subject?.let { Text(strings.text(R.string.toppers_batch_count_lecture_count_2, strings.subject(it), event.lecture?.lectureNumber ?: ""), style = MaterialTheme.typography.labelMedium, color = subjectProgressColor(it)) }
                                        if (kind == BatchEventKind.CLASS && (event.lecture?.classTime != null || event.lecture?.classStartsAt != null)) Text(event.lecture?.calendarTime(strings).orEmpty(), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                    }
                                    Text(if (kind == BatchEventKind.MILESTONE) milestoneLabel(event, strings) else event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if (event.lecture?.revisionTagged == true || kind == BatchEventKind.REVISION) com.composables.ui.components.Text(
                                        strings.text(R.string.toppers_batch_revision_count_2, if (kind == BatchEventKind.REVISION && event.completed) strings.text(R.string.toppers_batch_completed_count_2, event.actualCompletedDate?.let { " ${calendarDateLabel(it, strings)}" }.orEmpty()) else ""),
                                        modifier = Modifier.background(colors.secondaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                                    if ((kind == BatchEventKind.CLASS || kind == BatchEventKind.PLANNED) && event.lecture?.completedAt != null) Text(
                                        if (event.lecture.completionDateUnknown) strings.text(R.string.toppers_batch_previously_completed) else strings.text(R.string.toppers_batch_completed_on_count, calendarDateLabel(completionDay(event.lecture.completedAt), strings)), style = MaterialTheme.typography.bodySmall, color = colors.primary)
                                    if (kind == BatchEventKind.DONE) Text(strings.text(R.string.toppers_batch_completed_count, calendarDateLabel(day, strings)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                                if (event.lecture != null) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextClassCard(info: BatchSubjectDates, today: String, now: Instant, onJump: (String) -> Unit, modifier: Modifier) {
    val strings = rememberBatchStrings()
    val colors = MaterialTheme.colorScheme
    val next = info.nextClass
    val date = next?.scheduledFor?.let(::calendarDate)
    Surface(onClick = batchFeatureAction({ date?.let(onJump) }), enabled = date != null, modifier = modifier.testTag("calendar-next-${info.subject.key}"),
        shape = RoundedCornerShape(14.dp), color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(subjectProgressColor(info.subject), CircleShape))
                Text(strings.subject(info.subject), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                val start = next?.officialClassStart()
                val end = next?.officialClassEnd()
                if (next?.liveWindow?.enabled != false && info.subject.key != "mathematics" && start != null && end != null && !now.isBefore(start) && now.isBefore(end)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = colors.primaryContainer) {
                        Text(strings.text(R.string.toppers_batch_live_now), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
                    }
                }
                if (date != null) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            }
            if (next != null) {
                Text(next.displayTopic, style = MaterialTheme.typography.bodyMedium)
                Text("${if (date == today) strings.text(R.string.toppers_batch_today) else calendarDateLabel(date, strings, "d MMM yyyy")} · ${next.calendarTime(strings)}", style = MaterialTheme.typography.bodySmall, color = colors.primary)
            } else Text(strings.text(R.string.toppers_batch_date_not_set), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            info.nextPersonal?.let { Text(strings.text(R.string.toppers_batch_next_to_study_lecture_count, it.lectureNumber), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }
}

@Composable
internal fun ImportantDates(overview: BatchOverview, mode: BatchStudyMode = BatchStudyMode.PERSONAL) {
    val strings = rememberBatchStrings()
    val events = remember(overview, mode) { overview.calendarEvents(mode) }
    val subjects = remember(overview) { overview.calendarSubjects(ToppersBatchViewModel.indiaDay()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val onToggle = { expanded = !expanded }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = batchFeatureAction(onToggle), color = colors.surfaceVariant, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = strings.text(R.string.toppers_batch_count_important_dates, if (expanded) strings.text(R.string.toppers_batch_hide) else strings.text(R.string.toppers_batch_show)) }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(strings.text(R.string.toppers_batch_important_dates), fontWeight = FontWeight.SemiBold)
                    Text(if (mode == BatchStudyMode.PERSONAL) strings.text(R.string.toppers_batch_my_start_count, calendarDateLabel(overview.studyPlan?.startDate, strings))
                        else strings.text(R.string.toppers_batch_batch_start_count, calendarDateLabel(overview.course.officialStartDate, strings)), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp))
            }
        }
        if (expanded) {
            val milestones = listOfNotNull((strings.text(R.string.toppers_batch_my_start_date) to overview.studyPlan?.startDate).takeIf { mode == BatchStudyMode.PERSONAL },
                strings.text(R.string.toppers_batch_batch_start) to overview.course.officialStartDate,
                strings.text(R.string.toppers_batch_first_lecture_done) to events.firstOrNull { it.kind == BatchEventKind.DONE }?.date) +
                listOfNotNull(overview.studyPlan?.targetDate?.takeIf { mode == BatchStudyMode.PERSONAL }?.let { strings.text(R.string.toppers_batch_goal_date) to it })
            milestones.forEach { (label, date) -> StudyDateRow(label, date) }
            subjects.forEach { info ->
                HorizontalDivider(color = colors.outlineVariant)
                Text(strings.subject(info.subject), fontWeight = FontWeight.SemiBold)
                StudyDateRow(strings.text(R.string.toppers_batch_first_class), info.firstClass)
                StudyDateRow(strings.text(R.string.toppers_batch_first_lecture_done), info.firstCompletion)
            }
        }
    }
}

@Composable
private fun StudyDateRow(label: String, date: String?) {
    val strings = rememberBatchStrings()
    val valid = calendarDate(date)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (valid != null) Text(calendarDateLabel(valid, strings), style = MaterialTheme.typography.bodyMedium)
        else Text(strings.text(R.string.toppers_batch_not_set), style = MaterialTheme.typography.bodyMedium)
    }
}
