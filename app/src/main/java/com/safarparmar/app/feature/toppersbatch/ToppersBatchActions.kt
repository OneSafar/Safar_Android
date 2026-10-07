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
import androidx.compose.material.icons.filled.MoreVert
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
    val strings = rememberBatchStrings()
    var editingPlan by remember(lecture.id) { mutableStateOf(false) }
    if (editingPlan) {
        StudyPlanDialog(lecture, state, vm, onDismiss)
        return
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { UiText(strings.text(R.string.toppers_batch_lecture_details)) }
                IconButton(onClick = batchFeatureAction(onDismiss), modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_close_lecture_details) }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
        },
        text = { LectureCard(lecture, subject, state, vm, flat = true, onPlan = { editingPlan = true }) },
        confirmButton = { UiButton(onClick = batchFeatureAction(onDismiss), style = ButtonStyle.Ghost, modifier = Modifier.fillMaxWidth()) { UiText(strings.text(R.string.toppers_batch_close)) } },
        dismissButton = {})
}

@Composable
internal fun LectureCard(lecture: BatchLecture, subject: BatchSubject?, state: BatchUiState,
                         vm: ToppersBatchViewModel, focused: Boolean = false, flat: Boolean = false, onPlan: (() -> Unit)? = null) {
    BatchClampedContent { LectureCardContent(lecture, subject, state, vm, focused, flat, onPlan) }
}

@Composable
private fun LectureCardContent(lecture: BatchLecture, subject: BatchSubject?, state: BatchUiState,
                               vm: ToppersBatchViewModel, focused: Boolean, flat: Boolean, onPlan: (() -> Unit)?) {
    val strings = rememberBatchStrings()
    val pink = Color(0xFFBE185D)
    val lectureColour = (subject?.color ?: subject?.defaultColor ?: lecture.color)?.takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}$")) }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    val locked = lecture.isLocked(ToppersBatchViewModel.indiaDay())
    val accent = lectureColour ?: pink
    val cardColour = lectureColour?.let { lerp(MaterialTheme.colorScheme.surface, it, 0.10f) }
        ?: MaterialTheme.colorScheme.surface
    val busy = lecture.id in state.busyIds
    var menu by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf(false) }
    fun editPlan() { if (onPlan != null) onPlan() else plan = true }
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
    fun copy(key: String, fallback: String) = strings.copy(state.overview, key, fallback)
    val youtubeLabel = when (liveStatus) {
        "academy-only" -> copy("liveAcademyOnly", strings.text(R.string.toppers_batch_available_in_parmar_academy))
        "upcoming" -> copy("liveUpcoming", strings.text(R.string.toppers_batch_live_lecture_has_not_started))
        "ended" -> copy("liveEnded", strings.text(R.string.toppers_batch_live_lecture_is_over))
        "live" -> if (liveUrl != null) copy("openYoutube", strings.text(R.string.toppers_batch_open_in_youtube_app)) else copy("liveUnavailable", strings.text(R.string.toppers_batch_live_schedule_unavailable))
        else -> copy("liveUnavailable", strings.text(R.string.toppers_batch_live_schedule_unavailable))
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
        border = if (flat) null else BorderStroke(1.dp, lectureColour ?: if (lecture.completedAt == null)
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
                    onClick = batchFeatureAction({ haptics.performHapticFeedback(HapticFeedbackType.LongPress); vm.toggleDone(lecture) }),
                    enabled = !busy && !locked,
                    modifier = Modifier.size(44.dp).semantics { contentDescription =
                        "${if (lecture.completedAt == null) strings.text(R.string.toppers_batch_mark_as_done) else strings.text(R.string.toppers_batch_mark_as_not_done)}: ${lecture.displayTopic}" },
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
                    if (flat) Text(listOfNotNull(subject?.let(strings::subject), strings.text(R.string.toppers_batch_lecture_count, shownNumber.toString().padStart(2, '0'))).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else {
                        Text(strings.text(R.string.toppers_batch_lecture_count, shownNumber.toString().padStart(2, '0')), style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
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
                        if (flat && lecture.completedAt == null) UiDropdownMenuItem(enabled = !busy && !locked, onClick = batchFeatureAction({ close(); vm.toggleDone(lecture) })) { UiText(strings.text(R.string.toppers_batch_mark_as_completed)) }
                        if (lecture.pendingRevision) UiDropdownMenuItem(enabled = !busy && !locked, onClick = batchFeatureAction({ close(); vm.lectureAction(lecture, "revision/complete") })) {
                            UiText(strings.text(R.string.toppers_batch_revision_done))
                        }
                        val manuallyOlder = lecture.backlogAddedAt != null && lecture.backlogResolvedAt == null
                        if (lecture.completedAt != null) UiDropdownMenuItem(enabled = !busy, onClick = batchFeatureAction({ close(); vm.toggleDone(lecture) })) {
                            UiText(strings.text(R.string.toppers_batch_undo_completion))
                        }
                        UiDropdownMenuItem(enabled = !locked, onClick = batchFeatureAction({ close(); vm.lectureAction(lecture, "backlog", remove = manuallyOlder) })) {
                            UiText(if (manuallyOlder) strings.text(R.string.toppers_batch_remove_from_backlog) else strings.text(R.string.toppers_batch_add_to_backlog))
                        }
                        UiDropdownMenuItem(onClick = batchFeatureAction({ close(); edit = true })) { UiText(strings.text(R.string.toppers_batch_change_lecture_name)) }
                        UiDropdownMenuItem(onClick = batchFeatureAction({ close(); delete = true }), style = DropdownMenuItemStyle.Destructive) { UiText(strings.text(R.string.toppers_batch_delete_lecture)) }
                    } },
                ) {
                    IconButton(onClick = batchFeatureAction({ menu = !menu }), modifier = Modifier.semantics {
                        contentDescription = strings.text(R.string.toppers_batch_more_actions_for_count, lecture.displayTopic)
                    }) { Icon(Icons.Default.MoreHoriz, contentDescription = null) }
                }
            }
            if (flat) Text(lecture.displayTopic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Surface(color = if (lecture.completedAt == null && flat) MaterialTheme.colorScheme.primaryContainer else if (lecture.completedAt == null) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF198754).copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp)) {
                Text(if (locked) strings.text(R.string.toppers_batch_class_not_started) else if (lecture.completedAt == null) strings.text(R.string.toppers_batch_to_do) else strings.text(R.string.toppers_batch_done),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (lecture.completedAt == null) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF198754))
            }
            if (flat) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(12.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    @Composable fun dateRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, divider: Boolean = true, onClick: (() -> Unit)? = null) {
    val strings = rememberBatchStrings()
                        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(enabled = !busy, onClick = batchFeatureAction(onClick)).heightIn(min = 48.dp) else Modifier),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            if (onClick != null) Icon(Icons.Default.KeyboardArrowDown, contentDescription = strings.text(R.string.toppers_batch_edit_watch_date), modifier = Modifier.size(20.dp))
                        }
                    }
                    dateRow(strings.text(R.string.toppers_batch_official_schedule), calendarDateLabel(lecture.scheduledFor, strings, "EEE, d MMM yyyy") + (lecture.classTime?.let { strings.text(R.string.toppers_batch_count_ist_2, strings.time(it)) } ?: ""), Icons.Default.CalendarMonth, false)
                    if (state.studyMode == BatchStudyMode.PERSONAL && lecture.completedAt == null) dateRow(strings.text(R.string.toppers_batch_watch_on), lecture.studyPlannedFor?.let { calendarDateLabel(it, strings, "EEE, d MMM yyyy") } ?: strings.text(R.string.toppers_batch_choose_a_date), Icons.Default.PersonOutline, onClick = batchFeatureAction(::editPlan))
                    else if (state.studyMode == BatchStudyMode.PERSONAL) lecture.studyPlannedFor?.let { dateRow(strings.text(R.string.toppers_batch_planned_watch), calendarDateLabel(it, strings, "EEE, d MMM yyyy"), Icons.Default.PersonOutline) }
                    lecture.completedAt?.let { dateRow(strings.text(R.string.toppers_batch_completed_on), if (lecture.completionDateUnknown) strings.text(R.string.toppers_batch_previously_completed) else calendarDateLabel(completionDay(it), strings), Icons.Default.Check) }
                    lecture.revisionDate?.let { dateRow(strings.text(R.string.toppers_batch_next_revision), calendarDateLabel(it, strings) + strings.text(R.string.toppers_batch_count_count_done_2, lecture.sessions.count { session -> session.completedAt != null }, lecture.sessions.size), Icons.Default.Refresh) }
                    if (lecture.revisionTagged) UiText(strings.text(R.string.toppers_batch_revision_tagged))
                }
            } else {
                if (lecture.revisionTagged) UiText(strings.text(R.string.toppers_batch_revision_2))
                val scheduleLabel = lecture.scheduledFor?.take(10)?.let { date ->
                    runCatching { java.time.LocalDate.parse(date).format(
                        java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy", strings.locale))
                    }.getOrDefault(date)
                } ?: listOf(lecture.sourceMonth, lecture.sourceWeek).filter { it.isNotBlank() }.joinToString(" · ")
                if (scheduleLabel.isNotBlank()) Text(scheduleLabel +
                    (lecture.classTime?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (lecture.revisionDate != null) Text(strings.text(R.string.toppers_batch_revision_count_count_count, lecture.sessions.count { it.completedAt != null }, lecture.sessions.size, lecture.revisionDate),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            UiDropdownMenu(expanded = sourceChooser, onExpandedChange = { sourceChooser = it },
                alignment = DropdownMenuAlignment.End, panel = { BatchDropdownMenuPanel {
                    UiDropdownMenuItem(enabled = recordedUrl != null, onClick = batchFeatureAction({
                        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(recordedUrl))) }.isSuccess)
                            sourceChooser = false else sourceError = true
                    })) { UiText(copy("openAcademy", strings.text(R.string.toppers_batch_open_in_parmar_academy))) }
                    UiDropdownMenuItem(enabled = liveStatus == "live" && liveUrl != null, onClick = batchFeatureAction({
                        // Re-check at tap time, including the exact end boundary.
                        if (lecture.liveWindow?.status(Instant.now()) == "live" && liveUrl != null) {
                            val native = Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl)).setPackage("com.google.android.youtube")
                            val opened = runCatching { context.startActivity(native) }.isSuccess ||
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(liveUrl))) }.isSuccess
                            if (opened) sourceChooser = false else sourceError = true
                        }
                    })) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            UiText(copy("openYoutube", strings.text(R.string.toppers_batch_open_in_youtube_app)))
                            if (liveStatus != "live" || liveUrl == null) UiText(youtubeLabel)
                        }
                    }
                } }) {
                UiButton(onClick = batchFeatureAction({ sourceChooser = !sourceChooser; sourceError = false }),
                    style = ButtonStyle.Primary, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { UiText(copy("openLecture", strings.text(R.string.toppers_batch_open_lecture))) }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (state.studyMode == BatchStudyMode.PERSONAL || lecture.completedAt != null) UiButton(onClick = batchFeatureAction(::editPlan), enabled = !busy, style = ButtonStyle.Secondary,
                modifier = if (flat) Modifier.fillMaxWidth() else Modifier) {
                UiText(if (lecture.completedAt != null) {
                    if (lecture.pendingRevision) strings.text(R.string.toppers_batch_revise_count, calendarDateLabel(lecture.revisionDate, strings)) else strings.text(R.string.toppers_batch_plan_revision)
                } else lecture.studyPlannedFor?.let { strings.text(R.string.toppers_batch_watch_count, calendarDateLabel(it, strings)) } ?: strings.text(R.string.toppers_batch_plan_lecture))
            }
            state.overview?.studyWorkflow?.backlogReasons?.get(lecture.id)?.takeUnless { state.studyMode == BatchStudyMode.OFFICIAL && it == "missed" }?.let { reason ->
                UiText(when (reason) { "missed" -> strings.text(R.string.toppers_batch_planned_count, calendarDateLabel(lecture.studyPlannedFor, strings)); "skipped" -> strings.text(R.string.toppers_batch_skipped); else -> strings.text(R.string.toppers_batch_added_by_you) })
            }
            if (sourceError) Text(strings.text(R.string.toppers_batch_could_not_open_this_link), color = MaterialTheme.colorScheme.error)
            if (lecture.pendingRevision) {
                UiButton(onClick = batchFeatureAction({ vm.lectureAction(lecture, "revision/complete") }), enabled = !busy, style = ButtonStyle.Secondary, modifier = if (flat) Modifier.fillMaxWidth() else Modifier) { UiText(strings.text(R.string.toppers_batch_revision_done)) }
            }
        }
    }
    if (edit) TextEditDialog(strings.text(R.string.toppers_batch_change_lecture_name), strings.text(R.string.toppers_batch_lecture_name), lecture.displayTopic, 300,
        onDismiss = { edit = false }, error = state.error?.let(strings::notice)) { title -> vm.editLecture(lecture, title) { edit = false } }
    if (plan) StudyPlanDialog(state.overview?.lectures?.firstOrNull { it.id == lecture.id } ?: lecture, state, vm) { plan = false }
    if (delete) ConfirmDeleteDialog(strings.text(R.string.toppers_batch_delete_this_lecture), strings.text(R.string.toppers_batch_your_work_will_stay_saved_restore_it_in_lectures),
        onDismiss = { delete = false }, onConfirm = { vm.removeLecture(lecture) { delete = false } })
}

@Composable
internal fun SubjectOptions(subject: BatchSubject, state: BatchUiState, vm: ToppersBatchViewModel, tint: Color = MaterialTheme.colorScheme.onSurface) {
    val strings = rememberBatchStrings()
    var menu by remember(subject.id) { mutableStateOf(false) }
    var colour by remember(subject.id) { mutableStateOf(false) }
    var add by remember(subject.id) { mutableStateOf(false) }
    var delete by remember(subject.id) { mutableStateOf(false) }
    val busy = subject.id in state.busyIds
    UiDropdownMenu(expanded = menu, onExpandedChange = { menu = it }, alignment = DropdownMenuAlignment.End,
        panel = { BatchDropdownMenuPanel {
            UiDropdownMenuItem(enabled = !busy, onClick = batchFeatureAction({ menu = false; add = true })) { UiText(strings.text(R.string.toppers_batch_add_a_lecture_in_this_subject)) }
            UiDropdownMenuItem(enabled = !busy, onClick = batchFeatureAction({ menu = false; colour = true })) { UiText(strings.text(R.string.toppers_batch_change_colour)) }
            UiDropdownMenuItem(enabled = !busy, onClick = batchFeatureAction({ menu = false; delete = true }), style = DropdownMenuItemStyle.Destructive) { UiText(strings.text(R.string.toppers_batch_delete_subject)) }
        } }) {
        IconButton(onClick = batchFeatureAction({ menu = !menu }), modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_more_actions_for_count, strings.subject(subject)) }) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = tint)
        }
    }
    if (add) TextEditDialog(strings.text(R.string.toppers_batch_add_lecture_to_count, strings.subject(subject)), strings.text(R.string.toppers_batch_lecture_name), "", 300,
        onDismiss = { add = false }, error = state.error?.let(strings::notice)) { title -> vm.addLecture(subject, title) { add = false } }
    if (delete) ConfirmDeleteDialog(strings.text(R.string.toppers_batch_delete_count, strings.subject(subject)), strings.text(R.string.toppers_batch_your_lectures_and_progress_stay_saved_you_can_restore_this_subjec),
        onDismiss = { delete = false }, onConfirm = { vm.removeSubject(subject) { delete = false } })
    if (colour) ColourDialog(
        initial = subject.color ?: subject.defaultColor,
        title = strings.text(R.string.toppers_batch_subject_color),
        onDismiss = { colour = false },
        error = state.error?.let(strings::notice),
        blocked = state.overview?.subjects.orEmpty().filter { it.id != subject.id }.mapNotNull { it.color ?: it.defaultColor }.toSet(),
        resetColor = subject.defaultColor
    ) {
        vm.subjectColor(subject, it)
        colour = false
    }
}

@Composable
internal fun ConfirmDeleteDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val strings = rememberBatchStrings()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(body) },
        confirmButton= { Button(onClick = batchFeatureAction(onConfirm), destructive = true) { Text(strings.text(R.string.toppers_batch_delete)) } },
        dismissButton= { TextButton(onClick = batchFeatureAction(onDismiss)) { Text(strings.text(R.string.toppers_batch_cancel)) } })
}

@Composable
private fun TextEditDialog(title: String, field: String, initial: String, limit: Int,
                           onDismiss: () -> Unit, error: String?, onSave: (String) -> Unit) {
    val strings = rememberBatchStrings()
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column {
            BatchTextField(value = value, onValueChange = { value = it.take(limit) },
                label = { Text(field) }, accessibilityLabel = field, singleLine = true)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        } },
        confirmButton= { Button(onClick = batchFeatureAction({ onSave(value.trim()) }), enabled = value.isNotBlank()) { Text(strings.text(R.string.toppers_batch_save)) } },
        dismissButton= { TextButton(onClick = batchFeatureAction(onDismiss)) { Text(strings.text(R.string.toppers_batch_cancel)) } })
}

@Composable
internal fun DateButton(value: String, label: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    val strings = rememberBatchStrings()
    val context = LocalContext.current
    OutlinedButton(
        onClick = batchFeatureAction({
            val date = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
            val pickerContext = batchPickerContext(context)
            DatePickerDialog(pickerContext, { _, year, month, day ->
                onChange(LocalDate.of(year, month + 1, day).toString())
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }),
        enabled = enabled,
        modifier = Modifier
            .widthIn(min = 142.dp)
            .semantics { contentDescription = "$label, ${strings.date(value)}" },
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            runCatching {
                LocalDate.parse(value).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", strings.locale))
            }.getOrDefault(strings.text(R.string.toppers_batch_choose_date)),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun RevisionBottomSheet(lecture: BatchLecture, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    StudyPlanDialog(state.overview?.lectures?.firstOrNull { it.id == lecture.id } ?: lecture, state, vm, onDismiss)
}

@Composable
internal fun RevisionDialog(lecture: BatchLecture, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    RevisionBottomSheet(lecture, vm, onDismiss)
}

@Composable
private fun ColourDialog(initial: String?, title: String, onDismiss: () -> Unit, error: String?, blocked: Set<String> = emptySet(), resetColor: String? = null, onSave: (String?) -> Unit) {
    val strings = rememberBatchStrings()
    fun used(value: String?) = value != null && blocked.any { it.equals(value, ignoreCase = true) }
    val palette = listOf(
        strings.text(R.string.toppers_batch_pink) to "#BE185D", strings.text(R.string.toppers_batch_red) to "#B91C1C", strings.text(R.string.toppers_batch_orange) to "#EA580C", strings.text(R.string.toppers_batch_yellow) to "#CA8A04",
        strings.text(R.string.toppers_batch_green) to "#16A34A", strings.text(R.string.toppers_batch_teal) to "#0F766E", strings.text(R.string.toppers_batch_blue) to "#1D4ED8", strings.text(R.string.toppers_batch_purple) to "#7C3AED",
    )
    val choices = palette
    var colour by remember(initial) { mutableStateOf(initial ?: palette.first().second) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(strings.text(R.string.toppers_batch_choose_a_color), color = MaterialTheme.colorScheme.onSurfaceVariant)
            choices.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { (name, code) ->
                        val isSelected = colour.equals(code, ignoreCase = true)
                        val unavailable = used(code)
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Surface(onClick = batchFeatureAction({ colour = code }), enabled = !unavailable, modifier = Modifier.size(48.dp).semantics {
                                contentDescription = strings.text(R.string.toppers_batch_count_color, name)
                                selected = isSelected
                            }, shape = CircleShape, color = Color(android.graphics.Color.parseColor(code)),
                                border = if (isSelected) BorderStroke(3.dp, Color.Black) else null) {
                                if (isSelected) androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White,
                                        modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(if (unavailable) strings.text(R.string.toppers_batch_in_use) else name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (used(colour)) Text(strings.text(R.string.toppers_batch_this_color_is_used_by_another_subject), color = MaterialTheme.colorScheme.error)
            if (initial != null && resetColor != null && !initial.equals(resetColor, ignoreCase = true)) {
                TextButton(onClick = batchFeatureAction({ onSave(null) }), enabled = !used(resetColor)) { Text(strings.text(R.string.toppers_batch_reset_to_default_color)) }
            }
            DialogError(error)
        } },
        confirmButton= { Button(onClick = batchFeatureAction({ onSave(colour) }), enabled = !used(colour)) { Text(strings.text(R.string.toppers_batch_save)) } },
        dismissButton= { TextButton(onClick = batchFeatureAction(onDismiss)) { Text(strings.text(R.string.toppers_batch_cancel)) } })
}

@Composable
private fun LinkDialog(subject: BatchSubject, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val strings = rememberBatchStrings()
    val state by vm.state.collectAsStateWithLifecycle()
    var provider by remember { mutableStateOf(subject.externalProvider ?: "custom") }
    var url by remember { mutableStateOf(subject.externalUrl.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(strings.text(R.string.toppers_batch_course_link)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.text(R.string.toppers_batch_save_the_website_where_you_watch_this_course))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = provider == "parmar_academy", onClick = batchFeatureAction({ provider = "parmar_academy" }), label = { Text(strings.text(R.string.toppers_batch_parmar_academy)) })
                FilterChip(selected = provider == "custom", onClick = batchFeatureAction({ provider = "custom" }), label = { Text(strings.text(R.string.toppers_batch_other_website)) })
            }
            BatchTextField(url, { url = it }, label = { Text(strings.text(R.string.toppers_batch_website_link)) }, accessibilityLabel = strings.text(R.string.toppers_batch_website_link), singleLine = true)
            Text(strings.text(R.string.toppers_batch_use_a_secure_link_starting_with_https), color = MaterialTheme.colorScheme.onSurfaceVariant)
            DialogError(state.error?.let(strings::notice))
        } },
        confirmButton= { Button(onClick = batchFeatureAction({ vm.subjectLink(subject, provider, url, onDismiss) }),
            enabled = url.isBlank() || url.startsWith("https://")) { Text(strings.text(R.string.toppers_batch_save_link)) } },
        dismissButton= { TextButton(onClick = batchFeatureAction(onDismiss)) { Text(strings.text(R.string.toppers_batch_cancel)) } })
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
