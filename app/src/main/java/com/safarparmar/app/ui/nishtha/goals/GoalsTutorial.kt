package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.GoalLinkedSession
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.tutorial.FeatureTutorialPracticeWindow
import com.safarparmar.app.ui.tutorial.FeatureTutorialHost
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.util.IstDateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun GoalsTutorialEntry(dataStore: SafarDataStore, eligible: Boolean, model: GoalsTutorialViewModel, version: Int = 1) {
    if (!com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED) return
    com.safarparmar.app.ui.tutorial.RegisterTitliGuide(start = { model.start(GoalsTutorialLesson.CORE) }, lessons = model::open, feature = com.safarparmar.app.ui.tutorial.TitliFeature.GOALS)
    val seen by remember(dataStore, version) { dataStore.featureTutorialInvitationSeen("goals", version) }
        .collectAsStateWithLifecycle(initialValue = null)
    val completed by remember(dataStore, version) { dataStore.featureTutorialCompletedLessons("goals", version) }
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    var handled by rememberSaveable { mutableStateOf(false) }
    var readyToOffer by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(eligible) {
        readyToOffer = false
        if (eligible) { delay(600); readyToOffer = true }
    }
    LaunchedEffect(model.isOpen, eligible) {
        if (model.isOpen) {
            if (!eligible) { model.close(); busy = true }
            else { handled = true; dataStore.setFeatureTutorialInvitationSeen("goals", version) }
        }
    }
    LaunchedEffect(model.completedLesson) {
        model.completedLesson?.let {
            dataStore.completeFeatureTutorialLesson("goals", version, it.name)
            model.acknowledgeCompletion()
        }
    }
    if (com.safarparmar.app.ui.tutorial.LocalTitliController.current == null && seen == false && !handled && readyToOffer && eligible && !model.isOpen && !busy) {
        fun dismiss() { handled = true; scope.launch { dataStore.setFeatureTutorialInvitationSeen("goals", version) } }
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text(stringResource(R.string.goals_tutorial_invite)) },
            text = { Text(stringResource(R.string.goals_tutorial_invite_body)) },
            confirmButton = { TextButton(onClick = { dismiss(); model.start(GoalsTutorialLesson.CORE) }) { Text(stringResource(R.string.ekagra_tutorial_start)) } },
            dismissButton = { TextButton(onClick = ::dismiss) { Text(stringResource(R.string.ekagra_tutorial_later)) } },
        )
    }
    if (busy) AlertDialog(
        onDismissRequest = { busy = false }, title = { Text(stringResource(R.string.goals_tutorial_learn)) },
        text = { Text(stringResource(R.string.goals_tutorial_busy)) },
        confirmButton = { TextButton(onClick = { busy = false }) { Text(stringResource(R.string.ekagra_tutorial_done)) } },
    )
    if (model.isOpen && eligible) GoalsTutorialPractice(model, completed)
}

/** A modal practice window keeps real goal forms, selection and scroll state untouched. */
@Composable
internal fun GoalsTutorialPractice(model: GoalsTutorialViewModel, completed: Set<String> = emptySet()) {
    val dark = !MaterialTheme.colorScheme.background.isLightBackground()
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides dark) {
        FeatureTutorialPracticeWindow(
            onHelp = model::open,
            showHeader = model.state == null,
            titleRes = R.string.goals_tutorial_learn,
            practiceLabelRes = R.string.goals_tutorial_practice,
            isDarkTheme = dark, onExit = model::close,
            practiceTag = "goals.tutorial.practice", labelTag = "goals.tutorial.label", closeTag = "goals.tutorial.close",
            backgroundColor = GoalsFlatColors.Bg, contentColor = GoalsFlatColors.Text,
        ) {
            val state = model.state
            if (state == null) GoalsTutorialLessons(completed, model::start, model::close)
            else FeatureTutorialHost(state, model::complete, model::close,
                onHelp = model::open,
                practiceLabelRes = R.string.goals_tutorial_practice, practiceLabelTag = "goals.tutorial.label", closeTag = "goals.tutorial.close",
                // Extend the live Goals canvas beneath the translucent guidance,
                // just as Ekagra extends its selected timer backdrop.
                modifier = Modifier.weight(1f).background(GoalsFlatColors.Bg),
            ) { step ->
                GoalsPracticeStage(GoalsTutorialDemoState(GoalsTutorialScene.valueOf(step.id), LocalDate.now(IstDateUtils.zone)))
            }
        }
    }
}

@Composable
private fun GoalsTutorialLessons(completed: Set<String>, onStart: (GoalsTutorialLesson) -> Unit, onExit: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.goals_tutorial_lessons), style = MaterialTheme.typography.headlineSmall)
        GoalsTutorialLesson.entries.forEach { lesson ->
            OutlinedCard(onClick = { onStart(lesson) }, modifier = Modifier.fillMaxWidth().testTag("goals.tutorial.lesson.${lesson.name}")) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(lesson.title), style = MaterialTheme.typography.titleMedium)
                        if (lesson.name in completed) Text(stringResource(R.string.ekagra_tutorial_completed), style = MaterialTheme.typography.labelMedium)
                    }
                    if (lesson.name in completed) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().testTag("goals.tutorial.done")) { Text(stringResource(R.string.ekagra_tutorial_done)) }
    }
}

@Composable
private fun GoalsPracticeStage(demo: GoalsTutorialDemoState) {
    val scene = demo.scene
    val title = stringResource(R.string.goals_tutorial_sample)
    val details = stringResource(R.string.goals_tutorial_sample_details)
    val goal = demo.sampleGoal(title, details)
    val formScenes = setOf(GoalsTutorialScene.TITLE, GoalsTutorialScene.CREATE, GoalsTutorialScene.SCHEDULE,
        GoalsTutorialScene.DATE, GoalsTutorialScene.DAILY, GoalsTutorialScene.EDIT)
    Box(Modifier.fillMaxSize().background(GoalsFlatColors.Bg)) {
        when (scene) {
            in formScenes -> GoalCreationContent(
                title = if (scene == GoalsTutorialScene.EDIT) stringResource(R.string.goals_tutorial_sample_edited) else title,
                description = details, scheduled = scene == GoalsTutorialScene.DATE,
                repeatDaily = scene == GoalsTutorialScene.DAILY, selectedDate = demo.today.plusDays(1),
                onTitleChange = {}, onDescriptionChange = {}, onToggleSchedule = {}, onPickDate = {},
                onRepeatChange = {}, onCreate = {}, editing = scene == GoalsTutorialScene.EDIT,
            )
            GoalsTutorialScene.TIME, GoalsTutorialScene.DONE -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.goals_study_time_question), style = MaterialTheme.typography.titleLarge)
                Box(Modifier.tutorialTarget("goals.studyTime")) {
                    GoalStudyTimeContent(0, 25, {}, {})
                }
                Box(Modifier.tutorialTarget("goals.doneActions")) { GoalCompletionActions(0, 25, false, {}, {}) }
            }
            GoalsTutorialScene.REPEAT, GoalsTutorialScene.REPEAT_AUTO, GoalsTutorialScene.YESTERDAY -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                GoalRepeatContent(
                    goals = listOf(goal, goal.copy(id = "practice-yesterday", scheduledDate = demo.today.minusDays(1).toString())),
                    isSaving = false, onRepeat = { _, _, _ -> },
                    initialSource = if (scene == GoalsTutorialScene.YESTERDAY) 1 else 0,
                    initialDaily = scene == GoalsTutorialScene.REPEAT_AUTO,
                )
            }
            GoalsTutorialScene.DELETED -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                GoalDeletedContent(listOf(goal.copy(deletedAt = demo.today.toString())), false, false, {})
            }
            GoalsTutorialScene.MISSED_CHOICE -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val originalDate = demo.today.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.getDefault()))
                Text(stringResource(R.string.goals_earlier_day_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.goals_earlier_day_body, originalDate))
                Box(Modifier.tutorialTarget("goals.missedActions")) { GoalOverdueActions(originalDate, false, {}, {}, {}) }
            }
            GoalsTutorialScene.STATUS -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(stringResource(R.string.goals_status), style = MaterialTheme.typography.titleLarge)
                Box(Modifier.tutorialTarget("goals.statusGrid")) { StatusGrid(listOf(goal), EkagraAnalyticsStats()) }
            }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                GoalsOverviewHeader(
                    tabs = listOf(stringResource(R.string.common_today), stringResource(R.string.goals_upcoming),
                        stringResource(R.string.goals_missed), stringResource(R.string.goals_history)),
                    selectedTab = demo.tab, totalToday = demo.totalToday, doneToday = if (goal.completed) 1 else 0,
                    onSelectTab = {}, onAdd = {}, onStatus = {}, onRepeat = {}, onInsights = {}, onDeleted = {}, onLearn = {},
                )
                if (demo.tab == 3) {
                    val linked = if (scene == GoalsTutorialScene.LINKED_HISTORY) listOf(GoalLinkedSession(
                        id = "practice-focus", goalId = goal.id, goalTitle = title, durationMinutes = 25, durationSeconds = 1500,
                        startedAt = demo.today.atTime(9, 0).atZone(IstDateUtils.zone).toInstant().toString(), endedAt = demo.today.atTime(9, 25).atZone(IstDateUtils.zone).toInstant().toString(), timerMode = "focus",
                    )) else emptyList()
                    GoalHistoryContent(listOf(goal), false, {}, {}, linked)
                } else if (scene == GoalsTutorialScene.ADD) {
                    EmptyGoalsCard(stringResource(R.string.goals_nothing_today), stringResource(R.string.goals_add_to_get_started))
                } else {
                    GoalItem(goal, onComplete = {}, onEdit = {}, onDelete = {}, modifier = Modifier.padding(horizontal = 20.dp).tutorialTarget("goals.row"))
                }
            }
        }
    }
}
