/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4
 * Component: calendar · tone: simple and practical · theme: SAFAR Material tokens
 * Hierarchy: month → grouped day agenda → next classes → study dates. No decorative motion.
 */
package com.safarparmar.app.feature.toppersbatch

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
import androidx.compose.ui.graphics.vector.ImageVector
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

internal fun calendarDateLabel(date: String?, pattern: String = "d MMM yyyy") = calendarDate(date)?.let {
    LocalDate.parse(it).format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
} ?: "Not set"
private fun BatchLecture.calendarTime(): String = classStartsAt?.let {
    runCatching { Instant.parse(it).atZone(batchCalendarZone).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)) }.getOrNull()
} ?: classTime?.let { value -> runCatching { java.time.LocalTime.parse(value).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)) }.getOrDefault(value) } ?: "Time not set"
private fun BatchEventKind.label() = when (this) {
    BatchEventKind.CLASS -> "Scheduled classes"
    BatchEventKind.PLANNED -> "Planned lectures"
    BatchEventKind.DONE -> "Completed"
    BatchEventKind.REVISION -> "Revision"
    BatchEventKind.MILESTONE -> "Key dates"
}
private fun BatchEventKind.icon(): ImageVector = when (this) {
    BatchEventKind.CLASS -> Icons.Default.Schedule
    BatchEventKind.PLANNED -> Icons.Default.EventNote
    BatchEventKind.DONE -> Icons.Default.Check
    BatchEventKind.REVISION -> Icons.Default.Refresh
    BatchEventKind.MILESTONE -> Icons.Default.Flag
}
private fun milestoneLabel(title: String) = when (title) {
    "Your plan starts" -> "My start date"
    "Batch starts" -> "Batch start"
    "First lecture completed" -> "First lecture done"
    "Your target finish" -> "Goal date"
    else -> title
}

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
    val overview = state.overview ?: return
    val month = runCatching { YearMonth.parse(state.month) }.getOrNull() ?: return
    val events = remember(overview) { overview.calendarEvents() }
    val dates = remember(events) { events.groupBy { it.date } }
    val subjectDates = remember(overview, today) { overview.calendarSubjects(today) }
    var listMode by rememberSaveable { mutableStateOf(false) }
    var showDates by rememberSaveable { mutableStateOf(false) }
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
        // These are sections in one view, not separate navigation panes.
        val sideBySide = maxWidth >= 720.dp && scale <= 1.3f
        val columns = if (maxWidth >= 560.dp && scale <= 1.3f) 2 else 1
        val cardWidth = (maxWidth - 12.dp * (columns - 1)) / columns
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (sideBySide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                CalendarMonth(month, state.selectedDay, today, dates, listMode, { listMode = it }, onDay, onMonth, onToday, Modifier.weight(1f))
                CalendarDay(overview, state.selectedDay, today, dates[state.selectedDay].orEmpty(), events, onJump, onLecture, Modifier.weight(1f))
            } else {
                CalendarMonth(month, state.selectedDay, today, dates, listMode, { listMode = it }, onDay, onMonth, onToday)
                CalendarDay(overview, state.selectedDay, today, dates[state.selectedDay].orEmpty(), events, onJump, onLecture)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Next classes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (subjectDates.isEmpty()) Text("No subjects to show.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = columns) {
                    subjectDates.forEach { info -> NextClassCard(info, today, onJump, Modifier.width(cardWidth)) }
                }
            }
            StudyDates(overview, events, subjectDates, showDates, { showDates = !showDates }, onJump)
        }
    }
}

@Composable
private fun CalendarMonth(month: YearMonth, selectedDay: String, today: String,
    dates: Map<String, List<BatchCalendarEvent>>, listMode: Boolean, onList: (Boolean) -> Unit,
    onDay: (String) -> Unit, onMonth: (Long) -> Unit, onToday: () -> Unit, modifier: Modifier = Modifier) {
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
                FilterChip(selected = !listMode, onClick = { onList(false) }, label = { Text("Month") },
                    modifier = Modifier.heightIn(min = 48.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = colors.onPrimary))
                FilterChip(selected = listMode, onClick = { onList(true) }, label = { Text("List") },
                    modifier = Modifier.heightIn(min = 48.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary, selectedLabelColor = colors.onPrimary))
                TextButton(onClick = onToday, modifier = Modifier.heightIn(min = 48.dp).testTag("calendar-today")) { Text("Today") }
            }
            if (listMode) {
                val own = dates.filterKeys { it.startsWith(month.toString()) }
                if (own.isEmpty()) Text("No classes or activity this month.", color = colors.onSurfaceVariant)
                own.forEach { (date, rows) ->
                    Surface(onClick = { onDay(date) }, shape = RoundedCornerShape(10.dp),
                        color = if (date == selectedDay) colors.primaryContainer else colors.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(calendarDateLabel(date, "EEE, d MMM"), fontWeight = FontWeight.SemiBold)
                            Text(activitySummary(rows), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            } else {
                BoxWithConstraints {
                    val short = maxWidth < 360.dp
                    Column(Modifier.widthIn(max = 376.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(if (short) label.take(1) else label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                        val leading = month.atDay(1).dayOfWeek.value - 1
                        val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it).toString() }
                        val padded = cells + List((7 - cells.size % 7) % 7) { null }
                        val height = 52.dp
                        padded.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                week.forEach { date ->
                                    if (date == null) Spacer(Modifier.weight(1f).height(height))
                                    else {
                                        val rows = dates[date].orEmpty()
                                        val classes = rows.filter { it.kind == BatchEventKind.CLASS }.mapNotNull { it.subject }.distinctBy { it.id }
                                        val kinds = listOf(BatchEventKind.DONE, BatchEventKind.PLANNED, BatchEventKind.REVISION, BatchEventKind.MILESTONE).filter { kind -> rows.any { it.kind == kind } }
                                        val isSelected = date == selectedDay
                                        val interaction = remember { MutableInteractionSource() }
                                        val pressed by interaction.collectIsPressedAsState()
                                        val motionEnabled = com.safarparmar.app.performance.LocalMotionPolicy.current.animationsEnabled
                                        val pressScale by animateFloatAsState(if (pressed && motionEnabled) .97f else 1f,
                                            spring(dampingRatio = .8f, stiffness = 500f), label = "Calendar date press")
                                        Surface(onClick = { onDay(date) }, shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) colors.primary else if (BatchEventKind.DONE in kinds) colors.primaryContainer else colors.surface,
                                            contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                                            border = BorderStroke(1.dp, if (date == today) colors.primary else colors.surface),
                                            interactionSource = interaction,
                                            modifier = Modifier.weight(1f).testTag("calendar-date-$date").height(height).graphicsLayer { scaleX = pressScale; scaleY = pressScale }.semantics {
                                                selected = isSelected
                                                contentDescription = "$date${if (date == today) ", today" else ""}, ${activitySummary(rows)}"
                                            }) {
                                            Column(Modifier.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(date.takeLast(2).toInt().toString(), style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 16.sp), fontWeight = if (date == today || isSelected) FontWeight.Bold else FontWeight.Normal)
                                                Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    classes.forEach { subject ->
                                                        Box(Modifier.size(5.dp).background(if (isSelected) colors.onPrimary else subjectProgressColor(subject), CircleShape))
                                                    }
                                                }
                                                Row(Modifier.height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { kinds.forEach { kind -> Icon(kind.icon(), null, Modifier.size(10.dp), tint = if (isSelected) colors.onPrimary else colors.primary) } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(5.dp).background(colors.primary, CircleShape)); Text("Class", style = MaterialTheme.typography.bodySmall)
                    }
                    listOf(BatchEventKind.DONE, BatchEventKind.PLANNED, BatchEventKind.REVISION, BatchEventKind.MILESTONE).forEach { kind ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(kind.icon(), null, Modifier.size(14.dp), tint = colors.primary)
                            Text(if (kind == BatchEventKind.MILESTONE) "Key dates" else kind.title, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CalendarMonthTitle(month: YearMonth) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(month.year.toString(), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CalendarMonthArrow(delta: Long, onMonth: (Long) -> Unit) {
    IconButton(onClick = { onMonth(delta) }, Modifier.size(48.dp).semantics {
        contentDescription = if (delta < 0) "Previous month" else "Next month"
    }) { Icon(if (delta < 0) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, null) }
}

private fun activitySummary(rows: List<BatchCalendarEvent>): String = listOfNotNull(
    rows.count { it.kind == BatchEventKind.CLASS }.takeIf { it > 0 }?.let { "$it ${if (it == 1) "class" else "classes"}" },
    rows.count { it.kind == BatchEventKind.PLANNED }.takeIf { it > 0 }?.let { "$it planned to study" },
    rows.count { it.kind == BatchEventKind.DONE }.takeIf { it > 0 }?.let { "$it done" },
    rows.count { it.kind == BatchEventKind.REVISION }.takeIf { it > 0 }?.let { "$it revisions" },
    rows.count { it.kind == BatchEventKind.MILESTONE }.takeIf { it > 0 }?.let { "$it study ${if (it == 1) "date" else "dates"}" }
).joinToString(" · ").ifEmpty { "No activity" }

@Composable
private fun CalendarDay(overview: BatchOverview, day: String, today: String, rows: List<BatchCalendarEvent>, events: List<BatchCalendarEvent>,
    onJump: (String) -> Unit, onLecture: (BatchLecture) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().testTag("calendar-agenda"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (day == today) "Today · ${calendarDateLabel(day, "d MMM")}" else calendarDateLabel(day, "EEE, d MMM yyyy"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(overview.copyText("calendarNote", "All times are IST."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (rows.isEmpty()) {
            Surface(color = colors.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.EventAvailable, null, tint = colors.onSurfaceVariant)
                    Text("Nothing planned or done this day.", color = colors.onSurfaceVariant)
                    events.firstOrNull { it.date > day && it.kind == BatchEventKind.CLASS }?.let { next ->
                        Text(calendarDateLabel(next.date), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onJump(next.date) }) { Text("See next class") }
                    }
                }
            }
        }
        listOf(BatchEventKind.DONE, BatchEventKind.CLASS, BatchEventKind.PLANNED, BatchEventKind.REVISION, BatchEventKind.MILESTONE).forEach { kind ->
            val own = rows.filter { it.kind == kind }
            if (own.isNotEmpty() || kind == BatchEventKind.DONE || kind == BatchEventKind.CLASS) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${overview.copyText(when (kind) { BatchEventKind.DONE -> "calendarCompleted"; BatchEventKind.CLASS -> "calendarScheduled"; BatchEventKind.PLANNED -> "calendarPlanned"; BatchEventKind.REVISION -> "calendarRevision"; else -> "calendarMilestones" }, kind.label())} · ${own.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (own.isEmpty()) Text(if (kind == BatchEventKind.DONE) overview.copyText("calendarCompletedEmpty", "No lectures completed.") else overview.copyText("calendarScheduledEmpty", "No classes scheduled."), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                own.forEach { event ->
                    Surface(onClick = { event.lecture?.let(onLecture) }, enabled = event.lecture != null,
                        color = colors.surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, colors.outlineVariant),
                        modifier = Modifier.fillMaxWidth().testTag("calendar-event-${kind.name}-${event.lecture?.id ?: event.title}")) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    event.subject?.let { Text(it.name, style = MaterialTheme.typography.labelMedium, color = colors.primary) }
                                    if (kind == BatchEventKind.CLASS) Text(event.lecture?.calendarTime().orEmpty(), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                }
                                Text(if (kind == BatchEventKind.MILESTONE) milestoneLabel(event.title) else event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (event.lecture?.revisionTagged == true || kind == BatchEventKind.REVISION) com.composables.ui.components.Text(
                                    "↻ Revision${if (kind == BatchEventKind.REVISION && event.completed) " · Completed" else ""}",
                                    modifier = Modifier.background(colors.secondaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                                if (kind == BatchEventKind.CLASS && event.lecture?.completedAt != null) Text(
                                    "Completed on ${calendarDateLabel(completionDay(event.lecture.completedAt))}", style = MaterialTheme.typography.bodySmall, color = colors.primary)
                                if (kind == BatchEventKind.DONE) Text("Official class: ${calendarDateLabel(event.lecture?.scheduledFor)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            if (event.lecture != null) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextClassCard(info: BatchSubjectDates, today: String, onJump: (String) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val next = info.nextClass
    val date = next?.scheduledFor?.let(::calendarDate)
    Surface(onClick = { date?.let(onJump) }, enabled = date != null, modifier = modifier.testTag("calendar-next-${info.subject.key}"),
        shape = RoundedCornerShape(14.dp), color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(subjectProgressColor(info.subject), CircleShape))
                Text(info.subject.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                if (date != null) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = colors.onSurfaceVariant)
            }
            if (next != null) {
                Text(next.displayTopic, style = MaterialTheme.typography.bodyMedium)
                Text("${if (date == today) "Today" else calendarDateLabel(date, "d MMM yyyy")} · ${next.calendarTime()}", style = MaterialTheme.typography.bodySmall, color = colors.primary)
            } else Text("Date not set", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            info.nextPersonal?.let { Text("Next to study: Lecture ${it.lectureNumber}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun StudyDates(overview: BatchOverview, events: List<BatchCalendarEvent>, subjects: List<BatchSubjectDates>,
    expanded: Boolean, onToggle: () -> Unit, onJump: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = onToggle, color = colors.surfaceVariant, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${if (expanded) "Hide" else "Show"} study dates" }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Study dates", fontWeight = FontWeight.SemiBold)
                    Text("My start: ${calendarDateLabel(overview.studyPlan?.startDate)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp))
            }
        }
        if (expanded) {
            val milestones = listOf("My start date" to overview.studyPlan?.startDate,
                "Batch start" to overview.course.officialStartDate,
                "First lecture done" to events.firstOrNull { it.kind == BatchEventKind.DONE }?.date) +
                listOfNotNull(overview.studyPlan?.targetDate?.let { "Goal date" to it })
            milestones.forEach { (label, date) -> StudyDateRow(label, date, onJump) }
            subjects.forEach { info ->
                HorizontalDivider(color = colors.outlineVariant)
                Text(info.subject.name, fontWeight = FontWeight.SemiBold)
                StudyDateRow("First class", info.firstClass, onJump)
                StudyDateRow("First lecture done", info.firstCompletion, onJump)
            }
        }
    }
}

@Composable
private fun StudyDateRow(label: String, date: String?, onJump: (String) -> Unit) {
    val valid = calendarDate(date)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (valid != null) TextButton(onClick = { onJump(valid) }) { Text(calendarDateLabel(valid)) }
        else Text("Not set", style = MaterialTheme.typography.bodyMedium)
    }
}
