package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

// Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · SAFAR theme · task-first hierarchy.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import androidx.compose.material3.Checkbox
import androidx.compose.ui.semantics.contentDescription
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon
import com.composables.ui.components.ProgressIndicator
import com.composables.ui.components.Text

@Composable
internal fun StudyModePicker(mode: BatchStudyMode, onMode: (BatchStudyMode) -> Unit) {
    val strings = rememberBatchStrings()
    var helpMode by remember { mutableStateOf<BatchStudyMode?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(4.dp)) {
            val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
            @Composable fun choice(value: BatchStudyMode, modifier: Modifier) {
                val selected = mode == value
                val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Row(modifier.background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = batchFeatureAction({ onMode(value) }), style = ButtonStyle.Ghost,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { this.selected = selected }) {
                        Text(strings.text(value.titleRes), style = MaterialTheme.typography.labelLarge, color = foreground)
                    }
                    IconButton(onClick = batchFeatureAction({ helpMode = value }), modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Info, contentDescription = strings.text(R.string.toppers_batch_about_count, strings.text(value.titleRes)),
                            tint = foreground, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (stacked) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BatchStudyMode.entries.forEach { choice(it, Modifier.fillMaxWidth()) }
            } else Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BatchStudyMode.entries.forEach { choice(it, Modifier.weight(1f)) }
            }
        }
        Text(if (mode == BatchStudyMode.OFFICIAL) strings.text(R.string.toppers_batch_your_daily_list_follows_official_parmar_topper_batch_schedule)
            else strings.text(R.string.toppers_batch_your_dates_your_daily_workload),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    helpMode?.let { value ->
        BatchAlertDialog(onDismissRequest = { helpMode = null },
            title = { Text(strings.text(value.titleRes), color = MaterialTheme.colorScheme.onSurface) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.text(if (value == BatchStudyMode.OFFICIAL)
                    R.string.toppers_batch_follow_parmar_s_official_class_dates_today_shows_the_lectures_sch
                    else R.string.toppers_batch_choose_which_lectures_to_study_and_when_use_add_to_plan_a_release), color = MaterialTheme.colorScheme.onSurface)
                Text(strings.text(R.string.toppers_batch_switching_modes_keeps_your_saved_plans_and_completion_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }, confirmButton = { Button(onClick = batchFeatureAction({ helpMode = null })) {
                Text(strings.text(R.string.toppers_batch_got_it), color = MaterialTheme.colorScheme.onPrimary)
            } }, dismissButton = {})
    }

}

/** Consistent disclosure: a large touch target and a visibly separate count/date label. */
@Composable
internal fun PlanDisclosure(title: String, badge: String, subtitle: String? = null, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val strings = rememberBatchStrings()
    var expanded by rememberSaveable() { mutableStateOf(initiallyExpanded) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(onClick = batchFeatureAction({ expanded = !expanded })).semantics { stateDescription = if (expanded) strings.text(R.string.toppers_batch_expanded) else strings.text(R.string.toppers_batch_collapsed) }
            .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) strings.text(R.string.toppers_batch_collapse_count, title) else strings.text(R.string.toppers_batch_expand_count, title), modifier = Modifier.size(20.dp))
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun LecturePlanSummary(lecture: BatchLecture, subject: BatchSubject?, meta: String,
    modifier: Modifier = Modifier, completed: Boolean = false, showSubject: Boolean = true, onClick: () -> Unit) {
    val strings = rememberBatchStrings()
    Column(modifier.fillMaxWidth().clickable(onClick = batchFeatureAction(onClick)).heightIn(min = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showSubject) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).background(subject?.let(::subjectProgressColor) ?: MaterialTheme.colorScheme.primary, CircleShape))
            Text(subject?.let(strings::subject).orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(strings.text(R.string.toppers_batch_count_lecture_count, lecture.displayTopic, lecture.lectureNumber.toString().padStart(2, '0')), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
        val detail = meta.replace(strings.text(R.string.toppers_batch_lecture_count, lecture.lectureNumber), "").trim(' ', '·')
        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun OfficialSchedulePanel(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val today = state.studyDay
    val reference = state.studyMode == BatchStudyMode.PERSONAL
    val rows = remember(overview, today) { overview.officialLecturesFrom(today) }
    val todayRows = rows.filter { calendarDate(it.scheduledFor) == today }
    val nextRows = rows.filter { calendarDate(it.scheduledFor)!! > today }.distinctBy { it.subjectId }
    var detailId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.text(R.string.toppers_batch_official_classes_today), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(calendarDateLabel(today, strings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (todayRows.isEmpty()) Text(strings.text(R.string.toppers_batch_no_official_classes_scheduled_today), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else if (!reference) {
                    val done = todayRows.count { it.completedAt != null }
                    Text(strings.quantity(R.plurals.toppers_batch_count_of_count_lectures_done, todayRows.size, done, todayRows.size), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    ProgressIndicator(progress = done.toFloat() / todayRows.size, modifier = Modifier.fillMaxWidth(), height = 6.dp,
                        indicatorColor = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
                }
                todayRows.forEach { lecture ->
                    HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
                    val subject = overview.subjects.firstOrNull { it.id == lecture.subjectId }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!reference) Checkbox(checked = lecture.completedAt != null,
                            enabled = lecture.id !in state.busyIds && !lecture.isLocked(today), onCheckedChange = batchFeatureChange({ vm.toggleDone(lecture) }),
                            modifier = Modifier.size(48.dp).semantics {
                                contentDescription = strings.text(if (lecture.completedAt != null) R.string.toppers_batch_undo_subject_lecture else R.string.toppers_batch_complete_subject_lecture, subject?.let(strings::subject).orEmpty(), lecture.lectureNumber)
                            })
                        LecturePlanSummary(lecture, subject,
                            strings.text(R.string.toppers_batch_lecture_count, lecture.lectureNumber) + (lecture.classTime?.let { strings.text(R.string.toppers_batch_count_ist_2, strings.time(it)) } ?: ""), Modifier.weight(1f),
                            completed = lecture.completedAt != null) { detailId = lecture.id }
                    }
                }
            }
        }
        PlanDisclosure(strings.text(R.string.toppers_batch_next_official_classes), "${nextRows.size}") {
            if (nextRows.isEmpty()) Text(strings.text(R.string.toppers_batch_no_upcoming_classes_scheduled), color = MaterialTheme.colorScheme.onSurfaceVariant)
            nextRows.forEach { lecture ->
                LecturePlanSummary(lecture, overview.subjects.firstOrNull { it.id == lecture.subjectId },
                    strings.text(R.string.toppers_batch_lecture_count_count, lecture.lectureNumber, calendarDateLabel(lecture.scheduledFor, strings)) + (lecture.classTime?.let { strings.text(R.string.toppers_batch_count_ist_2, strings.time(it)) } ?: "")) { detailId = lecture.id }
            }
        }
    }
    overview.lectures.firstOrNull { it.id == detailId }?.let { lecture ->
        LectureDetailsDialog(lecture, overview.subjects.firstOrNull { it.id == lecture.subjectId }, state, vm) { detailId = null }
    }
}
