package com.safarparmar.app.ui.studyplanner.create

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.ui.studyplanner.create.steps.BuildingPreviewStep
import com.safarparmar.app.ui.studyplanner.create.steps.ChapterRatingStep
import com.safarparmar.app.ui.studyplanner.create.steps.ChoosePathStep
import com.safarparmar.app.ui.studyplanner.create.steps.DeepFocusOrderStep
import com.safarparmar.app.ui.studyplanner.create.steps.DailyTopicsStep
import com.safarparmar.app.ui.studyplanner.create.steps.ManualTopicTreeStep
import com.safarparmar.app.ui.studyplanner.create.steps.MixedBagSubjectPickerStep
import com.safarparmar.app.ui.studyplanner.create.steps.PasteSyllabusStep
import com.safarparmar.app.ui.studyplanner.create.steps.PlanPreviewStep
import com.safarparmar.app.ui.studyplanner.create.steps.PlanSettingsStep
import com.safarparmar.app.ui.studyplanner.create.steps.TemplatePickerStep
import com.safarparmar.app.ui.theme.SafarSemanticColors
import kotlinx.coroutines.delay

private fun stepTitle(step: CreatePlanStep): String = when (step) {
    CreatePlanStep.ChoosePath -> "New study plan"
    CreatePlanStep.TemplatePicker -> "Templates"
    CreatePlanStep.ManualTopicTree -> "Build it myself"
    CreatePlanStep.PasteSyllabus -> "Paste your syllabus"
    CreatePlanStep.PlanSettings -> "Plan settings"
    CreatePlanStep.ChapterRating -> "Rate your chapters"
    CreatePlanStep.DeepFocusOrder -> "Order your syllabus"
    CreatePlanStep.MixedBagSubjectPicker -> "Mixed Bag"
    CreatePlanStep.BuildingPreview -> "Building your plan"
    CreatePlanStep.Preview -> "Review your plan"
    CreatePlanStep.DailyTopics -> "Daily topics"
}

/**
 * Guided draft → preview → confirm plan-creation wizard. Nothing is saved as a real,
 * visible plan until the user reviews the Preview step and taps "Looks good". Registered
 * as its own nav route with its own ViewModel (not shared with StudyPlannerViewModel) —
 * this flow is self-contained and entirely throwaway if the user backs out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(
    canUsePremiumPlannerFeatures: Boolean,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
    onPlanConfirmed: (String) -> Unit,
    viewModel: CreatePlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCelebration by remember { mutableStateOf(false) }

    // The Preview step is the one place a plan is actually persisted (as a draft) before
    // the user has confirmed anything — so a successful confirm gets a brief celebration
    // beat instead of an instant jump, then hands off to the caller (which lands on Home).
    LaunchedEffect(state.confirmedPlanId) {
        if (state.confirmedPlanId != null) showCelebration = true
    }

    fun handleBack() {
        when (state.step) {
            CreatePlanStep.ChoosePath -> onBack()
            CreatePlanStep.TemplatePicker -> if (state.drillSubjectIndex != null) {
                viewModel.drillBack()
            } else if (state.selectedTemplateId != null) {
                viewModel.clearSelectedTemplate()
            } else {
                viewModel.goToStep(CreatePlanStep.ChoosePath)
            }
            CreatePlanStep.ManualTopicTree, CreatePlanStep.PasteSyllabus -> viewModel.goToStep(CreatePlanStep.ChoosePath)
            CreatePlanStep.PlanSettings -> viewModel.goToStep(
                when (state.source) {
                    PlanSource.Template -> CreatePlanStep.TemplatePicker
                    PlanSource.Manual -> CreatePlanStep.ManualTopicTree
                    PlanSource.Paste -> CreatePlanStep.PasteSyllabus
                    null -> CreatePlanStep.ChoosePath
                },
            )
            CreatePlanStep.ChapterRating -> viewModel.goToStep(CreatePlanStep.PlanSettings)
            CreatePlanStep.DeepFocusOrder -> if (!viewModel.drillBackDeepFocus()) {
                viewModel.goToStep(CreatePlanStep.PlanSettings)
            }
            CreatePlanStep.MixedBagSubjectPicker -> viewModel.goToStep(CreatePlanStep.PlanSettings)
            CreatePlanStep.BuildingPreview -> Unit // wait for the request to resolve
            CreatePlanStep.Preview -> {
                viewModel.discardDraft()
                viewModel.goToStep(CreatePlanStep.PlanSettings)
            }
            CreatePlanStep.DailyTopics -> viewModel.goToStep(CreatePlanStep.Preview)
        }
    }

    BackHandler(enabled = !showCelebration) { handleBack() }

    Scaffold(
        containerColor = SafarSemanticColors.plannerBackground(),
        topBar = {
            if (!showCelebration) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stepTitle(state.step).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { handleBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                )
            }
        },
    ) { padding ->
        if (showCelebration) {
            CelebrationOverlay(
                todayTopicCount = state.previewResult?.calendarPreview?.values?.firstOrNull()?.size ?: 0,
                onFinished = { state.confirmedPlanId?.let(onPlanConfirmed) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val slideDistance = { size: Int -> if (forward) size / 3 else -size / 3 }
                (slideInHorizontally(animationSpec = spring(stiffness = 380f), initialOffsetX = slideDistance) + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(animationSpec = spring(stiffness = 380f)) { -slideDistance(it) } + fadeOut(tween(150)))
            },
            label = "createPlanStep",
        ) { step ->
            when (step) {
                CreatePlanStep.ChoosePath -> ChoosePathStep(
                    onChoose = viewModel::chooseSource,
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.TemplatePicker -> TemplatePickerStep(
                    templates = state.templates,
                    loadingTemplates = state.loadingTemplates,
                    selectedTemplateId = state.selectedTemplateId,
                    templateDetail = state.templateDetail,
                    loadingTemplateDetail = state.loadingTemplateDetail,
                    excludedTopicKeys = state.excludedTopicKeys,
                    excludedTemplateSubjectIndices = state.excludedTemplateSubjectIndices,
                    excludedTemplateChapterKeys = state.excludedTemplateChapterKeys,
                    templateExtraChapters = state.templateExtraChapters,
                    templateExtraTopics = state.templateExtraTopics,
                    templateExtraSubjects = state.templateExtraSubjects,
                    drillSubjectIndex = state.drillSubjectIndex,
                    drillChapter = state.drillChapter,
                    canUsePremiumPlannerFeatures = canUsePremiumPlannerFeatures,
                    onUpgrade = onUpgrade,
                    onSelectTemplate = viewModel::selectTemplate,
                    onDrillIntoSubject = viewModel::drillIntoSubject,
                    onDrillIntoChapter = viewModel::drillIntoChapter,
                    onDrillBack = { viewModel.drillBack() },
                    onToggleTopic = viewModel::toggleExcludedTopic,
                    onRemoveOriginalSubject = viewModel::removeOriginalTemplateSubject,
                    onRemoveOriginalChapter = viewModel::removeOriginalTemplateChapter,
                    onAddTemplateSubject = viewModel::addTemplateSubject,
                    onRemoveTemplateSubject = viewModel::removeTemplateSubject,
                    onAddTemplateSubjectChapter = viewModel::addTemplateSubjectChapter,
                    onRemoveTemplateSubjectChapter = viewModel::removeTemplateSubjectChapter,
                    onAddTemplateSubjectTopic = viewModel::addTemplateSubjectTopic,
                    onRemoveTemplateSubjectTopic = viewModel::removeTemplateSubjectTopic,
                    onAddChapter = viewModel::addTemplateChapter,
                    onRemoveChapter = viewModel::removeTemplateChapter,
                    onAddTopicToNewChapter = viewModel::addTemplateTopicToNewChapter,
                    onRemoveTopicFromNewChapter = viewModel::removeTemplateTopicFromNewChapter,
                    onAddTopic = viewModel::addTemplateTopic,
                    onRemoveTopic = viewModel::removeTemplateTopic,
                    onContinue = { viewModel.goToStep(CreatePlanStep.PlanSettings) },
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.ManualTopicTree -> ManualTopicTreeStep(
                    title = state.title,
                    subjects = state.manualSubjects,
                    validationError = state.manualValidationError,
                    onTitleChange = viewModel::setTitle,
                    onAddSubject = viewModel::addManualSubject,
                    onRemoveSubject = viewModel::removeManualSubject,
                    onAddChapter = viewModel::addManualChapter,
                    onRemoveChapter = viewModel::removeManualChapter,
                    onAddTopic = viewModel::addManualTopic,
                    onRemoveTopic = viewModel::removeManualTopic,
                    onContinue = viewModel::continueFromManual,
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.PasteSyllabus -> PasteSyllabusStep(
                    pasteText = state.pasteText,
                    onPasteTextChange = viewModel::setPasteText,
                    isStructuring = state.isStructuring,
                    structureError = state.structureError,
                    structuredPreview = state.structuredPreview,
                    onStructure = viewModel::structurePaste,
                    onTryAgain = viewModel::clearStructureError,
                    onBuildManually = { viewModel.chooseSource(PlanSource.Manual) },
                    onContinue = { viewModel.goToStep(CreatePlanStep.PlanSettings) },
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.PlanSettings -> PlanSettingsStep(
                    examDate = state.examDate,
                    onExamDateChange = viewModel::setExamDate,
                    offDays = state.offDays,
                    onToggleOffDay = { day ->
                        viewModel.setOffDays(if (day in state.offDays) state.offDays - day else state.offDays + day)
                    },
                    studyStyle = state.studyStyle,
                    onStudyStyleChange = viewModel::setStudyStyle,
                    dailyGoal = state.dailyGoal,
                    onDailyGoalChange = viewModel::setDailyGoal,
                    topicCount = viewModel.currentTopicCount(),
                    subjectCount = viewModel.currentSubjectNames().size,
                    error = state.previewError,
                    premiumRequired = state.premiumRequired,
                    onBuildPlan = viewModel::continueFromSettings,
                    onOpenDeepFocusOrder = viewModel::openDeepFocusOrder,
                    onOpenMixedBagPicker = viewModel::openMixedBagPicker,
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.ChapterRating -> ChapterRatingStep(
                    outline = viewModel.ratingOutline(),
                    ratings = state.chapterRatings,
                    onRate = viewModel::setChapterRating,
                    onSkip = viewModel::skipChapterRating,
                    onContinue = viewModel::buildPreview,
                    studyStyle = state.studyStyle,
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.DeepFocusOrder -> DeepFocusOrderStep(
                    outline = viewModel.deepFocusOutline(),
                    drillSubjectIndex = state.deepFocusDrillSubjectIndex,
                    onDrillIntoSubject = viewModel::drillIntoDeepFocusSubject,
                    onMoveSubject = viewModel::moveDeepFocusSubject,
                    onMoveChapter = viewModel::moveDeepFocusChapter,
                    onMoveTopic = viewModel::moveDeepFocusTopic,
                    onContinue = { viewModel.goToStep(CreatePlanStep.PlanSettings) },
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.MixedBagSubjectPicker -> MixedBagSubjectPickerStep(
                    subjectNames = viewModel.currentSubjectNames(),
                    onConfirm = { names, orderMode ->
                        viewModel.setMixedBagPrioritySubjects(names, orderMode)
                    },
                    onSkip = viewModel::skipMixedBagSplit,
                    modifier = Modifier.padding(padding),
                )
                CreatePlanStep.BuildingPreview -> BuildingPreviewStep(modifier = Modifier.padding(padding))
                CreatePlanStep.Preview -> state.previewResult?.let { preview ->
                    PlanPreviewStep(
                        preview = preview,
                        isConfirming = state.isConfirming,
                        error = state.error,
                        onConfirm = { viewModel.goToStep(CreatePlanStep.DailyTopics) },
                        // Already stretched once — offering it again would do nothing.
                        onScheduleAnyway = if (state.allowOverload) null else viewModel::scheduleAnyway,
                        onAdjust = {
                            viewModel.discardDraft()
                            viewModel.goToStep(CreatePlanStep.PlanSettings)
                        },
                        onRenameTopic = viewModel::renameDraftTopic,
                        modifier = Modifier.padding(padding),
                    )
                }
                CreatePlanStep.DailyTopics -> DailyTopicsStep(
                    isSaving = state.isConfirming,
                    error = state.error,
                    onContinue = viewModel::finishDailyTopics,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

/** Brief celebration beat between "Looks good" and landing on Home — a scale-in
 *  checkmark plus a warm, specific message, not a bare instant navigation. */
@Composable
private fun CelebrationOverlay(
    todayTopicCount: Int,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "celebrationScale",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1400)
        onFinished()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(72.dp).scale(scale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Your SAFAR plan is ready.", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            val topicsLine = if (todayTopicCount > 0) {
                "Start with today's $todayTopicCount topic${if (todayTopicCount == 1) "" else "s"}. You've got this!"
            } else {
                "You've got this!"
            }
            Text(
                topicsLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
