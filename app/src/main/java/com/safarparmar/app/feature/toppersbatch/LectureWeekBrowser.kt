package com.safarparmar.app.feature.toppersbatch

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
    var expanded by remember { mutableStateOf(false) }
    val page = pages[selected]
    fun label(index: Int) = "${if (weekly) "Week" else "Page"} ${index + 1}"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSelect(selected - 1) }, enabled = selected > 0, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = "Previous week or page" }) { Text("‹") }
            Box(Modifier.weight(1f)) {
                DropdownMenu(expanded = expanded, onExpandedChange = { expanded = it },
                    panel = { BatchDropdownMenuPanel {
                        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                            pages.forEachIndexed { index, rows ->
                                DropdownMenuItem(onClick = { onSelect(index); expanded = false }) {
                                    Text("${if (index == selected) "✓ " else ""}${label(index)} · ${rows.count { it.completedAt != null }}/${rows.size} done")
                                }
                            }
                        }
                    } }) {
                    Button(onClick = { expanded = !expanded }, style = ButtonStyle.Outlined,
                        modifier = Modifier.fillMaxWidth()) { Text("${label(selected)} ▾") }
                }
            }
            Button(onClick = { onSelect(selected + 1) }, enabled = selected < pages.lastIndex, style = ButtonStyle.Outlined, modifier = Modifier.semantics { contentDescription = "Next week or page" }) { Text("›") }
        }
        val dates = page.mapNotNull { it.scheduledFor?.take(10) }.sorted()
        val range = if (dates.isEmpty()) "" else runCatching {
            "${LocalDate.parse(dates.first()).batchDate()} – ${LocalDate.parse(dates.last()).batchDate()}"
        }.getOrDefault("")
        Text(listOf(range, "${page.count { it.completedAt != null }}/${page.size} done").filter { it.isNotBlank() }.joinToString(" · "))
    }
}

/** Formats API dates; no local timetable is generated. */
internal fun java.time.LocalDate.batchDate(): String = format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH))
