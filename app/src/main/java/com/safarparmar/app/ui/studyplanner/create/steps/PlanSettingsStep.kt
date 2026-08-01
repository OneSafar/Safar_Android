package com.safarparmar.app.ui.studyplanner.create.steps

import com.safarparmar.app.ui.studyplanner.components.PlannerDialogText
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogAction
import com.safarparmar.app.ui.studyplanner.components.PlannerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.theme.isLightBackground
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.logic.jsDayOfWeek
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.studyplanner.plan.PlanRestDaysRow
import com.safarparmar.app.ui.theme.LoraFontFamily
import kotlin.math.ceil

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
    modifier: Modifier = Modifier,
) {
    val selectedStyle = studyStyle
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary

    val context = LocalContext.current
    var buildAttempted by remember { mutableStateOf(false) }
    var toastTrigger by remember { mutableIntStateOf(0) }
    val examDateOnly = examDate.take(10)

    LaunchedEffect(toastTrigger) {
        if (toastTrigger > 0) {
            Toast.makeText(context, "Please select exam date", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "A few details",
            fontFamily = LoraFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Normal,
            color = scheme.onSurface,
        )

        Spacer(Modifier.height(4.dp))

        SettingsSection(title = "When is your exam?") {
            PlannerExamDateField(
                examDateIso = examDate,
                onExamDateChange = onExamDateChange,
                isError = buildAttempted && examDateOnly.isBlank(),
            )
        }

        PlanHairline(alpha = 0.5f)

        SettingsSection(title = "How many days a week do you want to study?") {
            PlanRestDaysRow(selected = offDays, onToggle = onToggleOffDay)
        }

        PlanHairline(alpha = 0.5f)

        SettingsSection(title = "Choose your study style") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyStyleIconOption(
                    icon = Icons.Default.CenterFocusStrong,
                    accent = PlannerAccent.Coral,
                    title = "Deep Focus",
                    body = "Focus on one subject at a time.",
                    info = "Finish one subject before moving to the next.",
                    selected = selectedStyle == "deep_focus",
                    onClick = {
                        onStudyStyleChange("deep_focus")
                        onOpenDeepFocusOrder()
                    },
                )
                if (subjectCount > 2) {
                    StudyStyleIconOption(
                        icon = Icons.Default.Shuffle,
                        accent = PlannerAccent.Teal,
                        title = "Mixed Bag",
                        body = "Tackle your hardest subjects first.",
                        info = "Pick your 2-3 hardest subjects. They get scheduled first, in the order you choose, and your other subjects start once they're covered.",
                        selected = selectedStyle == "mixed_bag",
                        onClick = {
                            onStudyStyleChange("mixed_bag")
                            onOpenMixedBagPicker()
                        },
                    )
                }
                StudyStyleIconOption(
                    icon = Icons.Default.Bolt,
                    accent = PlannerAccent.Amber,
                    title = "Balanced",
                    body = "Study a little bit of all your subjects every day.",
                    info = "Study a steady mix of every subject each day, in the order you arrange. Nothing waits till the end.",
                    selected = selectedStyle == "balanced",
                    onClick = {
                        onStudyStyleChange("balanced")
                        // Balanced arranges the syllabus too. It reuses the exact
                        // same screen as Deep Focus — the two styles differ in what
                        // they DO with the order (Deep Focus finishes one subject
                        // before the next; Balanced only uses it to decide who
                        // leads the daily mix), not in how it is collected.
                        onOpenDeepFocusOrder()
                    },
                )
            }
        }

        PlanHairline(alpha = 0.5f)

        // Count actual STUDY days — every day between now and the exam minus the
        // weekly rest days the student picked. Dividing by plain calendar days was
        // the bug: choosing rest days left the recommendation unchanged even
        // though there are now fewer days to cover the same topics.
        val studyDaysEstimate = runCatching {
            val exam = java.time.LocalDate.parse(examDateOnly)
            var cursor = java.time.LocalDate.now()
            var count = 0
            while (!cursor.isAfter(exam)) {
                if (jsDayOfWeek(cursor) !in offDays) count++
                cursor = cursor.plusDays(1)
            }
            count.coerceAtLeast(1)
        }.getOrNull()

        SettingsSection(title = "How many topics per day?", subtitle = "Optional — we'll recommend one for you.") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (topicCount > 0 && studyDaysEstimate != null) {
                    val recommended = ceil(topicCount.toDouble() / studyDaysEstimate.toDouble()).toInt().coerceAtLeast(1)
                    Text(
                        text = buildAnnotatedString {
                            append("With ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accent)) {
                                append("$topicCount topics")
                            }
                            append(" over ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accent)) {
                                append("$studyDaysEstimate study days")
                            }
                            append(", we recommend studying ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accent)) {
                                append("$recommended topics/day")
                            }
                            append(".")
                        },
                        fontSize = 12.5.sp,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
                OutlinedTextField(
                    value = dailyGoal,
                    onValueChange = onDailyGoalChange,
                    label = { Text("Topics per day") },
                    supportingText = { Text("Big topics count as more.") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (premiumRequired) {
            Text(
                "Safar Premium is required to create plans from templates.",
                color = scheme.error,
                fontSize = 12.sp,
            )
        } else if (error != null) {
            Text(error, color = scheme.error, fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))
        PlanHairline()
        Spacer(Modifier.height(4.dp))

        val isLight = scheme.background.isLightBackground()
        val styleAccent = when (studyStyle) {
            "deep_focus" -> PlannerAccent.Coral
            "mixed_bag" -> PlannerAccent.Teal
            "balanced" -> PlannerAccent.Amber
            else -> null
        }

        MacOSPrimaryActionButton(
            text = "Build my plan",
            onClick = {
                if (examDateOnly.isBlank()) {
                    buildAttempted = true
                    toastTrigger++
                } else {
                    onBuildPlan()
                }
            },
            isLight = isLight,
            customAccent = styleAccent,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        subtitle?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    info: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.isLightBackground()
    var showInfo by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(20.dp)
    val bodyColor = if (selected) {
        accent.copy(alpha = if (isLight) 0.15f else 0.22f)
    } else {
        if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E).copy(alpha = 0.65f)
    }

    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            colors = listOf(
                if (selected) accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                if (selected) accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                if (selected) accent.copy(alpha = 0.6f) else Color(0xFFE5E5EA),
                if (selected) accent.copy(alpha = 0.3f) else Color(0xFFD1D1D6),
            )
        )
    }

    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (selected) (shadowElevation + 2.dp) else shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor,
            )
            .clip(shape)
            .background(bodyColor)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                brush = borderBrush,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else accent.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) accent else scheme.onSurface,
                )
                Text(body, fontSize = 11.5.sp, color = scheme.onSurfaceVariant, maxLines = 1)
            }
            IconButton(onClick = { showInfo = true }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Info, contentDescription = "About $title", tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
    if (showInfo) {
        PlannerDialog(
            onDismissRequest = { showInfo = false },
            title = title,
            text = { PlannerDialogText(info) },
            confirmButton = { PlannerDialogAction(text = "OK") { showInfo = false } },
        )
    }
}

