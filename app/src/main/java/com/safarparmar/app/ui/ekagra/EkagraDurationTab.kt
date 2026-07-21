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
import androidx.compose.ui.draw.scale
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.SafarGlassCard
import com.safarparmar.app.ui.glass.SafarGlassChromeRadius
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
internal fun DurationTab(
    modifier: Modifier = Modifier,
    focusMinutes: Int,
    breakMinutes: Int,
    onFocusChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    isMuted: Boolean,
    onMuteChange: (Boolean) -> Unit,
    autoStartBreak: Boolean,
    onAutoStartBreakChange: (Boolean) -> Unit,
    timerAlertStyle: com.safarparmar.app.data.local.TimerAlertStyle,
    onTimerAlertStyleChange: (com.safarparmar.app.data.local.TimerAlertStyle) -> Unit,
    onStartPomodoro: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)
    var showPomodoroDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pomodoroLoopsInput by remember { androidx.compose.runtime.mutableStateOf("4") }

    Column(
        modifier = modifier
            .fillMaxSize()
            // M3 background token
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp),
    ) {
        EkagraEyebrow("Settings", ink.secondaryText)
        Spacer(Modifier.height(6.dp))
        EkagraDisplayTitle("Set your rhythm", ink.primaryText)
        Spacer(Modifier.height(22.dp))

        DurationSection(
            label         = "Ekagra",
            value         = focusMinutes,
            range         = 1f..120f,
            presets       = listOf(15, 25, 45, 60),
            ink           = ink,
            onValueChange = onFocusChange,
        )
        DurationSection(
            label         = "Break",
            value         = breakMinutes,
            range         = 1f..60f,
            presets       = listOf(5, 10, 15, 30),
            ink           = ink,
            onValueChange = onBreakChange,
        )

        EkagraHairline(ink.hairline)
        SettingToggleRow(
            title    = "Audio",
            subtitle = if (isMuted) "Background audio is muted" else "Background audio is playing",
            checked  = !isMuted,
            ink      = ink,
            onCheckedChange = { onMuteChange(!it) },
        )

        EkagraHairline(ink.hairline)
        SettingToggleRow(
            title    = "Auto-start breaks",
            subtitle = if (autoStartBreak) "Break starts when the timer ends" else "Break stays paused until you start it",
            checked  = autoStartBreak,
            ink      = ink,
            onCheckedChange = onAutoStartBreakChange,
        )

        EkagraHairline(ink.hairline)
        TimerAlertStyleRow(
            selectedStyle = timerAlertStyle,
            ink = ink,
            onStyleSelected = onTimerAlertStyleChange,
        )
        EkagraHairline(ink.hairline)

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EkagraPrimaryAction(
                label   = "Save changes",
                accent  = scheme.primary,
                onClick = onSave,
            )
            EkagraGhostAction(
                label   = "Pomodoro",
                ink     = ink,
                onClick = { showPomodoroDialog = true },
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    if (showPomodoroDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPomodoroDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { androidx.compose.material3.Text("Start Pomodoro") },
            text = {
                Column {
                    androidx.compose.material3.Text("How many loops would you like to run?")
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pomodoroLoopsInput,
                        onValueChange = { pomodoroLoopsInput = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showPomodoroDialog = false
                    onStartPomodoro(pomodoroLoopsInput.toIntOrNull() ?: 4)
                }) {
                    androidx.compose.material3.Text("Start")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPomodoroDialog = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}

/**
 * One duration control: a hairline rule, a label, a serif numeral, a line slider
 * and its presets. No card, no icon — the rule above it is the container.
 */
@Composable
internal fun DurationSection(
    label: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    ink: EkagraInk,
    presets: List<Int> = emptyList(),
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true,
    // The slider is capped at [range], but users can type a longer custom value
    // (e.g. a 5-hour study block). The typed value is only bound by this cap and
    // the digit-count limit below — it is NOT clamped to the slider's max.
    customMaxMinutes: Int = 600,
) {
    val scheme = MaterialTheme.colorScheme
    var showCustomInput by remember { mutableStateOf(false) }
    var customText      by remember { mutableStateOf("") }
    val alpha = if (enabled) 1f else 0.5f

    EkagraHairline(ink.hairline)
    Column(
        modifier = Modifier.fillMaxWidth().alpha(alpha).padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Label ↔ serif value, sharing a baseline
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = ink.primaryText,
                modifier   = Modifier.weight(1f),
            )
            Text(
                "$value",
                fontFamily = EkagraSerif,
                fontWeight = FontWeight.Normal,
                fontSize   = 28.sp,
                color      = ink.primaryText,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "min",
                fontSize = 13.sp,
                color    = ink.mutedText,
                modifier = Modifier.padding(bottom = 3.dp),
            )
            // Type an exact value — a quiet pencil, not a filled icon button
            IconButton(
                onClick  = { showCustomInput = !showCustomInput; customText = "" },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Custom value",
                    modifier = Modifier.size(14.dp), tint = ink.mutedText)
            }
        }

        // Inline custom input (visible only when edit tapped)
        if (showCustomInput) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = customText,
                    onValueChange = { customText = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder   = { Text("Minutes") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                    shape         = RoundedCornerShape(SafarGlassChromeRadius),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                )
                EkagraPrimaryAction(
                    label  = "Set",
                    accent = scheme.primary,
                    onClick = {
                        // Accept any typed value from the range floor up to
                        // customMaxMinutes — NOT limited to the slider's max, so
                        // e.g. 300 / 400 min are honoured even though the slider
                        // only goes to range.endInclusive.
                        val v = customText.toIntOrNull()
                        if (v != null && v >= range.start.toInt() && v <= customMaxMinutes) {
                            onValueChange(v); showCustomInput = false
                        }
                    },
                )
            }
        }

        // Line slider — fill and knob track the active theme accent
        SlimSlider(
            value         = value.toFloat(),
            onValueChange = { if (enabled) onValueChange(it.roundToInt().coerceIn(range.start.toInt(), range.endInclusive.toInt())) },
            valueRange    = range,
            modifier      = Modifier.fillMaxWidth(),
            activeColor   = scheme.primary,
            inactiveColor = ink.trackFaint,
        )

        // Range end-caps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${range.start.toInt()} min", fontSize = 11.sp, color = ink.mutedText)
            Text("${range.endInclusive.toInt()} min", fontSize = 11.sp, color = ink.mutedText)
        }

        if (presets.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    EkagraPill(
                        label    = "$preset min",
                        selected = value == preset,
                        accent   = scheme.primary,
                        ink      = ink,
                        onClick  = { if (enabled) onValueChange(preset) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerAlertStyleRow(
    selectedStyle: com.safarparmar.app.data.local.TimerAlertStyle,
    ink: EkagraInk,
    onStyleSelected: (com.safarparmar.app.data.local.TimerAlertStyle) -> Unit,
) {
    var showSelector by remember { mutableStateOf(false) }
    val selectedLabel = when (selectedStyle) {
        com.safarparmar.app.data.local.TimerAlertStyle.SOUND -> "Sound"
        com.safarparmar.app.data.local.TimerAlertStyle.VIBRATE -> "Vibrate"
        com.safarparmar.app.data.local.TimerAlertStyle.OFF -> "Off"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSelector = true }
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Timer alert", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink.primaryText)
            Text("How Ekagra tells you a session ended", fontSize = 12.sp, color = ink.mutedText)
        }
        Text(selectedLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink.secondaryText)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Choose timer alert style",
            tint = ink.mutedText,
            modifier = Modifier.size(18.dp),
        )
    }

    if (showSelector) {
        AlertDialog(
            onDismissRequest = { showSelector = false },
            title = { Text("Timer Alert Style") },
            text = {
                Column {
                    listOf(
                        com.safarparmar.app.data.local.TimerAlertStyle.VIBRATE to "Vibrate",
                        com.safarparmar.app.data.local.TimerAlertStyle.SOUND to "Sound",
                        com.safarparmar.app.data.local.TimerAlertStyle.OFF to "Off",
                    ).forEach { (style, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStyleSelected(style)
                                    showSelector = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedStyle == style,
                                onClick = {
                                    onStyleSelected(style)
                                    showSelector = false
                                },
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelector = false }) { Text("Cancel") }
            },
        )
    }
}

/** Settings toggle as a plain row between hairlines — no card, no leading icon. */
@Composable
internal fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    ink: EkagraInk,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink.primaryText)
            Text(subtitle, fontSize = 12.sp, color = ink.mutedText)
        }
        // M3 Switch keeps its default colours so it follows the active theme.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f),
        )
    }
}

// ─── History tab ───────────────────────────────────────────────────────────────
