package com.safarparmar.app.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.safarparmar.app.ui.components.popups.SafarMaterialAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.ui.*
import com.safarparmar.app.feature.toppersbatch.BatchPracticeStage
import com.safarparmar.app.feature.toppersbatch.BatchPracticeTheme
import com.safarparmar.app.ui.dhyan.DhyanPracticeStage
import com.safarparmar.app.ui.mehfil.MehfilFlatColors
import com.safarparmar.app.ui.mehfil.MehfilPracticeStage
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The only persistence available to tutorials is versioned completion bookkeeping. */
@HiltViewModel
internal class ScreenPracticeProgressViewModel @Inject constructor(val dataStore: SafarDataStore) : ViewModel()

@Composable
internal fun ScreenPracticeEntry(feature: TitliFeature, isDarkTheme: Boolean, eligible: Boolean = true) {
    if (!com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED) return
    val model: ScreenPracticeViewModel = viewModel(key = "practice.${feature.name}")
    val progress: ScreenPracticeProgressViewModel = hiltViewModel()
    val store = progress.dataStore
    val key = "practice.${feature.name.lowercase(java.util.Locale.ROOT)}"
    var busy by remember { mutableStateOf(false) }
    fun start() { if (eligible) model.start(PracticeLesson.forFeature(feature).first()) else busy = true }
    fun lessons() { if (eligible) model.open() else busy = true }
    RegisterTitliGuide(::start, ::lessons, feature, ready = eligible)
    val completed by remember(store, key) { store.featureTutorialCompletedLessons(key, 1) }
        .collectAsStateWithLifecycle(initialValue = emptySet())
    LaunchedEffect(model.isOpen, eligible) {
        if (model.isOpen && !eligible) { model.close(); busy = true }
        if (model.isOpen && eligible) store.setFeatureTutorialInvitationSeen(key, 1)
    }
    LaunchedEffect(model.completedLesson) {
        model.completedLesson?.let { store.completeFeatureTutorialLesson(key, 1, it.name); model.acknowledgeCompletion() }
    }
    if (busy) AlertDialog(onDismissRequest = { busy = false }, title = { Text(stringResource(feature.practiceTitle())) },
        text = { Text(stringResource(R.string.practice_busy)) },
        confirmButton = { TextButton(onClick = { busy = false }) { Text(stringResource(R.string.feature_tutorial_finish)) } })
    if (model.isOpen && eligible) ScreenPracticeTutorial(feature, isDarkTheme, model, completed)
}

@Composable
internal fun ScreenPracticeTutorial(feature: TitliFeature, isDarkTheme: Boolean, model: ScreenPracticeViewModel, completed: Set<String> = emptySet()) {
    ScreenPracticeTheme(feature, isDarkTheme) {
        CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme, LocalHabitDarkTheme provides isDarkTheme) {
            val background = when (feature) {
                TitliFeature.HABITS -> HabitColors.Background
                TitliFeature.MEHFIL -> MehfilFlatColors.Bg
                TitliFeature.DHYAN -> com.safarparmar.app.ui.dhyan.DhyanFlatColors.Bg
                TitliFeature.STUDY_CIRCLE, TitliFeature.CHECK_IN, TitliFeature.JOURNAL, TitliFeature.STREAKS, TitliFeature.ANALYTICS -> com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors.BgCream
                TitliFeature.EXAM_PLANNER -> com.safarparmar.app.ui.theme.SafarSemanticColors.plannerBackground()
                else -> MaterialTheme.colorScheme.background
            }
            FeatureTutorialPracticeWindow(titleRes = feature.practiceTitle(), practiceLabelRes = R.string.goals_tutorial_practice,
                isDarkTheme = isDarkTheme, onExit = model::close, practiceTag = "practice.${feature.name}",
                labelTag = "practice.label", closeTag = "practice.close", showHeader = model.state == null,
                backgroundColor = background, onHelp = model::open) {
                val state = model.state
                if (state == null) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.practice_choose), style = MaterialTheme.typography.headlineSmall)
                        PracticeLesson.forFeature(feature).forEach { lesson ->
                            OutlinedCard(onClick = { model.start(lesson) }, modifier = Modifier.fillMaxWidth().testTag("practice.lesson.${lesson.name}")) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(lesson.title), style = MaterialTheme.typography.titleMedium)
                                    if (lesson.name in completed) Text(stringResource(R.string.ekagra_tutorial_completed), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        TextButton(onClick = model::close, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.feature_tutorial_finish)) }
                    }
                } else FeatureTutorialHost(state, model::complete, model::close,
                    modifier = Modifier.weight(1f).background(background), onHelp = model::open,
                    practiceLabelRes = R.string.goals_tutorial_practice, practiceLabelTag = "practice.label", closeTag = "practice.close") { step ->
                    ScreenPracticeStage(feature, PracticeScene.valueOf(step.id), isDarkTheme)
                }
            }
        }
    }
}

@Composable
private fun ScreenPracticeTheme(feature: TitliFeature, dark: Boolean, content: @Composable () -> Unit) {
    if (feature == TitliFeature.TOPPERS_BATCH) BatchPracticeTheme(dark, content) else content()
}

/** Only sample data and model-free presentation functions enter this boundary. */
@Composable
internal fun ScreenPracticeStage(feature: TitliFeature, scene: PracticeScene, dark: Boolean) {
    when (feature) {
        TitliFeature.TOPPERS_BATCH -> BatchPracticeTheme(dark) { BatchPracticeStage(scene) }
        TitliFeature.HABITS -> HabitPracticeStage(scene)
        TitliFeature.MEHFIL -> MehfilPracticeStage(scene)
        TitliFeature.DHYAN -> DhyanPracticeStage(scene, dark)
        TitliFeature.STUDY_CIRCLE -> com.safarparmar.app.ui.studycircle.StudyCirclePracticeStage(scene)
        TitliFeature.CHECK_IN, TitliFeature.JOURNAL, TitliFeature.STREAKS, TitliFeature.ANALYTICS -> com.safarparmar.app.ui.nishtha.NishthaPracticeStage(feature, scene, dark)
        TitliFeature.EXAM_PLANNER -> com.safarparmar.app.ui.studyplanner.screens.PlannerPracticeStage(scene, dark)
        else -> error("Unsupported practice feature: $feature")
    }
}
