package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.composables.ui.components.*
import java.time.LocalDate

internal fun lecturePages(lectures: List<BatchLecture>): List<List<BatchLecture>> =
    lectures.sortedWith(compareBy<BatchLecture> { it.order }.thenBy { it.lectureNumber }).chunked(7)

internal fun initialLecturePage(pages: List<List<BatchLecture>>, today: String): Int {
    val current = pages.indexOfFirst { page -> page.any { it.scheduledFor?.take(10) == today } }
    if (current >= 0) return current
    return pages.indexOfFirst { page -> page.any { it.completedAt == null && !it.isLocked(today) } }.coerceAtLeast(0)
}

@Composable
internal fun LectureWeekBrowser(pages: List<List<BatchLecture>>, selected: Int, weekly: Boolean,
                                onSelect: (Int) -> Unit) {
    val strings = rememberBatchStrings()
    var expanded by remember { mutableStateOf(false) }
    val page = pages[selected]
    fun label(index: Int) = "${if (weekly) strings.text(R.string.toppers_batch_week) else strings.text(R.string.toppers_batch_page)} ${index + 1}"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = batchFeatureAction({ onSelect(selected - 1) }), enabled = selected > 0, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_previous_week_or_page) }) { Text("‹") }
            Box(Modifier.weight(1f)) {
                DropdownMenu(expanded = expanded, onExpandedChange = { expanded = it },
                    panel = { BatchDropdownMenuPanel {
                        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                            pages.forEachIndexed { index, rows ->
                                DropdownMenuItem(onClick = batchFeatureAction({ onSelect(index); expanded = false })) {
                                    Text(strings.text(R.string.toppers_batch_count_count_count_count_done, if (index == selected) "✓ " else "", label(index), rows.count { it.completedAt != null }, rows.size))
                                }
                            }
                        }
                    } }) {
                    Button(onClick = batchFeatureAction({ expanded = !expanded }), style = ButtonStyle.Outlined,
                        modifier = Modifier.fillMaxWidth()) { Text("${label(selected)} ▾") }
                }
            }
            Button(onClick = batchFeatureAction({ onSelect(selected + 1) }), enabled = selected < pages.lastIndex, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_next_week_or_page) }) { Text("›") }
        }
        val dates = page.mapNotNull { it.scheduledFor?.take(10) }.sorted()
        val range = if (dates.isEmpty()) "" else runCatching {
            "${LocalDate.parse(dates.first()).batchDate(strings)} – ${LocalDate.parse(dates.last()).batchDate(strings)}"
        }.getOrDefault("")
        Text(listOf(range, strings.text(R.string.toppers_batch_count_count_done, page.count { it.completedAt != null }, page.size)).filter { it.isNotBlank() }.joinToString(" · "))
    }
}

/** Formats API dates; no local timetable is generated. */
internal fun java.time.LocalDate.batchDate(strings: BatchStrings): String = format(java.time.format.DateTimeFormatter.ofPattern("d MMM", strings.locale))
