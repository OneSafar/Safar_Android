package com.safarparmar.app.ui.studyplanner.plan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerOnboardingSteps
import com.safarparmar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.flattenTopics
import com.safarparmar.app.ui.studyplanner.logic.readableDate
import com.safarparmar.app.ui.studyplanner.logic.rollup
import com.safarparmar.app.ui.studyplanner.logic.todayKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class StudyPlannerTab {
    TODAY,
    OVERDUE,
    UPCOMING,
    COMPLETED
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlanTabScreen(
    plan: StudyPlan,
    actions: PlannerActions,
    onNavigate: (String) -> Unit,
    onboardingCompletedSteps: Set<String> = emptySet(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val today = remember { todayKey() }
    val refs = remember(plan.subjects) { plan.flattenTopics() }
    
    val todayTopics = remember(refs, today) {
        refs.filter { it.topic.plannedDate?.take(10) == today }
    }
    val overdueTopics = remember(refs, today) {
        refs.filter { (it.topic.plannedDate?.take(10) ?: "9999") < today && it.topic.status != TopicStatus.DONE }
    }
    val upcomingTopics = remember(refs, today) {
        refs
            .filter { (it.topic.plannedDate?.take(10) ?: "") > today && it.topic.status != TopicStatus.DONE }
            .sortedWith(
                compareBy<TopicRef> { it.topic.plannedDate?.take(10).orEmpty() }
                    .thenBy { it.topic.name.lowercase() },
            )
    }
    val upcomingByDate = remember(upcomingTopics) {
        upcomingTopics
            .groupBy { it.topic.plannedDate?.take(10).orEmpty() }
            .filterKeys { it.isNotBlank() }
            .toSortedMap()
    }
    val completedTopics = remember(refs) {
        refs.filter { it.topic.status == TopicStatus.DONE }
    }
    val hasDate = !plan.examDate.isNullOrBlank()
    val hasTopics = refs.isNotEmpty()
    val hasSchedule = refs.any { !it.topic.plannedDate.isNullOrBlank() } ||
        StudyPlannerOnboardingSteps.BUILD_SCHEDULE in onboardingCompletedSteps
    val hasReviewedCalendar = StudyPlannerOnboardingSteps.REVIEW_CALENDAR in onboardingCompletedSteps
    val hasCompletedFirstTopic = completedTopics.isNotEmpty() ||
        StudyPlannerOnboardingSteps.FIRST_TOPIC_DONE in onboardingCompletedSteps
    val showSetupGuide = !(hasDate && hasTopics && hasSchedule && hasReviewedCalendar && hasCompletedFirstTopic)
    
    val progress = remember(plan.id, plan.subjects) { plan.rollup() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            uri?.let {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            StudyPlannerExportUtils.generateStudyPlanPdf(plan, outputStream)
                        }
                    } catch (e: Exception) {
                        actions.setError("PDF export failed: ${e.localizedMessage}")
                    }
                }
            }
        },
    )

    var activeTab by remember(plan.id) { mutableStateOf(StudyPlannerTab.TODAY) }
    var isFlowModeActive by remember(plan.id) { mutableStateOf(false) }

    var showSettings by remember(plan.id) { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }
    var completionPromptTopic by remember { mutableStateOf<TopicRef?>(null) }

    // Editable Today's Agenda state
    var replaceSheetTopic by remember { mutableStateOf<TopicRef?>(null) }
    var showAddToTodaySheet by remember { mutableStateOf(false) }
    var removeFromTodayConfirmTopic by remember { mutableStateOf<TopicRef?>(null) }

    fun exportPlan() {
        exportLauncher.launch("${plan.title.replace(" ", "_")}_Syllabus.pdf")
    }

    if (showSettings) {
        PlanSettingsSheet(
            plan = plan,
            actions = actions,
            onExport = ::exportPlan,
            onReset = { resetConfirm = true },
            onDismiss = { showSettings = false },
        )
    }

    if (resetConfirm) {
        PlanConfirmDialog(
            title = "Reset plan?",
            body = "All topics will move back to Todo and dates will be removed.",
            onDismiss = { resetConfirm = false },
            onConfirm = {
                actions.resetPlan()
                resetConfirm = false
                showSettings = false
            },
        )
    }

    completionPromptTopic?.let { ref ->
        AlertDialog(
            onDismissRequest = { completionPromptTopic = null },
            title = { Text("Mark this topic as?") },
            text = {
                Text(
                    text = ref.topic.name,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            actions.updateTopic(ref.topic.id, status = TopicStatus.REVISION_NEEDED)
                            completionPromptTopic = null
                        },
                    ) {
                        Text("To Revise")
                    }
                    Button(
                        onClick = {
                            actions.updateTopic(ref.topic.id, status = TopicStatus.DONE)
                            completionPromptTopic = null
                        },
                    ) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { completionPromptTopic = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    fun handleTopicDoneCheck(ref: TopicRef, checked: Boolean) {
        if (checked && ref.topic.status != TopicStatus.DONE) {
            completionPromptTopic = ref
        } else if (!checked && ref.topic.status == TopicStatus.DONE) {
            actions.updateTopic(ref.topic.id, status = TopicStatus.TODO)
        }
    }

    // ── Replace Topic Sheet ────────────────────────────────────────
    replaceSheetTopic?.let { currentRef ->
        ReplaceTopicSheet(
            currentRef = currentRef,
            allRefs = refs,
            today = today,
            onSwap = { currentId, replacementId ->
                actions.swapTopicDates(currentId, replacementId)
            },
            onReplace = { currentId, replacementId, todayDate ->
                // Move replacement to today, unschedule the current one
                actions.replaceTopicToday(currentId, replacementId, todayDate)
            },
            onDismiss = { replaceSheetTopic = null },
        )
    }

    // ── Add Topic to Today Sheet ───────────────────────────────────
    if (showAddToTodaySheet) {
        AddTopicToTodaySheet(
            allRefs = refs,
            today = today,
            dailyGoal = plan.dailyGoal ?: 0,
            currentTodayCount = todayTopics.size,
            onAdd = { topicId, todayDate ->
                actions.moveTopicsToDate(listOf(topicId), todayDate)
            },
            onDismiss = { showAddToTodaySheet = false },
        )
    }

    // ── Remove from Today confirmation ─────────────────────────────
    removeFromTodayConfirmTopic?.let { ref ->
        PlanConfirmDialog(
            title = "Remove from today?",
            body = "\"${ref.topic.name}\" will be unscheduled. You can re-add it later.",
            onDismiss = { removeFromTodayConfirmTopic = null },
            onConfirm = {
                actions.clearTopicDates(listOf(ref.topic.id))
                removeFromTodayConfirmTopic = null
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "status", contentType = "status") {
                PlanStatusCard(
                    plan = plan,
                    progress = progress,
                    onExportClick = ::exportPlan,
                    onSettingsClick = { showSettings = true },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    filters = {
                        PlanTabFiltersRow(
                            activeTab = activeTab,
                            onTabSelected = { activeTab = it },
                            todayCount = todayTopics.size,
                            overdueCount = overdueTopics.size,
                            upcomingCount = upcomingTopics.size,
                            completedCount = completedTopics.size,
                        )
                    },
                )
            }

            item(key = "actions", contentType = "actions") {
                PlanActionRow(
                    onAddTopics = { onNavigate(Routes.ROUTE_SYLLABUS_SUBJECTS.replace("{planId}", plan.id)) },
                    onSchedule = { actions.autoDistribute(false, true) },
                    showSchedule = false,
                )
            }

            if (showSetupGuide) {
                item(key = "setupGuide", contentType = "setupGuide") {
                    PlanSetupBanner(
                        plan = plan,
                        actions = actions,
                        completedSteps = onboardingCompletedSteps,
                        onEditPlanDetails = { showSettings = true },
                    )
                }
            }

            // Tab Content Items
            when (activeTab) {
                StudyPlannerTab.TODAY -> {
                    val remainingToday = todayTopics.filter { it.topic.status != TopicStatus.DONE }
                    val todayCompleted = todayTopics.isNotEmpty() && remainingToday.isEmpty()

                    if (todayTopics.isEmpty()) {
                        item(key = "today_empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "No topics planned for today. Add topics or review upcoming tasks.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        if (todayCompleted) {
                            item(key = "today_conquered") {
                                val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = if (isDark) {
                                                        listOf(Color(0xFF14532D), Color(0xFF064E3B))
                                                    } else {
                                                        listOf(Color(0xFFDCFCE7), Color(0xFFA7F3D0))
                                                    }
                                                ),
                                                shape = MaterialTheme.shapes.large
                                            )
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .background(
                                                    color = if (isDark) Color(0xFF15803D) else Color(0xFF4ADE80),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎉", fontSize = 24.sp)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Today's Queue Conquered!",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDark) Color.White else Color(0xFF064E3B),
                                            )
                                            Text(
                                                text = "Nice. Your completed tasks stay below so the checkmarks feel visible and satisfying.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF047857),
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item(key = "today_flow_hero") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                ) {
                                    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = if (isDark) {
                                                        listOf(Color(0xFF311042), Color(0xFF1E1B4B))
                                                    } else {
                                                        listOf(Color(0xFFF3E8FF), Color(0xFFD8B4FE))
                                                    }
                                                )
                                            )
                                            .padding(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Start Studying",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isDark) Color.White else Color(0xFF581C87)
                                                )
                                                Text(
                                                    text = "Enter distraction-free Flow Mode to complete your tasks one by one.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isDark) Color.White.copy(alpha = 0.8f) else Color(0xFF6B21A8)
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Button(
                                                    onClick = { isFlowModeActive = true },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isDark) Color(0xFFA855F7) else Color(0xFF7E22CE),
                                                        contentColor = Color.White
                                                    ),
                                                    shape = ButtonDefaults.shape,
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "Start Study Flow (${remainingToday.size} left)",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            Text("⚡", fontSize = 44.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "today_list_header") {
                            PlanSectionHeader(title = "Today's Agenda", trailing = "${todayTopics.size} planned")
                        }
                        items(
                            items = todayTopics.take(10),
                            key = { ref -> "today_${ref.topic.id}" },
                            contentType = { "todayTopic" }
                        ) { ref ->
                            PlannerTaskRow(
                                ref = ref,
                                accent = if (ref.topic.status == TopicStatus.DONE) PlanTaskRowAccent.Done else PlanTaskRowAccent.Planned,
                                onDoneChange = { done ->
                                    handleTopicDoneCheck(ref, done)
                                },
                                onReplace = { replaceSheetTopic = ref },
                                onRemoveFromToday = { removeFromTodayConfirmTopic = ref },
                            )
                        }

                        // "Add Topic to Today" button
                        item(key = "today_add_topic") {
                            val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
                            OutlinedButton(
                                onClick = { showAddToTodaySheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                border = BorderStroke(
                                    width = 1.5.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = if (isDark) {
                                            listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                                        } else {
                                            listOf(Color(0xFF0F172A), Color(0xFF334155))
                                        },
                                    ),
                                ),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Add Topic to Today",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }

                        if (todayCompleted) {
                            val bonusTopics = (overdueTopics + upcomingTopics).take(5)
                            if (bonusTopics.isNotEmpty()) {
                                item(key = "bonus_header") {
                                    PlanSectionHeader(title = "Bonus Tasks to Get Ahead", trailing = "${bonusTopics.size} suggested")
                                }
                                items(
                                    items = bonusTopics,
                                    key = { ref -> "bonus_${ref.topic.id}" },
                                    contentType = { "bonusTopic" }
                                ) { ref ->
                                    PlannerTaskRow(
                                        ref = ref,
                                        accent = PlanTaskRowAccent.Planned,
                                        onDoneChange = { done ->
                                            handleTopicDoneCheck(ref, done)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                StudyPlannerTab.OVERDUE -> {
                    if (overdueTopics.isEmpty()) {
                        item(key = "overdue_empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎉", fontSize = 36.sp)
                                    Text(
                                        text = "No Overdue Topics!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Excellent time management! You're completely up to date with your studies.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "overdue_list_header") {
                            PlanSectionHeader(title = "Overdue Tasks", trailing = "${overdueTopics.size} total")
                        }
                        items(
                            items = overdueTopics,
                            key = { ref -> "overdue_${ref.topic.id}" },
                            contentType = { "overdueTopic" }
                        ) { ref ->
                            PlannerTaskRow(
                                ref = ref,
                                accent = PlanTaskRowAccent.Overdue,
                                onDoneChange = { done ->
                                    handleTopicDoneCheck(ref, done)
                                }
                            )
                        }
                    }
                }

                StudyPlannerTab.UPCOMING -> {
                    if (upcomingTopics.isEmpty()) {
                        item(key = "upcoming_empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📅", fontSize = 36.sp)
                                    Text(
                                        text = "No Upcoming Topics Scheduled",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Use the Schedule button at the top to distribute remaining topics into your calendar.",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "upcoming_list_header") {
                            PlanSectionHeader(
                                title = "Upcoming schedule",
                                trailing = "${upcomingTopics.size} topics · ${upcomingByDate.size} days",
                            )
                        }
                        upcomingByDate.forEach { (dateKey, dayTopics) ->
                            item(key = "upcoming_date_$dateKey") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        PlanSectionHeader(
                                            title = readableDate(dateKey),
                                            trailing = "${dayTopics.size} planned",
                                        )
                                        dayTopics.forEach { ref ->
                                            PlannerTaskRow(
                                                ref = ref,
                                                accent = PlanTaskRowAccent.Planned,
                                                onDoneChange = { done ->
                                                    handleTopicDoneCheck(ref, done)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                StudyPlannerTab.COMPLETED -> {
                    if (completedTopics.isEmpty()) {
                        item(key = "completed_empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("💪", fontSize = 36.sp)
                                    Text(
                                        text = "No Completed Topics Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Every great journey starts with a single step. Start ekagra flow and complete your first task!",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "completed_list_header") {
                            PlanSectionHeader(title = "Completed Tasks", trailing = "${completedTopics.size} total")
                        }
                        items(
                            items = completedTopics.take(15),
                            key = { ref -> "completed_${ref.topic.id}" },
                            contentType = { "completedTopic" }
                        ) { ref ->
                            PlannerTaskRow(
                                ref = ref,
                                accent = PlanTaskRowAccent.Done,
                                onDoneChange = { done ->
                                    handleTopicDoneCheck(ref, done)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen Flow Mode Overlay
        if (isFlowModeActive) {
            val incompleteTopics = todayTopics.filter { it.topic.status != TopicStatus.DONE }
            val totalCount = todayTopics.size
            val completedCount = totalCount - incompleteTopics.size
            val activeTopic = incompleteTopics.firstOrNull()

            // Breathing pulse animations
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E1B4B),
                                Color(0xFF2E1065)
                            )
                        )
                    )
                    .clickable(enabled = true, onClick = {}) // block clicks to background
                    .padding(24.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                // Exit button at top right
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(
                        onClick = { isFlowModeActive = false },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Flow Mode",
                            tint = Color.White
                        )
                    }
                }

                if (activeTopic != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Focus progress bar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = "Ekagra Queue: $completedCount of $totalCount Done",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = Color(0xFFA855F7),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Pulsing Focus Zone
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "ACTIVE FOCUS TOPIC",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFC084FC)
                                )
                            }

                            Text(
                                text = activeTopic.topic.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeTopic.subject.name,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = activeTopic.chapter.name,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Actions
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Button(
                                onClick = {
                                    actions.updateTopic(activeTopic.topic.id, status = TopicStatus.DONE)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .shadow(16.dp, shape = CircleShape, ambientColor = Color(0xFFA855F7), spotColor = Color(0xFFA855F7)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9333EA),
                                    contentColor = Color.White
                                ),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Mark as Completed", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                        }
                    }
                } else {
                    // Celebration screen
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎉",
                            fontSize = 64.sp
                        )
                        Text(
                            text = "Session Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Outstanding! You have successfully completed all your planned tasks for today.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { isFlowModeActive = false },
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1B4B)),
                            shape = CircleShape
                        ) {
                            Text("Back to Planner", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
