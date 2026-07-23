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

                val chunks = visualThemes.chunked(2)
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
    goals: List<com.safarparmar.app.domain.model.Goal>,
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
    var selectedGoal by remember { mutableStateOf<com.safarparmar.app.domain.model.Goal?>(null) }
    var markAsCompleted by remember { mutableStateOf(false) }
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
                        Text(
                            text = "EXAM PLANNER TOPIC",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlannerFlatColors.TextMuted,
                            letterSpacing = 1.sp
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
                                        if (markTopicDone) scheme.primary else PlannerFlatColors.TextMuted.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .background(
                                        if (markTopicDone) scheme.primary else Color.Transparent,
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
                            accentColor = scheme.primary,
                            onClick = { onSaveTopic(markTopicDone) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Goal linking or free save
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text(
                            text = "LINK TO A GOAL",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlannerFlatColors.TextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (goals.isEmpty()) {
                            Text(
                                "No active goals available",
                                fontSize = 14.sp,
                                color = PlannerFlatColors.TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            goals.take(4).forEachIndexed { index, goal ->
                                val selected = selectedGoal?.id == goal.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedGoal = if (selected) null else goal
                                            markAsCompleted = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Link,
                                        contentDescription = null,
                                        tint = if (selected) scheme.primary else PlannerFlatColors.TextMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = goal.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) scheme.primary else PlannerFlatColors.TextDark,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (index < goals.size - 1) {
                                    PlanHairline(alpha = 0.6f)
                                }
                            }
                        }

                        if (selectedGoal != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { markAsCompleted = !markAsCompleted }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(
                                            1.dp,
                                            if (markAsCompleted) scheme.primary else PlannerFlatColors.TextMuted.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                        .background(
                                            if (markAsCompleted) scheme.primary else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (markAsCompleted) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Mark goal as completed",
                                    fontSize = 14.sp,
                                    color = PlannerFlatColors.TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            ActionPill(
                                text = if (markAsCompleted) "Link & Finish Goal" else "Link Focus Session",
                                accentColor = scheme.primary,
                                onClick = { selectedGoal?.let { onLinkGoal(it, markAsCompleted) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    PlanHairline()

                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text(
                            text = "QUICK SAVE",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlannerFlatColors.TextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = titleInput,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = PlannerFlatColors.TextDark,
                                fontSize = 16.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
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
                            accentColor = scheme.primary,
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
