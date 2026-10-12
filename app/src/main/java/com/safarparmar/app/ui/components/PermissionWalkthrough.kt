package com.safarparmar.app.ui.components

import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Illustrations, not a replica of an OEM Settings app. No bitmap assets or blur layers. */
enum class PermissionWalkthroughKind { ACCESSIBILITY, USAGE, OVERLAY, NOTIFICATION_ACCESS, NOTIFICATIONS }

internal data class PermissionWalkthroughStep(val title: String, val instruction: String, val screen: String, val row: String)
internal data class PermissionWalkthroughContent(val title: String, val reason: String, val warning: String, val steps: List<PermissionWalkthroughStep>)

internal fun permissionWalkthroughContent(kind: PermissionWalkthroughKind): PermissionWalkthroughContent = when (kind) {
    PermissionWalkthroughKind.ACCESSIBILITY -> PermissionWalkthroughContent(
        "Connect YouTube Focus", "Let SAFAR identify the channel of an opened YouTube video.",
        "Android grants this service access to screen content and actions. Enable it only if you agree to YouTube Focus’s accessibility disclosure.",
        listOf(
            PermissionWalkthroughStep("Find the service", "In Accessibility, look under Downloaded apps or Installed services. Choose SAFAR YouTube Focus.", "Accessibility", "SAFAR YouTube Focus"),
            PermissionWalkthroughStep("Enable the service", "Turn on Use SAFAR YouTube Focus. Read Android’s confirmation and choose Allow if you agree.", "SAFAR YouTube Focus", "Use SAFAR YouTube Focus"),
            PermissionWalkthroughStep("Return to SAFAR", "Go back to SAFAR, then choose your productive channels. Accessibility alone does not finish setup.", "Back in SAFAR", "Choose productive channels"),
        ),
    )
    PermissionWalkthroughKind.USAGE -> PermissionWalkthroughContent(
        "Allow app check", "KAVACH uses app activity to detect when you open an app you chose to block.",
        "Usage Access exposes app usage information, not message or photo contents. You can revoke it in Settings.",
        listOf(
            PermissionWalkthroughStep("Choose SAFAR", "Open Usage Access and select SAFAR if a list appears. Some phones open SAFAR’s page directly.", "Usage access", "SAFAR"),
            PermissionWalkthroughStep("Allow usage access", "Turn on Permit usage access, then return to SAFAR.", "SAFAR", "Permit usage access"),
        ),
    )
    PermissionWalkthroughKind.OVERLAY -> PermissionWalkthroughContent(
        "Allow the focus screen", "KAVACH needs to show its block screen above distracting apps.",
        "This permission lets SAFAR draw over other apps. It does not itself grant access to their contents.",
        listOf(
            PermissionWalkthroughStep("Open SAFAR’s page", "Choose SAFAR under Display over other apps, Appear on top, or Floating windows if a list appears.", "Display over other apps", "SAFAR"),
            PermissionWalkthroughStep("Allow display over apps", "Enable the switch, then return to SAFAR to continue setup.", "SAFAR", "Allow display over other apps"),
        ),
    )
    PermissionWalkthroughKind.NOTIFICATION_ACCESS -> PermissionWalkthroughContent(
        "Set up Notification Shield", "Optional: hide notifications from blocked apps during your study timer.",
        "Android grants access to notification content and dismissal. KAVACH uses it to dismiss blocked-app notifications during focus; notification text is not saved.",
        listOf(
            PermissionWalkthroughStep("Choose Notification Shield", "In Notification Access, select SAFAR’s Notification shield service if a list appears.", "Notification access", "Notification shield"),
            PermissionWalkthroughStep("Review and allow", "Enable notification access. Read Android’s warning and confirm only if you agree, then return to SAFAR.", "Notification Shield", "Allow notification access"),
        ),
    )
    PermissionWalkthroughKind.NOTIFICATIONS -> PermissionWalkthroughContent(
        "Allow study notifications", "Receive timer progress, study reminders, and break alerts.",
        "Optional. This allows SAFAR to send notifications; it does not grant access to notifications from other apps.",
        listOf(PermissionWalkthroughStep("Allow notifications", "Choose Allow in Android’s prompt, or enable SAFAR notifications in Settings if the prompt is unavailable.", "SAFAR notifications", "Allow notifications")),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionWalkthroughSheet(
    kind: PermissionWalkthroughKind,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val content = remember(kind) { permissionWalkthroughContent(kind) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(content.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(content.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (kind != PermissionWalkthroughKind.NOTIFICATIONS) PermissionWalkthrough(kind)
            Text(content.warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(when (kind) {
                    PermissionWalkthroughKind.NOTIFICATIONS -> "Continue"
                    PermissionWalkthroughKind.NOTIFICATION_ACCESS -> "Agree & open Settings"
                    else -> "Open Settings"
                }, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Not now") }
        }
    }
}

@Composable
fun PermissionWalkthrough(kind: PermissionWalkthroughKind, modifier: Modifier = Modifier) {
    val content = remember(kind) { permissionWalkthroughContent(kind) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // No automatic progression with system motion disabled or a screen reader active.
    val accessibility = remember(context) {
        context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
    }
    fun canAnimate() = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f && !accessibility.isTouchExplorationEnabled
    var motionAllowed by remember(context) { mutableStateOf(canAnimate()) }
    DisposableEffect(context, accessibility) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { motionAllowed = canAnimate() }
        }
        val listener = android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { motionAllowed = canAnimate() }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        accessibility.addTouchExplorationStateChangeListener(listener)
        onDispose {
            resolver.unregisterContentObserver(observer)
            accessibility.removeTouchExplorationStateChangeListener(listener)
        }
    }
    var step by rememberSaveable(kind) { mutableIntStateOf(0) }
    var playing by rememberSaveable(kind) { mutableStateOf(motionAllowed && content.steps.size > 1) }
    LaunchedEffect(kind, playing, lifecycle, motionAllowed) {
        if (playing && motionAllowed) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(3200)
                if (step < content.steps.lastIndex) step++ else { playing = false; break }
            }
        }
    }
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("How to enable", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (motionAllowed && content.steps.size > 1) TextButton(onClick = {
                if (!playing && step == content.steps.lastIndex) step = 0
                playing = !playing
            }) { Text(if (playing) "Pause" else if (step == content.steps.lastIndex) "Replay" else "Play") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content.steps.indices.forEach { index ->
                FilterChip(selected = step == index, onClick = { playing = false; step = index }, label = { Text("Step ${index + 1}") })
            }
        }
        // Stable bounds; only opacity changes. No resizing, blur, looping media or per-frame layout.
        Surface(shape = RoundedCornerShape(16.dp), color = scheme.surfaceVariant, border = BorderStroke(1.dp, scheme.outlineVariant)) {
            Crossfade(targetState = step, animationSpec = tween(if (motionAllowed) 450 else 0), label = "Permission illustration") { index ->
                val current = content.steps[index]
                Column(Modifier.fillMaxWidth().heightIn(min = 148.dp).padding(20.dp).clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(current.screen, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    HorizontalDivider(color = scheme.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(current.row, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        if (index == 1 || content.steps.size == 1) {
                            // Display-only system switch. The real switch is in Android Settings.
                            Switch(checked = true, onCheckedChange = null)
                        } else {
                            Icon(if (index == 0) Icons.Default.ChevronRight else Icons.Default.Check, null, tint = scheme.primary)
                        }
                    }
                }
            }
        }
        Text(content.steps[step].title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(content.steps[step].instruction, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        Text("Illustration only · Settings labels vary by phone", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
    }
}
