package com.safarparmar.app.ui.studyplanner.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarResultSlot
import com.safarparmar.app.ui.components.SyllabusRowSkeleton
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerUiState
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.SubjectUiModel

internal sealed interface SyllabusDialogState {
    object Closed : SyllabusDialogState
    object AddSubject : SyllabusDialogState
    data class RenameSubject(val subject: SubjectUiModel) : SyllabusDialogState
    data class DeleteSubject(val subject: SubjectUiModel) : SyllabusDialogState
    data class AddChapter(val subject: SubjectUiModel) : SyllabusDialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusSubjectsScreen(
    viewModel: StudyPlannerViewModel,
    planId: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onPlannerSectionSelect: (PlannerSection) -> Unit,
    showBottomBar: Boolean = true,
) {
    val premiumViewModel: com.safarparmar.app.ui.premium.PremiumViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val actions: PlannerActions = viewModel

    var dialogState by remember { mutableStateOf<SyllabusDialogState>(SyllabusDialogState.Closed) }

    BackHandler(onBack = onBack)

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
    }

    when (val ds = dialogState) {
        is SyllabusDialogState.AddSubject -> {
            TextInputDialog("Add Subject", "Subject name", onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                actions.addSubject(it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.RenameSubject -> {
            TextInputDialog("Rename Subject", ds.subject.name, onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                actions.renameSubject(ds.subject.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.DeleteSubject -> {
            ConfirmActionDialog("Delete subject?", "This will delete ${ds.subject.name}.", { dialogState = SyllabusDialogState.Closed }) {
                actions.deleteSubject(ds.subject.id)
                dialogState = SyllabusDialogState.Closed
            }
        }
        is SyllabusDialogState.AddChapter -> {
            TextInputDialog("Add Chapter", "Chapter name", onDismiss = { dialogState = SyllabusDialogState.Closed }) {
                actions.addChapter(ds.subject.id, it)
                dialogState = SyllabusDialogState.Closed
            }
        }
        SyllabusDialogState.Closed -> {}
    }

    val selectedPlan = state.selectedPlan
    val rawSubjects = selectedPlan?.subjects.orEmpty()
    val totalTopics = rawSubjects.sumOf { subject -> subject.chapters.sumOf { it.topics.size } }
    val totalChapters = rawSubjects.sumOf { it.chapters.size }
    val doneTopics = rawSubjects.sumOf { subject ->
        subject.chapters.sumOf { chapter -> chapter.topics.count { it.status == TopicStatus.DONE } }
    }
    val planProgress = if (totalTopics > 0) (doneTopics * 100) / totalTopics else 0
    val isTemplatePlan = !selectedPlan?.templateId.isNullOrBlank()
    val shouldShowFullImport = rawSubjects.isEmpty() && !isTemplatePlan

    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
            ),
            bottomBar = {
                if (showBottomBar) {
                    PlannerBottomBar(
                        selected = PlannerSection.SYLLABUS,
                        onSelect = onPlannerSectionSelect,
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { dialogState = SyllabusDialogState.AddSubject },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    state.error != null && subjects.isEmpty() && !state.loading -> {
                        SafarResultSlot(modifier = Modifier.fillMaxSize()) {
                            SafarErrorState(message = state.error!!, onRetry = { actions.refreshPlans() })
                        }
                    }
                    state.loading && subjects.isEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(4) { SyllabusRowSkeleton() }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item {
                                SyllabusOverviewCard(
                                    planTitle = selectedPlan?.title.orEmpty().ifBlank { "Study Plan" },
                                    examType = selectedPlan?.examType,
                                    progress = planProgress,
                                    subjectCount = rawSubjects.size,
                                    chapterCount = totalChapters,
                                    topicCount = totalTopics,
                                    isTemplatePlan = isTemplatePlan,
                                )
                            }

                            item {
                                SyllabusQuickActions(
                                    onAddSubject = { dialogState = SyllabusDialogState.AddSubject },
                                    onBuildPlanner = { actions.autoDistribute(includeRevision = false, lockExisting = true) },
                                    canBuildPlanner = rawSubjects.isNotEmpty(),
                                )
                            }

                            if (shouldShowFullImport) {
                                item {
                                    SyllabusFullImportCard(
                                        state = state,
                                        actions = actions,
                                        canUseAiImport = premiumStatus.canUseStudyPlannerInsights,
                                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                                    )
                                }
                            }

                            item {
                                SyllabusSectionHeader(
                                    title = if (isTemplatePlan) "Template syllabus" else "Your syllabus",
                                    subtitle = "$totalChapters chapters • $totalTopics topics",
                                )
                            }

                            if (rawSubjects.isEmpty()) {
                                item {
                                    SyllabusEmptySubjectsCard(
                                        onAddSubject = { dialogState = SyllabusDialogState.AddSubject },
                                    )
                                }
                            } else {
                                items(rawSubjects, key = { it.id }) { subject ->
                                    val subjectUi = subjects.firstOrNull { it.id == subject.id }
                                    SyllabusSubjectListCard(
                                        subject = subject,
                                        subjectUi = subjectUi,
                                        onClick = {
                                            viewModel.selectSubject(subject.id)
                                            onNavigate(
                                                Routes.ROUTE_SYLLABUS_CHAPTERS
                                                    .replace("{planId}", planId)
                                                    .replace("{subjectId}", subject.id),
                                            )
                                        },
                                        onRename = {
                                            subjectUi?.let { dialogState = SyllabusDialogState.RenameSubject(it) }
                                        },
                                        onDelete = {
                                            subjectUi?.let { dialogState = SyllabusDialogState.DeleteSubject(it) }
                                        },
                                        onAddChapter = {
                                            subjectUi?.let { dialogState = SyllabusDialogState.AddChapter(it) }
                                        },
                                    )
                                }
                            }

                            if (!shouldShowFullImport) {
                                item {
                                    SyllabusImportTray(
                                        isTemplatePlan = isTemplatePlan,
                                        state = state,
                                        actions = actions,
                                        canUseAiImport = premiumStatus.canUseStudyPlannerInsights,
                                        onUpgrade = { onNavigate(Routes.PREMIUM) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusOverviewCard(
    planTitle: String,
    examType: String?,
    progress: Int,
    subjectCount: Int,
    chapterCount: Int,
    topicCount: Int,
    isTemplatePlan: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = planTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(examType?.takeIf { it.isNotBlank() }, if (isTemplatePlan) "Predefined template" else "Custom syllabus").joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.primary,
                )
            }

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyllabusMetaPill(label = "Subjects", value = subjectCount.toString(), modifier = Modifier.weight(1f))
                SyllabusMetaPill(label = "Chapters", value = chapterCount.toString(), modifier = Modifier.weight(1f))
                SyllabusMetaPill(label = "Topics", value = topicCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SyllabusMetaPill(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = scheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SyllabusQuickActions(
    onAddSubject: () -> Unit,
    onBuildPlanner: () -> Unit,
    canBuildPlanner: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onAddSubject,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Subject", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
            onClick = onBuildPlanner,
            enabled = canBuildPlanner,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Build Planner", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SyllabusImportTray(
    isTemplatePlan: Boolean,
    state: StudyPlannerUiState,
    actions: PlannerActions,
    canUseAiImport: Boolean,
    onUpgrade: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isTemplatePlan) "Need to update this syllabus?" else "Import or update syllabus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = if (isTemplatePlan) "Open only when you want to add or replace template topics." else "Paste syllabus text when you want to merge or replace topics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Box(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    SyllabusFullImportCard(
                        state = state,
                        actions = actions,
                        canUseAiImport = canUseAiImport,
                        onUpgrade = onUpgrade,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllabusSectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SyllabusEmptySubjectsCard(onAddSubject: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, scheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = scheme.outline,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Subjects Added",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = onAddSubject,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Text("Add Manually", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SyllabusSubjectListCard(
    subject: StudySubject,
    subjectUi: SubjectUiModel?,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddChapter: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val chapterCount = subject.chapters.size
    val topicCount = subject.chapters.sumOf { it.topics.size }
    val doneTopics = subject.chapters.sumOf { chapter -> chapter.topics.count { it.status == TopicStatus.DONE } }
    val progressPercent = if (topicCount > 0) doneTopics.toFloat() / topicCount else 0f
    val completionPercentage = if (topicCount > 0) (doneTopics * 100) / topicCount else subjectUi?.completionPercentage ?: 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SubjectInitialBadge(subject.name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "$chapterCount chapters • $topicCount topics",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                SubjectOverflowMenu(
                    onRename = onRename,
                    onDelete = onDelete,
                    onAddChapter = onAddChapter,
                )
            }
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceContainerHighest,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$completionPercentage% complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.onSurfaceVariant,
                )
            }
            if (subject.chapters.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainerLow)
                        .padding(vertical = 6.dp),
                ) {
                    subject.chapters.take(3).forEachIndexed { index, chapter ->
                        SyllabusChapterPreviewRow(
                            chapter = chapter,
                            showDivider = index < minOf(subject.chapters.size, 3) - 1,
                        )
                    }
                    if (subject.chapters.size > 3) {
                        Text(
                            text = "+${subject.chapters.size - 3} more chapters",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectInitialBadge(name: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primaryContainer.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "S" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SyllabusChapterPreviewRow(chapter: StudyChapter, showDivider: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val totalTopics = chapter.topics.size
    val doneTopics = chapter.topics.count { it.status == TopicStatus.DONE }
    val percent = if (totalTopics > 0) (doneTopics * 100) / totalTopics else 0
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (percent == 100 && totalTopics > 0) Icons.Default.CheckCircle else Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (percent == 100 && totalTopics > 0) scheme.primary else scheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$totalTopics topics • $percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 42.dp, end = 14.dp),
                color = scheme.outlineVariant.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun SubjectOverflowMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddChapter: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Add Chapter") }, onClick = { expanded = false; onAddChapter() })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}
