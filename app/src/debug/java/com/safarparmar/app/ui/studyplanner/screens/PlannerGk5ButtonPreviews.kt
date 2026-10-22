package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.safarparmar.app.ui.theme.SafarTheme

// These render the production component, with its draw clock frozen for visual review.
@Preview(widthDp = 375, heightDp = 200, showBackground = true)
@Composable
internal fun Gk5ButtonPreview() = Gk5PreviewContent()

@Preview(widthDp = 320, heightDp = 220, showBackground = true)
@Composable
internal fun Gk5ButtonSmallPreview() = Gk5PreviewContent()

@Preview(widthDp = 414, heightDp = 200, showBackground = true)
@Composable
internal fun Gk5ButtonWidePreview() = Gk5PreviewContent()

@Preview(widthDp = 768, heightDp = 200, showBackground = true)
@Composable
internal fun Gk5ButtonTabletPreview() = Gk5PreviewContent()

@Preview(widthDp = 375, heightDp = 200, showBackground = true)
@Composable
internal fun Gk5ButtonDarkPreview() = Gk5PreviewContent(dark = true)

@Preview(widthDp = 320, heightDp = 300, showBackground = true)
@Composable
internal fun Gk5ButtonLargeTextPreview() = Gk5PreviewContent(fontScale = 1.5f)

@Preview(widthDp = 414, heightDp = 620, showBackground = true)
@Composable
internal fun Gk5ButtonMotionPreview() = Gk5PreviewContent(showPhases = true)

@Composable
private fun Gk5PreviewContent(dark: Boolean = false, fontScale: Float = 1f, showPhases: Boolean = false) {
    val previewDensity = LocalDensity.current.density
    SafarTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalDensity provides Density(previewDensity, fontScale)) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val phases = if (showPhases) listOf(0f, 0.33f, 0.66f) else listOf(0.18f)
                    for (phase in phases) {
                        if (showPhases) Text("Motion phase $phase", style = MaterialTheme.typography.labelSmall)
                        PlannerGk5Button(
                            title = "Parmar GK 5.0 Batch Tracker",
                            subtitle = "GK-only lectures, revision and progress",
                            onClick = {},
                            decorationPhase = { phase },
                        )
                    }
                    if (showPhases) {
                        Text("Disabled / static", style = MaterialTheme.typography.labelSmall)
                        PlannerGk5Button("Parmar GK 5.0 Batch Tracker", "GK-only lectures, revision and progress",
                            onClick = {}, enabled = false, decorationPhase = { 0f })
                    }
                }
            }
        }
    }
}
