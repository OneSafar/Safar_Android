package com.safar.app.ui.studyplanner.plan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safar.app.domain.model.studyplanner.PlannerSection
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.studyplanner.PlannerActions
import com.safar.app.ui.studyplanner.importexport.StudyPlannerExportUtils
import com.safar.app.ui.studyplanner.logic.TopicRef
import com.safar.app.ui.studyplanner.logic.flattenTopics
import com.safar.app.ui.studyplanner.logic.rollup
import com.safar.app.ui.studyplanner.logic.todayKey
import com.safar.app.ui.studyplanner.screens.PlannerTopicDetailSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlanTabScreen(
    plan: StudyPlan,
    actions: PlannerActions,
    onNavigate: (String) -> Unit,
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
        refs.filter { (it.topic.plannedDate?.take(10) ?: "") > today && it.topic.status != TopicStatus.DONE }
    }
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

    var showSettings by remember(plan.id) { mutableStateOf(false) }
    var resetConfirm by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf<TopicRef?>(null) }
    var topicSheetNonce by remember(plan.id) { mutableIntStateOf(0) }

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

    selectedTopic?.let { ref ->
        PlannerTopicDetailSheet(
            ref = ref,
            openNonce = topicSheetNonce,
            actions = actions,
            onDismiss = { selectedTopic = null },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "status", contentType = "status") {
            PlanStatusCard(
                plan = plan,
                progress = progress,
                todayCount = todayTopics.size,
                todayDoneCount = todayTopics.count { it.topic.status == TopicStatus.DONE },
                overdueCount = overdueTopics.size,
                onExportClick = ::exportPlan,
                onSettingsClick = { showSettings = true },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

        item(key = "today", contentType = "today") {
            TodayMissionCard(
                topics = todayTopics,
                onTopicClick = { ref ->
                    topicSheetNonce += 1
                    selectedTopic = ref
                },
                onTopicDoneChange = { ref, done ->
                    actions.updateTopic(ref.topic.id, status = if (done) TopicStatus.DONE else TopicStatus.TODO)
                },
                onViewAllToday = { actions.setSection(PlannerSection.CALENDAR) },
            )
        }

        item(key = "actions", contentType = "actions") {
            PlanActionRow(
                onAddTopics = { onNavigate(Routes.ROUTE_SYLLABUS_SUBJECTS.replace("{planId}", plan.id)) },
                onSchedule = { actions.autoDistribute(false, true) },
            )
        }

        if (overdueTopics.isNotEmpty()) {
            item(key = "overdue_header", contentType = "sectionHeader") {
                PlanSectionHeader(title = "Overdue", trailing = "${overdueTopics.size} pending")
            }
            items(
                items = overdueTopics.take(3),
                key = { ref -> "overdue_${ref.topic.id}" },
                contentType = { "overdueTopic" },
            ) { ref ->
                PlannerTaskRow(
                    ref = ref,
                    accent = PlanTaskRowAccent.Overdue,
                    onClick = {
                        topicSheetNonce += 1
                        selectedTopic = ref
                    },
                    onDoneChange = { done ->
                        actions.updateTopic(ref.topic.id, status = if (done) TopicStatus.DONE else TopicStatus.TODO)
                    },
                )
            }
            if (overdueTopics.size > 3) {
                item(key = "overdue_more", contentType = "link") {
                    PlanTextLink(text = "View all overdue", onClick = {
                        actions.setSection(PlannerSection.CALENDAR)
                    })
                }
            }
        }

        if (upcomingTopics.isNotEmpty()) {
            item(key = "upcoming_header", contentType = "sectionHeader") {
                PlanSectionHeader(title = "Upcoming", trailing = "${upcomingTopics.size} queued")
            }
            items(
                items = upcomingTopics.take(3),
                key = { ref -> "upcoming_${ref.topic.id}" },
                contentType = { "upcomingTopic" },
            ) { ref ->
                PlannerTaskRow(
                    ref = ref,
                    accent = PlanTaskRowAccent.Planned,
                    onClick = {
                        topicSheetNonce += 1
                        selectedTopic = ref
                    },
                    onDoneChange = { done ->
                        actions.updateTopic(ref.topic.id, status = if (done) TopicStatus.DONE else TopicStatus.TODO)
                    },
                )
            }
        }
    }
}
