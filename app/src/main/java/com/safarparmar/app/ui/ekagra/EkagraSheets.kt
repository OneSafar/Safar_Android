package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.core.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
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
import com.safarparmar.app.ui.theme.isLightBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.navigationBarsPadding
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
    titleInput: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaveFree: () -> Unit,
    onSaveTopic: (Boolean) -> Unit = {},
    onLinkGoal: (com.safarparmar.app.domain.model.Goal, Boolean) -> Unit,
    onDiscard: () -> Unit,
    selectedTheme: VisualTheme? = null,
    isDarkTheme: Boolean = true,
) {
    val scrollState = rememberScrollState()
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    val focusedTimeLabel = formatTopicStudyTime(
        pending?.let(::topicStudyActualSeconds) ?: 0,
    )
    
    // Sheets follow the app mode, independently of the timer's visual backdrop.
    val isThemeDark = isDarkTheme
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White

    val primaryTextColor = if (isThemeDark) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isThemeDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = (if (isThemeDark) Color.White else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.2f)

    val accent = if (isThemeDark) Color(0xFFB9C9FF) else Color(0xFF354F9B)
    val onAccent = if (isThemeDark) Color(0xFF172449) else Color.White
    var selectedGoalId by remember(pending?.sessionId) { mutableStateOf<String?>(null) }
    val selectedGoal = todayGoals.firstOrNull { it.id == selectedGoalId }
    var showGoals by remember(pending?.sessionId) { mutableStateOf(false) }
    var markGoalDone by remember(selectedGoal?.id) { mutableStateOf(false) }
    var markTopicDone by remember(pending?.sessionId) { mutableStateOf(false) }
    val isTopicSession = pending?.topicId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = secondaryTextColor.copy(alpha = 0.4f)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .heightIn(max = maxSheetHeight)
                .imePadding(),
        ) {
            // One dominant headline, followed by a short explanation of the save destination.
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp)) {
                Text(
                    "SESSION COMPLETE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = secondaryTextColor,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$focusedTimeLabel focused",
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTextColor,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Save this session to your Ekagra history.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = secondaryTextColor,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isTopicSession) {
                    Text("Exam Planner topic", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                    Text(pending?.topicTitle ?: "Untitled topic", fontSize = 16.sp, color = primaryTextColor)
                    SessionCompletionOption(
                        label = "Mark topic as completed",
                        checked = markTopicDone,
                        onCheckedChange = { markTopicDone = it },
                        accent = accent,
                        textColor = primaryTextColor,
                    )
                } else {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = onTitleChange,
                        label = { Text("Session name", fontSize = 14.sp) },
                        placeholder = { Text("What did you work on?", fontSize = 14.sp) },
                        supportingText = { Text("Optional", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = dividerColor,
                            focusedLabelColor = accent,
                            unfocusedLabelColor = secondaryTextColor,
                            focusedPlaceholderColor = secondaryTextColor,
                            unfocusedPlaceholderColor = secondaryTextColor,
                            focusedSupportingTextColor = secondaryTextColor,
                            unfocusedSupportingTextColor = secondaryTextColor,
                            cursorColor = accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HorizontalDivider(color = dividerColor)
                    if (todayGoals.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                            Text(
                                "No open goals today. You can save this session on its own.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = secondaryTextColor,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGoals = !showGoals }
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Link to a goal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryTextColor)
                                Text(
                                    selectedGoal?.title ?: "Optional · add this time to today's goal",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = secondaryTextColor,
                                )
                            }
                            Icon(
                                if (showGoals) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showGoals) "Hide goals" else "Show goals",
                                tint = secondaryTextColor,
                            )
                        }
                        if (showGoals) {
                            todayGoals.forEach { goal ->
                                val selected = selectedGoal?.id == goal.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) accent.copy(alpha = 0.10f) else Color.Transparent)
                                        .selectable(
                                            selected = selected,
                                            role = androidx.compose.ui.semantics.Role.RadioButton,
                                            onClick = { selectedGoalId = if (selected) null else goal.id },
                                        )
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                        .heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = secondaryTextColor),
                                    )
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(goal.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryTextColor)
                                        goalRowSubtitle(goal)?.let {
                                            Text(it, fontSize = 12.sp, lineHeight = 18.sp, color = secondaryTextColor)
                                        }
                                    }
                                }
                            }
                        }
                        if (selectedGoal != null) {
                            Text("After saving", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = secondaryTextColor)
                            Column(Modifier.selectableGroup()) {
                                GoalSessionSaveChoice.entries.forEach { choice ->
                                    val selected = markGoalDone == choice.marksGoalDone
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .selectable(
                                                selected = selected,
                                                role = androidx.compose.ui.semantics.Role.RadioButton,
                                                onClick = { markGoalDone = choice.marksGoalDone },
                                            )
                                            .heightIn(min = 48.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = null,
                                            colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = secondaryTextColor),
                                        )
                                        Text(
                                            if (choice.marksGoalDone) "Mark Goal as Done" else "Keep Goal Open",
                                            fontSize = 14.sp,
                                            color = primaryTextColor,
                                        )
                                    }
                                }
                            }
                            Text(
                                if (markGoalDone) "The goal will appear in Goals → Completed."
                                else "The goal stays open for your next session.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = secondaryTextColor,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Keep the save action visible while the optional goal list scrolls.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDiscard, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Discard", fontSize = 14.sp, color = secondaryTextColor)
                }
                Button(
                    onClick = {
                        val goal = selectedGoal
                        when {
                            isTopicSession -> onSaveTopic(markTopicDone)
                            goal != null -> onLinkGoal(goal, markGoalDone)
                            else -> onSaveFree()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Text("Save session", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SessionCompletionOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onCheckedChange)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = accent))
        Text(label, fontSize = 14.sp, color = textColor)
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
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = if (isDark) 0.22f else 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.5f else 0.35f)),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun InfoNoticeCard(
    text: String,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = text,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = textColor,
            )
        }
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
        modifier = modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 0.8.sp,
        )
    }
}

/**
 * Last stop before a session is saved. Confirms whether study time is saved
 * as a free session or linked to a goal/topic, and whether the goal is marked done.
 */
@Composable
internal fun EkagraConfirmSaveDialog(
    label: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    completesTarget: Boolean = false,
    keepsGoalOpen: Boolean = false,
    linksTopic: Boolean = false,
    accentColor: Color = PlannerAccent.Teal,
) {
    val accent = accentColor
    val ink = rememberEkagraInk(onCanvas = false)
    val isQuickSave = label.equals("Quick Save", ignoreCase = true) || label.isBlank()
    val eyebrowText = if (isQuickSave) "CONFIRM SAVE" else "CONFIRM LINK"
    val titleText = if (isQuickSave) "Save this session?" else "Save to \"$label\"?"

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, ink.hairline),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                EkagraEyebrow(eyebrowText, accent)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = titleText,
                    fontFamily = EkagraSerif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp,
                    color = ink.primaryText,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        linksTopic && completesTarget -> "Your study time will be saved and this topic will be marked completed in Exam Planner."
                        linksTopic -> "Your study time will be saved to this Exam Planner topic. The topic stays open."
                        completesTarget -> "Your session will appear in Ekagra history, linked to this goal. The goal will also appear in Goals → Completed."
                        keepsGoalOpen -> "Your session will appear in Ekagra history, linked to this goal. The goal stays active for your next session."
                        isQuickSave -> "Your study time will be saved to your Ekagra history."
                        else -> "Your study time will be saved."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = ink.secondaryText,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EkagraGhostAction(
                        label = "Cancel",
                        ink = ink,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    EkagraPrimaryAction(
                        label = "Save",
                        accent = accent,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ─── Phase-1: Session naming dialog (shown when user presses "End") ─────────

/**
 * Shown when the user explicitly presses "End" — asks for a session name,
 * then calls [onSave] with the typed title. Auto-completed timers skip this
 * dialog and save as "Untitled • date/time" automatically.
 */
@Composable
internal fun SessionNameDialog(
    initialTitle: String,
    focusedTimeLabel: String,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit,
) {
    val accent = PlannerAccent.Amber
    var nameInput by remember { mutableStateOf(initialTitle) }

    androidx.compose.ui.window.Dialog(onDismissRequest = { /* non-dismissable, must choose */ }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PlannerFlatColors.CardWhite)
                .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(20.dp))
                .padding(22.dp),
        ) {
            Text(
                text = "Session complete",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$focusedTimeLabel focused",
                fontSize = 24.sp,
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                color = accent,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Name your session",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlannerFlatColors.TextDark,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = PlannerFlatColors.TextDark,
                    fontSize = 16.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { field ->
                    Box {
                        if (nameInput.isBlank()) {
                            Text(
                                "What were you working on?",
                                fontSize = 16.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }
                        field()
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionPill(
                    text = "Discard",
                    accentColor = PlannerFlatColors.TextMuted,
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                )
                ActionPill(
                    text = "Save",
                    accentColor = accent,
                    onClick = { onSave(nameInput.trim().ifBlank { "Untitled" }) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── Phase-2: Post-save goal-linking sheet ──────────────────────────────────

/**
 * Shown *after* a session has already been saved to Ekagra history (phase 1).
 * Asks the user if they want to dedicate this session to an existing goal.
 *
 * - **No** → dismiss, session stays in Ekagra history only.
 * - **Yes** → shows a goal list; after selecting, offers "Keep Goal Open" or
 *   "Mark Goal Done".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostSaveGoalLinkingSheet(
    savedSessionId: String,
    savedDurationSeconds: Int,
    todayGoals: List<com.safarparmar.app.domain.model.Goal>,
    onDismiss: () -> Unit,
    onLinkGoal: (com.safarparmar.app.domain.model.Goal, Boolean) -> Unit,
    selectedTheme: VisualTheme? = null,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val focusedTimeLabel = formatTopicStudyTime(savedDurationSeconds)

    var selectedGoal by remember { mutableStateOf<com.safarparmar.app.domain.model.Goal?>(null) }
    val shownGoals = todayGoals

    var pendingConfirmation by remember { mutableStateOf<PendingGoalLinkConfirmation?>(null) }

    val ink = rememberEkagraInk(onCanvas = false, theme = selectedTheme, isDarkTheme = false)
    val themeAccent = selectedTheme?.accent ?: MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ink.hairline) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
            ) {
                EkagraEyebrow("SESSION SAVED", themeAccent)
                Spacer(Modifier.height(4.dp))
                EkagraDisplayTitle("$focusedTimeLabel focused", ink.primaryText)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Saved in Ekagra history. Linking a goal keeps your session there and credits the same study time to the goal.",
                    fontSize = 13.sp,
                    color = ink.secondaryText,
                )
            }

            Spacer(Modifier.height(14.dp))
            EkagraHairline(ink.hairline)

            Text(
                text = "You can only link today's created goals.\nCreate a goal today if you have not created one.",
                fontSize = 12.sp,
                color = ink.secondaryText,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            EkagraHairline(ink.hairline)

            // ── Goal list — scrollable ─────────────────────────────────────────
            if (shownGoals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No open goals for today. Your session is already saved.",
                        fontSize = 14.sp,
                        color = ink.mutedText,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    itemsIndexed(shownGoals) { index, goal ->
                        val selected = selectedGoal?.id == goal.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) themeAccent.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedGoal = if (selected) null else goal
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            // Selection indicator
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) themeAccent else Color.Transparent
                                    )
                                    .border(
                                        width = if (selected) 0.dp else EkagraChrome.stroke(1f),
                                        color = if (selected) themeAccent else ink.hairline,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = contrastOn(themeAccent),
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    fontFamily = EkagraSerif,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) themeAccent else ink.primaryText,
                                    maxLines = 2,
                                )
                                goalRowSubtitle(goal)?.let { subtitle ->
                                    Text(
                                        text = subtitle,
                                        fontSize = 12.sp,
                                        color = ink.secondaryText,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                        if (index < shownGoals.size - 1) {
                            EkagraHairline(ink.hairline.copy(alpha = 0.5f))
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // ── Hairline before action buttons ─────────────────────────────────
            EkagraHairline(ink.hairline)

            // ── Action buttons — clean Ekagra capsule pills ────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val goalSelected = selectedGoal != null

                // Primary capsule action button
                EkagraPrimaryAction(
                    label = if (goalSelected) "Link Goal & Mark Done" else "Select a goal first",
                    accent = if (goalSelected) themeAccent else themeAccent.copy(alpha = 0.40f),
                    onClick = {
                        selectedGoal?.let { goal ->
                            pendingConfirmation = PendingGoalLinkConfirmation(
                                goal = goal,
                                markComplete = true,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Secondary ghost capsule button
                EkagraGhostAction(
                    label = "No thanks, keep in Ekagra",
                    ink = ink,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Confirmation dialog
    pendingConfirmation?.let { confirmation ->
        EkagraConfirmSaveDialog(
            label = confirmation.goal.title,
            completesTarget = confirmation.markComplete,
            keepsGoalOpen = !confirmation.markComplete,
            accentColor = themeAccent,
            onConfirm = {
                pendingConfirmation = null
                onLinkGoal(confirmation.goal, confirmation.markComplete)
            },
            onCancel = { pendingConfirmation = null },
        )
    }
}

private data class PendingGoalLinkConfirmation(
    val goal: com.safarparmar.app.domain.model.Goal,
    val markComplete: Boolean,
)
