package com.safarparmar.app.ui.drawer

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

/**
 * Material chooses the nearest new drawer anchor using its previous pixel offset.
 * Safar changes density on rotation, so a closed landscape offset can be closer
 * to the portrait Open anchor. Create a closed state before the new anchors are
 * measured on screens that request this policy. The content itself is not keyed,
 * which keeps the embedded WebView and its playback alive across rotation.
 */
@Composable
internal fun rememberViewportDrawerState(resetOnViewportChange: Boolean): DrawerState {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    val viewportKey = if (resetOnViewportChange) {
        DrawerViewportKey(
            configuration.orientation,
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            density,
        )
    } else Unit
    return key(viewportKey) { rememberDrawerState(DrawerValue.Closed) }
}

private data class DrawerViewportKey(
    val orientation: Int,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
)
