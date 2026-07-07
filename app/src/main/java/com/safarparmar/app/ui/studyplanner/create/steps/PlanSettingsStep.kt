package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.plan.PlanRestDaysRow
import kotlin.math.ceil

@Composable
fun PlanSettingsStep(
    examDate: String,
    onExamDateChange: (String) -> Unit,
    offDays: Set<Int>,
    onToggleOffDay: (Int) -> Unit,
    strategy: String,
    overloadMode: String,
    onStudyStyleChange: (String) -> Unit,
    dailyGoal: String,
    onDailyGoalChange: (String) -> Unit,
    topicCount: Int,
    error: String?,
    premiumRequired: Boolean,
    onBuildPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStyle = when {
        strategy == "sequential" -> "deep_focus"
        overloadMode == "strict" -> "balanced"
        else -> "mixed_bag"
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("A few details", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)

        SettingsSection(title = "When is your exam?") {
            PlannerExamDateField(examDateIso = examDate, onExamDateChange = onExamDateChange)
        }

        SettingsSection(title = "How many days a week do you want to study?") {
            PlanRestDaysRow(selected = offDays, onToggle = onToggleOffDay)
        }

        SettingsSection(title = "How do you like to study?") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyStyleIconOption(
                    icon = Icons.Default.CenterFocusStrong,
                    accent = PlannerAccent.Coral,
                    title = "Deep Focus",
                    body = "Finish topics in syllabus order.",
                    selected = selectedStyle == "deep_focus",
                    onClick = { onStudyStyleChange("deep_focus") },
                )
                StudyStyleIconOption(
                    icon = Icons.Default.Shuffle,
                    accent = PlannerAccent.Teal,
                    title = "Mixed Bag",
                    body = "Mix subjects across the week.",
                    selected = selectedStyle == "mixed_bag",
                    onClick = { onStudyStyleChange("mixed_bag") },
                )
                StudyStyleIconOption(
                    icon = Icons.Default.Bolt,
                    accent = PlannerAccent.Amber,
                    title = "Balanced",
                    body = "Keep your daily load steady.",
                    selected = selectedStyle == "balanced",
                    onClick = { onStudyStyleChange("balanced") },
                )
            }
        }

        val examDateOnly = examDate.take(10)
        val studyDaysEstimate = runCatching {
            val exam = java.time.LocalDate.parse(examDateOnly)
            val today = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(today, exam).toInt().coerceAtLeast(1)
        }.getOrNull()

        SettingsSection(title = "How many topics per day?", subtitle = "Optional — we'll recommend one for you.") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (topicCount > 0 && studyDaysEstimate != null) {
                    val recommended = ceil(topicCount.toDouble() / studyDaysEstimate.toDouble()).toInt().coerceAtLeast(1)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            "You have $topicCount topics and $studyDaysEstimate study days. We recommend $recommended topics/day to finish comfortably before your exam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = dailyGoal,
                    onValueChange = onDailyGoalChange,
                    label = { Text("Topics per day") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (premiumRequired) {
            Text(
                "Safar Premium is required to create plans from templates.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = onBuildPlan,
            enabled = examDateOnly.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Build my plan", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun StudyStyleIconOption(
    icon: ImageVector,
    accent: Color,
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else scheme.surfaceContainerLow,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else scheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
