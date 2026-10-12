package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import com.composeunstyled.ProvideTextStyle
import com.composables.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun BatchWeeklyAgenda(state: BatchUiState, vm: ToppersBatchViewModel) {
    BatchClampedContent {
        BatchWeeklyAgendaContent(state, vm)
    }
}

@Composable
private fun BatchWeeklyAgendaContent(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val subjects = overview.subjects.filter { it.enabled }
    val subject = subjects.firstOrNull { it.id == state.selectedSubjectId }
        ?: subjects.firstOrNull { it.key == "mathematics" } ?: subjects.firstOrNull() ?: run {
            Text(strings.text(R.string.toppers_batch_no_subjects_to_show))
            return
        }
    val today = LocalDate.parse(ToppersBatchViewModel.indiaDay())
    val own = overview.lectures.filter { it.subjectId == subject.id }.sortedBy { it.order }
    val weeks = own.groupBy { row ->
        row.scheduledFor?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
    }.entries.sortedBy { it.key ?: LocalDate.MAX }
    val key = "agenda-${subject.id}"
    val currentMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val initial = weeks.indexOfFirst { it.key == currentMonday }.coerceAtLeast(0)
    val selected = (state.lecturePages[key] ?: initial).coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
    val week = weeks.getOrNull(selected)
    val start = week?.key
    val backlogIds = overview.watchList.firstOrNull { it.subjectId == subject.id }?.backlogLectureIds.orEmpty().toSet()
    val visible = (if (state.lectureTab == LectureTab.LECTURES) week?.value.orEmpty() else own).filter { row ->
        when (state.lectureTab) {
            LectureTab.LECTURES -> true
            LectureTab.OLDER -> row.id in backlogIds && row.completedAt == null && !row.isLocked(today.toString())
            LectureTab.REVISION -> row.pendingRevision || row.revisionTagged
        }
    }
    var subjectMenu by remember { mutableStateOf(false) }
    var weekMenu by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember(strings.locale) { DateTimeFormatter.ofPattern("d MMM", strings.locale) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            @Composable fun subjectPicker(modifier: Modifier) {
            Box(modifier) {
                DropdownMenu(expanded = subjectMenu, onExpandedChange = { subjectMenu = it }, panel = {
                    BatchDropdownMenuPanel {
                        subjects.forEach { candidate ->
                            DropdownMenuItem(onClick = batchFeatureAction({
                                vm.selectSubject(candidate.id, state.lectureTab)
                                subjectMenu = false
                            })) { Text(strings.subject(candidate)) }
                        }
                    }
                }) {
                    Button(onClick = batchFeatureAction({ subjectMenu = !subjectMenu }), style = ButtonStyle.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = strings.text(R.string.toppers_batch_choose_subject_count, strings.subject(subject)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (subject.key == "mathematics") strings.text(R.string.toppers_batch_maths) else strings.subject(subject),
                                fontSize = 12.sp, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            }
            @Composable fun RowScope.tabs() {
            LectureTab.entries.forEach { tab ->
                Button(onClick = batchFeatureAction({ vm.selectLectureTab(tab) }),
                    style = if (state.lectureTab == tab) ButtonStyle.Primary else ButtonStyle.Outlined,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)) {
                    Text(when (tab) { LectureTab.LECTURES -> strings.text(R.string.toppers_batch_lectures); LectureTab.OLDER -> strings.text(R.string.toppers_batch_backlog); LectureTab.REVISION -> strings.text(R.string.toppers_batch_revision) },
                        fontSize = 12.sp, maxLines = 1)
                }
            }
            }
            if (maxWidth < 400.dp || LocalDensity.current.fontScale > 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectPicker(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { tabs() }
                }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                subjectPicker(Modifier.weight(1.25f))
                tabs()
            }
        }
        if (state.lectureTab == LectureTab.LECTURES) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = batchFeatureAction({ vm.selectLecturePage(key, selected - 1) }), enabled = selected > 0, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_previous_week) }) { Text("‹") }
            Box(Modifier.weight(1f)) {
                DropdownMenu(expanded = weekMenu, onExpandedChange = { weekMenu = it }, panel = {
                    BatchDropdownMenuPanel {
                        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                            weeks.forEachIndexed { index, entry ->
                                DropdownMenuItem(onClick = batchFeatureAction({ vm.selectLecturePage(key, index); weekMenu = false })) {
                                    Text(entry.key?.let { "${it.format(dateFormat)} – ${it.plusDays(6).format(dateFormat)}" } ?: strings.text(R.string.toppers_batch_undated))
                                }
                            }
                        }
                    }
                }) { Button(onClick = batchFeatureAction({ weekMenu = !weekMenu }), style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                    Text(start?.let { "${it.format(dateFormat)} – ${it.plusDays(6).format(dateFormat)} ▾" } ?: strings.text(R.string.toppers_batch_undated_2))
                } }
            }
            Button(onClick = batchFeatureAction({ vm.selectLecturePage(key, selected + 1) }), enabled = selected < weeks.lastIndex, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_next_week) }) { Text("›") }
        }
        if (start != null && state.lectureTab == LectureTab.LECTURES) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { offset ->
                val day = start.plusDays(offset.toLong())
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(if (day == today) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 8.dp)) {
                    Text(day.format(DateTimeFormatter.ofPattern("EEE", strings.locale)), fontSize = 12.sp, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                    Text(day.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        if (state.lectureTab != LectureTab.LECTURES && visible.isEmpty()) {
            val emptyMessage = if (state.lectureTab == LectureTab.OLDER) {
                strings.text(R.string.toppers_batch_there_are_no_backlogs_yet)
            } else {
                strings.text(R.string.toppers_batch_there_are_no_revisions_yet)
            }
            val minHeight = (LocalConfiguration.current.screenHeightDp - 260).coerceAtLeast(240).dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            HorizontalSeparator()
            Text(when (state.lectureTab) {
                LectureTab.OLDER -> strings.quantity(R.plurals.toppers_batch_pending_count, visible.size, visible.size)
                LectureTab.REVISION -> strings.quantity(R.plurals.toppers_batch_revisions_count, visible.size, visible.size)
                LectureTab.LECTURES -> strings.quantity(R.plurals.toppers_batch_week_lectures_count, visible.size, visible.size)
            }, fontWeight = FontWeight.Bold)
            if (visible.isEmpty()) Text(if (state.lectureTab == LectureTab.LECTURES) strings.text(R.string.toppers_batch_nothing_here_this_week) else strings.text(R.string.toppers_batch_no_items_to_show))
            visible.forEach { row ->
                Row(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, subjectProgressColor(subject), 0.10f), RoundedCornerShape(10.dp)).alpha(if (row.isLocked(today.toString())) 0.65f else 1f).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.width(52.dp)) {
                        val date = row.scheduledFor?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        Text(date?.format(DateTimeFormatter.ofPattern("EEE", strings.locale)) ?: "—", fontSize = 12.sp)
                        Text(date?.format(dateFormat) ?: "", fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    }
                    if (row.completedAt != null) BatchIconButton(
                        onClick = batchFeatureAction({ vm.toggleDone(row) }), enabled = row.id !in state.busyIds,
                        modifier = Modifier.size(40.dp).semantics { contentDescription = strings.text(R.string.toppers_batch_undo_completion_count, row.displayTopic) }
                    ) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF15803D),
                            modifier = Modifier.size(24.dp))
                    } else Checkbox(checked = false, onCheckedChange = batchFeatureChange({ vm.toggleDone(row) }),
                        enabled = !row.isLocked(today.toString()) && row.id !in state.busyIds,
                        modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_count_count_lecture_count_2, if (row.completedAt == null) strings.text(R.string.toppers_batch_complete) else strings.text(R.string.toppers_batch_undo_completion_2), row.displayTopic, row.lectureNumber) })
                    Button(onClick = batchFeatureAction({ detailId = row.id }), style = ButtonStyle.Ghost,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(row.displayTopic, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 3)
                            Text(strings.text(R.string.toppers_batch_lecture_count_count, row.lectureNumber.toString().padStart(2, '0'), if (row.completedAt != null) strings.text(R.string.toppers_batch_done) else if (row.isLocked(today.toString())) strings.text(R.string.toppers_batch_class_not_started) else strings.text(R.string.toppers_batch_to_do)), fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                    Button(onClick = batchFeatureAction({ detailId = row.id }), style = ButtonStyle.Ghost, modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_actions_for_lecture_count, row.lectureNumber) }) { Text("⋯") }
                }
                HorizontalSeparator()
            }
            if (state.lectureTab == LectureTab.LECTURES) {
                weeks.getOrNull(selected + 1)?.let { next ->
                    Button(onClick = batchFeatureAction({ vm.selectLecturePage(key, selected + 1) }), style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(strings.text(R.string.toppers_batch_next_week), fontSize = 12.sp, lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(next.value.first().displayTopic, fontSize = 14.sp, lineHeight = 20.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 2)
                                next.key?.let {
                                    Text(if (it > today) strings.text(R.string.toppers_batch_class_not_started_count, it.format(dateFormat)) else it.format(dateFormat),
                                        fontSize = 12.sp, lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    val detail = own.firstOrNull { it.id == detailId }
    if (detail != null) LectureDetailsDialog(detail, subject, state, vm) { detailId = null }
}
