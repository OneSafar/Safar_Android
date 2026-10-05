package com.safarparmar.app.feature.toppersbatch

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.view.ContextThemeWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
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
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
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
internal fun LectureDetailsDialog(lecture: BatchLecture, subject: BatchSubject?, state: BatchUiState,
    vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { UiText("Lecture details") }
                IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "Close lecture details" }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
        },
        text = { LectureCard(lecture, subject, state, vm, flat = true) },
        confirmButton = { UiButton(onClick = onDismiss, style = ButtonStyle.Ghost, modifier = Modifier.fillMaxWidth()) { UiText("Close") } },
        dismissButton = {})
}

@Composable
internal fun LectureCard(lecture: BatchLecture, subject: BatchSubject?, state: BatchUiState,
                         vm: ToppersBatchViewModel, focused: Boolean = false, flat: Boolean = false) {
    BatchClampedContent { LectureCardContent(lecture, subject, state, vm, focused, flat) }
}

@Composable
private fun LectureCardContent(lecture: BatchLecture, subject: BatchSubject?, state: BatchUiState,
                               vm: ToppersBatchViewModel, focused: Boolean, flat: Boolean) {
    val pink = Color(0xFFBE185D)
    val lectureColour = (lecture.color ?: subject?.color ?: subject?.defaultColor)?.takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}$")) }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    val locked = lecture.isLocked(ToppersBatchViewModel.indiaDay())
    val accent = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else lectureColour ?: pink
    val cardColour = if (locked) MaterialTheme.colorScheme.surfaceVariant else lectureColour?.let { lerp(MaterialTheme.colorScheme.surface, it, 0.10f) }
        ?: MaterialTheme.colorScheme.surface
    val busy = lecture.id in state.busyIds
    var menu by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var revise by remember { mutableStateOf(false) }
    var moveDate by remember { mutableStateOf(false) }
    var changeColour by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var sourceChooser by remember { mutableStateOf(false) }
    var sourceError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var liveNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(sourceChooser) {
        if (sourceChooser) while (true) { liveNow = Instant.now(); delay(1000) }
    }
    val liveStatus = lecture.liveWindow?.status(liveNow) ?: "unavailable"
    val liveUrl = lecture.liveWindow?.youtubeUrl?.takeIf { link ->
        runCatching { Uri.parse(link) }.getOrNull()?.let { uri -> uri.scheme == "https" &&
            uri.host in setOf("youtube.com", "www.youtube.com", "youtu.be", "www.youtu.be") } == true
    }
    fun copy(key: String, fallback: String) = state.overview?.copyText(key, fallback) ?: fallback
    val youtubeLabel = when (liveStatus) {
        "academy-only" -> copy("liveAcademyOnly", "Available in Parmar Academy")
        "upcoming" -> copy("liveUpcoming", "Live lecture has not started")
        "ended" -> copy("liveEnded", "Live Lecture is over")
        "live" -> if (liveUrl != null) copy("openYoutube", "Open in YouTube App") else copy("liveUnavailable", "Live schedule unavailable")
        else -> copy("liveUnavailable", "Live schedule unavailable")
    }

    val recordedUrl = listOfNotNull(state.overview?.course?.academyCourseUrl, subject?.externalUrl).firstOrNull { link ->
        runCatching { Uri.parse(link) }.getOrNull()?.let { uri -> uri.scheme == "https" &&
            uri.host in setOf("parmaracademy.in", "www.parmaracademy.in") } == true
    }
    val haptics = LocalHapticFeedback.current
    val shownNumber = lecture.lectureNumber
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (flat) null else BorderStroke(1.dp, if (locked) MaterialTheme.colorScheme.outlineVariant else lectureColour ?: if (lecture.completedAt == null)
            MaterialTheme.colorScheme.outlineVariant else Color(0xFF198754)),
        colors = CardDefaults.cardColors(
            containerColor = if (flat) MaterialTheme.colorScheme.surface else cardColour,
            contentColor = if (locked && !flat) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (flat) 0.dp else 1.dp),
    ) {
        Column(Modifier.padding(if (flat) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!flat) IconButton(
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.toggleDone(lecture) },
                    enabled = !busy && !locked,
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
                    if (flat) Text(listOfNotNull(subject?.name, "Lecture ${shownNumber.toString().padStart(2, '0')}").joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        Text("Lecture ${shownNumber.toString().padStart(2, '0')}", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                        Text(lecture.displayTopic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (state.celebratingId == lecture.id) Icon(Icons.Default.AutoAwesome, contentDescription = null,
                    tint = accent, modifier = Modifier.size(18.dp))
                if (!focused) UiDropdownMenu(
                    expanded = menu,
                    onExpandedChange = { menu = it },
                    alignment = DropdownMenuAlignment.End,
                    panel = { BatchDropdownMenuPanel {
                        fun close() { menu = false }
                        if (flat && lecture.completedAt == null) UiDropdownMenuItem(enabled = !busy && !locked, onClick = { close(); vm.toggleDone(lecture) }) { UiText("Mark as completed") }
                        val manuallyOlder = lecture.backlogAddedAt != null && lecture.backlogResolvedAt == null
                        if (lecture.completedAt != null) UiDropdownMenuItem(onClick = { close(); vm.toggleDone(lecture) }) {
                            UiText("Undo Completion")
                        }
                        UiDropdownMenuItem(enabled = !locked, onClick = { close(); vm.lectureAction(lecture, "backlog", remove = manuallyOlder) }) {
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
                        UiDropdownMenuItem(onClick = { close(); moveDate = true }) { UiText(copy("moveLecture", "Move to another study date")) }
                        if (lecture.studyPlannedFor != null) UiDropdownMenuItem(onClick = { close(); vm.lectureAction(lecture, "study-date", remove = true) }) { UiText("Clear personal study date") }
                        UiDropdownMenuItem(onClick = { close(); vm.lectureAction(lecture, "revision-tag", remove = lecture.revisionTagged) }) {
                            UiText(if (lecture.revisionTagged) copy("removeRevisionTag", "Remove revision tag") else copy("addRevisionTag", "Add revision tag"))
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
            if (flat) Text(lecture.displayTopic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Surface(color = if (lecture.completedAt == null && flat) MaterialTheme.colorScheme.primaryContainer else if (lecture.completedAt == null) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF198754).copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp)) {
                Text(if (locked) "Class not started" else if (lecture.completedAt == null) "To do" else "Done",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (lecture.completedAt == null) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF198754))
            }
            if (flat) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    @Composable fun dateRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, divider: Boolean = true) {
                        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    dateRow("Official schedule", calendarDateLabel(lecture.scheduledFor, "EEE, d MMM yyyy") + (lecture.classTime?.let { " · $it IST" } ?: ""), Icons.Default.CalendarMonth, false)
                    dateRow("Your study date", lecture.studyPlannedFor?.let { calendarDateLabel(it, "EEE, d MMM yyyy") } ?: "Not set", Icons.Default.PersonOutline)
                    lecture.completedAt?.let { dateRow("Completed on", calendarDateLabel(completionDay(it)), Icons.Default.Check) }
                    lecture.revisionDate?.let { dateRow("Next revision", calendarDateLabel(it) + " · ${lecture.sessions.count { session -> session.completedAt != null }}/${lecture.sessions.size} done", Icons.Default.Refresh) }
                    if (lecture.revisionTagged) UiText("↻ Revision tagged")
                }
            } else {
                if (lecture.revisionTagged) UiText("↻ Revision")
                lecture.studyPlannedFor?.let { UiText("Your study date: $it") }
                val scheduleLabel = lecture.scheduledFor?.take(10)?.let { date ->
                    runCatching { java.time.LocalDate.parse(date).format(
                        java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy", java.util.Locale.ENGLISH))
                    }.getOrDefault(date)
                } ?: listOf(lecture.sourceMonth, lecture.sourceWeek).filter { it.isNotBlank() }.joinToString(" · ")
                if (scheduleLabel.isNotBlank()) Text(scheduleLabel +
                    (lecture.classTime?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (lecture.revisionDate != null) Text("Revision ${lecture.sessions.count { it.completedAt != null }}/${lecture.sessions.size} · ${lecture.revisionDate}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            UiDropdownMenu(expanded = sourceChooser, onExpandedChange = { sourceChooser = it },
                alignment = DropdownMenuAlignment.End, panel = { BatchDropdownMenuPanel {
                    UiDropdownMenuItem(enabled = recordedUrl != null, onClick = {
                        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recordedUrl))) }.isSuccess)
                            sourceChooser = false else sourceError = true
                    }) { UiText(copy("openAcademy", "Open in Parmar Academy")) }
                    UiDropdownMenuItem(enabled = liveStatus == "live" && liveUrl != null, onClick = {
                        // Re-check at tap time, including the exact end boundary.
                        if (lecture.liveWindow?.status(Instant.now()) == "live" && liveUrl != null) {
                            val native = Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl)).setPackage("com.google.android.youtube")
                            val opened = runCatching { context.startActivity(native) }.isSuccess ||
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl))) }.isSuccess
                            if (opened) sourceChooser = false else sourceError = true
                        }
                    }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            UiText(copy("openYoutube", "Open in YouTube App"))
                            if (liveStatus != "live" || liveUrl == null) UiText(youtubeLabel)
                        }
                    }
                } }) {
                UiButton(onClick = { sourceChooser = !sourceChooser; sourceError = false },
                    style = ButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { UiText(copy("openLecture", "Open lecture")) }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (flat) UiButton(onClick = { moveDate = true }, enabled = !busy, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                UiText("Move study date")
            }
            if (sourceError) Text("Could not open this link.", color = MaterialTheme.colorScheme.error)
            if (lecture.completedAt != null && lecture.revisionDate == null) {
                UiButton(onClick = { revise = true }, enabled = !busy, style = ButtonStyle.Secondary, modifier = if (flat) Modifier.fillMaxWidth() else Modifier) { UiText("Plan revision") }
            }
            if (lecture.pendingRevision) {
                UiButton(onClick = { vm.lectureAction(lecture, "revision/complete") }, enabled = !busy, style = ButtonStyle.Secondary, modifier = if (flat) Modifier.fillMaxWidth() else Modifier) { UiText("Revision done") }
            }
        }
    }
    if (moveDate) MoveStudyDateDialog(lecture, state, vm) { moveDate = false }
    if (edit) TextEditDialog("Change lecture name", "Lecture name", lecture.displayTopic, 300,
        onDismiss = { edit = false }, error = state.error) { title -> vm.editLecture(lecture, title) { edit = false } }
    if (revise) RevisionBottomSheet(lecture, vm) { revise = false }
    if (changeColour) ColourDialog(lecture.color ?: subject?.color ?: subject?.defaultColor, "Lecture colour", onDismiss = { changeColour = false }, error = state.error) {
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
    if (colour) ColourDialog(subject.color ?: subject.defaultColor, "Subject colour", onDismiss = { colour = false }, error = state.error) {
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
    OutlinedButton(
        onClick = {
            val date = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
            val pickerContext = batchPickerContext(context)
            DatePickerDialog(pickerContext, { _, year, month, day ->
                onChange(LocalDate.of(year, month + 1, day).toString())
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        },
        enabled = enabled,
        modifier = Modifier
            .widthIn(min = 142.dp)
            .semantics { contentDescription = "$label, ${value.ifBlank { "no date selected" }}" },
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            runCatching {
                LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH))
            }.getOrDefault("Choose date"),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

private data class RevisionChoice(val selected: Boolean, val date: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RevisionBottomSheet(lecture: BatchLecture, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        BatchClampedContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Plan your revision",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Schedule spaced milestones for this lecture.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Lecture topic badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = lecture.displayTopic,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Revision Mode Selector (Equal Sized Paired Cards)
                RadioGroup(value = spaced, onValueChange = { spaced = it }, accessibilityLabel = "Revision date plan") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { spaced = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (spaced) 2.dp else 1.dp,
                                color = if (spaced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            color = if (spaced) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Suggested",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (spaced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Radio(value = true)
                                }
                                Text(
                                    "1, 3, 7, 14, 30 days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { spaced = false },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (!spaced) 2.dp else 1.dp,
                                color = if (!spaced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            color = if (!spaced) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Custom",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (!spaced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Radio(value = false)
                                }
                                Text(
                                    "Pick single date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (spaced) {
                    // Modern section header with live counter instead of a long explanation line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "REVISION TIMELINE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val activeCount = rows.count { it.selected }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                "$activeCount of ${rows.size} active",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            val intervalLabels = listOf("Day 1", "Day 3", "Week 1", "Week 2", "Month 1")
                            rows.forEachIndexed { index, row ->
                                val isLocked = index < locked
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Checkbox(
                                            checked = row.selected,
                                            enabled = !isLocked,
                                            onCheckedChange = { checked ->
                                                rows = rows.toMutableList().also { it[index] = row.copy(selected = checked) }
                                            },
                                            accessibilityLabel = "Revision ${index + 1}${if (isLocked) " done" else ""}",
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        "Revision ${index + 1}",
                                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (row.selected) MaterialTheme.colorScheme.onSurface
                                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    if (isLocked) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(6.dp),
                                                        ) {
                                                            Text(
                                                                "Done",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    intervalLabels.getOrElse(index) { "+${spacedDays.getOrElse(index) { 0 }}d" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }

                                    if (row.selected) {
                                        DateButton(
                                            value = row.date,
                                            label = "Revision ${index + 1}",
                                            enabled = !isLocked,
                                        ) { date ->
                                            rows = rows.toMutableList().also { it[index] = row.copy(date = date) }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(min = 142.dp)
                                                .heightIn(min = 48.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Text(
                                                    "Skipped",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                if (index < rows.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }

                    // Compact visual hint chip instead of a plain dangling text sentence
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                "Milestones appear on your lecture card as each date arrives.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    "Revision date",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Select when to revise this lecture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DateButton(customDate, "Revision on") { customDate = it }
                        }
                    }
                }

                if (validation != null) {
                    Text(
                        validation.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    )
                }
                DialogError(state.error)

                // Bottom Action buttons (Equal Sized 1f:1f Paired Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            if (spaced) {
                                val dates = rows.drop(locked).filter { it.selected }.map { it.date }
                                validation = when {
                                    dates.isEmpty() -> "Choose at least one future date."
                                    dates.distinct().size != dates.size -> "Choose different dates."
                                    dates.any { it < today } -> "Choose today or a future date."
                                    else -> null
                                }
                                if (validation == null) {
                                    vm.lectureAction(
                                        lecture,
                                        "revision",
                                        mapOf("mode" to "spaced", "dates" to dates),
                                        onSuccess = onDismiss,
                                    )
                                }
                            } else {
                                validation = if (customDate < today) "Choose today or a future date." else null
                                if (validation == null) {
                                    vm.lectureAction(
                                        lecture,
                                        "revision",
                                        mapOf("mode" to "custom", "date" to customDate),
                                        onSuccess = onDismiss,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save Plan", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RevisionDialog(lecture: BatchLecture, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    RevisionBottomSheet(lecture, vm, onDismiss)
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

/** Keep the Activity theme when overriding font scale for a framework picker. */
internal fun batchPickerContext(context: android.content.Context): ContextThemeWrapper =
    ContextThemeWrapper(context, R.style.ThemeOverlay_Safar_ToppersPicker).apply {
        applyOverrideConfiguration(android.content.res.Configuration().apply {
            fontScale = context.resources.configuration.fontScale.coerceIn(0.85f, 1.05f)
        })
    }

@Composable
private fun MoveStudyDateDialog(lecture: BatchLecture, state: BatchUiState, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    var date by remember(lecture.id) { mutableStateOf(lecture.studyPlannedFor ?: state.selectedDay) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Move to another study date") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lecture.displayTopic)
            Text("Choose when you want to study this lecture. The official class date and completion history stay the same.")
            DateButton(date, "Your study date") { date = it }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { Button(enabled = lecture.id !in state.busyIds, onClick = {
            vm.lectureAction(lecture, "study-date", mapOf("date" to date)) { onDismiss() }
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
