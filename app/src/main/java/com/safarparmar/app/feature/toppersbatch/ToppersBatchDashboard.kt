package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.ProgressIndicator
import com.composables.ui.components.Text
import com.composables.ui.components.Icon
import com.composables.ui.components.HorizontalSeparator
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

internal fun BatchSubject.displayName() = name

@Composable
internal fun ToppersBatchDashboard(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val done = overview.progress.completed
    val total = overview.progress.total
    val fraction = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val percent = "${(fraction * 100).roundToInt()}%"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.text(R.string.toppers_batch_overall_batch_progress), modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(percent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(strings.quantity(R.plurals.toppers_batch_count_of_count_lectures_done, total, done, total),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = strings.text(R.string.toppers_batch_count_count_of_count_lectures_count,
                        strings.text(R.string.toppers_batch_overall_batch_progress), done, total, percent)
                }, height = 8.dp, indicatorColor = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
            }
        }
        StudyModePicker(state.studyMode, vm::selectStudyMode)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(strings.text(R.string.toppers_batch_continue_learning), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            SubjectWatchGrid(overview.planningSubjects(), state, vm)
        }
        if (state.studyMode == BatchStudyMode.PERSONAL) PersonalPlanPanel(state, vm)
        else OfficialSchedulePanel(state, vm)
    }
}

@Composable
internal fun TodaySubjectFocus(state: BatchUiState, subject: BatchSubject, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    if (subject.key == "gk" && overview.lectures.none { it.subjectId == subject.id }) {
        Text(strings.copy(overview, "emptyLectures", strings.text(R.string.toppers_batch_lectures_will_appear_when_the_official_list_is_added)))
        return
    }
    val progress = overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }
    val done = progress?.completed ?: 0
    val total = progress?.total ?: 0
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val next = overview.nextStudyLecture(subject.id, state.studyMode, day = state.studyDay)
    val shown = overview.lectures.firstOrNull { it.id == state.focusedLectureId } ?: next
    val completed = shown?.completedAt != null

    Column(Modifier.fillMaxWidth().widthIn(max = 880.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(strings.text(R.string.toppers_batch_today_count, strings.subject(subject)), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.text(R.string.toppers_batch_your_progress), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(strings.quantity(R.plurals.toppers_batch_count_of_count_lectures_done, total, done, total, (fraction * 100).roundToInt()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth(), height = 7.dp,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer)
            }
        }
        Text(if (completed) strings.text(R.string.toppers_batch_lecture_completed) else if (state.studyMode == BatchStudyMode.OFFICIAL) strings.text(R.string.toppers_batch_next_official_lecture) else strings.text(R.string.toppers_batch_next_in_your_plan), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        if (shown != null) LectureCard(shown, subject, state, vm, focused = true)
        else Text(if (total == 0) strings.copy(overview, "emptyLectures", strings.text(R.string.toppers_batch_lectures_will_appear_when_the_official_list_is_added))
            else if (state.studyMode == BatchStudyMode.PERSONAL) strings.text(R.string.toppers_batch_no_lecture_planned_for_this_subject_add_one_from_today_or_choose)
            else strings.text(R.string.toppers_batch_no_upcoming_lecture_scheduled_for_this_subject_open_library_to_se),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (completed && next != null && next.id != shown?.id) {
            UiButton(onClick = batchFeatureAction({ vm.continueTodayLecture(subject.id) }), style = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(), enabled = shown?.id !in state.busyIds) {
                Text(strings.text(R.string.toppers_batch_continue_to_lecture_count, next.lectureNumber.toString().padStart(2, '0')))
            }
        }
        UiButton(onClick = batchFeatureAction({ vm.selectSubject(subject.id) }), style = ButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth()) { Text(strings.text(R.string.toppers_batch_view_all_count_lectures, strings.subject(subject))) }
    }
}

@Composable
private fun TodayLecturePanel(
    title: String,
    rows: List<Pair<BatchSubject, BatchLecture>>,
    emptyText: String,
    completed: Boolean,
    vm: ToppersBatchViewModel,
) {
    val strings = rememberBatchStrings()
    val accent = if (completed) Color(0xFF16A34A) else Color(0xFFE91E63)
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center) {
                if (completed) {
                    Box(Modifier.size(27.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                        tint = accent, modifier = Modifier.size(27.dp))
                }
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
        if (rows.isEmpty()) {
            Text(emptyText, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rows.forEachIndexed { index, (subject, lecture) ->
            if (index > 0) HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
            UiButton(
                onClick = batchFeatureAction({ vm.focusTodaySubject(subject.id, lecture.id) }),
                style = ButtonStyle.Ghost,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val subjectAccent = subjectProgressColor(subject)
                    Box(Modifier.size(40.dp).background(subjectAccent.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(strings.subject(subject).take(1), color = subjectAccent,
                            fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.text(R.string.toppers_batch_count_lecture_count_2, strings.subject(subject), lecture.lectureNumber),
                            fontSize = 15.sp,
                            color = subjectAccent)
                        Text(lecture.displayTopic, fontSize = 15.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (completed) {
                        Box(Modifier.size(24.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = strings.text(R.string.toppers_batch_completed), tint = Color.White,
                                modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubjectWatchGrid(
    subjects: List<BatchSubject>,
    state: BatchUiState,
    vm: ToppersBatchViewModel,
) {
    val overview = state.overview ?: return
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val columns = if (maxWidth < 400.dp || fontScale > 1.2f) 1 else 2
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        subjects.chunked(columns).forEach { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSubjects.forEach { subject ->
                    SubjectWatchChip(
                        subject = subject,
                        overview = overview,
                        modifier = Modifier.weight(1f),
                        onClick = batchFeatureAction({ vm.focusTodaySubject(subject.id) }),
                        menu = { SubjectOptions(subject, state, vm, tint = Color.White) }
                    )
                }
                if (columns == 2 && rowSubjects.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
    }
}

@Composable
private fun SubjectWatchChip(
    subject: BatchSubject,
    overview: BatchOverview,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    menu: @Composable () -> Unit,
) {
    val strings = rememberBatchStrings()
    val progress = overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }
    val subjectDone = progress?.completed ?: 0
    val subjectTotal = progress?.total ?: 0
    val fraction = if (subjectTotal == 0) 0f else (subjectDone.toFloat() / subjectTotal).coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()
    val accent = subjectProgressColor(subject)
    Card(
        onClick = batchFeatureAction(onClick),
        modifier = modifier.semantics {
            contentDescription = strings.text(R.string.toppers_batch_open_count_count_of_count_lectures_done,
                strings.subject(subject), subjectDone, subjectTotal)
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent, contentColor = Color.White),
        border = BorderStroke(1.dp, accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (subject.key == "gk") strings.text(R.string.toppers_batch_gk) else strings.subject(subject).take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (subject.key == "gk") 13.sp else 16.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = strings.subject(subject),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (subject.key == "gk" && subjectTotal == 0) strings.text(R.string.toppers_batch_timetable) else "$percent%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
            menu()
        }
    }
}

@Composable
internal fun DueRevisionPanel(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val subjects = overview.subjects.filter { it.enabled }
    val today = ToppersBatchViewModel.indiaDay()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val revisions = overview.dueRevisions(today)
            if (revisions.isNotEmpty()) {
                Text(strings.text(R.string.toppers_batch_revision_due_count, revisions.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                revisions.take(3).forEach { lecture ->
                    LectureCard(lecture, subjects.firstOrNull { it.id == lecture.subjectId }, state, vm)
                }
                UiButton(onClick = batchFeatureAction({ vm.searchLibrary(""); vm.filterLibrary(LibraryFilter.REVISION); vm.select(BatchSection.COURSES) }),
                    style = ButtonStyle.Secondary) { Text(strings.text(R.string.toppers_batch_manage_all_revisions)) }
            }
    }
}
