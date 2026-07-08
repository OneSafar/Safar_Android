package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

    val examDays = daysUntil(plan.examDate)

    Column(modifier = cardModifier) {
        // ── Hero Banner ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0B1221), Color(0xFF0F1C35))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Left — Plan title + subtitle
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!plan.examDate.isNullOrBlank()) {
                            Text(
                                text = readableDate(plan.examDate).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.55f),
                                letterSpacing = 1.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Right — Days left & Settings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = plannerExamCountdownHeroNumber(examDays),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF4D6D),
                                maxLines = 1,
                            )
                            Text(
                                text = plannerExamCountdownCaption(examDays).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                            )
                        }

                        // Settings menu
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Plan options",
                                    tint = Color.White.copy(alpha = 0.7f)
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

                Spacer(Modifier.height(24.dp))

                // Bottom row: Horizontal Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.completionPercent / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF60A5FA),
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                    Text(
                        text = "${progress.completionPercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
            }
        }

        // ── Filters (tab bar, etc.) rendered below the banner ───────
        if (true) { // always render, the caller controls content
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filters()
            }
        }
    }
}



/**
 * Premium vertical stat cards — each card spans full width and shows the label
 * prominently at the top with the number large below and an accent icon on the right.
 */
@Composable
fun PlanTodayStatsRow(
    toStudy: Int,
    done: Int,
    pendingRevision: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlanStatCard(
            value = toStudy,
            label = "TO STUDY",
            icon = Icons.Default.Schedule,
            accentColor = Color(0xFF6366F1),
            isDark = isDark,
            modifier = Modifier.fillMaxWidth()
        )
        PlanStatCard(
            value = done,
            label = "DONE TODAY",
            icon = Icons.Default.CheckCircle,
            accentColor = Color(0xFF10B981),
            isDark = isDark,
            modifier = Modifier.fillMaxWidth()
        )
        PlanStatCard(
            value = pendingRevision,
            label = "PENDING REVISION",
            icon = Icons.Default.SwapHoriz,
            accentColor = Color(0xFFF59E0B),
            isDark = isDark,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlanStatCard(
    value: Int,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val cardBg = if (isDark) Color(0xFF1A1F2E) else Color.White
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.25f else 0.18f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = if (isDark) 0.18f else 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Horizontal tab bar for Today / Overdue / Upcoming / Done views — full-width even
 * spacing so labels never overflow on any device width.
 */
@Composable
fun PlanTabQuickLinks(
    activeTab: StudyPlannerTab,
    onTabSelected: (StudyPlannerTab) -> Unit,
    overdueCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !scheme.background.isLightBackground()
    val tabs = buildList {
        add(StudyPlannerTab.TODAY to "Today")
        if (overdueCount > 0) add(StudyPlannerTab.OVERDUE to "Overdue")
        add(StudyPlannerTab.UPCOMING to "Upcoming")
        add(StudyPlannerTab.COMPLETED to "Done")
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { (tab, label) ->
            val selected = activeTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.dp,
                        color = if (selected) Color.Transparent else scheme.outlineVariant,
                        shape = RoundedCornerShape(50)
                    )
                    .background(
                        if (selected) {
                            if (isDark) Color(0xFF1E293B) else Color(0xFF0F1C35)
                        } else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
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
                                listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)
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
    onRebuildPlan: (() -> Unit)? = null,
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
        if (onRebuildPlan != null) {
            OutlinedButton(
                onClick = onRebuildPlan,
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
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Rebuild Plan",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlannerTaskRow(
    ref: TopicRef,
    accent: PlanTaskRowAccent,
    onClick: (() -> Unit)? = null,
    onDoneChange: (Boolean) -> Unit,
    onReplace: (() -> Unit)? = null,
    onRemoveFromToday: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val done = ref.topic.status == TopicStatus.DONE
    val needsRevision = ref.topic.status == TopicStatus.REVISION_NEEDED
    val cardBgColor = when {
        done -> if (isDark) Color(0xFF102A20) else Color(0xFFECFDF5)
        needsRevision -> if (isDark) Color(0xFF2B2015) else Color(0xFFFFFBEB)
        accent == PlanTaskRowAccent.Overdue -> if (isDark) Color(0xFF2D181A) else Color(0xFFFEF2F2)
        else -> if (isDark) Color(0xFF1E293B) else Color.White
    }
    val animatedCardBgColor by animateColorAsState(cardBgColor, label = "planTaskBg")
    val borderStroke = if (done || needsRevision || accent == PlanTaskRowAccent.Overdue) null else BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))

    var showMenu by remember { mutableStateOf(false) }
    val hasMenu = onEdit != null || onReplace != null || (onRemoveFromToday != null && !done)

    // A brief scale-pulse whenever this topic's status changes — visual confirmation
    // that a tap registered, independent of the status-change snackbar shown elsewhere.
    // Skips the very first composition so rows don't all pulse on initial render/scroll-in.
    val pulseScale = remember { Animatable(1f) }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(ref.topic.status) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            pulseScale.snapTo(0.95f)
            pulseScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f))
        }
    }

    val rowModifier = modifier.fillMaxWidth().scale(pulseScale.value).let {
        if (onClick != null || hasMenu) {
            it.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { if (hasMenu) showMenu = true },
            )
        } else {
            it
        }
    }

    Card(
        modifier = rowModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = animatedCardBgColor),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box {
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Edit topic") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { showMenu = false; onEdit() },
                )
            }
            if (onReplace != null) {
                DropdownMenuItem(
                    text = { Text("Replace topic") },
                    leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    onClick = { showMenu = false; onReplace() },
                )
            }
            if (onRemoveFromToday != null && !done) {
                DropdownMenuItem(
                    text = { Text("Remove from today") },
                    leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) },
                    onClick = { showMenu = false; onRemoveFromToday() },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Left accent bar
            val leftBarColor = when {
                done -> Color(0xFF10B981) // Green
                needsRevision -> Color(0xFFF97316) // Orange
                accent == PlanTaskRowAccent.Overdue -> Color(0xFFEF4444) // Red
                else -> subjectDotColor(ref.subject.color)
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(leftBarColor)
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
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
                    text = "${ref.subject.name} · ${ref.chapter.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
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
            Spacer(Modifier.width(8.dp))
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
}

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

    val scheme = MaterialTheme.colorScheme
    val premiumGradient = Brush.horizontalGradient(colors = listOf(Color(0xFF3E7C8C), Color(0xFF29638A)))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = scheme.surface,
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
                            .background(premiumGradient),
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
                        Text("Plan Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Fine-tune your exam and study routine",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
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
                        OutlinedTextField(
                            value = examType,
                            onValueChange = { examType = it },
                            label = { Text("Exam type") },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(premiumGradient, shape = RoundedCornerShape(16.dp)),
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
            if (isPlanScheduled) {
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
                                        text = if (hasDate) "Scheduled ${readableDate(ref.topic.plannedDate)}" else "Unscheduled topic",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopicToTodaySheet(
    allRefs: List<TopicRef>,
    today: String,
    dailyGoal: Int,
    currentTodayCount: Int,
    onAdd: (topicId: String, today: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val scheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }

    // Available: TODO topics that are NOT scheduled for today
    val candidates = remember(allRefs, today) {
        allRefs.filter { ref ->
            ref.topic.status != TopicStatus.DONE &&
            (ref.topic.plannedDate?.take(10) ?: "") != today
        }
    }
    val filteredCandidates = remember(candidates, searchQuery) {
        if (searchQuery.isBlank()) candidates
        else {
            val q = searchQuery.lowercase()
            candidates.filter {
                it.topic.name.lowercase().contains(q) ||
                it.subject.name.lowercase().contains(q) ||
                it.chapter.name.lowercase().contains(q)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // Header
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Add Topic to Today",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Today: $currentTodayCount topics" + if (dailyGoal > 0) " (goal: $dailyGoal/day)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search topics…") },
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
                    .padding(horizontal = 20.dp),
                singleLine = true,
            )

            Spacer(Modifier.height(8.dp))

            if (filteredCandidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching topics found" else "All topics are scheduled or completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(
                        items = filteredCandidates,
                        key = { it.topic.id },
                    ) { ref ->
                        val hasDate = !ref.topic.plannedDate.isNullOrBlank()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAdd(ref.topic.id, today)
                                    onDismiss()
                                },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                                        .background(if (hasDate) Color(0xFFF59E0B) else scheme.primary),
                                )
                                Column(
                                    modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
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
                                    if (hasDate) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.28f else 0.16f),
                                            contentColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                        ) {
                                            Text(
                                                text = "Currently: ${readableDate(ref.topic.plannedDate)}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to today",
                                    tint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
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
