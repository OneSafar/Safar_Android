package com.safarparmar.app.ui.studyplanner.components

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import com.safarparmar.app.ui.theme.isLightBackground

/** Strong compositor blur for macOS glass dialogs and overlays (API 31+). */
const val SafarBackdropBlurRadiusPx = 96

/** Moderate blur for small popup menus — heavy blur under a tiny panel reads muddy. */
const val SafarMenuBackdropBlurRadiusPx = 48

/**
 * Real backdrop blur for the planner's floating glass surfaces.
 *
 * [PlannerGlass] notes that Compose has no *composable-level* backdrop blur:
 * `Modifier.blur` blurs a composable's own content, not what sits behind it, so
 * it can't frost a background. That's still true — but it isn't the whole story,
 * and the glass surfaces that matter don't need it.
 *
 * Dialogs and popups are separate **windows**. Since API 31 a window can ask the
 * compositor to blur everything drawn behind it via [WindowManager.LayoutParams.FLAG_BLUR_BEHIND]
 * plus `blurBehindRadius`. The blur happens below the window in the compositor,
 * so a translucent panel — or a translucent [GlassButton] inside it — genuinely
 * shows blurred content through itself. That is the macOS effect, not a
 * simulation of it.
 *
 * Support is conditional and must be checked at runtime, not just by SDK level:
 *  - API < 31: no cross-window blur at all.
 *  - [WindowManager.isCrossWindowBlurEnabled] is false when the user has turned
 *    off transparency/animations, when battery saver is on, or when the device
 *    or emulator can't afford the effect. The system flips this at runtime.
 *
 * Whenever blur is unavailable the surfaces fall back to exactly what they
 * rendered before — translucency + top-edge light border + depth shadow — which
 * is why the scrim/translucency values are kept rather than replaced.
 *
 * Returns whether blur is actually active, so callers can lighten their scrim:
 * a blurred backdrop needs far less dimming to keep the panel legible, and
 * double-darkening it would hide the very blur we just enabled.
 */
fun safarGlassDialogScrimColor(isDarkTheme: Boolean, blurred: Boolean): Color = when {
    blurred && isDarkTheme -> Color.Black.copy(alpha = 0.28f)
    blurred -> Color(0xFF1C1C1E).copy(alpha = 0.12f)
    isDarkTheme -> Color.Black.copy(alpha = 0.55f)
    else -> Color(0xFF1C1C1E).copy(alpha = 0.28f)
}

/**
 * Full-screen host for floating macOS glass dialogs: enables cross-window backdrop
 * blur on the dialog window and paints a theme-aware scrim underneath the panel.
 */
@Composable
fun SafarGlassDialogHost(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = !MaterialTheme.colorScheme.background.isLightBackground(),
    blurRadiusPx: Int = SafarBackdropBlurRadiusPx,
    contentAlignment: Alignment = Alignment.Center,
    applyImePadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val blurred = rememberPlannerBackdropBlur(radiusPx = blurRadiusPx)
    val scrimColor = safarGlassDialogScrimColor(isDarkTheme, blurred)
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (applyImePadding) Modifier.imePadding() else Modifier)
            .background(scrimColor),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** Call at the top of [androidx.compose.material3.ModalBottomSheet] content to blur the page behind the sheet. */
@Composable
fun SafarEnableSheetBackdropBlur(radiusPx: Int = SafarBackdropBlurRadiusPx) {
    rememberPlannerBackdropBlur(radiusPx = radiusPx)
}

@Composable
fun rememberPlannerBackdropBlur(radiusPx: Int = SafarBackdropBlurRadiusPx): Boolean {
    val view = LocalView.current
    val supported = remember(view) { isCrossWindowBlurSupported(view) }

    DisposableEffect(view, supported, radiusPx) {
        if (!supported) return@DisposableEffect onDispose { }
        val applied = runCatching { applyBlurBehind(view, radiusPx) }.getOrDefault(false)
        onDispose {
            if (applied) runCatching { applyBlurBehind(view, 0) }
        }
    }
    return supported
}

private fun isCrossWindowBlurSupported(view: View): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val wm = view.context.getSystemService(WindowManager::class.java) ?: return false
    // Runtime check, not just an SDK check — the system disables cross-window
    // blur under battery saver, reduced-transparency, and on low-end hardware.
    return runCatching { wm.isCrossWindowBlurEnabled }.getOrDefault(false)
}

/**
 * Applies (or with [radiusPx] == 0, clears) blur-behind on whichever window this
 * composition lives in.
 *
 * Two hosts, two routes:
 *  - A `Dialog`'s parent implements Compose's [DialogWindowProvider], which
 *    hands back the real [Window].
 *  - A `Popup` (which is what a DropdownMenu is) has no such interface — its
 *    root view is added to the WindowManager directly, so its own layoutParams
 *    *are* the window's [WindowManager.LayoutParams] and are updated in place.
 */
private fun applyBlurBehind(view: View, radiusPx: Int): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    if (dialogWindow != null) {
        applyToDialogWindow(dialogWindow, radiusPx)
        return true
    }

    val params = view.layoutParams as? WindowManager.LayoutParams ?: return false
    val wm = view.context.getSystemService(WindowManager::class.java) ?: return false
    params.flags = if (radiusPx > 0) {
        params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
    } else {
        params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
    }
    params.blurBehindRadius = radiusPx
    wm.updateViewLayout(view, params)
    return true
}

private fun applyToDialogWindow(window: Window, radiusPx: Int) {
    if (radiusPx > 0) {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    }
    window.attributes = window.attributes.also { it.blurBehindRadius = radiusPx }
    // The dialog draws its own scrim, so the platform dim is left off — stacking
    // FLAG_DIM_BEHIND on top of the scrim would mute the blur back out.
    window.setDimAmount(0f)
}
