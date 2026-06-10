package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.safarparmar.app.domain.model.studyplanner.PlannerSection
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarResultSlot
import com.safarparmar.app.ui.components.SyllabusRowSkeleton
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.ui.studyplanner.SubjectUiModel
import com.safarparmar.app.ui.studyplanner.plan.PlanActionRow

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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val actions: PlannerActions = viewModel

    var dialogState by remember { mutableStateOf<SyllabusDialogState>(SyllabusDialogState.Closed) }

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
        viewModel.setSection(PlannerSection.SYLLABUS)
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

    val totalTopics = subjects.sumOf { it.topicCount }

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
                PlannerBottomBar(
                    selected = PlannerSection.SYLLABUS,
                    onSelect = onPlannerSectionSelect,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SyllabusScreenTopBar(
                    onBack = onBack,
                    subtitle = state.selectedPlan?.title?.takeIf { it.isNotBlank() },
                )

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
                                top = 8.dp,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            item {
                                SyllabusFullImportCard(state = state, actions = actions)
                            }

                            item {
                                PlanActionRow(
                                    onAddTopics = { dialogState = SyllabusDialogState.AddSubject },
                                    onSchedule = { actions.autoDistribute(includeRevision = false, lockExisting = true) },
                                )
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Subjects",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${subjects.size} subjects • $totalTopics topics",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (subjects.isEmpty()) {
                                item {
                                    SyllabusEmptySubjectsCard(
                                        onAddSubject = { dialogState = SyllabusDialogState.AddSubject },
                                    )
                                }
                            } else {
                                items(subjects, key = { it.id }) { subject ->
                                    SyllabusSubjectListCard(
                                        subject = subject,
                                        onClick = {
                                            viewModel.selectSubject(subject.id)
                                            onNavigate(
                                                Routes.ROUTE_SYLLABUS_CHAPTERS
                                                    .replace("{planId}", planId)
                                                    .replace("{subjectId}", subject.id),
                                            )
                                        },
                                        onRename = { dialogState = SyllabusDialogState.RenameSubject(subject) },
                                        onDelete = { dialogState = SyllabusDialogState.DeleteSubject(subject) },
                                        onAddChapter = { dialogState = SyllabusDialogState.AddChapter(subject) },
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
private fun SyllabusScreenTopBar(
    onBack: () -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Syllabus",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
    subject: SubjectUiModel,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddChapter: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                SubjectOverflowMenu(
                    onRename = onRename,
                    onDelete = onDelete,
                    onAddChapter = onAddChapter,
                )
            }
            val progressPercent = subject.completionPercentage / 100f
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant,
            )
            Text(
                text = "${subject.completionPercentage}% complete • ${subject.topicCount} topics",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
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
