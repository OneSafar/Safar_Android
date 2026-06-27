package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.data.remote.api.UpdatePlanRequest
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safarparmar.app.ui.studyplanner.components.PlannerExamDateField
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.daysUntil
import com.safarparmar.app.ui.studyplanner.logic.plannerExamCountdownCaption
import com.safarparmar.app.ui.studyplanner.logic.plannerExamCountdownHeroNumber
import com.safarparmar.app.ui.studyplanner.logic.readableDate

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlanStatusCard(
    plan: StudyPlan,
    progress: PlanProgress,
    onExportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    filters: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    var menuExpanded by remember { mutableStateOf(false) }

    val isPlanScheduled = remember(plan) {
        plan.subjects.isNotEmpty() && plan.subjects.any { s ->
            s.chapters.any { c ->
                c.topics.any { t -> !t.plannedDate.isNullOrBlank() }
            }
        }
    }
    
    val cardModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            modifier
                .fillMaxWidth()
                .sharedElement(
                    state = rememberSharedContentState(key = "study-plan-card:${plan.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
        }
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Banner Gradient Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            } else {
                                listOf(Color(0xFF0A1931), Color(0xFF15305B))
                            }
                        ),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 24.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Circle Progress Box
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                            )
                        }
                        CircularProgressIndicator(
                            progress = { progress.completionPercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF3B82F6),
                            strokeWidth = 6.dp,
                            trackColor = Color.Transparent
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${progress.completionPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Completed",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Countdown Box
                    val examDays = daysUntil(plan.examDate)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = plannerExamCountdownHeroNumber(examDays),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF59E0B),
                            fontSize = 28.sp
                        )
                        Text(
                            text = plannerExamCountdownCaption(examDays),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (examDays == null || examDays <= 0L) "" else "Until Exam",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Menu Options inside white icon
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Plan options",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Plan settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
                                },
                            )
                            if (isPlanScheduled) {
                                DropdownMenuItem(
                                    text = { Text("Export PDF") },
                                    leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onExportClick()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        filters()
    }
}

@Composable
fun PlanTabFiltersRow(
    activeTab: StudyPlannerTab,
    onTabSelected: (StudyPlannerTab) -> Unit,
    todayCount: Int,
    overdueCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudyPlannerTab.entries.forEach { tab ->
            val isSelected = activeTab == tab
            val count = when (tab) {
                StudyPlannerTab.TODAY -> todayCount
                StudyPlannerTab.OVERDUE -> overdueCount
                StudyPlannerTab.UPCOMING -> upcomingCount
                StudyPlannerTab.COMPLETED -> completedCount
            }
            val tabBg = if (isSelected) {
                Brush.horizontalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                    } else {
                        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    },
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    ),
                )
            }
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val border = if (isSelected) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = when (tab) {
                        StudyPlannerTab.TODAY -> "Today"
                        StudyPlannerTab.OVERDUE -> "Overdue"
                        StudyPlannerTab.UPCOMING -> "Upcoming"
                        StudyPlannerTab.COMPLETED -> "Done"
                    },
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tabBg)
                        .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun TodayMissionCard(
    topics: List<TopicRef>,
    onTopicClick: (TopicRef) -> Unit,
    onTopicDoneChange: (TopicRef, Boolean) -> Unit,
    onViewAllToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    
    if (topics.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isDark) {
                                listOf(Color(0xFF1E1B18), Color(0xFF35261B))
                            } else {
                                listOf(Color(0xFFFFF7ED), Color(0xFFFED7AA))
                            }
                        ),
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            color = if (isDark) Color(0xFF452D1D) else Color(0xFFFDBA74),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📖", fontSize = 24.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ready to conquer today? Add your first mission to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFDBA74) else Color(0xFF7C2D12),
                    )
                    Button(
                        onClick = onViewAllToday,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFF97316) else Color.White,
                            contentColor = if (isDark) Color.White else Color(0xFF7C2D12)
                        ),
                        shape = ButtonDefaults.shape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("+ Add Task", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = scheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.12f)),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanSectionHeader(title = "Today's mission", trailing = "${topics.size} planned")
                topics.take(3).forEach { ref ->
                    PlannerTaskRow(
                        ref = ref,
                        accent = PlanTaskRowAccent.Planned,
                        onClick = { onTopicClick(ref) },
                        onDoneChange = { done -> onTopicDoneChange(ref, done) },
                    )
                }
                if (topics.size > 3) {
                    TextButton(onClick = onViewAllToday, modifier = Modifier.align(Alignment.End)) {
                        Text("View all today")
                    }
                }
            }
        }
    }
}

@Composable
fun PlanActionRow(
    onAddTopics: () -> Unit,
    onSchedule: () -> Unit,
    showSchedule: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAddTopics,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A),
                contentColor = if (isDark) Color(0xFF0F172A) else Color.White
            ),
            shape = ButtonDefaults.shape,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Add Topics",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showSchedule) {
            OutlinedButton(
                onClick = onSchedule,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A)
                ),
                shape = ButtonDefaults.outlinedShape,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Build Planner",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PlannerTaskRow(
    ref: TopicRef,
    accent: PlanTaskRowAccent,
    onClick: (() -> Unit)? = null,
    onDoneChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val done = ref.topic.status == TopicStatus.DONE
    val needsRevision = ref.topic.status == TopicStatus.REVISION_NEEDED
    val dotColor = when {
        done -> Color(0xFF10B981)
        needsRevision -> Color(0xFFF59E0B)
        accent == PlanTaskRowAccent.Overdue -> scheme.error
        else -> scheme.primary
    }
    val cardBgColor = when {
        done -> if (isDark) Color(0xFF163B2A) else Color(0xFFECFDF5)
        needsRevision -> if (isDark) Color(0xFF3B2F16) else Color(0xFFFFFBEB)
        accent == PlanTaskRowAccent.Overdue -> if (isDark) Color(0xFF3B1E1E) else Color(0xFFFEF2F2)
        else -> if (isDark) Color(0xFF1E293B) else Color.White
    }
    val animatedDotColor by animateColorAsState(dotColor, label = "planTaskDot")
    val animatedCardBgColor by animateColorAsState(cardBgColor, label = "planTaskBg")
    val borderStroke = if (done || needsRevision || accent == PlanTaskRowAccent.Overdue) null else BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = rowModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = animatedCardBgColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(animatedDotColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = ref.topic.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${ref.subject.name} / ${ref.chapter.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val noteText = ref.topic.notes?.trim().orEmpty()
                if (noteText.isNotEmpty()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF475569),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (needsRevision) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.28f else 0.16f),
                        contentColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                    ) {
                        Text(
                            text = "To Revise",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Checkbox(
                checked = done,
                onCheckedChange = onDoneChange,
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF10B981),
                    uncheckedColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFFCBD5E1),
                    checkmarkColor = Color.White,
                ),
            )
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
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

@Composable
fun PlanSettingsEntryCard(
    plan: StudyPlan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, scheme.outline.copy(alpha = 0.12f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Plan settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${plan.examType ?: "Study Plan"} / ${plan.dailyGoal ?: 0} topics/day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlanTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSettingsSheet(
    plan: StudyPlan,
    actions: PlannerActions,
    onExport: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Plan settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Plan title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    value = examType,
                    onValueChange = { examType = it },
                    label = { Text("Exam type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item { PlannerExamDateField(examDateIso = examDate, onExamDateChange = { examDate = it }) }
            item {
                OutlinedTextField(
                    value = dailyGoal,
                    onValueChange = { dailyGoal = it.filter(Char::isDigit).take(2) },
                    label = { Text("Topics per day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                PlanRestDaysRow(
                    selected = offDays,
                    onToggle = { day ->
                        offDays = if (day in offDays) offDays - day else offDays + day
                    },
                )
            }
            item {
                Button(
                    onClick = {
                        actions.updatePlan(
                            UpdatePlanRequest(
                                title = title.trim().ifBlank { plan.title },
                                examType = examType.trim().ifBlank { null },
                                examDate = examDate.ifBlank { null },
                                dailyGoal = dailyGoal.toIntOrNull()?.coerceAtLeast(1) ?: 3,
                                offDays = offDays.toList(),
                            ),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = ButtonDefaults.shape,
                ) {
                    Text("Save details")
                }
            }
            item { HorizontalDivider() }
            if (isPlanScheduled) {
                item {
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = ButtonDefaults.outlinedShape,
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                }
            }
        }
    }
}
