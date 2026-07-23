package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import com.safarparmar.app.domain.model.studyplanner.progressPercentValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import com.safarparmar.app.ui.theme.LoraFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.remainingPoints
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.StudyPlannerTab
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.components.subjectDotColor
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.daysUntil
import com.safarparmar.app.ui.studyplanner.logic.plannerExamCountdownCaption
import com.safarparmar.app.ui.studyplanner.logic.plannerExamCountdownHeroNumber
import com.safarparmar.app.ui.studyplanner.logic.readableDate
import com.safarparmar.app.ui.studyplanner.components.flatCard
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.GlassButton















/**
 * One "how do you like to study" choice card — shared by the Create Plan sheet and the
 * post-creation Build Planner sheet so the same two options look identical everywhere.
 */
@Composable
fun StudyStyleOption(
    title: String,
    body: String,
    selected: Boolean = false,
    recommended: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) scheme.primaryContainer.copy(alpha = 0.5f) else scheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (recommended) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = scheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = "Your usual",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
fun PlanSectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            fontFamily = LoraFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSettingsSheet(
    plan: StudyPlan,
    actions: PlannerActions,
    onExport: (() -> Unit)?,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onExamDateChanged: (String) -> Unit = {},
) {
    var title by remember(plan.id) { mutableStateOf(plan.title) }
    var examType by remember(plan.id) { mutableStateOf(plan.examType.orEmpty()) }
    var examDate by remember(plan.id) { mutableStateOf(plan.examDate?.take(10).orEmpty()) }
    var dailyGoal by remember(plan.id) { mutableStateOf((plan.dailyGoal ?: 3).toString()) }
    var offDays by remember(plan.id) { mutableStateOf(plan.offDays.toSet()) }

    val isPlanScheduled = remember(plan) {
        plan.subjects.isNotEmpty() && plan.subjects.any { s ->
            s.chapters.any { c ->
                c.topics.any { t -> !t.plannedDate.isNullOrBlank() }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = PlannerFlatColors.BgCream,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PlannerFlatColors.PrimaryAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Plan Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PlannerFlatColors.TextDark)
                        Text(
                            "Fine-tune your exam and study routine",
                            style = MaterialTheme.typography.bodySmall,
                            color = PlannerFlatColors.TextMuted,
                        )
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            "EXAM DETAILS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Plan title") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        PlannerExamDateField(examDateIso = examDate, onExamDateChange = { examDate = it })
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            "STUDY LOAD",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        OutlinedTextField(
                            value = dailyGoal,
                            onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) },
                            label = { Text("Topics per day") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        PlanRestDaysRow(
                            selected = offDays,
                            onToggle = { day ->
                                offDays = if (day in offDays) offDays - day else offDays + day
                            },
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val newExamDate = examDate.ifBlank { null }
                        val examDateChanged =
                            newExamDate?.take(10) != plan.examDate?.take(10)
                        actions.updatePlan(
                            UpdatePlanRequest(
                                title = title.trim().ifBlank { plan.title },
                                examType = examType.trim().ifBlank { null },
                                examDate = newExamDate,
                                dailyGoal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                                offDays = offDays.toList(),
                            ),
                        )
                        onDismiss()
                        // A changed exam date invalidates the current schedule, so
                        // launch the guided re-plan (pick order + rebuild) flow.
                        if (examDateChanged && newExamDate != null) {
                            onExamDateChanged(newExamDate)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(PlannerFlatColors.PrimaryAccent, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Text("Save details", fontWeight = FontWeight.Bold)
                }
            }
            if (isPlanScheduled && onExport != null) {
                item {
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, scheme.outlineVariant),
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceTopicSheet(
    currentRef: TopicRef?,
    allRefs: List<TopicRef>,
    today: String,
    onSwap: (currentTopicId: String, replacementTopicId: String) -> Unit,
    onReplace: (currentTopicId: String, replacementTopicId: String, today: String) -> Unit,
    onPull: ((topicId: String) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val scheme = MaterialTheme.colorScheme
    var selectedSubjectKey by remember(currentRef?.topic?.id) { mutableStateOf<String?>(null) }
    var selectedChapterKey by remember(currentRef?.topic?.id) { mutableStateOf<String?>(null) }
    var searchQuery by remember(currentRef?.topic?.id) { mutableStateOf("") }

    // Available replacement candidates: TODO topics not scheduled for today, excluding the current topic
    val candidates = remember(allRefs, today, currentRef?.topic?.id) {
        allRefs.filter { ref ->
            (currentRef == null || ref.topic.id != currentRef.topic.id) &&
            ref.topic.status != TopicStatus.DONE &&
            (ref.topic.plannedDate?.take(10) ?: "") != today
        }
    }

    data class SubjectGroup(val key: String, val name: String, val refs: List<TopicRef>)
    data class ChapterGroup(val key: String, val name: String, val refs: List<TopicRef>)

    val subjectGroups = remember(candidates) {
        candidates
            .groupBy { it.subject.id.ifBlank { it.subject.name } }
            .map { (key, refs) -> SubjectGroup(key, refs.first().subject.name, refs) }
            .sortedBy { it.name.lowercase() }
    }
    val selectedSubject = remember(subjectGroups, selectedSubjectKey) {
        subjectGroups.firstOrNull { it.key == selectedSubjectKey }
    }
    val chapterGroups = remember(selectedSubject) {
        selectedSubject?.refs
            .orEmpty()
            .groupBy { it.chapter.id.ifBlank { it.chapter.name } }
            .map { (key, refs) -> ChapterGroup(key, refs.first().chapter.name, refs) }
            .sortedBy { it.name.lowercase() }
    }
    val selectedChapter = remember(chapterGroups, selectedChapterKey) {
        chapterGroups.firstOrNull { it.key == selectedChapterKey }
    }
    val filteredTopicRefs = remember(selectedChapter, searchQuery) {
        val refs = selectedChapter?.refs.orEmpty().sortedWith(
            compareBy<TopicRef> { it.topic.plannedDate?.take(10).orEmpty() }
                .thenBy { it.topic.name.lowercase() },
        )
        if (searchQuery.isBlank()) refs else {
            val q = searchQuery.lowercase()
            refs.filter { it.topic.name.lowercase().contains(q) }
        }
    }

    fun chooseReplacement(ref: TopicRef) {
        if (currentRef == null) {
            onPull?.invoke(ref.topic.id)
            onDismiss()
            return
        }
        val hasDate = !ref.topic.plannedDate.isNullOrBlank()
        if (hasDate) {
            // Both have dates → swap
            onSwap(currentRef.topic.id, ref.topic.id)
        } else {
            // Replacement is unscheduled → replace
            onReplace(currentRef.topic.id, ref.topic.id, today)
        }
        onDismiss()
    }

    @Composable
    fun StepPill(
        number: String,
        text: String,
        selected: Boolean,
        complete: Boolean,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
    ) {
        Surface(
            modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
            shape = CircleShape,
            color = when {
                selected -> scheme.primaryContainer
                complete -> scheme.secondaryContainer.copy(alpha = 0.68f)
                else -> scheme.surfaceContainerHigh
            },
            contentColor = when {
                selected -> scheme.onPrimaryContainer
                complete -> scheme.onSecondaryContainer
                else -> scheme.onSurfaceVariant
            },
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected || complete) scheme.primary else scheme.outlineVariant,
                    contentColor = if (selected || complete) scheme.onPrimary else scheme.onSurfaceVariant,
                ) {
                    Text(
                        text = number,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    fun SelectionCard(
        title: String,
        subtitle: String,
        badgeText: String,
        onClick: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = scheme.surfaceContainerLowest,
            ),
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (currentRef == null) "Pull topic to today" else "Choose topic to swap",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = scheme.surfaceContainerHigh,
                    tonalElevation = 1.dp,
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Replacing",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            currentRef?.topic?.name.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StepPill("1", "Subject", selectedSubject == null, complete = selectedSubject != null, modifier = Modifier.weight(1f), onClick = {
                    selectedSubjectKey = null
                    selectedChapterKey = null
                    searchQuery = ""
                })
                StepPill("2", selectedSubject?.name ?: "Chapter", selectedSubject != null && selectedChapter == null, complete = selectedChapter != null, modifier = Modifier.weight(1f), onClick = selectedSubject?.let {
                    {
                        selectedChapterKey = null
                        searchQuery = ""
                    }
                })
                StepPill("3", selectedChapter?.name ?: "Topic", selectedChapter != null, complete = false, modifier = Modifier.weight(1f))
            }

            if (candidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No available topics to swap",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (selectedSubject == null) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(subjectGroups, key = { it.key }) { group ->
                        val chapterCount = group.refs.map { it.chapter.id.ifBlank { it.chapter.name } }.distinct().size
                        SelectionCard(
                            title = group.name,
                            subtitle = "$chapterCount chapters • ${group.refs.size} topics available",
                            badgeText = group.refs.size.toString(),
                            onClick = {
                                selectedSubjectKey = group.key
                                selectedChapterKey = null
                                searchQuery = ""
                            },
                        )
                    }
                }
            } else if (selectedChapter == null) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(chapterGroups, key = { it.key }) { group ->
                        SelectionCard(
                            title = group.name,
                            subtitle = "${group.refs.size} topics available in ${selectedSubject.name}",
                            badgeText = group.refs.size.toString(),
                            onClick = {
                                selectedChapterKey = group.key
                                searchQuery = ""
                            },
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search topics in this chapter…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = CircleShape,
                    singleLine = true,
                )

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 460.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (filteredTopicRefs.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = scheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = "No matching topics found in this chapter",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    items(filteredTopicRefs, key = { it.topic.id }) { ref ->
                        val hasDate = !ref.topic.plannedDate.isNullOrBlank()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chooseReplacement(ref) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = scheme.surfaceContainerLowest,
                            ),
                            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasDate) Color(0xFFF59E0B).copy(alpha = if (isDark) 0.28f else 0.16f) else scheme.primaryContainer,
                                    contentColor = if (hasDate) Color(0xFF92400E) else scheme.onPrimaryContainer,
                                ) {
                                    Icon(
                                        imageVector = if (hasDate) Icons.Default.CalendarMonth else Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        modifier = Modifier.padding(8.dp).size(18.dp),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = ref.topic.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = if (hasDate) "Scheduled ${readableDate(ref.topic.plannedDate)}" else "Not Assigned topic",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = if (currentRef == null) Icons.Default.Add else Icons.Default.SwapHoriz,
                                    contentDescription = if (currentRef == null) "Pull" else if (hasDate) "Swap dates" else "Replace",
                                    tint = scheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



/**
 * Shown once, right after a plan is created with "Manual mode", so the user can set which
 * subject to study first. Uses plain up/down reordering rather than drag-and-drop —
 * subject count is small (a handful) and this only needs to happen once per plan, so the
 * extra robustness of arrow buttons over drag gestures is worth the simplicity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSubjectOrderSheet(
    subjects: List<com.safarparmar.app.domain.model.studyplanner.StudySubject>,
    onConfirm: (List<String>) -> Unit,
    onSkip: () -> Unit,
) {
    var ordered by remember(subjects) { mutableStateOf(subjects) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onSkip, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Which subject first?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "You picked Manual mode — set the order you want to study these subjects in. Topics will be built one subject at a time, in this order.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            ordered.forEachIndexed { index, subject ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(18.dp),
                        )
                        Box(Modifier.size(10.dp).clip(CircleShape).background(subjectDotColor(subject.color)))
                        Text(
                            text = subject.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    ordered = ordered.toMutableList().apply {
                                        add(index - 1, removeAt(index))
                                    }
                                }
                            },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(
                            onClick = {
                                if (index < ordered.lastIndex) {
                                    ordered = ordered.toMutableList().apply {
                                        add(index + 1, removeAt(index))
                                    }
                                }
                            },
                            enabled = index < ordered.lastIndex,
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                        }
                    }
                }
            }
            Button(
                onClick = { onConfirm(ordered.map { it.id }) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirm order", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now")
            }
        }
    }
}

@Composable
fun AnimatedCheckCircle(
    isDone: Boolean,
    scheme: androidx.compose.material3.ColorScheme,
    modifier: Modifier = Modifier
) {
    val checkScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "checkScale"
    )
    
    val outerScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDone) 1.05f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "outerScale"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(outerScale),
        contentAlignment = Alignment.Center
    ) {
        // Hollow circle
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 2.dp,
                    color = if (isDone) scheme.primary.copy(alpha = 0.5f) else scheme.outline.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        )
        
        // Checked circle scaling up on top
        if (checkScale > 0f) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Checked",
                tint = scheme.primary,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(checkScale)
            )
        }
    }
}



/**
 * One-time setup shown after a newly-created plan lands on Home. Entries are kept local
 * until the user saves, then persisted through the same plan field used by the Home card
 * and overall-progress calculation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTodoSetupSheet(
    plan: com.safarparmar.app.domain.model.studyplanner.StudyPlan,
    actions: com.safarparmar.app.ui.studyplanner.PlannerActions,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var taskName by remember { mutableStateOf("") }
    var pendingTodos by remember(plan.id) { mutableStateOf(plan.dailyTodos.orEmpty()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun addTask() {
        val name = taskName.trim()
        if (name.isEmpty() || pendingTodos.any { it.name.equals(name, ignoreCase = true) }) return
        pendingTodos = pendingTodos + com.safarparmar.app.domain.model.studyplanner.DailyTodo(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
        )
        taskName = ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Add daily topics?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSurface,
                )
                Text(
                    text = "Add topics you want to do every day. You'll see them on Home.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. Revise vocabulary") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                IconButton(
                    onClick = ::addTask,
                    enabled = taskName.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (taskName.isNotBlank()) scheme.primary else scheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add daily to-do topic",
                        tint = if (taskName.isNotBlank()) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                }
            }

            if (pendingTodos.isEmpty()) {
                Text(
                    text = "No topics yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = pendingTodos,
                        key = { it.id },
                    ) { todo ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.surfaceVariant.copy(alpha = 0.45f),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = todo.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = scheme.onSurface,
                                )
                                IconButton(onClick = { pendingTodos = pendingTodos - todo }) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Remove ${todo.name}",
                                        tint = scheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Not now")
                }
                Button(
                    onClick = {
                        actions.updatePlan(
                            com.safarparmar.app.data.remote.api.UpdatePlanRequest(dailyTodos = pendingTodos),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (pendingTodos.isEmpty()) "Continue" else "Save topics")
                }
            }
        }
    }
}

@Composable
fun DailyTodoSection(
    plan: com.safarparmar.app.domain.model.studyplanner.StudyPlan,
    actions: com.safarparmar.app.ui.studyplanner.PlannerActions,
    todayStr: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !scheme.background.isLightBackground()
    var newTaskName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    val todos = plan.dailyTodos.orEmpty()
    val logs = plan.dailyTodoLogs?.get(todayStr).orEmpty()
    val isAllDone = todos.isNotEmpty() && todos.all { it.id in logs }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlanSectionHeader(
                title = "Daily To-Do List", 
                trailing = "${todos.size} tasks",
                modifier = Modifier.weight(1f)
            )
            if (todos.isNotEmpty()) {
                TextButton(
                    onClick = {
                        if (isAllDone) {
                            actions.updatePlan(
                                com.safarparmar.app.data.remote.api.UpdatePlanRequest(
                                    dailyTodoLogs = plan.dailyTodoLogs.orEmpty() + (todayStr to emptyList())
                                )
                            )
                        } else {
                            actions.updatePlan(
                                com.safarparmar.app.data.remote.api.UpdatePlanRequest(
                                    dailyTodoLogs = plan.dailyTodoLogs.orEmpty() + (todayStr to todos.map { it.id })
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isAllDone) Icons.Default.CheckCircle else Icons.Default.CheckCircle, 
                        contentDescription = null, 
                        tint = if (isAllDone) scheme.primary else scheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isAllDone) "Untick All" else "Tick All", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Add new task input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = newTaskName,
                onValueChange = { newTaskName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Calculation Practice, Vocab, Table Learning", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = scheme.surface,
                    unfocusedContainerColor = scheme.surface
                )
            )
            androidx.compose.material3.IconButton(
                onClick = {
                    if (newTaskName.isNotBlank()) {
                        val newTodo = com.safarparmar.app.domain.model.studyplanner.DailyTodo(
                            id = java.util.UUID.randomUUID().toString(),
                            name = newTaskName.trim()
                        )
                        actions.updatePlan(
                            com.safarparmar.app.data.remote.api.UpdatePlanRequest(
                                dailyTodos = todos + newTodo
                            )
                        )
                        newTaskName = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (newTaskName.isNotBlank()) scheme.primary else scheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                enabled = newTaskName.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Add, 
                    contentDescription = "Add",
                    tint = if (newTaskName.isNotBlank()) scheme.onPrimary else scheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        
        if (todos.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = scheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No daily tasks yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                    Text(
                        text = "Add a few recurring habits you want to track every day.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    todos.forEachIndexed { index, todo ->
                        val isDone = todo.id in logs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newLogsForToday = if (isDone) logs - todo.id else logs + todo.id
                                    actions.updatePlan(
                                        com.safarparmar.app.data.remote.api.UpdatePlanRequest(
                                            dailyTodoLogs = plan.dailyTodoLogs.orEmpty() + (todayStr to newLogsForToday)
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = todo.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isDone) FontWeight.Medium else FontWeight.Normal,
                                color = if (isDone) scheme.onSurface.copy(alpha = 0.5f) else scheme.onSurface,
                                textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedCheckCircle(
                                isDone = isDone,
                                scheme = scheme,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                        if (index < todos.lastIndex) {
                            androidx.compose.material3.HorizontalDivider(
                                color = scheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
