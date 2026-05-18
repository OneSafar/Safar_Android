package com.safar.app.ui.studyplanner.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.ui.studyplanner.PlannerActions
import com.safar.app.ui.studyplanner.logic.flattenTopics

@Composable
fun PlanSetupBanner(
    plan: StudyPlan,
    actions: PlannerActions,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val hasDate = !plan.examDate.isNullOrBlank()
    val hasTopics = plan.flattenTopics().isNotEmpty()
    val hasSchedule = plan.flattenTopics().any { !it.topic.plannedDate.isNullOrBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PlanShapes.banner)
            .padding(horizontal = PlanSpacing.horizontal, vertical = PlanSpacing.section),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Finish setup",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
        )
        PlanSetupStep("Set exam date", hasDate) { /* already on plan tab */ }
        PlanSetupStep("Add topics", hasTopics) { actions.setSection(PlannerSection.SYLLABUS) }
        PlanSetupStep("Build schedule", hasSchedule) { actions.autoDistribute(false, true) }
        PlanSetupStep("Review calendar", false) { actions.setSection(PlannerSection.CALENDAR) }
    }
}

@Composable
private fun PlanSetupStep(
    label: String,
    done: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (done) Color(0xFF16A34A) else scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = scheme.onSurface,
        )
        Text(
            text = if (done) "Done" else "Next",
            fontSize = 11.sp,
            color = scheme.onSurfaceVariant,
        )
    }
}
