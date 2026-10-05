package com.safarparmar.app.feature.toppersbatch

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
    val overview = state.overview ?: return
    val subjects = overview.subjects.filter { it.enabled }
    val subject = subjects.firstOrNull { it.id == state.selectedSubjectId }
        ?: subjects.firstOrNull { it.key == "mathematics" } ?: subjects.firstOrNull() ?: run {
            Text("No subjects to show")
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
    val visible = week?.value.orEmpty().filter { row ->
        when (state.lectureTab) {
            LectureTab.LECTURES -> true
            LectureTab.OLDER -> row.id in backlogIds && row.completedAt == null && !row.isLocked(today.toString())
            LectureTab.REVISION -> row.pendingRevision
        }
    }
    var subjectMenu by remember { mutableStateOf(false) }
    var weekMenu by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1.25f)) {
                DropdownMenu(expanded = subjectMenu, onExpandedChange = { subjectMenu = it }, panel = {
                    BatchDropdownMenuPanel {
                        subjects.forEach { candidate ->
                            DropdownMenuItem(onClick = {
                                vm.selectSubject(candidate.id, state.lectureTab)
                                subjectMenu = false
                            }) { Text(candidate.name) }
                        }
                    }
                }) {
                    Button(onClick = { subjectMenu = !subjectMenu }, style = ButtonStyle.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Choose subject: ${subject.name}" }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (subject.key == "mathematics") "Maths" else subject.name,
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
            LectureTab.entries.forEach { tab ->
                Button(onClick = { vm.selectLectureTab(tab) },
                    style = if (state.lectureTab == tab) ButtonStyle.Primary else ButtonStyle.Outlined,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f)) {
                    Text(when (tab) { LectureTab.LECTURES -> "Lectures"; LectureTab.OLDER -> "Backlog"; LectureTab.REVISION -> "Revision" },
                        fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.selectLecturePage(key, selected - 1) }, enabled = selected > 0, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = "Previous week" }) { Text("‹") }
            Box(Modifier.weight(1f)) {
                DropdownMenu(expanded = weekMenu, onExpandedChange = { weekMenu = it }, panel = {
                    BatchDropdownMenuPanel {
                        Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                            weeks.forEachIndexed { index, entry ->
                                DropdownMenuItem(onClick = { vm.selectLecturePage(key, index); weekMenu = false }) {
                                    Text(entry.key?.let { "${it.format(dateFormat)} – ${it.plusDays(6).format(dateFormat)}" } ?: "Undated")
                                }
                            }
                        }
                    }
                }) { Button(onClick = { weekMenu = !weekMenu }, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                    Text(start?.let { "${it.format(dateFormat)} – ${it.plusDays(6).format(dateFormat)} ▾" } ?: "Undated ▾")
                } }
            }
            Button(onClick = { vm.selectLecturePage(key, selected + 1) }, enabled = selected < weeks.lastIndex, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = "Next week" }) { Text("›") }
        }
        if (start != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (0..6).forEach { offset ->
                val day = start.plusDays(offset.toLong())
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(if (day == today) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 8.dp)) {
                    Text(day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)), fontSize = 12.sp, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                    Text(day.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        HorizontalSeparator()
        Text("${visible.size} ${when (state.lectureTab) { LectureTab.OLDER -> "pending"; LectureTab.REVISION -> "revisions"; else -> "lectures" }} this week", fontWeight = FontWeight.Bold)
        if (visible.isEmpty()) Text("Nothing here this week")
        visible.forEach { row ->
            Row(Modifier.fillMaxWidth().background(if (row.isLocked(today.toString())) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp)).alpha(if (row.isLocked(today.toString())) 0.65f else 1f).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(52.dp)) {
                    val date = row.scheduledFor?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    Text(date?.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) ?: "—", fontSize = 12.sp)
                    Text(date?.format(dateFormat) ?: "", fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                }
                if (row.completedAt != null) BatchIconButton(
                    onClick = { vm.toggleDone(row) }, enabled = row.id !in state.busyIds,
                    modifier = Modifier.size(40.dp).semantics { contentDescription = "Undo completion: ${row.displayTopic}" }
                ) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.CheckCircle,
                        contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF15803D),
                        modifier = Modifier.size(24.dp))
                } else Checkbox(checked = false, onCheckedChange = { vm.toggleDone(row) },
                    enabled = !row.isLocked(today.toString()) && row.id !in state.busyIds,
                    modifier = Modifier.semantics { contentDescription = "${if (row.completedAt == null) "Complete" else "Undo completion"} ${row.displayTopic}, lecture ${row.lectureNumber}" })
                Button(onClick = { detailId = row.id }, style = ButtonStyle.Ghost,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(row.displayTopic, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 3)
                        Text("Lecture ${row.lectureNumber.toString().padStart(2, '0')} · ${if (row.completedAt != null) "Done" else if (row.isLocked(today.toString())) "Class not started" else "To do"}", fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Button(onClick = { detailId = row.id }, style = ButtonStyle.Ghost, modifier = Modifier.semantics { contentDescription = "Actions for lecture ${row.lectureNumber}" }) { Text("⋯") }
            }
            HorizontalSeparator()
        }
        weeks.getOrNull(selected + 1)?.let { next ->
            Button(onClick = { vm.selectLecturePage(key, selected + 1) }, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Next week", fontSize = 12.sp, lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(next.value.first().displayTopic, fontSize = 14.sp, lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 2)
                        next.key?.let {
                            Text(if (it > today) "Class not started · ${it.format(dateFormat)}" else it.format(dateFormat),
                                fontSize = 12.sp, lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    val detail = own.firstOrNull { it.id == detailId }
    if (detail != null) LectureDetailsDialog(detail, subject, state, vm) { detailId = null }
}
