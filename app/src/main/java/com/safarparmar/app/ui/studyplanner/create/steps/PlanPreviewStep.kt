package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.data.remote.api.PlanPreviewResult
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.subjectDotColor
import com.safarparmar.app.ui.studyplanner.components.TopicEffortBars
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState

private val dayFormatter = DateTimeFormatter.ofPattern("MMM d")

private data class PreviewWeek(val label: String, val days: List<Pair<LocalDate, List<CalendarTopicItem>>>) {
    val topicCount: Int get() = days.sumOf { it.second.size }
    val studyDays: Int get() = days.count { it.second.isNotEmpty() }
}

private enum class DayLoad(val label: String) { REST("Rest day"), LIGHT("Light"), FULL("Full"), HEAVY("Heavy") }

private fun dayLoadOf(items: List<CalendarTopicItem>, dailyGoal: Int): DayLoad {
    if (items.isEmpty()) return DayLoad.REST
    val budget = (dailyGoal.coerceAtLeast(1) * 2).toFloat()
    val points = items.sumOf { it.points }.toFloat()
    return when {
        points < budget * 0.75f -> DayLoad.LIGHT
        points <= budget * 1.1f -> DayLoad.FULL
        else -> DayLoad.HEAVY
    }
}

private fun buildWeeks(preview: PlanPreviewResult): List<PreviewWeek> {
    val dates = preview.calendarPreview.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (dates.isEmpty()) return emptyList()
    val start = dates.min()
    val end = dates.max()
    val allDays = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    return allDays.chunked(7).mapIndexed { index, week ->
        PreviewWeek(
            label = "Week ${index + 1}",
            days = week.map { date -> date to (preview.calendarPreview[date.toString()] ?: emptyList()) },
        )
    }
}

@Composable
fun PlanPreviewStep(
    preview: PlanPreviewResult,
    isConfirming: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onAdjust: () -> Unit,
    onScheduleAnyway: (() -> Unit)?,
    onRenameTopic: (topicId: String, newName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val weeks = remember(preview) { buildWeeks(preview) }
    val examDateLabel = remember(preview.examDate) {
        preview.examDate?.take(10)?.let { runCatching { LocalDate.parse(it).format(dayFormatter) }.getOrNull() }
    }
    val scheme = MaterialTheme.colorScheme
    // The planner's own coral, not Material's generic blue primary — the blue read
    // as off-brand and washed-out on the cream light-mode sheet.
    val accent = PlannerFlatColors.PrimaryAccent

    var weekIndex by remember(weeks) { mutableIntStateOf(0) }

    // Days start collapsed so a whole week fits without scrolling. A SET, not a
    // single index, so opening one day never closes another — the student can line
    // two days up side by side.
    val expandedDays = remember(weeks) { mutableStateListOf<String>() }
    // Long-press target for the rename dialog. The pen icon was dropped for space,
    // so press-and-hold on the row is the entry point now.
    var renameTarget by remember { mutableStateOf<CalendarTopicItem?>(null) }
    val week = weeks.getOrNull(weekIndex)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        val goal = preview.dailyGoal ?: 0
        val needed = preview.summary.requiredPerDay ?: 0
        val skipped = preview.summary.scheduleSkipped

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Here's your schedule",
                    fontFamily = LoraFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                PlanHairline()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 12.dp),
                ) {
                    // Days to exam is the number the whole app orbits — the one a
                    // student feels. It carries the signature colour; the other two
                    // are context and stay quiet.
                    val stats = listOf(
                        Triple(preview.summary.scheduleAssigned.toString(), "Topics scheduled", false),
                        Triple(if (goal > 0) goal.toString() else needed.toString(), "Goal / day", false),
                        Triple((preview.summary.daysUntilExam ?: 0).toString(), "Days to exam", true),
                    )
                    stats.forEachIndexed { index, (value, label, isHero) ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(scheme.outlineVariant.copy(alpha = 0.4f)),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = value,
                                fontFamily = LoraFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isHero) PlannerAccent.Coral else scheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label.uppercase(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (isHero) {
                                    PlannerAccent.Coral.copy(alpha = 0.75f)
                                } else {
                                    scheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                PlanHairline()
            }

            if (skipped > 0) {
                item {
                    PlanCoverageMeter(
                        totalTopics = preview.summary.totalTopics,
                        scheduledTopics = preview.summary.scheduleAssigned,
                        skippedTopics = skipped,
                        neededPerDay = needed,
                        goalPerDay = goal,
                        onScheduleAnyway = onScheduleAnyway,
                        scheme = scheme,
                    )
                }
            } else if (needed > goal && goal > 0) {
                item {
                    PreviewVerdictCard(
                        accent = PlannerAccent.Amber,
                        icon = Icons.Default.Warning,
                        title = "This plan needs about $needed topics a day",
                        body = "You asked for $goal a day. Every topic still has a date, but some days will have more than you planned.",
                    )
                }
            } else {
                item {
                    // Derive the "no day is over your goal" claim from the actual
                    // schedule instead of asserting it. Strict scheduling now
                    // guarantees it, but flex deliberately overshoots, and the
                    // banner previously promised it either way — sitting directly
                    // above days visibly labelled "Busy".
                    val busyDays = remember(preview, goal) {
                        preview.calendarPreview.values.count { items ->
                            dayLoadOf(items, goal) == DayLoad.HEAVY
                        }
                    }
                    PreviewVerdictCard(
                        accent = PlannerAccent.Teal,
                        icon = Icons.Default.CheckCircle,
                        title = "Your plan fits before your exam",
                        body = if (busyDays == 0) {
                            "All ${preview.summary.totalTopics} topics have a date, and no day goes over your goal."
                        } else {
                            "All ${preview.summary.totalTopics} topics have a date. " +
                                "$busyDays ${if (busyDays == 1) "day is" else "days are"} a little over your goal."
                        },
                    )
                }
            }

            preview.warnings.forEach { warning ->
                item {
                    DeferredSubjectsCard(warning = warning, scheme = scheme)
                }
            }

            if (week == null) {
                item {
                    Text(
                        "We couldn't build a schedule preview yet.",
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    WeekNavHeader(
                        weekNumber = weekIndex + 1,
                        rangeLabel = "${week.days.first().first.format(dayFormatter)} – ${week.days.last().first.format(dayFormatter)}",
                        loadLabel = "${week.topicCount} topics · ${week.studyDays} study days",
                        onPrevious = { weekIndex = (weekIndex - 1).coerceAtLeast(0) },
                        onNext = { weekIndex = (weekIndex + 1).coerceAtMost(weeks.lastIndex) },
                        hasPrevious = weekIndex > 0,
                        hasNext = weekIndex < weeks.lastIndex,
                        accent = accent,
                        scheme = scheme,
                    )
                }

                // Long-press is invisible on its own, and these students are the
                // least likely to discover it by accident. One quiet line for the
                // whole screen beats a pen icon on every row.
                item {
                    Text(
                        "Tap a day to see its topics · press and hold a topic to rename it",
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                    )
                }

                // The whole week, top to bottom: every day is a labelled block with
                // its topics beneath it. A student sees "Monday: Physics, Physics,
                // Maths / Tuesday: rest / Wednesday: …" in one scroll — the way a
                // real timetable reads — instead of tapping through seven days.
                week.days.forEach { (date, dayTopics) ->
                    val dateKey = date.toString()
                    item(key = "dayhead_$date") {
                        DayHeader(
                            date = date,
                            topicCount = dayTopics.size,
                            load = dayLoadOf(dayTopics, goal),
                            scheme = scheme,
                            expanded = dateKey in expandedDays,
                            onToggle = if (dayTopics.isEmpty()) null else {
                                {
                                    if (dateKey in expandedDays) expandedDays.remove(dateKey)
                                    else expandedDays.add(dateKey)
                                }
                            },
                        )
                    }
                    if (dayTopics.isEmpty()) {
                        item(key = "rest_$date") {
                            Text(
                                "No study — take a break",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                            )
                        }
                    } else if (dateKey in expandedDays) {
                        items(dayTopics, key = { "${date}_${it.topicId}" }) { topic ->
                            TopicPill(
                                topic = topic,
                                scheme = scheme,
                                onLongPress = { renameTarget = topic },
                            )
                        }
                    }
                }

                // The exam is the anchor of the whole plan, shown once after the
                // final week so the student ends the review on the deadline.
                if (weekIndex == weeks.lastIndex && examDateLabel != null) {
                    item {
                        Column(modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                            PlanHairline(alpha = 0.6f)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Your exam",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = scheme.onSurfaceVariant,
                            )
                            Text(
                                examDateLabel,
                                fontFamily = LoraFontFamily,
                                fontSize = 20.sp,
                                color = PlannerAccent.Coral,
                            )
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    Text(error, color = scheme.error, fontSize = 12.sp)
                }
            }
        }

        PlanHairline()
        Spacer(Modifier.height(8.dp))

        val isLight = scheme.background.isLightBackground()

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    .clickable(enabled = !isConfirming) { onAdjust() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Go Back", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            }
            MacOSPrimaryActionButton(
                text = "Looks Good",
                onClick = onConfirm,
                isLoading = isConfirming,
                enabled = !isConfirming,
                isLight = isLight,
                modifier = Modifier.weight(1.2f),
            )
        }
    }

    renameTarget?.let { target ->
        var draft by remember(target.topicId) { mutableStateOf(target.topicName) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename topic") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    label = { Text("Topic name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank() && draft.trim() != target.topicName,
                    onClick = {
                        onRenameTopic(target.topicId, draft.trim())
                        renameTarget = null
                    },
                ) { Text("Save", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun WeekNavHeader(
    weekNumber: Int,
    rangeLabel: String,
    loadLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    accent: Color,
    scheme: androidx.compose.material3.ColorScheme,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = hasPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous week",
                tint = if (hasPrevious) accent else scheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "WEEK $weekNumber",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = accent,
            )
            Text(
                rangeLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = scheme.onSurface,
            )
            Text(
                loadLabel,
                fontSize = 11.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext, enabled = hasNext) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next week",
                tint = if (hasNext) accent else scheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
    }
}

/**
 * One day's heading in the whole-week list: the weekday and date on the left, and
 * on the right a plain word for how full the day is (Rest / Light / Full / Busy)
 * with a matching colour dot. Words + a dot, so the load reads at a glance without
 * decoding a chart — the whole point for a first-time user.
 */
@Composable
private fun DayHeader(
    date: LocalDate,
    topicCount: Int,
    load: DayLoad,
    scheme: androidx.compose.material3.ColorScheme,
    expanded: Boolean = false,
    /** Null on rest days — there is nothing to open. */
    onToggle: (() -> Unit)? = null,
) {
    val loadColor = when (load) {
        DayLoad.REST -> scheme.onSurfaceVariant
        DayLoad.LIGHT -> PlannerAccent.Teal
        DayLoad.FULL -> PlannerAccent.Amber
        DayLoad.HEAVY -> scheme.error
    }
    // "Busy" is friendlier than "Heavy" for the over-goal day.
    val loadWord = if (load == DayLoad.HEAVY) "Busy" else load.label

    Column {
        Spacer(Modifier.height(12.dp))
        PlanHairline(alpha = 0.6f)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    date.format(dayFormatter),
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(loadColor),
                )
                Text(
                    text = if (topicCount == 0) loadWord else "$topicCount topics · $loadWord",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = loadColor,
                )
                if (onToggle != null) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "dayChevron",
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Hide topics" else "Show topics",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).rotate(rotation),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopicPill(
    topic: CalendarTopicItem,
    scheme: androidx.compose.material3.ColorScheme,
    /** Press-and-hold to rename. Replaces the old pen icon, which no longer fits
     *  the row now that subject, chapter and effort all share it. */
    onLongPress: (() -> Unit)? = null,
) {

    val haptics = LocalHapticFeedback.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onLongPress != null) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            },
                        )
                    } else Modifier,
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The dot carries the SUBJECT, not the size. Subject is the thing a
            // student scans a day for ("how much Physics today?"), and the server
            // has always sent a distinct colour per subject that this screen
            // never used. Size moved to the bars on the right, so one row now
            // shows both dimensions without reading either.
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(subjectDotColor(topic.subjectColor)),
            )
            Column(
                modifier = Modifier.padding(start = 10.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    topic.topicName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (topic.subjectName.isNotBlank()) {
                        // Muted, not coloured — the subject is already carried by the
                        // dot on the left, so a blue label on every single row just
                        // shouted and crowded the list.
                        Text(
                            topic.subjectName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (topic.subjectName.isNotBlank() && topic.chapterName.isNotBlank()) {
                        Text(
                            "·",
                            fontSize = 11.sp,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                    if (topic.chapterName.isNotBlank()) {
                        Text(
                            topic.chapterName,
                            fontSize = 11.sp,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
            TopicEffortBars(
                size = topic.size ?: TopicSize.MEDIUM,
                activeColor = scheme.onSurfaceVariant,
                inactiveColor = scheme.outlineVariant.copy(alpha = 0.45f),
                large = true,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        PlanHairline(alpha = 0.4f)
    }
}

@Composable
private fun PreviewVerdictCard(
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    actionHint: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp).padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = body,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, color = accent, fontSize = 12.sp)
                }
                if (actionHint != null) {
                    Text(
                        text = actionHint,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCoverageMeter(
    totalTopics: Int,
    scheduledTopics: Int,
    skippedTopics: Int,
    neededPerDay: Int,
    goalPerDay: Int,
    onScheduleAnyway: (() -> Unit)?,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val assignedPercent = if (totalTopics > 0) (scheduledTopics.toFloat() / totalTopics.toFloat() * 100).roundToInt() else 100

    // One plain sentence, one bar, one fix — down from four stacked lines that
    // all said "some topics don't fit" three different ways.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = scheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "$skippedTopics topics won't fit before your exam",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.error,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(scheme.error.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((assignedPercent / 100f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(PlannerAccent.Teal),
            )
        }

        if (onScheduleAnyway != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PlannerFlatColors.PrimaryAccent, RoundedCornerShape(12.dp))
                    .clickable { onScheduleAnyway() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (neededPerDay > 0) "Fit them all — study $neededPerDay a day" else "Fit them all anyway",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.PrimaryAccent,
                )
            }
        }
    }
}

private data class ParsedDeferralWarning(
    val subjectChips: List<String>,
    val timelineText: String,
    val adviceText: String,
)

private fun parseDeferralWarning(warning: String): ParsedDeferralWarning? {
    if (!warning.contains("won't start until")) return null
    val parts = warning.split("won't start until")
    if (parts.size < 2) return null

    val rawSubjects = parts[0].trim()
    val remainder = parts[1].trim()

    val subjects = rawSubjects
        .split(", ")
        .flatMap { part ->
            if (part.contains(" and ")) {
                part.split(" and ")
            } else {
                listOf(part)
            }
        }
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val dotSplit = remainder.split(". ")
    val timeline = dotSplit.getOrNull(0)?.let { "Starts $it" } ?: remainder
    val advice = dotSplit.getOrNull(1) ?: ""

    return ParsedDeferralWarning(
        subjectChips = subjects,
        timelineText = timeline,
        adviceText = advice,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeferredSubjectsCard(
    warning: String,
    scheme: androidx.compose.material3.ColorScheme,
) {
    val parsed = remember(warning) { parseDeferralWarning(warning) }
    var expanded by remember { mutableStateOf(false) }

    if (parsed == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = warning,
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // One quiet tappable line by default — the fit warning above is the
            // headline, and a second bold alarm here just piled on. Details
            // (when each subject starts, the list) open only on tap.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PlannerAccent.Amber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${parsed.subjectChips.size} subjects start late in your plan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "Hide ▴" else "Details ▾",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.PrimaryAccent,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = parsed.timelineText,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        parsed.subjectChips.forEach { subjectName ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = subjectName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = scheme.onSurface,
                                )
                            }
                        }
                    }
                    if (parsed.adviceText.isNotBlank()) {
                        Text(
                            text = parsed.adviceText,
                            fontSize = 11.5.sp,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}
