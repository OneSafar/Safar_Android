package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.studyplanner.StudySubject

private const val STYLE_BALANCED = "balanced"
private const val STYLE_MIXED_BAG = "mixed_bag"
private const val STYLE_DEEP_FOCUS = "deep_focus"
private const val MAX_PRIORITY_SUBJECTS = 3

/**
 * Shown right after the user changes the exam date. Lets them re-pick how the
 * syllabus should be ordered (the same three styles offered at plan creation)
 * and then either rebuild immediately or reorder the syllabus first. The chosen
 * style maps to a scheduling ORDER only:
 *   Deep Focus  -> sequential
 *   Balanced    -> interleaved
 *   Mixed Bag   -> priority_split (chosen subjects first), or interleaved when none picked
 * All of them respect the plan's daily goal.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RescheduleFlowSheet(
    subjects: List<StudySubject>,
    onRebuildNow: (strategy: String, prioritySubjectNames: List<String>, priorityOrderMode: String?) -> Unit,
    onReorderFirst: (strategy: String, prioritySubjectNames: List<String>, priorityOrderMode: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var style by remember { mutableStateOf(STYLE_BALANCED) }
    // Ordered, like the create-flow picker: pick order drives the schedule when
    // the "In my order" mode is chosen.
    var priority by remember { mutableStateOf(listOf<String>()) }
    var priorityOrderMode by remember { mutableStateOf("sequential") }

    data class RebuildChoice(
        val strategy: String,
        val priority: List<String>,
        val orderMode: String?,
    )

    // Style picks an order and nothing else; whether a day may exceed the daily
    // goal is no longer bundled into that choice.
    fun resolve(): RebuildChoice = when (style) {
        STYLE_DEEP_FOCUS -> RebuildChoice("sequential", emptyList(), null)
        STYLE_MIXED_BAG ->
            if (priority.isNotEmpty()) RebuildChoice("priority_split", priority, priorityOrderMode)
            else RebuildChoice("interleaved", emptyList(), null)
        else -> RebuildChoice("interleaved", emptyList(), null)
    }

    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.planner_exam_date_updated),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.planner_replan_window_body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            StudyStyleOption(
                title = stringResource(R.string.planner_balanced),
                body = stringResource(R.string.planner_balanced_body),
                selected = style == STYLE_BALANCED,
                onClick = { style = STYLE_BALANCED },
            )
            StudyStyleOption(
                title = stringResource(R.string.planner_mixed_bag),
                body = stringResource(R.string.planner_mixed_bag_body),
                selected = style == STYLE_MIXED_BAG,
                onClick = { style = STYLE_MIXED_BAG },
            )
            StudyStyleOption(
                title = stringResource(R.string.planner_deep_focus),
                body = stringResource(R.string.planner_deep_focus_body),
                selected = style == STYLE_DEEP_FOCUS,
                onClick = { style = STYLE_DEEP_FOCUS },
            )

            if (style == STYLE_MIXED_BAG && subjects.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.planner_priority_subjects_limit, MAX_PRIORITY_SUBJECTS),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { subject ->
                        val name = subject.name
                        val pickIndex = priority.indexOf(name)
                        val checked = pickIndex >= 0
                        FilterChip(
                            selected = checked,
                            onClick = {
                                priority = when {
                                    checked -> priority - name
                                    priority.size < MAX_PRIORITY_SUBJECTS -> priority + name
                                    else -> priority
                                }
                            },
                            label = {
                                Text(
                                    text = if (checked) "${pickIndex + 1}. $name" else name,
                                    maxLines = 1,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }

                if (priority.size >= 2) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = priorityOrderMode == "sequential",
                            onClick = { priorityOrderMode = "sequential" },
                            label = { Text(stringResource(R.string.planner_in_my_order), maxLines = 1) },
                        )
                        FilterChip(
                            selected = priorityOrderMode == "balanced",
                            onClick = { priorityOrderMode = "balanced" },
                            label = { Text(stringResource(R.string.planner_mix_together), maxLines = 1) },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val choice = resolve()
                    onRebuildNow(choice.strategy, choice.priority, choice.orderMode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.planner_rebuild_schedule_now), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    val choice = resolve()
                    onReorderFirst(choice.strategy, choice.priority, choice.orderMode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.planner_reorder_syllabus_first), fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = stringResource(R.string.planner_reorder_first_body),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
