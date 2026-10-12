package com.safarparmar.app.performance

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp

/** Preserve the content and tint while avoiding an offscreen blur pass on constrained devices. */
fun Modifier.adaptiveBlur(radius: Dp): Modifier = composed {
    if (LocalMotionPolicy.current.constrained) this else blur(radius)
}
