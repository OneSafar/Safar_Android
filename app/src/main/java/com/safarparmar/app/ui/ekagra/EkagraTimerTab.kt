package com.safarparmar.app.ui.ekagra

import com.composables.ui.components.Text
import com.composables.ui.components.Icon

import com.safarparmar.app.ui.adaptive.isSpaciousLandscape
import com.safarparmar.app.ui.tutorial.tutorialTarget
import androidx.compose.animation.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.constrainHeight
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.glassLiftShadow
import com.safarparmar.app.ui.glass.liquidGlass
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
internal fun EkagraPipOverlay(
    secondsLeft: Int,
    timerMode: TimerMode,
    progress: Float,
    timerRunning: Boolean,
    focusShieldActive: Boolean,
    primary: Color,
) {
    val shieldActive = focusShieldActive && timerRunning
    val pipBg     = if (shieldActive) com.safarparmar.app.ui.ekagra.focusshield.KavachDesign.Primary else Color(0xFF05070A)
    val pipAccent = if (shieldActive) Color.White else primary

    Box(
        Modifier.fillMaxSize().background(pipBg).padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (shieldActive) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(pipAccent.copy(alpha = 0.18f))
                        .border(1.dp, pipAccent.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Shield, contentDescription = null, tint = pipAccent, modifier = Modifier.size(21.dp)) }
            }
            Text(
                if (timerMode == TimerMode.STOPWATCH) formatElapsedDuration(secondsLeft.toLong())
                else "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                fontSize = when {
                    timerMode == TimerMode.STOPWATCH && secondsLeft >= 3600 -> 28.sp
                    shieldActive -> 36.sp
                    else -> 42.sp
                },
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
            )
            Box(Modifier.fillMaxWidth(0.82f).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(0.16f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp)).background(pipAccent))
            }
            Text(
                when { shieldActive -> stringResource(R.string.ekagra_shield_active); timerRunning -> stringResource(R.string.ekagra_focusing); else -> stringResource(R.string.ekagra_paused) },
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
                color = if (shieldActive) pipAccent else Color.White.copy(0.65f),
            )
        }
    }
}

// ─── Mode tabs ─────────────────────────────────────────────────────────────────

/**
 * Mode selector as plain text with an accent underline on the active entry —
 * the redesign drops the filled segmented pill in favour of type and a hairline.
 * POMODORO is not user-selectable here, so it reads as FOCUS.
 */
@Composable
internal fun ModeTabs(
    selected: TimerMode,
    accentColor: Color,
    ink: EkagraInk,
    isDarkTheme: Boolean = false,
    onSelect: (TimerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = remember { TimerMode.entries.filter { it.showInPill && it != TimerMode.BREAK } }

    val trackBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val trackBorder = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(trackBg)
            .border(1.dp, trackBorder, CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            val pillBg = if (isSelected) accentColor else Color.Transparent
            val contentColor = if (isSelected) contrastOn(accentColor) else MaterialTheme.colorScheme.onSurfaceVariant

            val iconVector = when (mode) {
                TimerMode.FOCUS -> Icons.Default.HourglassEmpty
                TimerMode.BREAK -> Icons.Default.FreeBreakfast
                TimerMode.STOPWATCH -> Icons.Default.Timer
                else -> Icons.Default.Timer
            }

            com.composables.ui.components.Button(
                onClick = { onSelect(mode) },
                style = com.composables.ui.components.ButtonStyle.Ghost,
                shape = CircleShape,
                contentColor = contentColor,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).background(pillBg, CircleShape),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    com.composables.ui.components.Text(
                        text = stringResource(mode.labelRes),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ─── Timer / Focus tab ─────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TimerFocusTab(
    modifier: Modifier,
    timerMode: TimerMode,
    /** The mode shown as selected in the pill tab bar.
     *  Defaults to [timerMode]. Callers can pass a different value so the user
     *  can browse/preview other modes without touching the running session. */
    selectedDisplayMode: TimerMode = timerMode,
    secondsLeft: Int,
    isRunning: Boolean,
    progress: Float,
    hasProgress: Boolean,
    mottoText: String,
    plannerTopicTitle: String? = null,
    kavachActive: Boolean = false,
    kavachBlockedCount: Int = 0,
    controlsVisible: Boolean = true,
    onOpenKavachSession: () -> Unit = {},
    onModeChange: (TimerMode) -> Unit,
    onPlayPause: () -> Unit,
    canStartBreak: Boolean,
    onStartBreak: () -> Unit,
    onReset: () -> Unit,
    isBrowsingOtherMode: Boolean = false,
    activeModeLabel: String = "",
    onGoToDuration: () -> Unit = {},
    shieldState: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldUiState,
    isDarkTheme: Boolean,
    themeAccent: Color,
    onToggleKavach: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onNavigate: (String) -> Unit,
    selectedTheme: VisualTheme? = null,
    timerDiameter: Dp = EkagraChrome.size(252f),
    onOpenAnalytics: () -> Unit = {},
    studyCircleLiveCount: Int = 0,
    myCircles: List<com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto> = emptyList(),
    selectedStudyCircle: com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto? = null,
    onSelectStudyCircle: (com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto) -> Unit = {},
    summaryContent: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = selectedTheme?.let { ekagraWarmPalette(it.name) } ?: LocalEkagraWarmPalette.current
    val ink = rememberEkagraInk(onCanvas = true)
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val width = minOf(maxWidth, 600.dp)
        val diameter = minOf(width - 72.dp, timerDiameter).coerceAtLeast(160.dp)
        EkagraBalancedTimerLayout(
            modifier = Modifier.width(width).verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight).padding(horizontal = 16.dp, vertical = 12.dp),
            summary = {
                if (controlsVisible) {
                    if (summaryContent != null) summaryContent() else com.safarparmar.app.feature.kavachanalytics.ui.KavachSummaryPills(
                        ink = rememberEkagraInk(false), isDarkTheme = isDarkTheme, themeAccent = scheme.primary,
                        onOpenAnalytics = onOpenAnalytics, myCircles = myCircles, selectedCircle = selectedStudyCircle,
                        onSelectCircle = onSelectStudyCircle, onNavigate = onNavigate)
                }
            },
        ) {
            if (controlsVisible) {
                ModeTabs(selectedDisplayMode, scheme.primary, rememberEkagraInk(false), isDarkTheme, onModeChange,
                    Modifier.widthIn(max = 300.dp).fillMaxWidth().tutorialTarget("ekagra.mode"))
            }
            if (!plannerTopicTitle.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.ekagra_exam_planner_topic) + ": " + plannerTopicTitle,
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (controlsVisible) {
                    if (shieldState.hasUsageStats && shieldState.hasOverlayPermission) {
                        val alwaysOn = shieldState.isAlwaysOnMode
                        com.composables.ui.components.Button(onClick = { onToggleKavach(!shieldState.isEnabled) }, enabled = !alwaysOn,
                            style = com.composables.ui.components.ButtonStyle.Ghost, shape = CircleShape,
                            contentColor = Color.White, modifier = Modifier.background(Color.White.copy(alpha = .15f), CircleShape).heightIn(min = 48.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(when {
                                shieldState.isProtectionActive && alwaysOn -> R.string.ekagra_kavach_always_active
                                shieldState.isProtectionActive -> R.string.ekagra_kavach_active
                                shieldState.isProtectionStarting -> R.string.ekagra_kavach_starting
                                shieldState.isEnabled && !isRunning -> R.string.ekagra_kavach_ready
                                shieldState.isEnabled -> R.string.ekagra_kavach_unavailable
                                else -> R.string.ekagra_kavach_off
                            }), fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
                Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
                    com.safarparmar.app.ui.components.SafarCircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize(),
                        color = palette.light, trackColor = palette.light.copy(alpha = .24f), strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round, gapSize = 0.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val h = secondsLeft / 3600
                        val m = (secondsLeft % 3600) / 60
                        val secs = secondsLeft % 60
                        val text = if (timerMode == TimerMode.STOPWATCH) formatElapsedDuration(secondsLeft.toLong())
                            else if (h > 0) "%d:%02d:%02d".format(h, m, secs) else "%02d:%02d".format(m, secs)
                        Text(text, fontFamily = EkagraSerif, fontSize = if (secondsLeft >= 3600) 32.sp else 48.sp,
                            color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (controlsVisible) Text(stringResource(when (timerMode) {
                            TimerMode.STOPWATCH -> if (isRunning) R.string.ekagra_stopwatch_running else R.string.ekagra_ready_start
                            TimerMode.BREAK -> if (isRunning) R.string.ekagra_break_running else R.string.ekagra_ready_break
                            TimerMode.POMODORO -> if (isRunning) R.string.ekagra_pomodoro_running else R.string.ekagra_ready_pomodoro
                            else -> if (isRunning) R.string.ekagra_running else R.string.ekagra_ready
                        }), fontSize = 12.sp, color = Color.White.copy(alpha = .85f))
                    }
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isBrowsingOtherMode) EkagraTimerSideAction(
                        stringResource(if (timerMode == TimerMode.BREAK) R.string.ekagra_end_break else R.string.common_end),
                        Icons.Default.Stop, onReset, Modifier.tutorialTarget("ekagra.end"))
                    com.composables.ui.components.Button(onClick = onPlayPause, style = com.composables.ui.components.ButtonStyle.Ghost,
                        shape = CircleShape, contentColor = palette.accent,
                        modifier = Modifier.weight(1f).heightIn(min = 54.dp).background(palette.light, CircleShape).tutorialTarget("ekagra.play")) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null,
                            tint = palette.accent, modifier = Modifier.size(22.dp))
                        Text(stringResource(if (isRunning) R.string.common_pause else if (hasProgress) R.string.common_resume else R.string.common_start),
                            color = palette.accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    if (canStartBreak) EkagraTimerSideAction(stringResource(R.string.ekagra_break), Icons.Default.FreeBreakfast,
                        onStartBreak, Modifier.tutorialTarget("ekagra.break"))
                }
                if (isBrowsingOtherMode) Text(stringResource(R.string.ekagra_other_mode_active, activeModeLabel), color = Color.White,
                    textAlign = TextAlign.Center, fontSize = 12.sp)
                if (controlsVisible && mottoText.isNotBlank()) Text(mottoText, color = Color.White.copy(alpha = .7f),
                    textAlign = TextAlign.Center, fontSize = 10.sp)
            }
            if (controlsVisible) EkagraAppsToBlockButton(onOpenAppPicker)
        }
    }
}

/** Split unused height evenly above and below the timer chunk; short windows scroll normally. */
@Composable
private fun EkagraBalancedTimerLayout(
    modifier: Modifier = Modifier,
    summary: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val gap = with(LocalDensity.current) { 14.dp.roundToPx() }
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = {
            Box(Modifier.fillMaxWidth()) { summary() }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val header = measurables[0].measure(loose)
        val timer = measurables[1].measure(loose)
        val contentHeight = header.height + gap + timer.height
        val height = constraints.constrainHeight(contentHeight)
        val extraSpace = (height - contentHeight).coerceAtLeast(0)
        layout(constraints.maxWidth, height) {
            header.placeRelative(0, 0)
            timer.placeRelative(0, header.height + gap + extraSpace / 2)
        }
    }
}

@Composable
private fun EkagraTimerSideAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        com.composables.ui.components.IconButton(onClick = onClick, style = com.composables.ui.components.ButtonStyle.Ghost,
            shape = CircleShape, contentColor = Color.White, modifier = Modifier.size(54.dp).border(1.dp, Color.White.copy(alpha = .6f), CircleShape)) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

// ─── App blocking shortcut ───────────────────────────────────────────────────

@Composable
internal fun EkagraAppsToBlockButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    com.composables.ui.components.Button(
        onClick = onClick,
        style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = shape,
        contentColor = scheme.onSurface,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.widthIn(max = 280.dp).fillMaxWidth().heightIn(min = 52.dp)
            .background(scheme.surface, shape)
            .border(1.dp, scheme.outlineVariant, shape)
            .tutorialTarget("ekagra.appsToBlock"),
    ) {
        Icon(Icons.Default.Apps, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.ekagra_apps_to_block), color = scheme.onSurface, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}
