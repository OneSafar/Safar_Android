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
internal fun DurationTab(
    modifier: Modifier = Modifier,
    focusMinutes: Int,
    breakMinutes: Int,
    onFocusChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    isMuted: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onStartPomodoro: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var showPomodoroDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pomodoroLoopsInput by remember { androidx.compose.runtime.mutableStateOf("4") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            // M3 background token
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Timer duration",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = scheme.onSurface)

        DurationCard(
            icon          = Icons.Default.Timer,
            title         = "Ekagra duration",
            value         = focusMinutes,
            range         = 1f..120f,
            onValueChange = onFocusChange,
        )
        DurationCard(
            icon          = Icons.Default.FreeBreakfast,
            title         = "Break duration",
            value         = breakMinutes,
            range         = 1f..60f,
            onValueChange = onBreakChange,
        )

        ToggleCard(
            icon          = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            title         = "Audio",
            subtitle      = if (isMuted) "Background audio is muted" else "Background audio is playing",
            checked       = !isMuted,
            onCheckedChange = { onMuteChange(!it) }
        )

        Spacer(Modifier.height(6.dp))

        // M3 FilledButton — primary CTA, 56dp tall for prominence
        Button(
            onClick        = onSave,
            modifier       = Modifier.fillMaxWidth().height(56.dp),
            shape          = RoundedCornerShape(16.dp),
            colors         = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor   = scheme.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick        = { showPomodoroDialog = true },
            modifier       = Modifier.fillMaxWidth().height(56.dp),
            shape          = RoundedCornerShape(16.dp),
            colors         = ButtonDefaults.buttonColors(
                containerColor = scheme.secondaryContainer,
                contentColor   = scheme.onSecondaryContainer,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Text("Start Pomodoro Session", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
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

@Composable
internal fun DurationCard(
    icon: ImageVector,
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    var showCustomInput by remember { mutableStateOf(false) }
    var customText      by remember { mutableStateOf("") }
    val alpha = if (enabled) 1f else 0.5f

    // M3 ElevatedCard: surfaceContainerLow + outlineVariant border at 0.5dp
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth().alpha(alpha),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null,
                    tint     = scheme.primary,
                    modifier = Modifier.size(20.dp))
                Text(title,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 16.sp,
                    color      = scheme.onSurface,
                    modifier   = Modifier.weight(1f))
                // M3 displayLarge-ish value badge
                Text("${value}m",
                    fontWeight = FontWeight.Bold,
                    color      = scheme.onSurface,
                    fontSize   = 26.sp)
                // Small edit icon button
                IconButton(
                    onClick  = { showCustomInput = !showCustomInput; customText = "" },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Custom value",
                        modifier = Modifier.size(15.dp), tint = scheme.onSurfaceVariant)
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
                        shape         = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                    Button(
                        onClick = {
                            val v = customText.toIntOrNull()
                            if (v != null && v >= range.start.toInt() && v <= range.endInclusive.toInt()) {
                                onValueChange(v); showCustomInput = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Set") }
                }
            }

            // M3 Slider — track = secondaryContainer, thumb + fill = primary
            SlimSlider(
                value         = value.toFloat(),
                onValueChange = { if (enabled) onValueChange(it.roundToInt().coerceIn(range.start.toInt(), range.endInclusive.toInt())) },
                valueRange    = range,
                modifier      = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                activeColor   = scheme.primary,
                inactiveColor = scheme.secondaryContainer,
            )

            // Preset chips — M3 InputChip style
            val presets = if (range.endInclusive <= 60f) listOf(1, 5, 15, 30) else listOf(1, 25, 45, 60, 90)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    val isSel = value == preset
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            // M3: primary when selected, surfaceContainerHigh when not
                            .background(if (isSel) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { if (enabled) onValueChange(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${preset}m",
                            fontSize   = 14.sp,
                            // M3: onPrimary when selected, onSurfaceVariant when not
                            color      = if (isSel) scheme.onPrimary else scheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// ─── History tab ───────────────────────────────────────────────────────────────
