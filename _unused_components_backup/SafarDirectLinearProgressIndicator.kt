package com.safarparmar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics

/** Direct progress for the timed breathing cue; no extra interpolation or delay. */
@Composable
fun SafarDirectLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
) {
    val fraction = progress().coerceIn(0f, 1f)
    Box(modifier.background(trackColor).semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
    }) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))
    }
}
