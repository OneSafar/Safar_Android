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
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
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
) {
    val scheme  = MaterialTheme.colorScheme
    val configuration   = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600
    // The Timer tab floats over the scrimmed video/gradient canvas, so its ink is
    // always light-on-dark regardless of the app's light/dark setting.
    val ink = rememberEkagraInk(onCanvas = true)

    val pulse by animateFloatAsState(
        targetValue    = if (isRunning) 1f else 0f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label          = "timerPulse",
    )

    Box(modifier = modifier) {
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
                    Spacer(Modifier.height(if (isCompactHeight) 16.dp else 40.dp))

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(1.dp, ink.hairline, CircleShape)
                                .clickable { onToggleKavach(!shieldState.isEnabled) }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (shieldState.isEnabled) Modifier.background(themeAccent)
                                        else Modifier.border(1.dp, ink.mutedText, CircleShape)
                                    ),
                            )
                            Text(
                                text = if (shieldState.isEnabled) "Kavach on" else "Kavach off",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (shieldState.isEnabled) ink.primaryText else ink.mutedText,
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

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(252.dp)) {
                // Track ring
                CircularProgressIndicator(
                    progress      = { 1f },
                    modifier      = Modifier.fillMaxSize(),
                    color         = trackColor,
                    strokeWidth   = 5.dp,
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
                    strokeWidth   = (5f + pulse * 0.8f).dp,
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
                            secondsLeft >= 3600 -> 34.sp
                            secondsLeft >= 60   -> 50.sp
                            else                -> 62.sp
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
                            fontWeight    = FontWeight.Normal,
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
                            fontSize      = 12.sp,
                            fontWeight    = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            color         = ink.secondaryText,
                            textAlign     = TextAlign.Center,
                        )
                    }
                }
            }


            Spacer(Modifier.height(28.dp))

            // ── Controls — one quiet text action beside one accent pill ──
            // The accent is whatever the active visual theme supplies, so this
            // still recolours with the user's chosen theme.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)) +
                       shrinkVertically(animationSpec = tween(500, easing = FastOutSlowInEasing))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (canStartBreak) {
                        Spacer(Modifier.height(4.dp))
                        EkagraGhostAction(
                            label = "Take a break",
                            ink   = ink,
                            onClick = onStartBreak,
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text       = mottoText,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color      = ink.mutedText,
                        textAlign  = TextAlign.Center,
                    )

                }
            }
        }
    }
}

// ─── Bottom navigation ─────────────────────────────────────────────────────────
