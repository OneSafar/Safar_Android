package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.safarparmar.app.performance.LocalMotionPolicy

/**
 * Design system motion tokens and micro-interaction utilities for the Habit Tracker feature.
 * Follows Material Motion and Dan Saffer's 4-part micro-interaction model:
 * Trigger -> Rules -> Feedback -> Loops & Modes.
 *
 * Micro-interactions are compositor-driven (scale via graphicsLayer), respect reduced motion
 * via [LocalMotionPolicy], and provide authentic tactile spring physics.
 */
object HabitMotion {
    const val ScaleIcon = 0.91f
    const val ScaleButton = 0.96f
    const val ScaleCard = 0.985f
    const val ScaleCheckbox = 0.88f
    const val ScaleChip = 0.94f

    val PressSpecDown: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val PressSpecUp: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val TogglePopSpec: SpringSpec<Float> = spring(
        dampingRatio = 0.52f,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * Applies a tactile spring press animation to any composable without adding click handling.
 * Useful when integrating with existing clickable or button components.
 */
fun Modifier.habitPress(
    scaleDown: Float = HabitMotion.ScaleButton,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val motion = LocalMotionPolicy.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = if (!motion.animationsEnabled) tween(0)
        else if (isPressed) HabitMotion.PressSpecDown
        else HabitMotion.PressSpecUp,
        label = "habitPressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable modifier with authentic tactile spring press feedback and ripple.
 */
fun Modifier.habitClickable(
    enabled: Boolean = true,
    scaleDown: Float = HabitMotion.ScaleButton,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val motion = LocalMotionPolicy.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = if (!motion.animationsEnabled) tween(0)
        else if (isPressed) HabitMotion.PressSpecDown
        else HabitMotion.PressSpecUp,
        label = "habitClickableScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
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
 * Returns a State<Float> that scales from 1.0 -> 1.18 -> 1.0 when checked.
 */
@Composable
fun rememberHabitCompletionPop(completed: Boolean): State<Float> {
    val motion = LocalMotionPolicy.current
    val animatable = remember { Animatable(1f) }
    val currentCompleted = rememberUpdatedState(completed)

    LaunchedEffect(completed) {
        if (completed && motion.animationsEnabled) {
            animatable.snapTo(0.92f)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = HabitMotion.TogglePopSpec
            )
        } else {
            animatable.snapTo(1f)
        }
    }

    return animatable.asState()
}
