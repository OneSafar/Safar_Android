package com.safar.app.ui.studyplanner.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.theme.isLightBackground
import com.safar.app.ui.studyplanner.ChapterUiModel
import com.safar.app.ui.studyplanner.PlannerActions
import com.safar.app.ui.components.SafarErrorState
import com.safar.app.ui.components.SafarResultSlot
import com.safar.app.ui.components.SyllabusRowSkeleton
import com.safar.app.ui.studyplanner.StudyPlannerViewModel

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

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()

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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF8F9F7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Syllabus > ${currentSubject?.name ?: "Chapters"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSubject?.name ?: "Chapters",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { addChapter = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Chapter",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }

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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onChapterClick),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chapter.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ChapterOverflowMenu(
                                        onRename = onRenameChapter,
                                        onDelete = onDeleteChapter,
                                        onAddTopic = onAddTopic,
                                        onBulkAdd = onBulkAdd,
                                    )
                                }
                                
                                val progressPercent = chapter.completionPercentage / 100f
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progressPercent },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                    )
                                    Text(
                                        text = "${chapter.completionPercentage}% • ${chapter.topicCount} topics",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
}

@Composable
private fun ChapterOverflowMenu(onRename: () -> Unit, onDelete: () -> Unit, onAddTopic: () -> Unit, onBulkAdd: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
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
