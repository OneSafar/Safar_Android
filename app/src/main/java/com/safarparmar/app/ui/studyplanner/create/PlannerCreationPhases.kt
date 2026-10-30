package com.safarparmar.app.ui.studyplanner.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R

/** Visual grouping only: the existing wizard still owns every conditional step. */
internal fun creationPhase(step: CreatePlanStep): Int = when (step) {
    CreatePlanStep.ChoosePath, CreatePlanStep.TemplatePicker, CreatePlanStep.ManualTopicTree,
    CreatePlanStep.PasteSyllabus, CreatePlanStep.SavedSyllabusPicker -> 0
    CreatePlanStep.PlanSettings, CreatePlanStep.ChapterRating, CreatePlanStep.DeepFocusOrder,
    CreatePlanStep.MixedBagSubjectPicker -> 1
    CreatePlanStep.BuildingPreview, CreatePlanStep.Preview, CreatePlanStep.DailyTopics -> 2
}

@Composable
internal fun PlannerCreationPhases(step: CreatePlanStep) {
    val current = creationPhase(step)
    val ids = listOf(R.string.planner_warm_syllabus, R.string.planner_warm_routine, R.string.planner_warm_review)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ids.forEachIndexed { index, id ->
            val label = stringResource(id)
            Column(Modifier.weight(1f).semantics { contentDescription = "${index + 1}/3 $label" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = if (current == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (current == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelLarge) }
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (current == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
