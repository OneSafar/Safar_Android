package com.safarparmar.app.ui.calmcentre

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/** The cue and its fill share the artwork clock; a pause freezes both. No independent animator. */
@Composable
internal fun CalmTimedText(text: String, style: TextStyle, base: Color, active: Color,
    progress: () -> Float, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(text, style = style, color = base, textAlign = TextAlign.Center)
        Text(text, style = style, color = active, textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { }.drawWithContent {
                clipRect(right = size.width * progress().coerceIn(0f, 1f)) { this@drawWithContent.drawContent() }
            })
    }
}

internal fun calmCueProgress(practice: CalmPractice, elapsed: Long, duration: Long): Float {
    if (practice == CalmPractice.BREATHE) {
        val cycle = elapsed.coerceAtLeast(0L) % 10000L
        return if (cycle < 4000L) cycle / 4000f else (cycle - 4000L) / 6000f
    }
    val steps = if (practice == CalmPractice.GROUND) 5 else 4
    val stepDuration = duration.coerceAtLeast(1L).toDouble() / steps
    val withinStep = elapsed.coerceAtLeast(0L) % stepDuration
    val fillDuration = if (practice == CalmPractice.REST) minOf(4500.0, stepDuration * 0.35) else stepDuration
    return (withinStep / fillDuration).toFloat().coerceIn(0f, 1f)
}
