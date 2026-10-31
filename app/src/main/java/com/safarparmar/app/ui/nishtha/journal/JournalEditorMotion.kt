package com.safarparmar.app.ui.nishtha.journal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Icon
import com.composeunstyled.DialogPanel
import com.composeunstyled.Scrim
import com.composeunstyled.UnstyledDialog
import com.safarparmar.app.R
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.ui.components.popups.LocalSafarPopupMaxWidth
import com.safarparmar.app.ui.components.popups.SafarPopupContent
import com.safarparmar.app.ui.components.popups.SafarPopupEnvironment

/** Local editor motion: one scrim and one measured panel, no bounds/height morph. */
@Composable
internal fun JournalEditorPopup(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    negativeButton: @Composable () -> Unit,
) {
    val policy = LocalMotionPolicy.current
    val enterMs = if (policy.constrained) 180 else 240
    val exitMs = if (policy.constrained) 120 else 160
    val travel = with(LocalDensity.current) { (if (policy.constrained) 12.dp else 20.dp).roundToPx() }
    val enabled = policy.animationsEnabled
    val maximumWidth = minOf(560.dp, LocalSafarPopupMaxWidth.current)
    val surface = MaterialTheme.colorScheme.surface
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = .42f)
    val enter = if (enabled) fadeIn(tween(enterMs, easing = LinearOutSlowInEasing)) +
        slideInVertically(tween(enterMs, easing = LinearOutSlowInEasing)) { travel } +
        scaleIn(tween(enterMs, easing = LinearOutSlowInEasing), initialScale = .97f, transformOrigin = TransformOrigin(.5f, .75f))
    else EnterTransition.None
    val exit = if (enabled) fadeOut(tween(exitMs, easing = FastOutLinearInEasing)) +
        slideOutVertically(tween(exitMs, easing = FastOutLinearInEasing)) { travel / 2 } +
        scaleOut(tween(exitMs, easing = FastOutLinearInEasing), targetScale = .985f)
    else ExitTransition.None

    // Adapted from Composables UI AlertDialogPanel; the panel transition is scoped
    // to this editor and remains composed while visible changes in either direction.
    SafarPopupEnvironment {
        UnstyledDialog(visible = visible, onDismissRequest = onDismissRequest, overlay = {
            Scrim(scrimColor = scrim,
                enter = if (enabled) fadeIn(tween(enterMs)) else EnterTransition.None,
                exit = if (enabled) fadeOut(tween(exitMs)) else ExitTransition.None)
        }) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(24.dp),
                contentAlignment = Alignment.Center) {
                DialogPanel(
                    modifier = Modifier.widthIn(max = maximumWidth).fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)).background(surface).padding(24.dp),
                    paneTitle = stringResource(R.string.nishtha_new_entry), enter = enter, exit = exit,
                ) {
                    SafarPopupContent {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            title()
                            Box(Modifier.weight(1f, fill = false)) { text() }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                positiveButton()
                                negativeButton()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One brief press response; no repeating motion or artificial delay before opening. */
@Composable
internal fun JournalComposeFab(editorOpen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val policy = LocalMotionPolicy.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        if (editorOpen) .88f else if (pressed) .94f else 1f,
        tween(if (policy.animationsEnabled) 120 else 0), label = "journalFabPress")
    val alpha by animateFloatAsState(if (editorOpen) 0f else 1f,
        tween(if (policy.animationsEnabled) 120 else 0), label = "journalFabFade")
    val rotation by animateFloatAsState(if (editorOpen) 45f else 0f,
        tween(if (policy.animationsEnabled) 180 else 0), label = "journalFabPlus")
    FloatingActionButton(
        onClick = {
            if (!editorOpen) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = modifier.size(56.dp).graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
        shape = CircleShape, containerColor = Color(0xFF542347), contentColor = Color(0xFFFFF5F0),
        interactionSource = interaction,
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.warm_new_entry),
            modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = rotation })
    }
}
