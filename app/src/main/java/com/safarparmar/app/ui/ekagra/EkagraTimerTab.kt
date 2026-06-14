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
internal fun ModePill(selected: TimerMode, onSelect: (TimerMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            // M3 token: surfaceContainerHigh
            .background(scheme.surfaceContainerHigh)
            .border(0.5.dp, scheme.outlineVariant, RoundedCornerShape(50.dp))
            .padding(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimerMode.entries.filter { it.showInPill }.forEach { mode ->
                val isSelected = mode == selected
                // Icon resource: use light (white) icon when selected on primary bg,
                // dark icon when unselected on surfaceContainerHigh bg.
                val iconRes = if (isSelected) mode.lightIconRes else mode.darkIconRes

                Box(
                    modifier = Modifier
                        // Fixed 48dp square chip — no text label
                        .size(48.dp)
                        .clip(RoundedCornerShape(50.dp))
                        // M3 token: primary when selected, transparent when not
                        .background(if (isSelected) scheme.primary else Color.Transparent)
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter            = painterResource(iconRes),
                        contentDescription = mode.label, // keep for a11y — screen reader reads this
                        // M3 token: onPrimary when selected, onSurfaceVariant when not
                        tint               = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier           = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

// ─── Timer / Focus tab ─────────────────────────────────────────────────────────

@Composable
internal fun TimerFocusTab(
    modifier: Modifier,
    timerMode: TimerMode,
    secondsLeft: Int,
    isRunning: Boolean,
    progress: Float,
    mottoText: String,
    kavachActive: Boolean = false,
    kavachBlockedCount: Int = 0,
    onOpenKavachSession: () -> Unit = {},
    onModeChange: (TimerMode) -> Unit,
    onPlayPause: () -> Unit,
    canStartBreak: Boolean,
    onStartBreak: () -> Unit,
    onReset: () -> Unit,
    shieldState: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldUiState,
    isDarkTheme: Boolean,
    themeAccent: Color,
    onToggleKavach: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val scheme  = MaterialTheme.colorScheme
    // On the timer tab the background is always the video scrim (dark).
    // Use onSurface tokens from our M3 scheme, which the dark themeColorScheme
    // already resolves to light colours.
    val configuration   = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600

    // Pulse animation for the ring inner glow
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
        ) {
            Spacer(Modifier.height(if (isCompactHeight) 16.dp else 56.dp))

            // Icon-only mode pill
            ModePill(selected = timerMode, onSelect = onModeChange)

            Spacer(Modifier.height(32.dp))

            // ── Ring — NO card wrapper ───────────────────────────────────────────
            // The ring floats directly on the video background.
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
                // Progress glow bloom ring (thicker, translucent, pulses when running)
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
                // Subtle inner glow, pulses when running
                Box(
                    Modifier
                        .size((180 + pulse * 10).dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.05f + pulse * 0.04f)),
                )
                // Timer text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))
                    ) {
                        Text(
                            "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                            fontSize     = 54.sp,
                            fontWeight   = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            // M3 onSurface — white in dark theme
                            color        = scheme.onSurface,
                            textAlign    = TextAlign.Center,
                        )
                    }
                    Text(
                        if (isRunning) "Focus running" else "Ready to focus",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        // M3 onSurfaceVariant
                        color      = scheme.onSurfaceVariant,
                        textAlign  = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Control buttons ──────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // End — M3 FilledTonalButton style
                val isLight = scheme.background.luminance() > 0.5f
                FilledTonalButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isLight) Color(0xD9FFFFFF) else scheme.onSurface,
                        contentColor   = if (isLight) scheme.onSurface else scheme.surface,
                    ),
                    border = if (isLight) BorderStroke(1.dp, scheme.outlineVariant) else null,
                    elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 2.dp),
                    shape  = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("End", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Start/Pause — M3 FilledButton
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor   = scheme.onPrimary,
                    ),
                    shape     = RoundedCornerShape(100.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(if (isRunning) "Pause" else "Start", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Take Break — M3 FilledTonalButton, only when a focus session is active
            if (canStartBreak) {
                Spacer(Modifier.height(12.dp))
                val isLight = scheme.background.luminance() > 0.5f
                FilledTonalButton(
                    onClick = onStartBreak,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isLight) Color(0xD9FFFFFF) else scheme.onSurfaceVariant,
                        contentColor   = if (isLight) scheme.onSurfaceVariant else scheme.surface,
                    ),
                    border = if (isLight) BorderStroke(1.dp, scheme.outlineVariant) else null,
                    elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 2.dp),
                    shape  = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FreeBreakfast, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("Take break", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Motto line
            Text(
                text       = mottoText,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color      = scheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
            )

            // Kavach active pill
            if (kavachActive) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = onOpenKavachSession,
                    shape   = RoundedCornerShape(100.dp),
                    color   = com.safarparmar.app.ui.ekagra.focusshield.KavachDesign.Primary.copy(alpha = 0.92f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.kavach_active_status, kavachBlockedCount),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Inline KAVACH card
            com.safarparmar.app.ui.ekagra.focusshield.EkagraKavachInlineCard(
                shieldState = shieldState,
                accent = themeAccent,
                isDarkTheme = isDarkTheme,
                forceExpanded = false,
                isSessionRunning = isRunning,
                onToggleEnabled = onToggleKavach,
                onOpenAppPicker = onOpenAppPicker,
                onSetupPermissions = { onNavigate(Routes.KAVACH_PERMISSION_ONBOARDING) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            )
        }
    }
}

// ─── Bottom navigation ─────────────────────────────────────────────────────────
