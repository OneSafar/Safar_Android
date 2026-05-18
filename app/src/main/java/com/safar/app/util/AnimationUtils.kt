package com.safar.app.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A custom modifier that adds a bounce scale effect when pressed.
 * It uses a [MutableInteractionSource] to track the pressed state and
 * applies a [spring] animation to the scale in the drawing phase via [graphicsLayer].
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // Removes the default ripple. Set to local indication if ripple is desired.
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}
