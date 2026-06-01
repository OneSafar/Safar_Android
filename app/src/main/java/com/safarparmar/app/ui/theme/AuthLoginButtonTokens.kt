package com.safarparmar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Matches [com.safarparmar.app.ui.auth.AuthScreen] login primary action (Sign in).
 * Derives colors from the active Material 3 color scheme so that light/dark
 * theme switches are handled automatically — no hard-coded hex values needed.
 */
object AuthLoginButtonTokens {
    val container: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val content: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary
}

/** Same luminance heuristic as the auth screen for light vs dark UI. */
fun Color.isLightBackground(): Boolean {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return luminance > 0.5f
}
