package com.safarparmar.app.feature.toppersbatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

internal data class BatchFeatureAccess(val allowed: Boolean = true, val onUpgrade: () -> Unit = {}) {
    fun run(action: () -> Unit) {
        if (allowed) action() else onUpgrade()
    }
}

// Navigation and scrolling remain available; feature controls share this check
// for touch, keyboard, and accessibility activation.
internal val LocalBatchFeatureAccess = staticCompositionLocalOf { BatchFeatureAccess() }

@Composable
internal fun batchFeatureAction(action: () -> Unit): () -> Unit {
    val access = LocalBatchFeatureAccess.current
    return { access.run(action) }
}

@Composable
internal fun <T> batchFeatureChange(action: (T) -> Unit): (T) -> Unit {
    val access = LocalBatchFeatureAccess.current
    return { value -> access.run { action(value) } }
}
