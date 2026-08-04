package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.TextureView
import android.graphics.SurfaceTexture
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.alpha
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.SafarGlassCard
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
internal fun VisualThemeDialog(current: VisualTheme, onSelect: (VisualTheme) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)
    val dialogBg = scheme.background

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(Modifier.fillMaxWidth()) {
                EkagraEyebrow("Theme", ink.secondaryText)
                Spacer(Modifier.height(4.dp))
                EkagraDisplayTitle("Visual theme", ink.primaryText)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Select a backdrop and music theme to personalize your focus session.",
                    fontSize = 12.sp,
                    color = ink.mutedText
                )
                EkagraHairline(ink.hairline)

                val chunks = selectableVisualThemes.chunked(2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    chunks.forEach { pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            pair.forEach { theme ->
                                val isSelected = theme.name == current.name
                                val cardBorder = if (isSelected) {
                                    BorderStroke(1.5.dp, theme.accent)
                                } else {
                                    BorderStroke(1.dp, ink.hairline)
                                }
                                val cardBg = if (isSelected) theme.accent.copy(alpha = 0.12f) else scheme.surfaceContainerLow

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBg)
                                        .border(cardBorder, RoundedCornerShape(16.dp))
                                        .clickable { onSelect(theme) }
                                        .padding(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(theme.accent.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(theme.emoji, fontSize = 15.sp)
                                            }
                                            if (isSelected) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(theme.accent)
                                                    )
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = theme.accent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            theme.name,
                                            fontFamily = EkagraSerif,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = ink.primaryText
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

// ─── Organize free focus sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizeFreeFocusSheet(
    sheetState: SheetState,
    pending: PendingEndedEkagraSession?,
    todayGoals: List<com.safarparmar.app.domain.model.Goal>,
    missedGoals: List<com.safarparmar.app.domain.model.Goal>,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaveFree: () -> Unit,
    onSaveTopic: (Boolean) -> Unit = {},
    onLinkGoal: (com.safarparmar.app.domain.model.Goal, Boolean) -> Unit,
    onDiscard: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val ignoredSheetState = sheetState
    val sheetStateLocal = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)
    val focusedTimeLabel = formatTopicStudyTime(
        pending?.let(::topicStudyActualSeconds) ?: 0,
    )
    // One accent per way of saving, so the two paths are never confusable.
    // All three resolve per-theme, so nothing depends on scheme.primary (which is
    // an off-brand blue in light mode).
    val goalAccent = PlannerAccent.Teal      // Link to a goal
    val quickAccent = PlannerAccent.Amber    // Quick Save
    val topicAccent = PlannerAccent.Coral    // Exam planner topic
    var selectedGoal by remember { mutableStateOf<com.safarparmar.app.domain.model.Goal?>(null) }
    var selectedGoalTab by remember { mutableIntStateOf(0) }
    val shownGoals = if (selectedGoalTab == 0) todayGoals else missedGoals
    var markTopicDone by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetStateLocal,
        containerColor = SafarSemanticColors.plannerBackground(), // Opaque warm canvas
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Black.copy(alpha = 0.3f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .padding(bottom = 24.dp)
        ) {
            // 2. Header Block
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = "Session complete",
                    fontFamily = LoraFontFamily, // Editorial Serif
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = "$focusedTimeLabel focused",
                    fontSize = 28.sp,
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = PlannerFlatColors.TextDark,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Choose how to save your focus session",
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }

            PlanHairline() // 1px separator rule

            // 3. Scrollable List Internal to 60% Sheet
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()) {
                if (pending?.topicId != null) {
                    // Exam Planner topic save
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SaveSectionHeader(
                            label = "EXAM PLANNER TOPIC",
                            accent = topicAccent,
                            icon = Icons.Default.MenuBook,
                        )
                        Text(
                            text = pending.topicTitle ?: "Untitled topic",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextDark
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { markTopicDone = !markTopicDone }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        1.dp,
                                        if (markTopicDone) topicAccent else PlannerFlatColors.TextMuted.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .background(
                                        if (markTopicDone) topicAccent else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (markTopicDone) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Mark topic as completed",
                                fontSize = 14.sp,
                                color = PlannerFlatColors.TextDark
                            )
                        }
                    }
                    PlanHairline()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            text = "Discard",
                            accentColor = PlannerFlatColors.TextMuted,
                            onClick = onDiscard,
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            text = "Save Topic",
                            accentColor = topicAccent,
                            onClick = { onSaveTopic(markTopicDone) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Goal linking or free save
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        SaveSectionHeader(
                            label = "LINK TO A GOAL",
                            accent = goalAccent,
                            icon = Icons.Default.Link,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GoalListTab(
                                text = "Today (${todayGoals.size})",
                                selected = selectedGoalTab == 0,
                                accent = goalAccent,
                                onClick = {
                                    selectedGoalTab = 0
                                    selectedGoal = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                            GoalListTab(
                                text = "Missed (${missedGoals.size})",
                                selected = selectedGoalTab == 1,
                                accent = goalAccent,
                                onClick = {
                                    selectedGoalTab = 1
                                    selectedGoal = null
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (shownGoals.isEmpty()) {
                            Text(
                                if (selectedGoalTab == 0) "No goals for today" else "No missed goals",
                                fontSize = 14.sp,
                                color = PlannerFlatColors.TextMuted,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        } else {

                            shownGoals.forEachIndexed { index, goal ->
                                val selected = selectedGoal?.id == goal.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGoal = if (selected) null else goal
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) goalAccent
                                                else goalAccent.copy(alpha = 0.12f),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (selected) Icons.Default.Check else Icons.Default.Link,
                                            contentDescription = null,
                                            tint = if (selected) Color.White else goalAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    // Nothing stops a student naming two goals the
                                    // same thing, and linking is irreversible — so
                                    // never show a bare title they cannot tell apart.
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) goalAccent else PlannerFlatColors.TextDark,
                                            maxLines = 1,
                                        )
                                        goalRowSubtitle(goal)?.let { subtitle ->
                                            Text(
                                                text = subtitle,
                                                fontSize = 11.5.sp,
                                                color = PlannerFlatColors.TextMuted,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                                if (index < shownGoals.size - 1) {
                                    PlanHairline(alpha = 0.6f)
                                }
                            }
                        }

                        if (selectedGoal != null) {
                            // There used to be a "Mark goal as completed" checkbox
                            // here, defaulting to OFF. Linking a finished study
                            // session to a goal and NOT finishing the goal made no
                            // sense to students: they linked the session, saw the
                            // goal still open, and had to go to the Goals screen and
                            // tick it a second time. Linking now always completes it.
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionPill(
                                text = "Link & Complete Goal",
                                accentColor = goalAccent,
                                onClick = { selectedGoal?.let { onLinkGoal(it, true) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    PlanHairline()

                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        SaveSectionHeader(
                            label = "QUICK SAVE",
                            accent = quickAccent,
                            icon = Icons.Default.BookmarkBorder,
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = titleInput,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = PlannerFlatColors.TextDark,
                                fontSize = 16.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(quickAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            decorationBox = { field ->
                                Box {
                                    if (titleInput.isBlank()) {
                                        Text("What were you working on?", fontSize = 16.sp, color = PlannerFlatColors.TextMuted)
                                    }
                                    field()
                                }
                            },
                        )
                    }

                    PlanHairline()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            text = "Discard",
                            accentColor = PlannerFlatColors.TextMuted,
                            onClick = onDiscard,
                            modifier = Modifier.weight(1f)
                        )
                        ActionPill(
                            text = "Save Session",
                            accentColor = quickAccent,
                            onClick = onSaveFree,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalListTab(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent else accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (selected) 1f else 0.25f)),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

/**
 * A short line under a goal's title so two goals sharing a name are still
 * distinguishable — its kind plus, for dated goals, the day it belongs to.
 * Returns null when there is genuinely nothing extra worth saying.
 */
private fun goalRowSubtitle(goal: com.safarparmar.app.domain.model.Goal): String? {
    val kind = when (goal.goalKind) {
        "repeat" -> "Repeat"
        "scheduled" -> "Scheduled"
        "one_time" -> "One-time"
        else -> null
    }
    val day = goal.scheduledDate?.takeIf { it.isNotBlank() }?.take(10)
    return when {
        kind != null && day != null -> "$kind \u00B7 $day"
        kind != null -> kind
        day != null -> day
        else -> null
    }
}

/**
 * Colour-coded section heading. The two ways to file a session — linking it to a
 * goal (teal) versus a free Quick Save (amber) — are otherwise identical grey
 * lists, so the accent is what tells them apart at a glance. Both accents resolve
 * per-theme via [PlannerAccent], so they stay legible in light and dark.
 */
@Composable
private fun SaveSectionHeader(
    label: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
        }
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 1.sp,
        )
    }
}

/**
 * Last stop before a session is filed. Saving is irreversible — a session can
 * never be renamed, nor moved between Quick Save and a goal, once saved — so an
 * accidental tap should not be able to commit it.
 */
@Composable
internal fun EkagraConfirmSaveDialog(
    label: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    /** True when confirming will also mark the linked goal/topic finished, so the
     *  dialog can say so instead of springing it on the student. */
    completesTarget: Boolean = false,
) {
    val accent = PlannerAccent.Teal
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PlannerFlatColors.CardWhite)
                .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Text(
                text = "Save to \"$label\"?",
                fontFamily = LoraFontFamily,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (completesTarget) {
                    "This also marks \"$label\" as done. You cannot change it later."
                } else {
                    "You cannot change this later. Your session will stay here."
                },
                fontSize = 13.5.sp,
                color = PlannerFlatColors.TextMuted,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionPill(
                    text = "Go back",
                    accentColor = PlannerFlatColors.TextMuted,
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                )
                ActionPill(
                    text = "Yes, save",
                    accentColor = accent,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
