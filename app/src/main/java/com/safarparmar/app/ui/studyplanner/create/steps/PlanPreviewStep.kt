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
    modifier: Modifier = Modifier,
) {
    val weeks = remember(preview) { buildWeeks(preview) }
    val examDateLabel = remember(preview.examDate) {
        preview.examDate?.take(10)?.let { runCatching { LocalDate.parse(it).format(dayFormatter) }.getOrNull() }
    }
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary

    var weekIndex by remember(weeks) { mutableIntStateOf(0) }
    val week = weeks.getOrNull(weekIndex)

    var dayIndex by remember(weekIndex, weeks) {
        val today = LocalDate.now()
        val defaultIndex = week?.days?.indexOfFirst { it.first == today }?.takeIf { it >= 0 }
            ?: week?.days?.indexOfFirst { it.second.isNotEmpty() }?.takeIf { it >= 0 }
            ?: 0
        mutableIntStateOf(defaultIndex)
    }

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
                    PreviewVerdictCard(
                        accent = PlannerAccent.Teal,
                        icon = Icons.Default.CheckCircle,
                        title = "Your plan fits before your exam",
                        body = "All ${preview.summary.totalTopics} topics have a date, and no day goes over your goal.",
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

                item {
                    DayChipRow(
                        days = week.days.map { it.first },
                        dayItems = week.days.map { it.second },
                        loads = week.days.map { dayLoadOf(it.second, goal) },
                        selectedIndex = dayIndex,
                        accent = accent,
                        scheme = scheme,
                        onSelect = { dayIndex = it },
                    )
                }

                val selectedDay = week.days.getOrNull(dayIndex)
                if (selectedDay != null) {
                    val load = dayLoadOf(selectedDay.second, goal)
                    if (selectedDay.second.isNotEmpty()) {
                        item {
                            Text(
                                text = "${selectedDay.second.size} topics · ${load.label.lowercase()} day",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (load) {
                                    DayLoad.HEAVY -> scheme.error
                                    DayLoad.FULL -> PlannerAccent.Amber
                                    else -> PlannerAccent.Teal
                                },
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(selectedDay.second, key = { it.topicId }) { topic ->
                            TopicPill(
                                topic = topic,
                                scheme = scheme,
                            )
                        }
                    } else {
                        item {
                            Text(
                                "Rest day 🌿",
                                fontSize = 13.sp,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                if (examDateLabel != null) {
                    item {
                        Text(
                            "Exam Date: $examDateLabel 🎯",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                        )
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

@Composable
private fun DayChipRow(
    days: List<LocalDate>,
    dayItems: List<List<CalendarTopicItem>>,
    loads: List<DayLoad>,
    selectedIndex: Int,
    accent: Color,
    scheme: androidx.compose.material3.ColorScheme,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            days.forEachIndexed { index, date ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .then(
                            if (selected) Modifier.background(accent)
                            else Modifier.border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Workload as a mini bar chart across the week: bar height is that day's
        // topic count against the busiest day, so the shape of the row answers
        // "which days are heavy?" without tapping into any of them.
        val busiest = dayItems.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.indices.forEach { index ->
                DayVolumeBar(
                    topicCount = dayItems.getOrNull(index)?.size ?: 0,
                    busiestCount = busiest,
                    isOverGoal = loads.getOrNull(index) == DayLoad.HEAVY,
                    isSelected = index == selectedIndex,
                    accent = accent,
                    scheme = scheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Taller = busier",
                fontSize = 10.sp,
                color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TopicPill(
    topic: CalendarTopicItem,
    scheme: androidx.compose.material3.ColorScheme,
) {

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                        Text(
                            topic.subjectName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.primary,
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
    val accent = scheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$skippedTopics topics don't fit",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.error,
            )
            Text(
                text = "$assignedPercent% fit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(scheme.error.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((assignedPercent / 100f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(accent),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$scheduledTopics of $totalTopics topics got a date",
                fontSize = 11.5.sp,
                color = scheme.onSurfaceVariant,
            )

            if (onScheduleAnyway != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, scheme.error.copy(alpha = 0.6f), CircleShape)
                        .clickable { onScheduleAnyway() }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (neededPerDay > 0) "Fit all · $neededPerDay a day" else "Fit them all anyway",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.error,
                    )
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "${parsed.subjectChips.size} subjects start close to exam",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (expanded) "Hide ▴" else "View (${parsed.subjectChips.size}) ▾",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    modifier = Modifier
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            Text(
                text = parsed.timelineText,
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
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

/**
 * One day's workload as a single bar, scaled against the busiest day of the
 * week. Height alone carries the comparison, so it stays readable at the ~40dp
 * a day chip gets on a phone — where proportional subject bands collapsed into
 * unreadable slivers. Red marks a day that runs past the student's daily goal.
 */
@Composable
private fun DayVolumeBar(
    topicCount: Int,
    busiestCount: Int,
    isOverGoal: Boolean,
    isSelected: Boolean,
    accent: Color,
    scheme: ColorScheme,
    modifier: Modifier = Modifier,
) {
    val fraction = if (busiestCount <= 0) 0f else topicCount.toFloat() / busiestCount
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
        if (topicCount == 0) {
            // Rest day: a faint rule keeps the week continuous instead of a gap.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(scheme.outlineVariant.copy(alpha = 0.35f)),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction.coerceIn(0.18f, 1f))
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(
                        when {
                            isOverGoal -> scheme.error
                            isSelected -> accent
                            else -> accent.copy(alpha = 0.35f)
                        },
                    ),
            )
        }
    }
}
