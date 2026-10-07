package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

// Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · SAFAR theme · daily checklist.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.composables.ui.components.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import androidx.compose.material3.Checkbox
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.ProgressIndicator
import com.composables.ui.components.Text

@Composable
internal fun PersonalPlanPanel(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val today = state.studyDay
    val plan = remember(overview, today) { overview.todayPlan(today) }
    val missed = remember(overview, today) { overview.projectStudyWorkflow(today).second.missed }
    val upcoming = remember(overview, today) { overview.upcomingStudyActivities(today) }
    val suggestions = remember(overview, today) { overview.nextPlanningLectures(today) }
    val subjects = overview.planningSubjects()
    var detailId by remember { mutableStateOf<String?>(null) }
    var planId by remember { mutableStateOf<String?>(null) }
    var reschedule by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.text(R.string.toppers_batch_your_plan_today), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(calendarDateLabel(today, strings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (plan.total == 0) {
                    Text(strings.text(R.string.toppers_batch_nothing_planned_for_today), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(strings.quantity(R.plurals.toppers_batch_count_of_count_tasks_done_count_left, plan.total, plan.completed, plan.total, plan.remaining), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    ProgressIndicator(progress = plan.completed.toFloat() / plan.total, modifier = Modifier.fillMaxWidth(), height = 6.dp,
                        indicatorColor = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
                    plan.subjectGroups(subjects, completed = false).forEach { group ->
                        group.tasks.forEach { task ->
                            PersonalTaskRow(task, group.subject, state, vm, { detailId = it }, { planId = it })
                        }
                    }
                    val completedGroups = plan.subjectGroups(subjects, completed = true)
                    if (completedGroups.isNotEmpty()) PlanDisclosure(strings.text(R.string.toppers_batch_completed_in_your_plan), "${plan.completed}") {
                        completedGroups.forEach { group ->
                            group.tasks.forEach { task ->
                                PersonalTaskRow(task, group.subject, state, vm, { detailId = it }, { planId = it })
                            }
                        }
                    }
                }
            }
        }
        key(plan.total == 0) {
            PlanDisclosure(strings.text(R.string.toppers_batch_add_lectures_to_today), "", initiallyExpanded = plan.total == 0) {
                subjects.forEach { subject ->
                    val lecture = suggestions[subject.id]
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (lecture != null) {
                                LecturePlanSummary(lecture, subject, "") { detailId = lecture.id }
                            } else {
                                Text(strings.subject(subject), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                val own = overview.lectures.filter { it.subjectId == subject.id && it.completedAt == null }
                                Text(when {
                                    overview.lectures.none { it.subjectId == subject.id } -> strings.text(R.string.toppers_batch_lectures_will_appear_when_added)
                                    own.isEmpty() -> strings.text(R.string.toppers_batch_all_lectures_completed)
                                    own.none { !it.isLocked(today) } -> strings.text(R.string.toppers_batch_next_class_isn_t_released_yet)
                                    else -> strings.text(R.string.toppers_batch_available_lectures_already_have_study_dates)
                                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (lecture != null) {
                            val busy = lecture.id in state.busyIds
                            Button(onClick = batchFeatureAction({ vm.addToToday(lecture) }), enabled = !busy,
                                modifier = Modifier.heightIn(min = 48.dp).semantics {
                                    contentDescription = strings.text(R.string.toppers_batch_add_count_lecture_count_to_today, strings.subject(subject), lecture.lectureNumber)
                                }, style = ButtonStyle.Primary) {
                                Text(if (busy) strings.text(R.string.toppers_batch_adding) else strings.text(R.string.toppers_batch_add), maxLines = 1, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                    HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        if (missed.isNotEmpty()) PlanDisclosure(strings.text(R.string.toppers_batch_missed_plans), "${missed.size}") {
            Button(onClick = batchFeatureAction({ reschedule = true }), style = ButtonStyle.Outlined) {
                Text(strings.text(R.string.toppers_batch_reschedule_plans), maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
            }
            missed.forEach { activity ->
                val lecture = overview.lectures.firstOrNull { it.id == activity.lectureId } ?: return@forEach
                LecturePlanSummary(lecture, subjects.firstOrNull { it.id == lecture.subjectId },
                    "${if (activity.kind == "watch") strings.text(R.string.toppers_batch_watch) else strings.text(R.string.toppers_batch_revise)} · ${calendarDateLabel(activity.date, strings)}") { planId = lecture.id }
            }
        }
        if (upcoming.isNotEmpty()) PlanDisclosure(strings.text(R.string.toppers_batch_next_in_your_plan), "${upcoming.size}") {
            upcoming.forEach { activity ->
                val lecture = overview.lectures.firstOrNull { it.id == activity.lectureId } ?: return@forEach
                val meta = listOfNotNull(if (activity.kind == "revision") strings.text(R.string.toppers_batch_revise) else null,
                    calendarDateLabel(activity.date, strings)).joinToString(" · ")
                LecturePlanSummary(lecture, subjects.firstOrNull { it.id == lecture.subjectId }, meta) { detailId = lecture.id }
            }
        }
    }
    if (reschedule) ReschedulePlansDialog(state, vm) { reschedule = false }
    overview.lectures.firstOrNull { it.id == detailId }?.let { LectureDetailsDialog(it, subjects.firstOrNull { subject -> subject.id == it.subjectId }, state, vm) { detailId = null } }
    overview.lectures.firstOrNull { it.id == planId }?.let { StudyPlanDialog(it, state, vm) { planId = null } }
}

@Composable
private fun PersonalTaskRow(task: TodayPlanTask, subject: BatchSubject, state: BatchUiState,
    vm: ToppersBatchViewModel, onDetails: (String) -> Unit, onDate: (String) -> Unit) {
    val strings = rememberBatchStrings()
    var showActions by remember { mutableStateOf(false) }
    var confirmRevisionRemoval by remember { mutableStateOf(false) }
    val lecture = task.lecture
    val busy = lecture.id in state.busyIds
    val canTick = !busy && !lecture.isLocked(state.studyDay) &&
        (task.kind == "watch" || (!task.completed && task.earlierRevisionDate == null))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = task.completed, enabled = canTick, onCheckedChange = batchFeatureChange({
            if (task.kind == "watch") vm.toggleDone(lecture)
            else if (it) vm.lectureAction(lecture, "revision/complete")
        }), modifier = Modifier.size(48.dp).semantics {
            contentDescription = strings.text(when {
                task.kind == "revision" -> R.string.toppers_batch_complete_subject_revision
                task.completed -> R.string.toppers_batch_undo_subject_lecture
                else -> R.string.toppers_batch_complete_subject_lecture
            }, strings.subject(subject), lecture.lectureNumber)
        })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LecturePlanSummary(lecture, subject, if (task.kind == "watch") "" else strings.text(R.string.toppers_batch_revise),
                completed = task.completed, showSubject = true) { onDetails(lecture.id) }
            task.reminderTime?.let { Text(strings.text(R.string.toppers_batch_reminder_count_ist, strings.time(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (!task.completed && lecture.isLocked(state.studyDay)) Text(strings.text(R.string.toppers_batch_waiting_for_release), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            task.earlierRevisionDate?.let { Text(strings.text(R.string.toppers_batch_complete_the_revision_planned_for_count_first, calendarDateLabel(it, strings)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (!task.completed) Box {
            IconButton(onClick = batchFeatureAction({ showActions = true }), enabled = !busy, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = strings.text(R.string.toppers_batch_more_actions_for_count, lecture.displayTopic), modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                DropdownMenuItem(text = { Text(strings.text(R.string.toppers_batch_choose_date), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = batchFeatureAction({ showActions = false; onDate(lecture.id) }))
                DropdownMenuItem(text = { Text(strings.text(if (task.kind == "revision") R.string.toppers_batch_remove_revision_plan else R.string.toppers_batch_remove_from_today), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = batchFeatureAction({ showActions = false; if (task.kind == "revision") confirmRevisionRemoval = true else vm.removeTodayTask(task) }),
                    modifier = Modifier.semantics {
                        contentDescription = strings.text(if (task.kind == "revision") R.string.toppers_batch_remove_subject_revision else R.string.toppers_batch_remove_subject_today, strings.subject(subject), lecture.lectureNumber)
                    })
            }
        }
    }
    HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
    if (confirmRevisionRemoval) BatchAlertDialog(
        onDismissRequest = { if (!busy) confirmRevisionRemoval = false },
        title = { Text(strings.text(R.string.toppers_batch_remove_revision_plan_2), color = MaterialTheme.colorScheme.onSurface) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.text(R.string.toppers_batch_this_removes_all_pending_revision_dates_for_lecture_count_includi, lecture.lectureNumber), color = MaterialTheme.colorScheme.onSurface)
            state.error?.let { Text(strings.notice(it), color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { Button(enabled = !busy, onClick = batchFeatureAction({ vm.removeTodayTask(task) { confirmRevisionRemoval = false } })) {
            Text(if (busy) strings.text(R.string.toppers_batch_removing) else strings.text(R.string.toppers_batch_remove_plan), color = MaterialTheme.colorScheme.onPrimary)
        } },
        dismissButton = { Button(enabled = !busy, onClick = batchFeatureAction({ confirmRevisionRemoval = false }), style = ButtonStyle.Ghost) { Text(strings.text(R.string.toppers_batch_cancel), color = MaterialTheme.colorScheme.onSurface) } })
}
