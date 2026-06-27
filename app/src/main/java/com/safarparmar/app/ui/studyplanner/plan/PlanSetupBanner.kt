package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerOnboardingSteps
import com.safarparmar.app.ui.studyplanner.logic.flattenTopics
import com.safarparmar.app.domain.model.studyplanner.TopicStatus

@Composable
fun PlanSetupBanner(
    plan: StudyPlan,
    actions: PlannerActions,
    completedSteps: Set<String>,
    onEditPlanDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val topics = plan.flattenTopics()
    val hasDate = !plan.examDate.isNullOrBlank()
    val hasTopics = topics.isNotEmpty()
    val hasSchedule = topics.any { !it.topic.plannedDate.isNullOrBlank() } ||
        StudyPlannerOnboardingSteps.BUILD_SCHEDULE in completedSteps
    val reviewedCalendar = StudyPlannerOnboardingSteps.REVIEW_CALENDAR in completedSteps
    val completedFirstTopic = topics.any { it.topic.status == TopicStatus.DONE } ||
        StudyPlannerOnboardingSteps.FIRST_TOPIC_DONE in completedSteps
    val doneCount = listOf(hasDate, hasTopics, hasSchedule, reviewedCalendar, completedFirstTopic).count { it }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = PlanShapes.banner,
        color = scheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = PlanSpacing.horizontal, vertical = PlanSpacing.section),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFEEF8),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_butterfly_tour),
                        contentDescription = null,
                        modifier = Modifier.padding(5.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (doneCount == 5) "Plan ready" else "Finish setup",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "$doneCount of 5 steps complete",
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            PlanSetupStep(
                label = "Set exam date",
                helper = "Tell us your exam date.",
                done = hasDate,
                enabled = true,
                onClick = onEditPlanDetails,
            )
            PlanSetupStep(
                label = "Add syllabus",
                helper = "Add subjects, chapters, and topics.",
                done = hasTopics,
                enabled = true,
                onClick = { actions.setSection(PlannerSection.SYLLABUS) },
            )
            PlanSetupStep(
                label = "Tap Build Planner",
                helper = if (hasDate && hasTopics) {
                    "Go to Syllabus and create your daily plan."
                } else {
                    "Add an exam date and topics first."
                },
                done = hasSchedule,
                enabled = hasDate && hasTopics,
                onClick = { actions.setSection(PlannerSection.SYLLABUS) },
            )
            PlanSetupStep(
                label = "Review calendar",
                helper = if (hasSchedule) "Check if your daily load is okay." else "Tap Build Planner in Syllabus first.",
                done = reviewedCalendar,
                enabled = hasSchedule,
                onClick = { actions.setSection(PlannerSection.CALENDAR) },
            )
            PlanSetupStep(
                label = "Complete first topic",
                helper = if (hasSchedule) "Study one topic and mark it done." else "Tap Build Planner to create Today's Agenda.",
                done = completedFirstTopic,
                enabled = hasSchedule,
                onClick = { actions.setSection(PlannerSection.PLAN) },
            )
        }
    }
}

@Composable
private fun PlanSetupStep(
    label: String,
    helper: String,
    done: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when {
                    done -> Icons.Default.Check
                    enabled -> Icons.AutoMirrored.Filled.ArrowForward
                    else -> Icons.Default.Lock
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when {
                    done -> Color(0xFF16A34A)
                    enabled -> scheme.primary
                    else -> scheme.onSurfaceVariant.copy(alpha = 0.62f)
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled || done) scheme.onSurface else scheme.onSurfaceVariant,
            )
            Text(
                text = helper,
                fontSize = 11.sp,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = when {
                done -> "Done"
                enabled -> "Next"
                else -> "Locked"
            },
            fontSize = 11.sp,
            color = scheme.onSurfaceVariant,
        )
    }
}
