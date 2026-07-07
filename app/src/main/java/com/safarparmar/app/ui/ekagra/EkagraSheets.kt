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
internal fun VisualThemeDialog(current: VisualTheme, onSelect: (VisualTheme) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = scheme.surface,
        title  = { Text("Visual theme", fontWeight = FontWeight.Bold) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visualThemes.forEach { theme ->
                    val isSelected = theme.name == current.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            // M3: primaryContainer tint when selected
                            .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh)
                            .then(if (isSelected) Modifier.border(1.5.dp, scheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onSelect(theme) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(theme.accent),
                            contentAlignment = Alignment.Center) {
                            Text(theme.emoji, fontSize = 18.sp)
                        }
                        Text(theme.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                            modifier   = Modifier.weight(1f))
                        if (isSelected)
                            Icon(Icons.Default.Check, contentDescription = null,
                                tint = scheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

// SongPickerSheet removed in favor of shared AudioLibraryPanel

// ─── Organize free focus sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizeFreeFocusSheet(
    pending: PendingEndedEkagraSession?,
    goals: List<com.safarparmar.app.domain.model.Goal>,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaveFree: () -> Unit,
    onLinkGoal: (com.safarparmar.app.domain.model.Goal, Boolean) -> Unit,
    onDiscard: () -> Unit,
) {
    val scheme      = MaterialTheme.colorScheme
    val focusedSeconds = pending?.let {
        if (it.mode.equals("stopwatch", ignoreCase = true)) {
            it.secondsLeft
        } else {
            it.totalSeconds - it.secondsLeft
        }
    } ?: 0
    val focusedMins = focusedSeconds.coerceAtLeast(60) / 60

    // Which goal is currently selected (pending confirmation)
    var selectedGoal    by remember { mutableStateOf<com.safarparmar.app.domain.model.Goal?>(null) }
    var markAsCompleted by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = scheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "You focused for ${focusedMins} min.",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
            Text("What were you working on?", fontSize = 13.sp, color = scheme.onSurfaceVariant)

            Text(
                "Link to a goal",
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant,
            )

            if (goals.isEmpty()) {
                Text("No open goals available.", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            } else {
                goals.take(5).forEach { goal ->
                    val isSelected = selectedGoal?.id == goal.id
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainer)
                            .then(if (isSelected) Modifier.border(1.5.dp, scheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable {
                                selectedGoal = goal
                                markAsCompleted = false
                            }
                            .padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.CheckCircle else Icons.Default.Link,
                                contentDescription = null,
                                tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                goal.title,
                                fontSize   = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color      = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                                modifier   = Modifier.weight(1f),
                                maxLines   = 1,
                            )
                        }
                        // "Mark as completed" toggle — only shown for the selected goal
                        if (isSelected) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(scheme.surfaceContainerHighest)
                                    .clickable { markAsCompleted = !markAsCompleted }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Checkbox(
                                    checked         = markAsCompleted,
                                    onCheckedChange = { markAsCompleted = it },
                                    colors          = CheckboxDefaults.colors(checkedColor = scheme.primary),
                                )
                                Column {
                                    Text(
                                        "Mark goal as completed",
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = scheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confirm link button — only shown once a goal is selected
            if (selectedGoal != null) {
                Button(
                    onClick = {
                        val g = selectedGoal ?: return@Button
                        onLinkGoal(g, markAsCompleted)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor   = scheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (markAsCompleted) "Link & Complete Goal" else "Link Session",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            OutlinedTextField(
                value         = titleInput,
                onValueChange = onTitleChange,
                placeholder   = { Text("Or add a free title") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard", fontSize = 12.sp)
                }
                Button(
                    onClick  = onSaveFree,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor   = scheme.onPrimary,
                    ),
                ) {
                    Text("Save Free", fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Private data class ────────────────────────────────────────────────────────
