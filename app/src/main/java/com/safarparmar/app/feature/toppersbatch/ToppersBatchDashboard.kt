package com.safarparmar.app.feature.toppersbatch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

internal fun BatchSubject.displayName() = name

@Composable
internal fun ToppersBatchDashboard(state: BatchUiState, vm: ToppersBatchViewModel) {
    val overview = state.overview ?: return
    val subjects = overview.subjects.filter { it.enabled }
    val done = overview.progress.completed
    val total = overview.progress.total
    val fraction = if (total == 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val today = ToppersBatchViewModel.indiaDay()
    val completedToday = overview.lectures.filter { lecture ->
        lecture.completedAt?.let { completedAt ->
            runCatching { Instant.parse(completedAt).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().toString() }
                .getOrNull() == today
        } == true
    }.sortedByDescending { it.completedAt }.take(3).mapNotNull { lecture ->
        subjects.firstOrNull { it.id == lecture.subjectId }?.let { it to lecture }
    }
    val comingUpToday = subjects.mapNotNull { subject ->
        val id = overview.watchList.firstOrNull { it.subjectId == subject.id }?.nextLectureId
        overview.lectures.firstOrNull { it.id == id && !it.isLocked(today) }?.let { subject to it }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(overview.copyText("progressTitle", "Overall batch progress"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("$done of $total lectures · ${(fraction * 100).roundToInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth(), height = 9.dp,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(overview.copyText("todayTitle", "Today’s Watch List"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SubjectWatchGrid(subjects, overview, vm)
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TodayLecturePanel(overview.copyText("completedToday", "Completed today"), completedToday, overview.copyText("completedEmpty", "No lectures completed today."),
                completed = true, vm = vm)
            TodayLecturePanel(overview.copyText("comingUpToday", "Coming up today"), comingUpToday, overview.copyText("upcomingEmpty", "You’re caught up with the available lectures."),
                completed = false, vm = vm)
            val revisions = overview.libraryRows("", LibraryFilter.REVISION)
                .filter { it.revisionDate!! <= today }
            if (revisions.isNotEmpty()) {
                Text("Revision due · ${revisions.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                revisions.take(3).forEach { lecture ->
                    LectureCard(lecture, subjects.firstOrNull { it.id == lecture.subjectId }, state, vm)
                }
                UiButton(onClick = { vm.searchLibrary(""); vm.filterLibrary(LibraryFilter.REVISION); vm.select(BatchSection.COURSES) },
                    style = ButtonStyle.Secondary) { Text("Manage all revisions") }
            }
            if (state.today?.date == today && state.today.lectures.isNotEmpty()) {
                Text(overview.copyText("officialTodayTitle", "Official classes today"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(overview.copyText("officialTodayNote", "Batch timetable · your next lecture still follows your progress."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.today.lectures.forEach { lecture ->
                    LectureCard(lecture, subjects.firstOrNull { it.id == lecture.subjectId }, state, vm)
                }
            }
            UiButton(onClick = { vm.select(BatchSection.PROGRESS) }, style = ButtonStyle.Ghost) {
                Text("My progress and dates")
            }
        }
    }
}

@Composable
internal fun TodaySubjectFocus(state: BatchUiState, subject: BatchSubject, vm: ToppersBatchViewModel) {
    val overview = state.overview ?: return
    if (subject.key == "gk" && overview.lectures.none { it.subjectId == subject.id }) {
        Text(overview.copyText("emptyLectures", "Lectures will appear when the official list is added."))
        return
    }
    val progress = overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }
    val done = progress?.completed ?: 0
    val total = progress?.total ?: 0
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val nextId = overview.watchList.firstOrNull { it.subjectId == subject.id }?.nextLectureId
    val next = overview.lectures.firstOrNull { it.id == nextId }
    val shown = overview.lectures.firstOrNull { it.id == state.focusedLectureId } ?: next
    val completed = shown?.completedAt != null

    Column(Modifier.fillMaxWidth().widthIn(max = 880.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Today · ${subject.displayName()}", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("$done of $total lectures done · ${(fraction * 100).roundToInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ProgressIndicator(progress = fraction, modifier = Modifier.fillMaxWidth(), height = 7.dp,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer)
            }
        }
        Text(if (completed) "Lecture completed" else "Your lecture", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        if (shown != null) LectureCard(shown, subject, state, vm, focused = true)
        else Text(if (total == 0) overview.copyText("emptyLectures", "Lectures will appear when the official list is added.")
            else "You’ve completed all available lectures in this subject.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (completed && next != null && next.id != shown?.id) {
            UiButton(onClick = { vm.continueTodayLecture(subject.id) }, style = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(), enabled = shown?.id !in state.busyIds) {
                Text("Continue to lecture ${next.lectureNumber.toString().padStart(2, '0')} →")
            }
        }
        UiButton(onClick = { vm.selectSubject(subject.id) }, style = ButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth()) { Text("View all ${subject.displayName()} lectures") }
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
    val accent = if (completed) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(34.dp).background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (completed) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                onClick = { vm.focusTodaySubject(subject.id) },
                style = ButtonStyle.Ghost,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val subjectAccent = subjectProgressColor(subject)
                    Box(Modifier.size(34.dp).background(subjectAccent.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(subject.displayName().take(1), color = subjectAccent,
                            fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${subject.displayName()} · Lecture ${lecture.lectureNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(lecture.displayTopic, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (completed) {
                        Box(Modifier.size(22.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White,
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
    overview: BatchOverview,
    vm: ToppersBatchViewModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        subjects.chunked(2).forEach { rowSubjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSubjects.forEach { subject ->
                    SubjectWatchChip(
                        subject = subject,
                        overview = overview,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.focusTodaySubject(subject.id) }
                    )
                }
                if (rowSubjects.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
) {
    val progress = overview.progress.bySubject.firstOrNull { it.subjectId == subject.id }
    val subjectDone = progress?.completed ?: 0
    val subjectTotal = progress?.total ?: 0
    val fraction = if (subjectTotal == 0) 0f else (subjectDone.toFloat() / subjectTotal).coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()
    val accent = subjectProgressColor(subject)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = accent, contentColor = Color.White),
        border = BorderStroke(1.dp, accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (subject.key == "gk") "GK" else subject.displayName().take(1),
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
                    text = subject.displayName(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (subject.key == "gk" && subjectTotal == 0) "Timetable" else "$percent%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
