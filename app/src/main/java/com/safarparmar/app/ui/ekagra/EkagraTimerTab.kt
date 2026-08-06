package com.safarparmar.app.ui.ekagra

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
                when { shieldActive -> "SHIELD ACTIVE"; timerRunning -> "FOCUSING"; else -> "PAUSED" },
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
    onSelect: (TimerMode) -> Unit,
) {
    val modes = remember { TimerMode.entries.filter { it.showInPill } }

    EkagraTextTabs(
        items = modes,
        selected = selected,
        accent = accentColor,
        ink = ink,
        label = { it.label },
        icon = { mode, color ->
            val iconVector = when (mode) {
                TimerMode.FOCUS -> Icons.Default.HourglassEmpty
                TimerMode.BREAK -> Icons.Default.FreeBreakfast
                TimerMode.STOPWATCH -> Icons.Default.Timer
                else -> Icons.Default.Timer
            }
            EkagraChromeIcon(
                imageVector = iconVector,
                contentDescription = null,
                tint = color,
                baseSizeDp = 16f,
            )
        },
        onSelect = onSelect,
    )
}

// ─── Timer / Focus tab ─────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TimerFocusTab(
    modifier: Modifier,
    timerMode: TimerMode,
    secondsLeft: Int,
    isRunning: Boolean,
    progress: Float,
    hasProgress: Boolean,
    mottoText: String,
    kavachActive: Boolean = false,
    kavachBlockedCount: Int = 0,
    controlsVisible: Boolean = true,
    onOpenKavachSession: () -> Unit = {},
    onModeChange: (TimerMode) -> Unit,
    onPlayPause: () -> Unit,
    canStartBreak: Boolean,
    onStartBreak: () -> Unit,
    onReset: () -> Unit,
    onGoToDuration: () -> Unit = {},
    shieldState: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldUiState,
    isDarkTheme: Boolean,
    themeAccent: Color,
    onToggleKavach: (Boolean) -> Unit,
    onToggleStrictMode: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onNavigate: (String) -> Unit,
    isBeastMode: Boolean = false,
    selectedTheme: VisualTheme? = null,
    onOpenAnalytics: () -> Unit = {},
) {
    val scheme  = MaterialTheme.colorScheme
    val configuration   = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600
    val ink = rememberEkagraInk(onCanvas = true, theme = selectedTheme, isDarkTheme = isDarkTheme)

    val pulse by animateFloatAsState(
        targetValue    = if (isRunning) 1f else 0f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label          = "timerPulse",
    )

    Box(modifier = modifier) {
      Column(Modifier.fillMaxSize()) {
        // Pinned above the scrolling, vertically-centred timer column so the
        // summary sits just under the top bar. Inside controlsVisible on purpose:
        // once a session is running the chrome hides, and a usage counter is the
        // last thing a student should be reading mid-focus.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)),
        ) {
            com.safarparmar.app.feature.kavachanalytics.ui.KavachSummaryPills(
                ink = ink,
                onClick = onOpenAnalytics,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                       shrinkVertically(animationSpec = tween(500, easing = FastOutSlowInEasing))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(if (isCompactHeight) 8.dp else 12.dp))

                    ModeTabs(
                        selected = timerMode,
                        accentColor = themeAccent,
                        ink = ink,
                        onSelect = onModeChange,
                    )
                    Spacer(Modifier.height(if (isCompactHeight) 20.dp else 36.dp))

                    val hasAllPermissions = shieldState.hasUsageStats &&
                                            shieldState.hasOverlayPermission

                    if (hasAllPermissions && !isBeastMode) {
                        // Status chip — a hairline outline and a single accent dot
                        // instead of a frosted panel with a full switch.
                        // Under Always On the chip is read-only: tapping it used to
                        // run setEnabled(false), which clears the Always On flag and
                        // silently drops the user back to Normal mode.
                        val alwaysOn = shieldState.isAlwaysOnMode
                        val dotColor = when {
                            alwaysOn -> ink.mutedText
                            shieldState.isEnabled -> themeAccent
                            else -> null
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(EkagraChrome.stroke(1f), ink.hairline, CircleShape)
                                .then(
                                    if (alwaysOn) Modifier
                                    else Modifier.clickable { onToggleKavach(!shieldState.isEnabled) }
                                )
                                .padding(horizontal = EkagraChrome.size(14f), vertical = EkagraChrome.size(7f)),
                        ) {
                            Box(
                                Modifier
                                    .size(EkagraChrome.size(6f))
                                    .clip(CircleShape)
                                    .then(
                                        if (dotColor != null) Modifier.background(dotColor)
                                        else Modifier.border(EkagraChrome.stroke(1f), ink.mutedText, CircleShape)
                                    ),
                            )
                            Text(
                                text = when {
                                    alwaysOn -> "Kavach always on"
                                    shieldState.isEnabled -> "Kavach on"
                                    else -> "Kavach off"
                                },
                                fontSize = EkagraChrome.text(12f),
                                fontWeight = FontWeight.SemiBold,
                                color = if (shieldState.isEnabled && !alwaysOn) ink.primaryText else ink.mutedText,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            val clampedProgress = progress.coerceIn(0f, 1f)
            // ── One thin ring, one accent ─────────────────────────────────────
            // The redesign replaces the 18dp band + bloom + frosted glass disc
            // with a single hairline-weight arc, so the numerals carry the screen.
            val ringColor  = themeAccent
            val trackColor = ink.trackFaint

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(EkagraChrome.size(252f))) {
                // Track ring
                CircularProgressIndicator(
                    progress      = { 1f },
                    modifier      = Modifier.fillMaxSize(),
                    color         = trackColor,
                    strokeWidth   = EkagraChrome.stroke(5f),
                    strokeCap     = StrokeCap.Round,
                    trackColor    = Color.Transparent,
                    gapSize       = 0.dp,
                )
                // Progress ring — a soft breath of width while running is the
                // only motion left on the ring.
                CircularProgressIndicator(
                    progress      = { clampedProgress },
                    modifier      = Modifier.fillMaxSize(),
                    color         = ringColor,
                    strokeWidth   = EkagraChrome.stroke(5f + pulse * 0.8f),
                    strokeCap     = StrokeCap.Round,
                    trackColor    = Color.Transparent,
                    gapSize       = 0.dp,
                )

                // Timer text inside the ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))
                    ) {
                        // Build time text — all modes support h:mm:ss when >= 1 hour
                        val h = secondsLeft / 3600
                        val m = (secondsLeft % 3600) / 60
                        val s = secondsLeft % 60
                        val timerText = when (timerMode) {
                            TimerMode.STOPWATCH -> formatElapsedDuration(secondsLeft.toLong())
                            else -> if (h > 0) "%d:%02d:%02d".format(h, m, s)
                                    else "%02d:%02d".format(m, s)
                        }

                        // 3-tier adaptive font size: shrinks as content grows
                        // Tier 1 — under 1 min   → e.g. "59s"         → 62sp (largest)
                        // Tier 2 — 1 min – 59 min → e.g. "59m 59s"    → 50sp
                        // Tier 3 — 1 h+          → e.g. "1h 59m 59s"  → 34sp (fits comfortably)
                        val timerFontSize = when {
                            secondsLeft >= 3600 -> EkagraChrome.text(34f)
                            secondsLeft >= 60   -> EkagraChrome.text(50f)
                            else                -> EkagraChrome.text(62f)
                        }
                        val timerLetterSpacing = if (secondsLeft >= 3600) 0.sp else 1.sp

                        // The 3-2-1 countdown itself is rendered by the full-screen
                        // scrim in EkagraScreen (above the dim overlay, not inside
                        // this ring) — this inner circle only ever shows the running
                        // time once the countdown finishes.
                        Text(
                            timerText,
                            fontFamily    = EkagraSerif,
                            fontSize      = timerFontSize,
                            fontWeight    = FontWeight.Medium,
                            letterSpacing = timerLetterSpacing,
                            color         = ink.primaryText,
                            textAlign     = TextAlign.Center,
                            maxLines      = 1,
                        )
                    }
                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                               shrinkVertically(animationSpec = tween(400, easing = FastOutSlowInEasing))
                    ) {
                        val subtext = when (timerMode) {
                            TimerMode.STOPWATCH -> if (isRunning) "Stopwatch running" else "Ready to start"
                            TimerMode.BREAK -> if (isRunning) "Break running" else "Ready to break"
                            TimerMode.POMODORO -> if (isRunning) "Pomodoro running" else "Ready for Pomodoro"
                            else -> if (isRunning) "Ekagra running" else "Ready to ekagra"
                        }
                        Text(
                            subtext,
                            fontSize      = EkagraChrome.text(12f),
                            fontWeight    = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color         = ink.secondaryText,
                            textAlign     = TextAlign.Center,
                        )
                    }
                }
            }


            Spacer(Modifier.height(28.dp))

            // ── Controls — symmetrical horizontal control group ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EkagraGhostAction(
                    label = if (timerMode == TimerMode.BREAK) "End break" else "End",
                    ink   = ink,
                    onClick = onReset,
                )
                EkagraPrimaryAction(
                    label = when {
                        isRunning   -> "Pause"
                        hasProgress -> "Resume"
                        else        -> "Start"
                    },
                    accent  = themeAccent,
                    onClick = onPlayPause,
                )
                if (canStartBreak) {
                    EkagraGhostAction(
                        label = "Break",
                        ink   = ink,
                        onClick = onStartBreak,
                    )
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                       shrinkVertically(animationSpec = tween(500, easing = FastOutSlowInEasing))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))

                    Text(
                        text       = mottoText,
                        fontSize   = EkagraChrome.text(10f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color      = ink.secondaryText,
                        textAlign  = TextAlign.Center,
                    )

                }
            }
        }
    }
}
}

// ─── Bottom navigation ─────────────────────────────────────────────────────────
