package com.safarparmar.app.feature.toppersbatch

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.view.ContextThemeWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import com.safarparmar.app.feature.toppersbatch.BatchAlertDialog as AlertDialog
import com.safarparmar.app.feature.toppersbatch.BatchButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.composables.ui.components.Checkbox
import com.composables.ui.components.Radio
import com.composables.ui.components.RadioGroup
import com.composables.ui.components.DropdownMenu as UiDropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem as UiDropdownMenuItem
import com.composables.ui.components.DropdownMenuItemStyle
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text as UiText
import com.safarparmar.app.feature.toppersbatch.BatchFilterChip as FilterChip
import com.composables.ui.components.Icon
import com.safarparmar.app.feature.toppersbatch.BatchIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import com.safarparmar.app.feature.toppersbatch.BatchOutlinedButton as OutlinedButton
import com.safarparmar.app.feature.toppersbatch.BatchTextField
import androidx.compose.material3.Surface
import com.composables.ui.components.Text
import com.safarparmar.app.feature.toppersbatch.BatchTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun LectureCard(lecture: BatchLecture, subject: BatchSubject?, state: BatchUiState,
                         vm: ToppersBatchViewModel, focused: Boolean = false) {
    val pink = Color(0xFFBE185D)
    val lectureColour = (lecture.color ?: subject?.color)?.takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}$")) }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    val accent = lectureColour ?: pink
    val cardColour = lectureColour?.let { lerp(MaterialTheme.colorScheme.surface, it, 0.10f) }
        ?: MaterialTheme.colorScheme.surface
    val busy = lecture.id in state.busyIds
    var menu by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var revise by remember { mutableStateOf(false) }
    var changeColour by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var sourceChooser by remember { mutableStateOf(false) }
    var sourceError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val liveUrl = lecture.liveYoutubeUrl?.takeIf { link ->
        runCatching { Uri.parse(link) }.getOrNull()?.let { uri -> uri.scheme == "https" &&
            uri.host in setOf("youtube.com", "www.youtube.com", "youtu.be", "www.youtu.be") } == true
    }
    val recordedUrl = listOfNotNull(lecture.recordedUrl, subject?.externalUrl).firstOrNull { link ->
        runCatching { Uri.parse(link) }.getOrNull()?.let { uri -> uri.scheme == "https" &&
            uri.host in setOf("parmaracademy.in", "www.parmaracademy.in") } == true
    }
    val haptics = LocalHapticFeedback.current
    val shownNumber = lecture.lectureNumber
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, lectureColour ?: if (lecture.completedAt == null)
            MaterialTheme.colorScheme.outlineVariant else Color(0xFF198754)),
        colors = CardDefaults.cardColors(
            containerColor = cardColour,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.toggleDone(lecture) },
                    enabled = !busy,
                    modifier = Modifier.size(44.dp).semantics { contentDescription =
                        "${if (lecture.completedAt == null) "Mark as done" else "Mark as not done"}: ${lecture.displayTopic}" },
                ) {
                    Surface(shape = CircleShape, color = if (lecture.completedAt == null) Color.Transparent else Color(0xFF198754),
                        border = BorderStroke(2.dp, if (lecture.completedAt == null) accent else Color(0xFF198754))) {
                        androidx.compose.foundation.layout.Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            if (lecture.completedAt != null) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Lecture ${shownNumber.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                    Text(lecture.displayTopic, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                }
                if (state.celebratingId == lecture.id) Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = accent, modifier = Modifier.size(18.dp))
                if (!focused) UiDropdownMenu(
                    expanded = menu,
                    onExpandedChange = { menu = it },
                    alignment = DropdownMenuAlignment.End,
                    panel = { DropdownMenuPanel {
                        fun close() { menu = false }
                        val manuallyOlder = lecture.backlogAddedAt != null && lecture.backlogResolvedAt == null
                        if (lecture.completedAt != null) UiDropdownMenuItem(onClick = { close(); vm.toggleDone(lecture) }) {
                            UiText("Undo Completion")
                        }
                        UiDropdownMenuItem(onClick = { close(); vm.lectureAction(lecture, "backlog", remove = manuallyOlder) }) {
                            UiText(if (manuallyOlder) "Remove from backlog" else "Add to backlog")
                        }
                        if (lecture.completedAt != null || lecture.revisionDate != null) {
                            UiDropdownMenuItem(onClick = { close(); revise = true }) {
                                UiText(if (lecture.revisionDate == null) "Plan revision" else "Change revision dates")
                            }
                        }
                        if (lecture.revisionDate != null) UiDropdownMenuItem(onClick = { close(); vm.lectureAction(lecture, "revision", remove = true) }) {
                            UiText("Remove revision dates")
                        }
                        UiDropdownMenuItem(onClick = { close(); edit = true }) { UiText("Change lecture name") }
                        UiDropdownMenuItem(onClick = { close(); changeColour = true }) { UiText("Change colour") }
                        UiDropdownMenuItem(onClick = { close(); delete = true }, style = DropdownMenuItemStyle.Destructive) { UiText("Delete lecture") }
                    } },
                ) {
                    IconButton(onClick = { menu = !menu }, modifier = Modifier.semantics {
                        contentDescription = "More actions for ${lecture.displayTopic}"
                    }) { Icon(Icons.Default.MoreHoriz, contentDescription = null) }
                }
            }
            Surface(color = if (lecture.completedAt == null) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF198754).copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp)) {
                Text(if (lecture.completedAt == null) "Not completed" else "Completed",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (lecture.completedAt == null) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF198754))
            }
            if (!focused && lecture.section != null) Text(lecture.section, color = MaterialTheme.colorScheme.onSurfaceVariant)
            UiButton(onClick = { sourceChooser = true; sourceError = false }, style = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()) { Text("Open Lecture") }
            if (!focused && lecture.scheduledFor != null) Text("Parmar class: ${lecture.scheduledFor}" +
                (lecture.classTime?.let { " at $it" } ?: ""), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (lecture.completedAt != null) Text("Finished on ${runCatching {
                Instant.parse(lecture.completedAt).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().toString()
            }.getOrDefault(lecture.completedAt.take(10))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (lecture.revisionDate != null) Text("Revision: ${lecture.sessions.count { it.completedAt != null }} of ${lecture.sessions.size} done" +
                if (lecture.pendingRevision) " · Next: ${lecture.revisionDate}" else " · Done",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (lecture.completedAt != null && lecture.revisionDate == null) {
                OutlinedButton(onClick = { revise = true }, enabled = !busy) { Text("Plan revision") }
            }
            if (lecture.pendingRevision) {
                OutlinedButton(onClick = { vm.lectureAction(lecture, "revision/complete") }, enabled = !busy) { Text("Revision done") }
            }
        }
    }
    if (sourceChooser) AlertDialog(onDismissRequest = { sourceChooser = false },
        title = { Text("Open Lecture") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(lecture.displayTopic)
            if (liveUrl != null) OutlinedButton(onClick = {
                if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl))) }.isSuccess)
                    sourceChooser = false else sourceError = true
            }, modifier = Modifier.fillMaxWidth()) { Text("Live on YouTube") }
            OutlinedButton(onClick = {
                if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recordedUrl))) }.isSuccess)
                    sourceChooser = false else sourceError = true
            }, enabled = recordedUrl != null, modifier = Modifier.fillMaxWidth()) { Text("Recorded on Parmar Academy") }
            if (sourceError) Text("Could not open this link.", color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { Button(onClick = { sourceChooser = false }) { Text("Close") } },
        dismissButton = { })
    if (edit) TextEditDialog("Change lecture name", "Lecture name", lecture.displayTopic, 300,
        onDismiss = { edit = false }, error = state.error) { title -> vm.editLecture(lecture, title) { edit = false } }
    if (revise) RevisionDialog(lecture, vm) { revise = false }
    if (changeColour) ColourDialog(lecture.color ?: subject?.color, "Lecture colour", onDismiss = { changeColour = false }, error = state.error) {
        vm.lectureColor(lecture, it); changeColour = false
    }
    if (delete) ConfirmDeleteDialog("Delete this lecture?", "Your work will stay saved. Restore it in Lectures.",
        onDismiss = { delete = false }, onConfirm = { vm.removeLecture(lecture) { delete = false } })
}

@Composable
internal fun SubjectOptions(subject: BatchSubject, state: BatchUiState, vm: ToppersBatchViewModel) {
    var colour by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Change this subject here. Deleted work can be restored in Lectures.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { colour = true }) { Text("Change subject colour") }
        OutlinedButton(onClick = { link = true }) { Text("Change course link") }
        OutlinedButton(onClick = { delete = true }) { Text("Delete subject") }
    }
    if (colour) ColourDialog(subject.color, "Subject colour", onDismiss = { colour = false }, error = state.error) {
        vm.subjectColor(subject, it); colour = false
    }
    if (link) LinkDialog(subject, vm) { link = false }
    if (delete) ConfirmDeleteDialog("Delete ${subject.name}?", "Your saved work will stay. Restore it in Lectures.",
        onDismiss = { delete = false }, onConfirm = { vm.removeSubject(subject) { delete = false; vm.backToCourses() } })
}

@Composable
internal fun ConfirmDeleteDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton= { Button(onClick = onConfirm, destructive = true) { Text("Delete") } },
        dismissButton= { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun TextEditDialog(title: String, field: String, initial: String, limit: Int,
                           onDismiss: () -> Unit, error: String?, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column {
            BatchTextField(value = value, onValueChange = { value = it.take(limit) },
                label = { Text(field) }, singleLine = true)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton= { Button(onClick = { onSave(value.trim()) }, enabled = value.isNotBlank()) { Text("Save") } },
        dismissButton= { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun DateButton(value: String, label: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val date = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
        DatePickerDialog(ContextThemeWrapper(context, R.style.ThemeOverlay_Safar_ToppersPicker), { _, year, month, day ->
            onChange(LocalDate.of(year, month + 1, day).toString())
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, enabled = enabled, modifier = Modifier.semantics { contentDescription = "$label, ${value.ifBlank { "no date selected" }}" }) {
        Text(runCatching { LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH)) }.getOrDefault("Choose date"))
    }
}

private data class RevisionChoice(val selected: Boolean, val date: String)

@Composable
internal fun RevisionDialog(lecture: BatchLecture, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = ToppersBatchViewModel.indiaDay()
    val spacedDays = listOf(1L, 3L, 7L, 14L, 30L)
    val existing = lecture.sessions
    val activeSpaced = (lecture.revisionMode ?: lecture.legacyRevisionMode) == "spaced" && lecture.pendingRevision && existing.any { it.completedAt == null }
    val locked = if (activeSpaced) existing.count { it.completedAt != null } else 0
    var spaced by remember(lecture.id) { mutableStateOf(lecture.revisionMode != "custom" && lecture.legacyRevisionMode != "custom") }
    var customDate by remember(lecture.id) { mutableStateOf(lecture.revisionDate ?: today) }
    var rows by remember(lecture.id) { mutableStateOf(spacedDays.mapIndexed { index, days ->
        RevisionChoice(selected = !activeSpaced || index < existing.size,
            date = if (activeSpaced) existing.getOrNull(index)?.date ?: LocalDate.parse(today).plusDays(days).toString()
                   else LocalDate.parse(today).plusDays(days).toString())
    }) }
    var validation by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Plan your revision") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(lecture.displayTopic)
            Text("Choose dates to revise this lecture. You can change upcoming dates later.")
            RadioGroup(value = spaced, onValueChange = { spaced = it }, accessibilityLabel = "Revision date plan") {
                Column {
                    Radio(true) { Text("Suggested revision dates") }
                    Radio(false) { Text("One revision date I choose") }
                }
            }
            if (spaced) {
                Text("Suggested: 1, 3, 7, 14 and 30 days from today. Change or untick future dates. Keep at least one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                rows.forEachIndexed { index, row ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Checkbox(checked = row.selected, enabled = index >= locked,
                                onCheckedChange = { checked -> rows = rows.toMutableList().also { it[index] = row.copy(selected = checked) } },
                                accessibilityLabel = "Revision ${index + 1}${if (index < locked) " done" else ""}") {
                                Text("Revision ${index + 1}${if (index < locked) " · Done" else ""}",
                                    style = MaterialTheme.typography.labelLarge)
                            }
                            if (row.selected) DateButton(row.date, "Revision ${index + 1}", enabled = index >= locked) { date ->
                                rows = rows.toMutableList().also { it[index] = row.copy(date = date) }
                            }
                        }
                    }
                }
                Text("Dates are saved in date order. Tap ‘Revision done’ after each one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else DateButton(customDate, "Revision on") { customDate = it }
            if (validation != null) Text(validation.orEmpty(), color = MaterialTheme.colorScheme.error)
            DialogError(state.error)
        } },
        confirmButton= { Button(onClick = {
            if (spaced) {
                val dates = rows.drop(locked).filter { it.selected }.map { it.date }
                validation = when {
                    dates.isEmpty() -> "Choose at least one future date."
                    dates.distinct().size != dates.size -> "Choose different dates."
                    dates.any { it < today } -> "Choose today or a future date."
                    else -> null
                }
                if (validation == null) vm.lectureAction(lecture, "revision", mapOf("mode" to "spaced", "dates" to dates), onSuccess = onDismiss)
            } else {
                validation = if (customDate < today) "Choose today or a future date." else null
                if (validation == null) vm.lectureAction(lecture, "revision", mapOf("mode" to "custom", "date" to customDate), onSuccess = onDismiss)
            }
        }) { Text("Save revision plan") } },
        dismissButton= { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ColourDialog(initial: String?, title: String, onDismiss: () -> Unit, error: String?, onSave: (String?) -> Unit) {
    val palette = listOf(
        "Pink" to "#BE185D", "Red" to "#B91C1C", "Orange" to "#EA580C", "Yellow" to "#CA8A04",
        "Teal" to "#0F766E", "Blue" to "#1D4ED8", "Purple" to "#7C3AED", "Grey" to "#475569",
    )
    val choices = if (initial != null && palette.none { it.second.equals(initial, ignoreCase = true) } &&
        initial.matches(Regex("^#[0-9a-fA-F]{6}$"))) palette + ("Current" to initial) else palette
    var colour by remember(initial) { mutableStateOf(initial ?: palette.first().second) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Choose a colour.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            choices.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (name, code) ->
                        val isSelected = colour.equals(code, ignoreCase = true)
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Surface(onClick = { colour = code }, modifier = Modifier.size(48.dp).semantics {
                                contentDescription = "$name colour"
                                selected = isSelected
                            }, shape = CircleShape, color = Color(android.graphics.Color.parseColor(code)),
                                border = if (isSelected) BorderStroke(3.dp, Color.Black) else null) {
                                if (isSelected) androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White,
                                        modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (initial != null) TextButton(onClick = { onSave(null) }) { Text("Reset colour") }
            DialogError(error)
        } },
        confirmButton= { Button(onClick = { onSave(colour) }) { Text("Save") } },
        dismissButton= { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun LinkDialog(subject: BatchSubject, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var provider by remember { mutableStateOf(subject.externalProvider ?: "custom") }
    var url by remember { mutableStateOf(subject.externalUrl.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Course link") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Save the website where you watch this course.")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = provider == "parmar_academy", onClick = { provider = "parmar_academy" }, label = { Text("Parmar Academy") })
                FilterChip(selected = provider == "custom", onClick = { provider = "custom" }, label = { Text("Other website") })
            }
            BatchTextField(url, { url = it }, label = { Text("Website link") }, singleLine = true)
            Text("Use a secure link starting with https://", color = MaterialTheme.colorScheme.onSurfaceVariant)
            DialogError(state.error)
        } },
        confirmButton= { Button(onClick = { vm.subjectLink(subject, provider, url, onDismiss) },
            enabled = url.isBlank() || url.startsWith("https://")) { Text("Save link") } },
        dismissButton= { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun DialogError(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error)
}
