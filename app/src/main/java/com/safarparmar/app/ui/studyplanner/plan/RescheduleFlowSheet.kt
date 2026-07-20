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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.StudySubject

private const val STYLE_BALANCED = "balanced"
private const val STYLE_MIXED_BAG = "mixed_bag"
private const val STYLE_DEEP_FOCUS = "deep_focus"
private const val MAX_PRIORITY_SUBJECTS = 3

/**
 * Shown right after the user changes the exam date. Lets them re-pick how the
 * syllabus should be ordered (the same three styles offered at plan creation)
 * and then either rebuild immediately or reorder the syllabus first. The chosen
 * style maps to the server scheduling knobs:
 *   Deep Focus  -> sequential   (flex)
 *   Balanced    -> interleaved  (strict)
 *   Mixed Bag   -> interleaved  (flex), or priority_split when priority subjects picked
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RescheduleFlowSheet(
    subjects: List<StudySubject>,
    onRebuildNow: (strategy: String, overloadMode: String?, prioritySubjectNames: List<String>) -> Unit,
    onReorderFirst: (strategy: String, overloadMode: String?, prioritySubjectNames: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var style by remember { mutableStateOf(STYLE_BALANCED) }
    var priority by remember { mutableStateOf(setOf<String>()) }

    fun resolve(): Triple<String, String, List<String>> = when (style) {
        STYLE_DEEP_FOCUS -> Triple("sequential", "flex", emptyList())
        STYLE_MIXED_BAG ->
            if (priority.isNotEmpty()) Triple("priority_split", "flex", priority.toList())
            else Triple("interleaved", "flex", emptyList())
        else -> Triple("interleaved", "strict", emptyList())
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
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Exam date updated — re-plan your syllabus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onSurface,
            )
            Text(
                text = "Choose how SAFAR should place your topics into the new window. " +
                    "Anything you've already completed and any dates you set by hand stay exactly where they are.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            StudyStyleOption(
                title = "Balanced",
                body = "Mix subjects evenly across each study day.",
                selected = style == STYLE_BALANCED,
                onClick = { style = STYLE_BALANCED },
            )
            StudyStyleOption(
                title = "Mixed Bag",
                body = "Rotate subjects, and optionally give your toughest ones a topic every day.",
                selected = style == STYLE_MIXED_BAG,
                onClick = { style = STYLE_MIXED_BAG },
            )
            StudyStyleOption(
                title = "Deep Focus",
                body = "Finish topics in the exact order of your syllabus.",
                selected = style == STYLE_DEEP_FOCUS,
                onClick = { style = STYLE_DEEP_FOCUS },
            )

            if (style == STYLE_MIXED_BAG && subjects.isNotEmpty()) {
                Text(
                    text = "Priority subjects (optional, up to $MAX_PRIORITY_SUBJECTS)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { subject ->
                        val name = subject.name
                        val checked = name in priority
                        FilterChip(
                            selected = checked,
                            onClick = {
                                priority = when {
                                    checked -> priority - name
                                    priority.size < MAX_PRIORITY_SUBJECTS -> priority + name
                                    else -> priority
                                }
                            },
                            label = { Text(name, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val (strategy, mode, prio) = resolve()
                    onRebuildNow(strategy, mode, prio)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Rebuild schedule now", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    val (strategy, mode, prio) = resolve()
                    onReorderFirst(strategy, mode, prio)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Reorder syllabus first", fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = "Reorder first opens your Syllabus so you can drag subjects, chapters and topics — then tap \"Build re-ordered syllabus\" to apply this plan.",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
