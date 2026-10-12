package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Checkbox
import com.composables.ui.components.Text
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Saved reminder data may be malformed or include seconds on older servers. */
internal fun normalizedStudyReminderTime(value: String?): String = runCatching {
    LocalTime.parse(value?.trim()).format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))
}.getOrDefault("19:00")

internal fun validRescheduleActivities(items: List<BatchStudyActivity>): List<BatchStudyActivity> =
    items.mapNotNull { item -> calendarDate(item.date)?.let { item.copy(date = it) } }

@Composable
internal fun StudyPlanDialog(lecture: BatchLecture, state: BatchUiState, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val strings = rememberBatchStrings()
    val revision = lecture.completedAt != null
    val today = state.studyDay
    val pending = lecture.sessions.filter { it.completedAt == null }
    var date by remember(lecture.id) { mutableStateOf((if (revision) pending.firstOrNull()?.date ?: lecture.revisionDate else lecture.studyPlannedFor)?.coerceAtLeast(today) ?: today) }
    var repeat by remember(lecture.id) { mutableStateOf(revision && pending.size > 1) }
    var dates by remember(lecture.id) { mutableStateOf(if (pending.size > 1) pending.map { it.date } else listOf(1L, 3L, 7L, 14L, 30L).map { LocalDate.parse(today).plusDays(it).toString() }) }
    var bookmark by remember(lecture.id) { mutableStateOf(revision && lecture.revisionTagged && pending.isEmpty() && (lecture.revisionDate == null || lecture.revisionCompletedAt != null)) }
    val context = LocalContext.current
    fun permitted() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var reminder by remember(lecture.id) { mutableStateOf(permitted() && (if (revision) lecture.revisionReminderTime else lecture.studyReminderTime) != null) }
    var showReminder by remember(lecture.id) { mutableStateOf(reminder) }
    var time by remember(lecture.id) { mutableStateOf(normalizedStudyReminderTime(if (revision) lecture.revisionReminderTime else lecture.studyReminderTime)) }
    var notice by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminder = granted
        notice = if (granted) strings.text(R.string.toppers_batch_uses_your_study_notification_settings_delivery_needs_a_connection) else strings.text(R.string.toppers_batch_notifications_are_disabled_your_study_date_can_still_be_saved)
    }
    val busy = lecture.id in state.busyIds
    val existing = if (revision) lecture.revisionDate != null || lecture.revisionTagged else lecture.studyPlannedFor != null
    val valid = bookmark || (if (repeat) dates else listOf(date)).let { values -> values.isNotEmpty() && values.distinct().size == values.size && values.all { calendarDate(it) != null && it >= today } }
    fun save(remove: Boolean = false) {
        val body = mutableMapOf<String, Any>("kind" to if (revision) "revision" else "watch")
        if (remove) body["remove"] = true
        else if (bookmark) body["bookmark"] = true
        else {
            body["dates"] = if (repeat) dates else listOf(date)
            if (reminder) body["reminderTime"] = time
        }
        vm.savePlan(lecture, body, onDismiss)
    }
    BatchAlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (revision) strings.text(R.string.toppers_batch_plan_revision) else strings.text(R.string.toppers_batch_watch_date)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lecture.displayTopic)
            if (revision) Text(strings.text(R.string.toppers_batch_revise_your_notes_or_practise_questions))
            if (!bookmark) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = batchFeatureAction({ repeat = false; date = today }), style = if (!repeat && date == today) ButtonStyle.Primary else ButtonStyle.Outlined) { Text(strings.text(R.string.toppers_batch_today)) }
                    val tomorrow = LocalDate.parse(today).plusDays(1).toString()
                    Button(onClick = batchFeatureAction({ repeat = false; date = tomorrow }), style = if (!repeat && date == tomorrow) ButtonStyle.Primary else ButtonStyle.Outlined) { Text(strings.text(R.string.toppers_batch_tomorrow)) }
                }
                if (!repeat) DateButton(date, strings.text(R.string.toppers_batch_choose_date)) { date = it }
                if (revision) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = repeat, onCheckedChange = batchFeatureChange({ repeat = it }), accessibilityLabel = strings.text(R.string.toppers_batch_repeat_revision))
                    Text(strings.text(R.string.toppers_batch_repeat_revision))
                }
                if (repeat) {
                    Text(strings.text(R.string.toppers_batch_suggested_dates_change_or_remove_any_date))
                    dates.forEachIndexed { index, value -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateButton(value, strings.text(R.string.toppers_batch_revision_count, index + 1)) { updated -> dates = dates.mapIndexed { i, old -> if (i == index) updated else old } }
                        Button(enabled = dates.size > 1, onClick = batchFeatureAction({ dates = dates.filterIndexed { i, _ -> i != index } }), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_remove)) }
                    } }
                }
                if (!showReminder) Button(enabled = !busy, onClick = batchFeatureAction({ showReminder = true }), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_add_reminder_optional)) }
                if (showReminder) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = reminder, onCheckedChange = batchFeatureChange({ enabled ->
                        if (!enabled) reminder = false
                        else if (permitted()) { reminder = true; notice = strings.text(R.string.toppers_batch_uses_your_study_notification_settings_delivery_needs_a_connection) }
                        else permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }), accessibilityLabel = strings.text(R.string.toppers_batch_remind_me))
                    Text(strings.text(R.string.toppers_batch_remind_me))
                }
                if (reminder) Button(onClick = batchFeatureAction({
                    TimePickerDialog(context, { _, hour, minute -> time = "%02d:%02d".format(Locale.ROOT, hour, minute) }, time.take(2).toInt(), time.takeLast(2).toInt(), true).show()
                }), style = ButtonStyle.Outlined) { Text(strings.text(R.string.toppers_batch_count_ist, strings.time(time))) }
            }
            if (revision) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = bookmark, onCheckedChange = batchFeatureChange({ bookmark = it }), accessibilityLabel = strings.text(R.string.toppers_batch_save_for_revision_without_a_date))
                Text(strings.text(R.string.toppers_batch_save_for_revision_without_a_date))
            }
            if (!valid) Text(strings.text(R.string.toppers_batch_choose_today_or_a_future_date), color = MaterialTheme.colorScheme.error)
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            state.error?.let { Text(strings.notice(it), color = MaterialTheme.colorScheme.error) }
            if (existing) Button(enabled = !busy, onClick = batchFeatureAction({ save(true) }), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_remove_plan)) }
        } }, confirmButton = { Button(enabled = !busy && valid, onClick = batchFeatureAction({ save() })) { Text(if (busy) strings.text(R.string.toppers_batch_saving) else strings.text(R.string.toppers_batch_save)) } },
        dismissButton = { Button(enabled = !busy, onClick = batchFeatureAction(onDismiss), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_cancel)) } })
}

@Composable
internal fun PriorProgressDialog(state: BatchUiState, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val active = overview.subjects.filter { it.enabled }
    var subject by remember { mutableStateOf(active.firstOrNull()?.id) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var preview by remember { mutableStateOf(false) }
    var knownDate by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(ToppersBatchViewModel.indiaDay()) }
    val busy = "prior-progress" in state.busyIds
    val rows = overview.lectures.filter { it.subjectId == subject && it.completedAt == null && !it.isLocked(ToppersBatchViewModel.indiaDay()) }
    var showInfo by remember { mutableStateOf(false) }
    BatchAlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.text(R.string.toppers_batch_previously_watched), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Button(modifier = Modifier.semantics { contentDescription = strings.text(R.string.toppers_batch_about_previously_watched_lectures) }, onClick = batchFeatureAction({ showInfo = true }), style = ButtonStyle.Ghost) { Text("ⓘ") }
        }
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(strings.text(R.string.toppers_batch_select_completed_lectures), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                active.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { row ->
                            Button(modifier = Modifier.weight(1f), enabled = !busy, onClick = batchFeatureAction({ subject = row.id }),
                                style = if (subject == row.id) ButtonStyle.Primary else ButtonStyle.Outlined) { Text(strings.subject(row)) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { index, row ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Checkbox(checked = row.id in selected, onCheckedChange = batchFeatureChange({ if (!busy) selected = if (it) selected + row.id else selected - row.id }), accessibilityLabel = strings.text(R.string.toppers_batch_select_lecture_count, row.lectureNumber))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(strings.text(R.string.toppers_batch_lecture_count, row.lectureNumber), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(row.displayTopic, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(enabled = !busy, onClick = batchFeatureAction({ selected = selected + rows.take(index + 1).map { it.id } }), style = ButtonStyle.Ghost) {
                                Text(strings.text(R.string.toppers_batch_mark_all_done_till_count, row.lectureNumber), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (rows.isEmpty()) Text(strings.text(R.string.toppers_batch_no_lectures_to_select), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.text(R.string.toppers_batch_count_selected, selected.size), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                if (selected.isNotEmpty()) {
                    Button(onClick = batchFeatureAction({ preview = !preview }), style = ButtonStyle.Ghost) { Text(if (preview) strings.text(R.string.toppers_batch_hide) else strings.text(R.string.toppers_batch_review)) }
                    Button(enabled = !busy, onClick = batchFeatureAction({ selected = emptySet() }), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_clear)) }
                }
            }
            if (preview && selected.isNotEmpty()) Column(Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                overview.lectures.filter { it.id in selected }.forEach { row ->
                    Text(strings.text(R.string.toppers_batch_count_lecture_count_n_count, overview.subjects.firstOrNull { it.id == row.subjectId }?.let(strings::subject).orEmpty(), row.lectureNumber, row.displayTopic), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Checkbox(checked = knownDate, onCheckedChange = batchFeatureChange({ if (!busy) knownDate = it }), accessibilityLabel = strings.text(R.string.toppers_batch_add_completion_date))
                Text(strings.text(R.string.toppers_batch_add_completion_date), style = MaterialTheme.typography.bodyMedium)
            }
            if (knownDate) DateButton(date, strings.text(R.string.toppers_batch_completion_date)) { if (!busy) date = it }
            else Text(strings.text(R.string.toppers_batch_date_unknown), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (knownDate && date > ToppersBatchViewModel.indiaDay()) Text(strings.text(R.string.toppers_batch_choose_today_or_an_earlier_date), color = MaterialTheme.colorScheme.error)
            state.error?.let { Text(strings.notice(it), color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(enabled = !busy && selected.isNotEmpty() && (!knownDate || date <= ToppersBatchViewModel.indiaDay()), onClick = batchFeatureAction({ vm.importPriorProgress(selected.toList(), if (knownDate) date else null, onDismiss) })) { Text(if (busy) strings.text(R.string.toppers_batch_saving) else strings.text(R.string.toppers_batch_save_count, selected.size)) } },
        dismissButton = { Button(enabled = !busy, onClick = batchFeatureAction(onDismiss), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_cancel)) } })
    if (showInfo) BatchAlertDialog(onDismissRequest = { showInfo = false }, title = { Text(strings.text(R.string.toppers_batch_previously_watched)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.text(R.string.toppers_batch_selected_lectures_count_toward_your_progress))
            Text(strings.text(R.string.toppers_batch_tick_a_checkbox_to_select_one_lecture_mark_all_done_till_a_lectur))
            Text(strings.text(R.string.toppers_batch_add_a_date_to_show_completions_on_your_calendar_without_a_date_th))
        }
    }, confirmButton = { Button(onClick = batchFeatureAction({ showInfo = false })) { Text(strings.text(R.string.toppers_batch_got_it)) } }, dismissButton = {})
}

@Composable
internal fun ReschedulePlansDialog(state: BatchUiState, vm: ToppersBatchViewModel, onDismiss: () -> Unit) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val items = validRescheduleActivities(overview.studyWorkflow.missed)
    fun key(item: BatchStudyActivity) = "${item.lectureId}:${item.kind}:${item.date}"
    var selected by remember { mutableStateOf(setOf<String>()) }
    var date by remember { mutableStateOf(LocalDate.parse(ToppersBatchViewModel.indiaDay()).plusDays(1).toString()) }
    val chosen = items.filter { key(it) in selected }
    val earliest = chosen.minOfOrNull { it.date }
    val offset = earliest?.let { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it), LocalDate.parse(date)) } ?: 0
    val busy = "reschedule" in state.busyIds
    BatchAlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(strings.text(R.string.toppers_batch_reschedule_missed_plans)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.text(R.string.toppers_batch_choose_plans_and_a_new_first_date_the_gaps_between_selected_dates))
            Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    val lecture = overview.lectures.firstOrNull { it.id == item.lectureId } ?: return@forEach
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = key(item) in selected, onCheckedChange = batchFeatureChange({ selected = if (it) selected + key(item) else selected - key(item) }), accessibilityLabel = strings.text(R.string.toppers_batch_select_count, lecture.displayTopic))
                        Text("${if (item.kind == "watch") strings.text(R.string.toppers_batch_watch) else strings.text(R.string.toppers_batch_revise)} · ${lecture.displayTopic}\n${calendarDateLabel(item.date, strings)}", modifier = Modifier.weight(1f))
                    }
                }
            }
            DateButton(date, strings.text(R.string.toppers_batch_new_first_date)) { date = it }
            if (chosen.isNotEmpty()) {
                Text(strings.text(R.string.toppers_batch_preview), style = MaterialTheme.typography.titleSmall)
                Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chosen.forEach { item ->
                        val lecture = overview.lectures.firstOrNull { it.id == item.lectureId } ?: return@forEach
                        Text("${lecture.displayTopic}\n${calendarDateLabel(item.date, strings)} → ${calendarDateLabel(LocalDate.parse(item.date).plusDays(offset).toString(), strings)}")
                        if (item.waitingForRelease) Text(strings.text(R.string.toppers_batch_waiting_for_release_this_plan_may_be_before_the_lecture_is_availa), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.error?.let { Text(strings.notice(it), color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        Button(enabled = !busy && chosen.isNotEmpty() && date >= ToppersBatchViewModel.indiaDay(), onClick = batchFeatureAction({
            vm.reschedulePlans(date, chosen.map { item -> mapOf("lectureId" to item.lectureId, "kind" to item.kind, "date" to item.date, "version" to (overview.lectures.firstOrNull { it.id == item.lectureId }?.planVersion ?: 0)) }, onDismiss)
        })) { Text(strings.quantity(R.plurals.toppers_batch_save_count_plans, chosen.size, chosen.size)) }
    }, dismissButton = { Button(enabled = !busy, onClick = batchFeatureAction(onDismiss), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_cancel)) } })
}
