package com.safarparmar.app.performance

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState

/** No transition child is registered while decoration is disabled or its destination is hidden. */
@Composable
fun InfiniteTransition.decorativeFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "decoration",
): State<Float> = if (decorativeMotionEnabled()) {
    animateFloat(initialValue, targetValue, animationSpec, label)
} else {
    rememberUpdatedState(initialValue)
}
