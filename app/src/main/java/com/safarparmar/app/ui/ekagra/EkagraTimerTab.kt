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
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
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
            Text("%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                fontSize = if (shieldActive) 36.sp else 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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

// ─── Mode pill (icon-only) ─────────────────────────────────────────────────────

@Composable
internal fun ModePill(selected: TimerMode, accentColor: Color, onSelect: (TimerMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val modes = remember { TimerMode.entries.filter { it.showInPill } }
    val selectedIndex = when {
        selected == TimerMode.POMODORO -> 0
        else -> modes.indexOf(selected).coerceAtLeast(0)
    }

    val animatedXOffset by animateDpAsState(
        targetValue = (selectedIndex * 52).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "modePillIndicator"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.8f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
            .padding(4.dp),
    ) {
        // Sliding indicator background
        Box(
            modifier = Modifier
                .offset(x = animatedXOffset)
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            modes.forEachIndexed { index, mode ->
                val isSelected = index == selectedIndex
                val iconRes = if (isSelected) mode.lightIconRes else mode.darkIconRes

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter            = painterResource(iconRes),
                        contentDescription = mode.label,
                        tint               = if (isSelected) Color.White else scheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier           = Modifier.size(20.dp),
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
    secondsLeft: Int,
    isRunning: Boolean,
    progress: Float,
    hasProgress: Boolean,
    mottoText: String,
    countdownValue: Int,
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
) {
    val scheme  = MaterialTheme.colorScheme
    val configuration   = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600

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
                    Spacer(Modifier.height(if (isCompactHeight) 16.dp else 56.dp))
                    
                    val hasAllPermissions = shieldState.hasUsageStats &&
                                            shieldState.hasAccessibilityService &&
                                            shieldState.hasNotificationSuppressionAccess &&
                                            shieldState.hasNotifications

                    if (hasAllPermissions) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleKavach(!shieldState.isEnabled) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (shieldState.isEnabled) themeAccent else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (shieldState.isEnabled) "Kavach enabled" else "Kavach disabled",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (shieldState.isEnabled) themeAccent else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    ModePill(selected = timerMode, accentColor = themeAccent, onSelect = onModeChange)
                    Spacer(Modifier.height(32.dp))
                }
            }

            val clampedProgress = progress.coerceIn(0f, 1f)
            val isLight = scheme.background.luminance() > 0.5f
            val ringColor = scheme.primary
            val trackColor = if (isLight) {
                Color(
                    red = (scheme.primary.red * 0.45f).coerceIn(0f, 1f),
                    green = (scheme.primary.green * 0.45f).coerceIn(0f, 1f),
                    blue = (scheme.primary.blue * 0.45f).coerceIn(0f, 1f),
                    alpha = 0.9f
                )
            } else {
                scheme.secondaryContainer
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(232.dp)) {
                // Track ring
                CircularProgressIndicator(
                    progress      = { 1f },
                    modifier      = Modifier.fillMaxSize(),
                    color         = trackColor,
                    strokeWidth   = 14.dp,
                    strokeCap     = StrokeCap.Round,
                )
                // Progress glow bloom ring
                CircularProgressIndicator(
                    progress      = { clampedProgress },
                    modifier      = Modifier.fillMaxSize(),
                    color         = ringColor.copy(alpha = 0.25f + pulse * 0.15f),
                    strokeWidth   = (14f + pulse * 2f).dp,
                    strokeCap     = StrokeCap.Round,
                )
                // Progress ring
                CircularProgressIndicator(
                    progress      = { clampedProgress },
                    modifier      = Modifier.fillMaxSize(),
                    color         = ringColor,
                    strokeWidth   = 14.dp,
                    strokeCap     = StrokeCap.Round,
                )

                // Glowing pointer dot at the sweeping tip of the progress bar
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 14.dp.toPx()
                    val radiusPx = (size.width - strokeWidthPx) / 2f
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val angleRad = (clampedProgress * 360f - 90f) * (Math.PI / 180f)
                    val endX = centerX + radiusPx * kotlin.math.cos(angleRad).toFloat()
                    val endY = centerY + radiusPx * kotlin.math.sin(angleRad).toFloat()
                    
                    if (clampedProgress > 0f) {
                        val glowRadius = (8.dp.toPx()) + (pulse * 4.dp.toPx())
                        drawCircle(
                            color = ringColor.copy(alpha = 0.4f + pulse * 0.2f),
                            radius = glowRadius,
                            center = androidx.compose.ui.geometry.Offset(endX, endY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(endX, endY)
                        )
                    }
                }

                // Frosted glass inner center circle
                Box(
                    Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                )

                // Timer text inside inner circle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))
                    ) {
                        if (countdownValue > 0) {
                            Text(
                                countdownValue.toString(),
                                fontSize     = 80.sp,
                                fontWeight   = FontWeight.Bold,
                                color        = Color.White,
                                textAlign    = TextAlign.Center,
                            )
                        } else {
                            Text(
                                "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                                fontSize     = 54.sp,
                                fontWeight   = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color        = Color.White,
                                textAlign    = TextAlign.Center,
                            )
                        }
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
                            else -> if (isRunning) "Ekagra running" else "Ready to ekagra"
                        }
                        Text(
                            subtext,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color.White.copy(alpha = 0.7f),
                            textAlign  = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Control buttons styled as squircles
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // End / Reset button
                FilledTonalButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor   = Color.White,
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape  = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) "End" else "End Break",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Start/Pause button
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor   = themeAccent,
                    ),
                    shape     = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val playPauseLabel = when {
                            isRunning -> "Pause"
                            hasProgress -> "Resume"
                            else -> "Start"
                        }
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(playPauseLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
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
                    if (canStartBreak) {
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = onStartBreak,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors   = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.08f),
                                contentColor   = Color.White,
                            ),
                            border    = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            shape  = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FreeBreakfast, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Take break", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text       = mottoText,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        color      = Color.White.copy(alpha = 0.7f),
                        textAlign  = TextAlign.Center,
                    )

                }
            }
        }
    }
}

// ─── Bottom navigation ─────────────────────────────────────────────────────────
