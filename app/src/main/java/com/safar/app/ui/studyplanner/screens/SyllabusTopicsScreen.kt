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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.domain.model.studyplanner.TopicStatus
import com.safar.app.ui.studyplanner.PlannerActions
import com.safar.app.ui.studyplanner.StudyPlannerViewModel
import com.safar.app.ui.studyplanner.TopicUiModel
import com.safar.app.ui.theme.isLightBackground
import com.safar.app.ui.components.SafarErrorState
import com.safar.app.ui.components.SafarResultSlot
import com.safar.app.ui.components.SyllabusRowSkeleton
import com.safar.app.ui.studyplanner.logic.TopicRef

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusTopicsScreen(
    viewModel: StudyPlannerViewModel,
    planId: String,
    subjectId: String,
    chapterId: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val actions: PlannerActions = viewModel

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()

    var addTopic by remember { mutableStateOf(false) }
    var renameTopic by remember { mutableStateOf<TopicUiModel?>(null) }
    var deleteTopic by remember { mutableStateOf<TopicUiModel?>(null) }
    
    // For opening TopicDetailSheet
    var selectedTopicRef by remember { mutableStateOf<TopicRef?>(null) }
    var detailNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(planId) {
        if (state.selectedPlan?.id != planId) {
            viewModel.openPlan(planId)
        }
    }
    
    LaunchedEffect(subjectId) {
        viewModel.selectSubject(subjectId)
    }
    
    LaunchedEffect(chapterId) {
        viewModel.selectChapter(chapterId)
    }

    val currentSubject = subjects.find { it.id == subjectId }
    val currentChapter = chapters.find { it.id == chapterId }
    
    val currentSubjectRaw = state.selectedPlan?.subjects?.find { it.id == subjectId }
    val currentChapterRaw = currentSubjectRaw?.chapters?.find { it.id == chapterId }

    if (addTopic) TextInputDialog("Add Topic", "Topic name", onDismiss = { addTopic = false }) { actions.addTopic(subjectId, chapterId, it); addTopic = false }
    renameTopic?.let { topic -> TextInputDialog("Rename Topic", topic.name, onDismiss = { renameTopic = null }) { actions.updateTopic(topic.id, name = it); renameTopic = null } }
    deleteTopic?.let { topic -> ConfirmActionDialog("Delete topic?", "This will delete ${topic.name}.", { deleteTopic = null }) { actions.deleteTopic(topic.id); deleteTopic = null } }

    selectedTopicRef?.let { ref ->
        PlannerTopicDetailSheet(ref, detailNonce, actions, onDismiss = { selectedTopicRef = null })
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
                            text = "${currentSubject?.name ?: "Subject"} > ${currentChapter?.name ?: "Chapter"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentChapter?.name ?: "Topics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { addTopic = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Topic",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            if (state.error != null && topics.isEmpty() && !state.loading) {
                SafarResultSlot(modifier = Modifier.fillMaxSize()) {
                    SafarErrorState(message = state.error!!, onRetry = { actions.refreshPlans() })
                }
            } else if (state.loading && topics.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(6) {
                        SyllabusRowSkeleton()
                    }
                }
            } else if (topics.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyPlannerCard(
                        title = "No topics yet",
                        body = "Add topics to start making progress.",
                        action = "Add Topic",
                        onAction = { addTopic = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                items(topics, key = { it.id }) { topic ->
                    val isDone = topic.status == TopicStatus.DONE
                    val plannedDate = remember(topic.plannedDate) { topic.plannedDate?.take(10) }
                    val onTopicClick = remember(topic.id, currentSubjectRaw, currentChapterRaw) {
                        {
                            if (currentSubjectRaw != null && currentChapterRaw != null) {
                                val topicRaw = currentChapterRaw.topics.find { it.id == topic.id }
                                if (topicRaw != null) {
                                    selectedTopicRef = TopicRef(currentSubjectRaw, currentChapterRaw, topicRaw)
                                    detailNonce++
                                }
                            }
                        }
                    }
                    val onRenameTopic = remember(topic) { { renameTopic = topic } }
                    val onDeleteTopic = remember(topic) { { deleteTopic = topic } }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTopicClick),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusDot(topic.status)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                                if (plannedDate != null) {
                                    Text(
                                        text = "Scheduled: $plannedDate",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TopicOverflowMenu(
                                onRename = onRenameTopic,
                                onDelete = onDeleteTopic,
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun TopicOverflowMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { expanded = false; onDelete() })
        }
    }
}
