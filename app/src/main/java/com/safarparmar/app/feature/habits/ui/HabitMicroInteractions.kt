package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.Role
import com.safarparmar.app.performance.LocalMotionPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Design system motion tokens and micro-interaction utilities for the Habit Tracker feature.
 * Follows Material Motion and Dan Saffer's 4-part micro-interaction model:
 * Trigger -> Rules -> Feedback -> Loops & Modes.
 *
 * Micro-interactions are compositor-driven (scale via graphicsLayer), respect reduced motion
 * via [LocalMotionPolicy], and provide authentic tactile spring physics.
 */
object HabitMotion {
    const val ScaleIcon = 0.86f       // 14% tactile scale down for icon buttons
    const val ScaleButton = 0.92f     // 8% scale down for primary / filled / text buttons
    const val ScaleCard = 0.96f       // 4% scale down for rows and cards
    const val ScaleCheckbox = 0.82f   // 18% compression for completion checks
    const val ScaleChip = 0.90f       // 10% scale down for day pills and filters

    // Snappy tactile compression on touch down
    val PressSpecDown: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Elastic spring release with gentle overshoot bounce
    val PressSpecUp: SpringSpec<Float> = spring(
        dampingRatio = 0.45f,
        stiffness = Spring.StiffnessMediumLow
    )

    // Celebration pop for completed habits
    val TogglePopSpec: SpringSpec<Float> = spring(
        dampingRatio = 0.42f,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * Applies an authentic tactile spring press animation to any composable (Buttons, IconButtons,
 * Tabs, Cards, Rows) without consuming pointer clicks.
 *
 * Uses [PointerEventPass.Initial] to detect finger down/up before internal clickables consume them,
 * guaranteeing press feedback even on components that manage their own internal interaction source.
 * Also enforces a brief hold minimum (85ms) on quick taps so the compression is always visible.
 */
fun Modifier.habitPress(
    scaleDown: Float = HabitMotion.ScaleButton,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val motion = LocalMotionPolicy.current
    val animScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    if (!motion.animationsEnabled || !enabled) {
        return@composed this
    }

    // If an external interactionSource is supplied, also react to it
    if (interactionSource != null) {
        LaunchedEffect(interactionSource, scaleDown) {
            var downTime = 0L
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        downTime = System.currentTimeMillis()
                        animScale.animateTo(scaleDown, HabitMotion.PressSpecDown)
                    }
                    is PressInteraction.Release -> {
                        val elapsed = System.currentTimeMillis() - downTime
                        if (elapsed < 85) delay(85 - elapsed)
                        animScale.animateTo(1f, HabitMotion.PressSpecUp)
                    }
                    is PressInteraction.Cancel -> {
                        animScale.animateTo(1f, HabitMotion.PressSpecUp)
                    }
                }
            }
        }
    }

    this
        .graphicsLayer {
            scaleX = animScale.value
            scaleY = animScale.value
        }
        .pointerInput(enabled, scaleDown) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val down = awaitPointerEvent(PointerEventPass.Initial)
                    if (down.changes.any { it.pressed }) {
                        val downTime = System.currentTimeMillis()
                        val animJob = scope.launch {
                            animScale.animateTo(scaleDown, HabitMotion.PressSpecDown)
                        }

                        // Monitor until released or cancelled (e.g. scroll drag)
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val stillPressed = event.changes.any { it.pressed }
                            val isDragScroll = event.changes.any { it.isConsumed && it.positionChanged() }
                            if (!stillPressed || isDragScroll) {
                                val elapsed = System.currentTimeMillis() - downTime
                                scope.launch {
                                    if (!isDragScroll && elapsed < 85) {
                                        delay(85 - elapsed)
                                    } else {
                                        animJob.join()
                                    }
                                    animScale.animateTo(1f, HabitMotion.PressSpecUp)
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
}

/**
 * Clickable modifier with authentic tactile spring press feedback and ripple.
 * Uses [LaunchedEffect] on the interaction source to ensure quick taps are guaranteed
 * a minimum visible compression window before spring-bouncing back.
 */
fun Modifier.habitClickable(
    enabled: Boolean = true,
    scaleDown: Float = HabitMotion.ScaleButton,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val motion = LocalMotionPolicy.current
    val animScale = remember { Animatable(1f) }

    if (!motion.animationsEnabled || !enabled) {
        return@composed this.clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
    }

    LaunchedEffect(source, scaleDown) {
        var downTime = 0L
        source.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    downTime = System.currentTimeMillis()
                    animScale.animateTo(scaleDown, HabitMotion.PressSpecDown)
                }
                is PressInteraction.Release -> {
                    val elapsed = System.currentTimeMillis() - downTime
                    if (elapsed < 85) {
                        delay(85 - elapsed)
                    }
                    animScale.animateTo(1f, HabitMotion.PressSpecUp)
                }
                is PressInteraction.Cancel -> {
                    animScale.animateTo(1f, HabitMotion.PressSpecUp)
                }
            }
        }
    }

    this
        .graphicsLayer {
            scaleX = animScale.value
            scaleY = animScale.value
        }
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Tactile spring bounce for primary/secondary buttons.
 */
fun Modifier.habitBounceClick(
    scaleDown: Float = HabitMotion.ScaleButton,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = habitClickable(
    enabled = enabled,
    scaleDown = scaleDown,
    role = Role.Button,
    onClick = onClick
)

/**
 * A celebratory spring pop scale when a habit or item transitions to `completed = true`.
 * Returns a State<Float> that plays an energetic celebration pop:
 * 1.0 -> 0.82 (initial squash) -> 1.25 (overshoot pop) -> 1.0 (elastic settle).
 * Prevents triggering on initial screen composition when habit was already completed.
 */
@Composable
fun rememberHabitCompletionPop(completed: Boolean): State<Float> {
    val motion = LocalMotionPolicy.current
    val animatable = remember { Animatable(1f) }
    var previousCompleted by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(completed) {
        val was = previousCompleted
        previousCompleted = completed
        if (was != null && !was && completed && motion.animationsEnabled) {
            animatable.snapTo(0.82f)
            animatable.animateTo(
                targetValue = 1.25f,
                animationSpec = spring(
                    dampingRatio = 0.48f,
                    stiffness = Spring.StiffnessMedium
                )
            )
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            animatable.snapTo(1f)
        }
    }

    return animatable.asState()
}
