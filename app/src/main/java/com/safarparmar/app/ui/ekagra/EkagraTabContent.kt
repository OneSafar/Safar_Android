package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.safarparmar.app.performance.LocalMotionPolicy

/** Compose only the requested screen. A transition must never keep expensive old screens alive. */
@Composable
internal fun EkagraTabContent(selectedTab: EkagraNavTab, content: @Composable (EkagraNavTab) -> Unit) {
    val motion = LocalMotionPolicy.current
    val savedState = rememberSaveableStateHolder()
    key(selectedTab) {
        val reveal = remember { Animatable(if (motion.animationsEnabled) 0.86f else 1f) }
        LaunchedEffect(motion.animationsEnabled) {
            if (motion.animationsEnabled) reveal.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
            else reveal.snapTo(1f)
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = reveal.value }) {
            savedState.SaveableStateProvider(selectedTab) { content(selectedTab) }
        }
    }
}
