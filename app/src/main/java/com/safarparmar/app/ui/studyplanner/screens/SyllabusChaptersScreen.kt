package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.ChapterUiModel
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.SafarResultSlot
import com.safarparmar.app.ui.components.SyllabusRowSkeleton
import com.safarparmar.app.ui.components.SafarExpressiveHeader
import com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel
import com.safarparmar.app.domain.model.studyplanner.StudyChapter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusChaptersScreen(
    viewModel: StudyPlannerViewModel,
    planId: String,
    subjectId: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val actions: PlannerActions = viewModel

    var addChapter by remember { mutableStateOf(false) }
    var renameChapter by remember { mutableStateOf<ChapterUiModel?>(null) }
    var deleteChapter by remember { mutableStateOf<ChapterUiModel?>(null) }
    var addTopicFor by remember { mutableStateOf<ChapterUiModel?>(null) }
    var bulkFor by remember { mutableStateOf<ChapterUiModel?>(null) }

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
    }
    
    LaunchedEffect(subjectId) {
        viewModel.selectSubject(subjectId)
    }

    val currentSubject = subjects.find { it.id == subjectId }
    val currentSubjectRaw = state.selectedPlan?.subjects?.find { it.id == subjectId }

    if (addChapter) TextInputDialog("Add Chapter", "Chapter name", onDismiss = { addChapter = false }) { actions.addChapter(subjectId, it); addChapter = false }
    renameChapter?.let { chapter -> TextInputDialog("Rename Chapter", chapter.name, onDismiss = { renameChapter = null }) { actions.renameChapter(subjectId, chapter.id, it); renameChapter = null } }
    deleteChapter?.let { chapter -> ConfirmActionDialog("Delete chapter?", "This will delete ${chapter.name}.", { deleteChapter = null }) { actions.deleteChapter(subjectId, chapter.id); deleteChapter = null } }
    addTopicFor?.let { chapter -> TextInputDialog("Add Topic", "Topic name", onDismiss = { addTopicFor = null }) { actions.addTopic(subjectId, chapter.id, it); addTopicFor = null } }
    
    if (bulkFor != null && currentSubjectRaw != null) {
        val chapterRaw = currentSubjectRaw.chapters.find { it.id == bulkFor?.id }
        if (chapterRaw != null) {
            BulkAddSheet(Pair(currentSubjectRaw, chapterRaw), state, actions, onDismiss = { bulkFor = null })
        } else {
            bulkFor = null
        }
    }

    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { addChapter = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Chapter")
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SafarExpressiveHeader(
                    title = currentSubject?.name ?: "Chapters",
                    subtitle = "Syllabus > Chapters",
                    onBackClick = onBack
                )

                if (state.error != null && chapters.isEmpty() && !state.loading) {
                    SafarResultSlot(modifier = Modifier.fillMaxSize()) {
                        SafarErrorState(message = state.error!!, onRetry = { actions.refreshPlans() })
                    }
                } else if (state.loading && chapters.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(5) {
                            SyllabusRowSkeleton()
                        }
                    }
                } else if (chapters.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyPlannerCard(
                            title = "No chapters yet",
                            body = "Add your first chapter to organize the syllabus.",
                            action = "Add Chapter",
                            onAction = { addChapter = true }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chapters, key = { it.id }) { chapter ->
                            val onChapterClick = remember(chapter.id, planId, subjectId, onNavigate) {
                                {
                                    viewModel.selectChapter(chapter.id)
                                    onNavigate(
                                        Routes.ROUTE_SYLLABUS_TOPICS
                                            .replace("{planId}", planId)
                                            .replace("{subjectId}", subjectId)
                                            .replace("{chapterId}", chapter.id)
                                    )
                                }
                            }
                            val onRenameChapter = remember(chapter) { { renameChapter = chapter } }
                            val onDeleteChapter = remember(chapter) { { deleteChapter = chapter } }
                            val onAddTopic = remember(chapter) { { addTopicFor = chapter } }
                            val onBulkAdd = remember(chapter) { { bulkFor = chapter } }
                            SyllabusChapterListCard(
                                chapter = chapter,
                                rawChapter = currentSubjectRaw?.chapters?.find { it.id == chapter.id },
                                onClick = onChapterClick,
                                onRename = onRenameChapter,
                                onDelete = onDeleteChapter,
                                onAddTopic = onAddTopic,
                                onBulkAdd = onBulkAdd,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyllabusChapterListCard(
    chapter: ChapterUiModel,
    rawChapter: StudyChapter?,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddTopic: () -> Unit,
    onBulkAdd: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val completion = chapter.completionPercentage
    val progressPercent = completion / 100f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val badgeBg = when {
                    completion == 100 -> scheme.primaryContainer.copy(alpha = 0.72f)
                    completion > 0 -> scheme.secondaryContainer.copy(alpha = 0.64f)
                    else -> scheme.surfaceContainerHigh
                }
                val iconColor = when {
                    completion == 100 -> scheme.onPrimaryContainer
                    completion > 0 -> scheme.onSecondaryContainer
                    else -> scheme.onSurfaceVariant
                }
                val badgeIcon = when {
                    completion == 100 -> Icons.Default.Check
                    completion > 0 -> Icons.Default.Book
                    else -> Icons.AutoMirrored.Filled.MenuBook
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = chapter.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${chapter.topicCount} topics",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                ChapterOverflowMenu(
                    onRename = onRename,
                    onDelete = onDelete,
                    onAddTopic = onAddTopic,
                    onBulkAdd = onBulkAdd,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = scheme.primary,
                    trackColor = scheme.surfaceContainerHighest,
                )
                Text(
                    text = "$completion%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (completion == 100) scheme.primary else scheme.onSurfaceVariant,
                )
            }

            rawChapter?.topics?.take(3)?.takeIf { it.isNotEmpty() }?.let { topics ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainerLow)
                        .padding(vertical = 5.dp),
                ) {
                    topics.forEachIndexed { index, topic ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusDot(topic.status)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = topic.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (index < topics.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 36.dp, end = 14.dp),
                                color = scheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                    if (rawChapter.topics.size > 3) {
                        Text(
                            text = "+${rawChapter.topics.size - 3} more topics",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Open topics",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Navigate to topics",
                    tint = scheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ChapterOverflowMenu(onRename: () -> Unit, onDelete: () -> Unit, onAddTopic: () -> Unit, onBulkAdd: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Add Topic") }, onClick = { expanded = false; onAddTopic() })
            DropdownMenuItem(text = { Text("Bulk Add Topics") }, onClick = { expanded = false; onBulkAdd() })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { expanded = false; onDelete() })
        }
    }
}
