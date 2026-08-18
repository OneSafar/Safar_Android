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

private fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
    val r = color1.red * ratio + color2.red * (1f - ratio)
    val g = color1.green * ratio + color2.green * (1f - ratio)
    val b = color1.blue * ratio + color2.blue * (1f - ratio)
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
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
    @Suppress("UNUSED_VARIABLE")
    val ignoredSheetState = sheetState
    val sheetStateLocal = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.65f
    val focusedTimeLabel = formatTopicStudyTime(
        pending?.let(::topicStudyActualSeconds) ?: 0,
    )
    
    // Theme awareness: adapt surface color to selected visual theme or dark mode
    val isThemeDark = isDarkTheme || (selectedTheme?.gradientColors != null && selectedTheme.gradientColors.isNotEmpty())
    
    val containerColor = remember(selectedTheme, isDarkTheme, MaterialTheme.colorScheme.surface) {
        if (isThemeDark) {
            val bgSeed = selectedTheme?.gradientColors?.firstOrNull()
                ?: selectedTheme?.accent
                ?: Color(0xFF1E293B)
            blendColors(bgSeed, Color(0xFF14181E), 0.35f)
        } else {
            val bgSeed = selectedTheme?.accent ?: Color(0xFFF8FAFC)
            blendColors(bgSeed, Color(0xFFF8FAFC), 0.10f)
        }
    }

    val primaryTextColor = if (isThemeDark) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isThemeDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = (if (isThemeDark) Color.White else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.2f)

    // Deep, richer shades of blue (Link to a Goal) and orange (Save to Ekagra)
    val goalAccent = if (isThemeDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A)      // Deep Navy/Dark Blue
    val quickAccent = if (isThemeDark) Color(0xFFFB923C) else Color(0xFFC2410C)     // Deep Burnt Orange
    val topicAccent = if (isThemeDark) Color(0xFFF87171) else Color(0xFFB91C1C)     // Deep Crimson Red
    var selectedGoal by remember { mutableStateOf<com.safarparmar.app.domain.model.Goal?>(null) }
    val shownGoals = todayGoals
    var markTopicDone by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetStateLocal,
        containerColor = containerColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = secondaryTextColor.copy(alpha = 0.4f)) },
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
                    text = "Session Complete",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                )
                Text(
                    text = "$focusedTimeLabel focused",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Save normally in Ekagra or link it to today's goal.",
                    fontSize = 13.sp,
                    color = secondaryTextColor,
                )
            }

            HorizontalDivider(color = dividerColor)

            // 3. Scrollable List Internal to Sheet
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
                            color = primaryTextColor,
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
                                        if (markTopicDone) topicAccent else secondaryTextColor.copy(alpha = 0.5f),
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
                                color = primaryTextColor,
                            )
                        }
                    }
                    HorizontalDivider(color = dividerColor)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            text = "Discard",
                            accentColor = secondaryTextColor,
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
                        InfoNoticeCard(
                            text = "You can only link today's created goals. Create a goal today if you have not created one.",
                            accent = goalAccent,
                            textColor = secondaryTextColor,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )

                        if (shownGoals.isEmpty()) {
                            Text(
                                "No open goals for today.",
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(vertical = 8.dp),
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
                                                else goalAccent.copy(alpha = 0.14f),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) goalAccent else primaryTextColor,
                                            maxLines = 1,
                                        )
                                        goalRowSubtitle(goal)?.let { subtitle ->
                                            Text(
                                                text = subtitle,
                                                fontSize = 11.5.sp,
                                                color = secondaryTextColor,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                                if (index < shownGoals.size - 1) {
                                    HorizontalDivider(color = dividerColor)
                                }
                            }
                        }

                        if (selectedGoal != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Time will be added to this goal.",
                                fontSize = 12.5.sp,
                                color = secondaryTextColor,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ActionPill(
                                text = "Keep Goal Open",
                                accentColor = goalAccent,
                                onClick = {
                                    selectedGoal?.let {
                                        onLinkGoal(it, GoalSessionSaveChoice.KEEP_GOAL_OPEN.marksGoalDone)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Keep goal active.",
                                fontSize = 11.5.sp,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ActionPill(
                                text = "Mark Goal Done",
                                accentColor = goalAccent,
                                onClick = {
                                    selectedGoal?.let {
                                        onLinkGoal(it, GoalSessionSaveChoice.MARK_GOAL_DONE.marksGoalDone)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Complete goal now.",
                                fontSize = 11.5.sp,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    HorizontalDivider(color = dividerColor)

                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        SaveSectionHeader(
                            label = "SAVE TO EKAGRA",
                            accent = quickAccent,
                            icon = Icons.Default.BookmarkBorder,
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = titleInput,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = primaryTextColor,
                                fontSize = 16.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(quickAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            decorationBox = { field ->
                                Box {
                                    if (titleInput.isBlank()) {
                                        Text("Session name (optional)", fontSize = 16.sp, color = secondaryTextColor)
                                    }
                                    field()
                                }
                            },
                        )
                    }

                    HorizontalDivider(color = dividerColor)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill(
                            text = "Discard",
                            accentColor = secondaryTextColor,
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
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = if (isDark) 0.22f else 0.12f),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = if (isDark) 0.5f else 0.35f)),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 16.dp),
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
}

@Composable
private fun InfoNoticeCard(
    text: String,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 16.sp,
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
                        completesTarget -> "Your study time will be saved and this goal will be marked done."
                        keepsGoalOpen -> "Your study time will be saved. This goal will stay open for your next session."
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
                .fillMaxSize()
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
                    text = "Select a goal to credit this session to",
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
