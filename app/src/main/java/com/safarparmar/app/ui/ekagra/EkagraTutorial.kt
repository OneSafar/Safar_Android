package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
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
import com.safarparmar.app.data.local.TimerAlertStyle
import com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto
import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.feature.kavachanalytics.ui.KavachSummaryPillsContent
import com.safarparmar.app.ui.audio.AudioLibrary
import com.safarparmar.app.ui.audio.AudioLibraryContent
import com.safarparmar.app.ui.ekagra.focusshield.AppPickerContent
import com.safarparmar.app.ui.ekagra.focusshield.BlockedAppInfo
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldUiState
import com.safarparmar.app.ui.ekagra.focusshield.KavachProfileChoices
import com.safarparmar.app.ui.ekagra.focusshield.KavachControlCenterContainer
import com.safarparmar.app.ui.studycircle.StudyCircleTopButtons
import com.safarparmar.app.ui.studycircle.StudyCircleInputContent
import com.safarparmar.app.ui.studycircle.PublicCircleRow
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.data.remote.dto.PublicStudyCircleDto
import com.safarparmar.app.ui.tutorial.FeatureTutorialPracticeWindow
import com.safarparmar.app.ui.tutorial.FeatureTutorialHost
import com.safarparmar.app.ui.tutorial.tutorialTarget
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant

@Composable
internal fun EkagraTutorialEntry(
    dataStore: SafarDataStore,
    eligible: Boolean,
    isDarkTheme: Boolean,
    model: EkagraTutorialViewModel,
    progressFeature: String = "ekagra",
    selectedTheme: VisualTheme = selectableVisualThemes.first(),
) {
    if (!com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED) return
    com.safarparmar.app.ui.tutorial.RegisterTitliGuide(start = { model.start(EkagraTutorialLesson.CORE) }, lessons = model::open, feature = com.safarparmar.app.ui.tutorial.TitliFeature.EKAGRA)
    val invitationSeen by remember(dataStore, progressFeature) { dataStore.featureTutorialInvitationSeen(progressFeature, 1) }
        .collectAsStateWithLifecycle(initialValue = null)
    val completed by remember(dataStore, progressFeature) { dataStore.featureTutorialCompletedLessons(progressFeature, 1) }
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    var invitationHandled by rememberSaveable { mutableStateOf(false) }
    var showBusy by remember { mutableStateOf(false) }
    var offerReady by remember { mutableStateOf(false) }
    LaunchedEffect(eligible) {
        offerReady = false
        if (eligible) { delay(600); offerReady = true }
    }

    LaunchedEffect(model.isOpen, eligible, progressFeature) {
        if (model.isOpen) {
            if (!eligible) { model.close(); showBusy = true }
            else {
                invitationHandled = true
                dataStore.setFeatureTutorialInvitationSeen(progressFeature, 1)
            }
        }
    }
    LaunchedEffect(model.completedLesson, progressFeature) {
        model.completedLesson?.let {
            dataStore.completeFeatureTutorialLesson(progressFeature, 1, it.name)
            model.acknowledgeCompletion()
        }
    }
    if (com.safarparmar.app.ui.tutorial.LocalTitliController.current == null && invitationSeen == false && !invitationHandled && eligible && offerReady && !model.isOpen) {
        fun dismissInvitation() {
            invitationHandled = true
            scope.launch { dataStore.setFeatureTutorialInvitationSeen(progressFeature, 1) }
        }
        AlertDialog(
            onDismissRequest = ::dismissInvitation,
            title = { Text(stringResource(R.string.ekagra_tutorial_invite)) },
            text = { Text(stringResource(R.string.ekagra_tutorial_invite_body)) },
            confirmButton = {
                TextButton(onClick = { dismissInvitation(); model.start(EkagraTutorialLesson.CORE) }) {
                    Text(stringResource(R.string.ekagra_tutorial_start))
                }
            },
            dismissButton = { TextButton(onClick = ::dismissInvitation) { Text(stringResource(R.string.ekagra_tutorial_later)) } },
        )
    }
    if (showBusy) AlertDialog(
        onDismissRequest = { showBusy = false },
        title = { Text(stringResource(R.string.ekagra_tutorial_learn)) },
        text = { Text(stringResource(R.string.ekagra_tutorial_busy)) },
        confirmButton = { TextButton(onClick = { showBusy = false }) { Text(stringResource(R.string.ekagra_tutorial_done)) } },
    )
    if (model.isOpen && eligible) EkagraTutorialPractice(model, completed, isDarkTheme, selectedTheme)
}

/** The only inputs are local tutorial state, completion labels, and appearance. */
@Composable
internal fun EkagraTutorialPractice(
    model: EkagraTutorialViewModel,
    completedLessons: Set<String> = emptySet(),
    isDarkTheme: Boolean = false,
    selectedTheme: VisualTheme = selectableVisualThemes.first(),
) {
    FeatureTutorialPracticeWindow(
        onHelp = model::open,
        showHeader = model.state == null,
        titleRes = R.string.ekagra_tutorial_learn,
        practiceLabelRes = R.string.ekagra_tutorial_practice,
        isDarkTheme = isDarkTheme, onExit = model::close,
        practiceTag = "tutorial.practice", labelTag = "tutorial.practiceLabel", closeTag = "tutorial.close",
    ) {
        val state = model.state
        if (state == null) {
            LessonPicker(completedLessons, model::start, model::close)
        } else {
            val demo = EkagraTutorialDemoState(EkagraTutorialScene.valueOf(state.currentStep.id))
            FeatureTutorialHost(state, model::complete, model::close,
                onHelp = model::open,
                modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface),
                background = {
                    if (demo.scene.usesTimerCanvas) {
                        EkagraTimerBackground(selectedTheme, isDarkTheme, demo.mode == TimerMode.BREAK)
                    }
                },
            ) { step ->
                EkagraPracticeStage(EkagraTutorialDemoState(EkagraTutorialScene.valueOf(step.id)), isDarkTheme, selectedTheme)
            }
        }
    }
}

@Composable
private fun LessonPicker(completed: Set<String>, onStart: (EkagraTutorialLesson) -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(if (completed.contains(EkagraTutorialLesson.CORE.name)) R.string.ekagra_tutorial_complete else R.string.ekagra_tutorial_invite), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.ekagra_tutorial_lessons), style = MaterialTheme.typography.bodyMedium)
        EkagraTutorialLesson.entries.forEach { lesson ->
            OutlinedCard(onClick = { onStart(lesson) }, modifier = Modifier.fillMaxWidth().testTag("tutorial.lesson.${lesson.name}")) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(lesson.titleRes), style = MaterialTheme.typography.titleMedium)
                        if (lesson.name in completed) Text(stringResource(R.string.ekagra_tutorial_completed), style = MaterialTheme.typography.labelMedium)
                    }
                    if (lesson.name in completed) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().testTag("tutorial.done")) { Text(stringResource(R.string.ekagra_tutorial_done)) }
    }
}

@Composable
private fun EkagraPracticeStage(demo: EkagraTutorialDemoState, isDarkTheme: Boolean, selectedTheme: VisualTheme) {
    val scene = demo.scene
    val settingsScenes = setOf(EkagraTutorialScene.FOCUS_DURATION, EkagraTutorialScene.BREAK_DURATION,
        EkagraTutorialScene.POMODORO_ENTRY, EkagraTutorialScene.MUTED, EkagraTutorialScene.ALERTS, EkagraTutorialScene.AUTO_BREAK)
    val historyScenes = setOf(EkagraTutorialScene.HISTORY, EkagraTutorialScene.HISTORY_CATEGORIES, EkagraTutorialScene.HISTORY_FILTERS)
    val saveScenes = setOf(EkagraTutorialScene.SAVE, EkagraTutorialScene.SAVE_GOAL, EkagraTutorialScene.SAVE_GOAL_DONE,
        EkagraTutorialScene.SAVE_TOPIC, EkagraTutorialScene.SAVE_TOPIC_DONE)
    // Panels keep their feature surface; the timer reveals the shared feature canvas.
    Surface(
        modifier = Modifier.fillMaxSize().testTag("tutorial.presentation"),
        color = if (scene.usesTimerCanvas) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
            when (scene) {
                in settingsScenes -> DurationTab(
                    modifier = Modifier.fillMaxSize(), focusMinutes = 25, breakMinutes = 5,
                    onFocusChange = {}, onBreakChange = {}, isMuted = scene == EkagraTutorialScene.MUTED,
                    onMuteChange = {}, autoStartBreak = scene == EkagraTutorialScene.AUTO_BREAK,
                    onAutoStartBreakChange = {}, timerAlertStyle = TimerAlertStyle.SOUND,
                    onTimerAlertStyleChange = {}, onStartPomodoro = {}, onSave = {},
                    alarmAccessContent = {}, // No Android permission requests or lifecycle reads in practice.
                )
                in historyScenes -> {
                    val sampleName = stringResource(R.string.ekagra_tutorial_sample_name)
                    val sampleTag = stringResource(R.string.ekagra_tutorial_sample_tag)
                    val now = remember { Instant.now() }
                    val analytics = remember(sampleName, sampleTag) {
                        EkagraAnalyticsStats(focusSessions = listOf(
                            EkagraAnalyticsFocusSession(id = "practice-focus", taskText = EkagraTagUtils.formatTaggedTask(sampleTag, sampleName),
                                startedAt = now.minusSeconds(720).toString(), endedAt = now.toString(),
                                durationMinutes = 25, actualMinutes = 12, actualSeconds = 720,
                                status = "ended_early", rawStatus = "ended_early", timerMode = "focus"),
                            EkagraAnalyticsFocusSession(id = "practice-stopwatch", taskText = sampleName,
                                startedAt = now.minusSeconds(300).toString(), endedAt = now.toString(),
                                actualMinutes = 5, actualSeconds = 300, timerMode = "stopwatch"),
                        ))
                    }
                    FocusHistoryContent(Modifier.fillMaxSize(), analytics,
                        ekagraTags = listOf(stringResource(R.string.ekagra_tutorial_sample_tag)))
                }
                in saveScenes -> PracticeSave(demo, isDarkTheme)
                EkagraTutorialScene.SAVE_CONFIRM, EkagraTutorialScene.SAVE_GOAL_CONFIRM, EkagraTutorialScene.SAVE_TOPIC_CONFIRM ->
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                        EkagraConfirmSaveContent(
                            label = stringResource(when (scene) {
                                EkagraTutorialScene.SAVE_GOAL_CONFIRM -> R.string.ekagra_tutorial_sample_goal
                                EkagraTutorialScene.SAVE_TOPIC_CONFIRM -> R.string.ekagra_tutorial_sample_topic
                                else -> R.string.ekagra_quick_save
                            }),
                            onConfirm = {}, onCancel = {},
                            completesTarget = scene != EkagraTutorialScene.SAVE_CONFIRM,
                            linksTopic = scene == EkagraTutorialScene.SAVE_TOPIC_CONFIRM,
                        )
                    }
                EkagraTutorialScene.END_CONFIRM -> PracticeEndConfirmation()
                EkagraTutorialScene.POMODORO_TRADITIONAL, EkagraTutorialScene.POMODORO_CUSTOM -> {
                    Column(Modifier.fillMaxSize().padding(20.dp)) {
                        Text(stringResource(R.string.ekagra_start_pomodoro), style = MaterialTheme.typography.titleLarge)
                        PomodoroSettingsContent(
                            style = if (scene == EkagraTutorialScene.POMODORO_CUSTOM) PomodoroStyle.CUSTOM else PomodoroStyle.TRADITIONAL,
                            focusInput = "25", breakInput = "5", loopsInput = "4", onStyleChange = {},
                            onFocusChange = {}, onBreakChange = {}, onLoopsChange = {},
                            modifier = Modifier.weight(1f).tutorialTarget("ekagra.pomodoroSettings"),
                        )
                    }
                }
                EkagraTutorialScene.MUSIC -> Box(Modifier.fillMaxSize().tutorialTarget("ekagra.music")) {
                    AudioLibraryContent(AudioLibrary.TRACKS.first().id, {}, {}, isDarkTheme = isDarkTheme)
                }
                EkagraTutorialScene.THEME -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Box(Modifier.tutorialTarget("ekagra.theme")) { VisualThemeChoices(selectedTheme, {}) }
                }
                EkagraTutorialScene.KAVACH_SETTINGS -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.ekagra_short_kavach_title), style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.tutorialTarget("ekagra.kavachSettings")) { KavachProfileChoices(false, {}) }
                    KavachControlCenterContainer(blockedAppCount = 0, accent = MaterialTheme.colorScheme.primary,
                        onOpenAppPicker = {}, onGoToEkagra = {})
                }
                EkagraTutorialScene.CIRCLE_JOIN -> CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.ekagra_short_join_title), style = MaterialTheme.typography.titleLarge)
                        Column(Modifier.tutorialTarget("ekagra.circleJoin"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            StudyCircleTopButtons(false, true, {}, {})
                            PublicCircleRow(PublicStudyCircleDto(id = "practice-public", name = stringResource(R.string.ekagra_tutorial_sample_circle), memberCount = 12),
                                rank = 1, enabled = true, busy = false, onJoin = {})
                        }
                    }
                }
                EkagraTutorialScene.CIRCLE_CREATE -> CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        StudyCircleInputContent(true, stringResource(R.string.ekagra_tutorial_sample_circle), "private", false, {}, {}, {}, { _, _ -> })
                    }
                }
                EkagraTutorialScene.KAVACH_APPS -> PracticeAppSelection()
                EkagraTutorialScene.FLOATING_MENU -> PracticeFloatingMenu()
                EkagraTutorialScene.FLOATING_PREVIEW -> PracticeFloatingPreview()
                EkagraTutorialScene.CIRCLE_CHOICES -> PracticeCircleChoices()
                else -> PracticeTimer(demo, isDarkTheme, selectedTheme)
            }
    }
}

@Composable
private fun PracticeTimer(demo: EkagraTutorialDemoState, isDarkTheme: Boolean, selectedTheme: VisualTheme) {
    val circles = if (demo.scene == EkagraTutorialScene.CIRCLES) emptyList() else practiceCircles()
    val accent = MaterialTheme.colorScheme.primary
    val ink = rememberEkagraInk(onCanvas = true, theme = selectedTheme, isDarkTheme = isDarkTheme)
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EkagraTopBar(ink, onOpenDrawer = {}, trailing = {
                com.safarparmar.app.ui.tutorial.TitliGuideButton(onClick = {})
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, stringResource(R.string.common_more_options), tint = ink.primaryText) }
            })
        },
        bottomBar = {
            Box(Modifier.testTag("tutorial.ekagra.navigation")) {
                EkagraBottomNav(EkagraNavTab.TIMER, {}, isOnVideo = true, isDarkTheme = isDarkTheme, selectedTheme = selectedTheme)
            }
        },
    ) { padding ->
        TimerFocusTab(
            modifier = Modifier.fillMaxSize().padding(padding), timerMode = demo.mode,
            selectedDisplayMode = if (demo.mode == TimerMode.POMODORO) TimerMode.FOCUS else demo.mode,
            secondsLeft = demo.seconds, isRunning = demo.running, progress = if (demo.hasProgress) 0.48f else 0f,
            hasProgress = demo.hasProgress, mottoText = "",
            kavachActive = demo.scene == EkagraTutorialScene.KAVACH_ACTIVE, kavachBlockedCount = 0,
            onModeChange = {}, onPlayPause = {}, canStartBreak = demo.running && demo.mode == TimerMode.FOCUS,
            onStartBreak = {}, onReset = {}, shieldState = FocusShieldUiState(
                isEnabled = demo.kavachEnabled, hasUsageStats = true, hasOverlayPermission = true,
                blockedPackages = setOf("practice.social"), isProtectionActive = demo.scene == EkagraTutorialScene.KAVACH_ACTIVE,
            ),
            isDarkTheme = isDarkTheme, selectedTheme = selectedTheme, themeAccent = accent, onToggleKavach = {}, onOpenAppPicker = {}, onNavigate = {},
            showYoutubeBanner = demo.scene == EkagraTutorialScene.YOUTUBE,
            summaryContent = {
                KavachSummaryPillsContent(distractingSeconds = 0, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp), isDarkTheme = isDarkTheme, themeAccent = accent, ink = ink,
                    myCircles = circles, selectedCircle = circles.firstOrNull())
            },
        )
    }
}

@Composable
private fun PracticeSave(demo: EkagraTutorialDemoState, isDarkTheme: Boolean) {
    val isTopic = demo.scene in setOf(EkagraTutorialScene.SAVE_TOPIC, EkagraTutorialScene.SAVE_TOPIC_DONE)
    val isGoal = demo.scene in setOf(EkagraTutorialScene.SAVE_GOAL, EkagraTutorialScene.SAVE_GOAL_DONE)
    val isComplete = demo.scene in setOf(EkagraTutorialScene.SAVE_GOAL_DONE, EkagraTutorialScene.SAVE_TOPIC_DONE)
    val sampleName = stringResource(R.string.ekagra_tutorial_sample_name)
    val sampleGoal = Goal(id = "practice-goal", title = stringResource(R.string.ekagra_tutorial_sample_goal))
    OrganizeFreeFocusContent(
        pending = PendingEndedEkagraSession("practice-session", 1500, 780, "focus", null,
            topicId = if (isTopic) "practice-topic" else null,
            planId = if (isTopic) "practice-plan" else null,
            topicTitle = if (isTopic) stringResource(R.string.ekagra_tutorial_sample_topic) else null),
        todayGoals = listOf(sampleGoal), titleInput = sampleName, onTitleChange = {},
        onSaveFree = {}, onSaveTopic = {}, onLinkGoal = { _, _ -> }, onDiscard = {},
        isDarkTheme = isDarkTheme, availableTags = listOf(stringResource(R.string.ekagra_tutorial_sample_tag)),
        selectedTag = if (!isGoal && !isTopic) stringResource(R.string.ekagra_tutorial_sample_tag) else null,
        initialGoalId = if (isGoal) sampleGoal.id else null,
        initialShowGoals = isGoal, initialMarkComplete = isComplete, scrollWholeContent = true,
    )
}

@Composable
private fun PracticeEndConfirmation() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp, modifier = Modifier.tutorialTarget("ekagra.endConfirm")) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.ekagra_end_session_question), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.ekagra_end_session_body))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {}) { Text(stringResource(R.string.common_cancel)) }
                    EkagraPrimaryAction(stringResource(R.string.common_continue), MaterialTheme.colorScheme.primary, {})
                }
            }
        }
    }
}

@Composable
private fun PracticeAppSelection() {
    val social = stringResource(R.string.ekagra_tutorial_sample_app_one)
    val video = stringResource(R.string.ekagra_tutorial_sample_app_two)
    val apps = remember(social, video) {
        listOf(
            BlockedAppInfo("practice.social", social, null, true),
            BlockedAppInfo("practice.video", video, null, true),
            BlockedAppInfo("practice.browser", "Chrome", null),
            BlockedAppInfo("practice.calendar", "Calendar", null),
            BlockedAppInfo("practice.camera", "Camera", null),
        )
    }
    AppPickerContent(
        apps = apps, searchQuery = "", isLoading = false, onSearchChange = {}, onToggleApp = {},
        onSelectDistracting = {}, onSelectAll = {}, onClearAll = {}, onBack = {}, onSave = {},
        animateRows = false,
    )
}

@Composable
private fun PracticeFloatingMenu() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.End) {
        Text(stringResource(R.string.common_more_options), style = MaterialTheme.typography.titleLarge)
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.ekagra_tutorial_floating_action), modifier = Modifier.tutorialTarget("ekagra.floatingMenu").padding(20.dp))
        }
    }
}

@Composable
private fun PracticeFloatingPreview() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)) {
        Text(stringResource(R.string.ekagra_tutorial_other_apps), style = MaterialTheme.typography.titleLarge)
        Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.CenterEnd).width(160.dp).height(180.dp).tutorialTarget("ekagra.floatingPreview")) {
            EkagraPipOverlay(780, TimerMode.FOCUS, 0.48f, true, false, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun practiceCircles() = listOf(
    StudyCircleSummaryDto(id = "practice-circle", name = stringResource(R.string.ekagra_tutorial_sample_circle), memberCount = 8, focusingCount = 3),
    StudyCircleSummaryDto(id = "practice-circle-two", name = stringResource(R.string.ekagra_tutorial_sample_circle_two), memberCount = 5, focusingCount = 2),
)

@Composable
private fun PracticeCircleChoices() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(R.string.kavach_select_group), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.tutorialTarget("ekagra.circleChoices")) {
            practiceCircles().forEachIndexed { index, circle ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(index == 1, onClick = {})
                    Text(circle.name)
                }
            }
        }
    }
}
