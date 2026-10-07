package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

// Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · existing SAFAR theme · released lecture comparison.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.ProgressIndicator
import com.composables.ui.components.Text

@Composable
internal fun BatchReleaseProgressPanel(state: BatchUiState, vm: ToppersBatchViewModel) {
    val strings = rememberBatchStrings()
    val overview = state.overview ?: return
    val progress = remember(overview, state.studyDay) { overview.releaseProgress(state.studyDay) }
    var showRemaining by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.text(R.string.toppers_batch_your_progress_vs_batch), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (progress.releasedCount == 0) {
                Text(strings.text(R.string.toppers_batch_no_lectures_released_yet), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.text(R.string.toppers_batch_your_progress_will_appear_here_when_lectures_are_released), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(strings.quantity(R.plurals.toppers_batch_released_total, progress.releasedCount, progress.releasedCount),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.text(R.string.toppers_batch_count_of_count_completed, progress.completedCount, progress.releasedCount),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ProgressIndicator(progress = progress.fraction, modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = strings.text(R.string.toppers_batch_count_of_count_released_lectures_completed_count_left_to_watch, progress.completedCount, progress.releasedCount, progress.remainingCount)
                }, height = 8.dp, indicatorColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer)
                Text(if (progress.remainingCount == 0) strings.text(R.string.toppers_batch_you_ve_completed_every_released_lecture)
                    else strings.quantity(R.plurals.toppers_batch_released_remaining, progress.remainingCount, progress.remainingCount),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (progress.remainingCount > 0) BatchOutlinedButton(onClick = batchFeatureAction({ showRemaining = true }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = strings.text(R.string.toppers_batch_view_remaining_released_lectures) }) {
                    Text(strings.text(R.string.toppers_batch_view_remaining))
                }
                HorizontalSeparator(color = MaterialTheme.colorScheme.outlineVariant)
                Text(strings.text(R.string.toppers_batch_released_lectures_by_subject), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                progress.subjects.forEach { group ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.subject(group.subject), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(if (group.releasedCount == 0) strings.text(R.string.toppers_batch_no_lectures_released_yet_2)
                            else strings.text(R.string.toppers_batch_count_of_count_completed_count_left, group.completedCount, group.releasedCount, group.remaining.size),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showRemaining) BatchAlertDialog(onDismissRequest = { showRemaining = false },
        title = { Text(strings.text(R.string.toppers_batch_released_lectures_to_watch)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.text(R.string.toppers_batch_tap_a_lecture_to_open_its_details), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (progress.releasedCount == 0) Text(strings.text(R.string.toppers_batch_no_lectures_released_yet))
            else if (progress.remainingCount == 0) Text(strings.text(R.string.toppers_batch_you_ve_completed_every_released_lecture))
            else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                progress.subjects.forEach { group ->
                    val pending = group.remaining
                    if (pending.isNotEmpty()) {
                        item(key = "subject:${group.subject.id}") {
                            Text(strings.text(R.string.toppers_batch_count_count_to_watch, strings.subject(group.subject), pending.size), style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                        }
                        items(pending, key = { "lecture:${it.id}" }) { lecture ->
                            LecturePlanSummary(lecture, group.subject, meta = "", showSubject = false) {
                                showRemaining = false
                                detailId = lecture.id
                            }
                        }
                    }
                }
            }
        } }, confirmButton = { BatchTextButton(onClick = batchFeatureAction({ showRemaining = false })) { Text(strings.text(R.string.toppers_batch_close)) } }, dismissButton = {})

    overview.lectures.firstOrNull { it.id == detailId }?.let { lecture ->
        LectureDetailsDialog(lecture, overview.subjects.firstOrNull { it.id == lecture.subjectId }, state, vm) { detailId = null }
    }
}
