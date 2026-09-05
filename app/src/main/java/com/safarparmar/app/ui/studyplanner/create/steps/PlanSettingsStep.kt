package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.logic.jsDayOfWeek
import com.safarparmar.app.ui.studyplanner.plan.PlanRestDaysRow
import java.time.LocalDate
import kotlin.math.ceil

/** Hallmark: task-first setup; existing Material/Planner theme, no new scheduling rules. */
@Composable
fun PlanSettingsStep(
    examDate: String,
    onExamDateChange: (String) -> Unit,
    offDays: Set<Int>,
    onToggleOffDay: (Int) -> Unit,
    studyStyle: String,
    onStudyStyleChange: (String) -> Unit,
    dailyGoal: String,
    onDailyGoalChange: (String) -> Unit,
    topicCount: Int,
    subjectCount: Int,
    error: String?,
    premiumRequired: Boolean,
    onBuildPlan: () -> Unit,
    onOpenDeepFocusOrder: () -> Unit,
    onOpenMixedBagPicker: () -> Unit,
    onCustomizeDifficulty: () -> Unit = {},
    subjectNames: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var customize by rememberSaveable { mutableStateOf(false) }
    val exam = runCatching { LocalDate.parse(examDate.take(10)) }.getOrNull()
    val today = LocalDate.now()
    val studyDays = remember(exam, offDays, today) {
        if (exam == null || exam.isBefore(today)) 0 else {
            var cursor = today
            var count = 0
            while (!cursor.isAfter(exam)) {
                if (jsDayOfWeek(cursor) !in offDays) count++
                cursor = cursor.plusDays(1)
            }
            count
        }
    }
    val suggested = if (studyDays > 0) ceil(topicCount.toDouble() / studyDays).toInt().coerceAtLeast(1) else null
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Date and workload", style = MaterialTheme.typography.headlineSmall)
        Text("$topicCount topics · $subjectCount subjects", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PlannerExamDateField(examDateIso = examDate, onExamDateChange = onExamDateChange)
        PlanRestDaysRow(selected = offDays, onToggle = onToggleOffDay)
        OutlinedTextField(
            value = dailyGoal, onValueChange = onDailyGoalChange,
            label = { Text("Daily target") },
            supportingText = { Text("Topics per day · check the schedule in preview") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
        )
        if (suggested != null) {
            Text("$topicCount topics / $studyDays study days ≈ $suggested per day", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onDailyGoalChange(suggested.toString()) }) { Text("Use suggested target") }
        }
        TextButton(onClick = { customize = !customize }) {
            Text(if (customize) "Close customization" else "Customize plan")
        }
        if (customize) {
            val modes = listOf("balanced" to "Mix subjects", "deep_focus" to "One subject at a time", "mixed_bag" to "Mix + priorities")
            modes.forEach { (id, label) ->
                if (id != "mixed_bag" || subjectCount > 2) {
                    Row {
                        RadioButton(selected = studyStyle == id, onClick = { onStudyStyleChange(id) })
                        TextButton(onClick = { onStudyStyleChange(id) }) { Text(label) }
                    }
                }
            }
            TextButton(onClick = onOpenDeepFocusOrder) { Text("Edit subject and chapter order") }
            if (studyStyle == "mixed_bag") TextButton(onClick = onOpenMixedBagPicker) { Text("Choose priority subjects") }
            TextButton(onClick = onCustomizeDifficulty) { Text("Daily count and chapter difficulty") }
        }
        if (premiumRequired) Text("Premium required", color = MaterialTheme.colorScheme.error)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (exam != null && studyDays == 0) Text("No study days available", color = MaterialTheme.colorScheme.error)
        Button(onClick = onBuildPlan, enabled = exam != null && studyDays > 0, modifier = Modifier.fillMaxWidth()) {
            Text("Preview my plan")
        }
    }
}
