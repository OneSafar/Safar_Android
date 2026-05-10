package com.safar.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Matches [com.safar.app.ui.auth.AuthScreen] login primary action (Sign in).
 */
object AuthLoginButtonTokens {
    fun container(isDark: Boolean): Color =
        if (isDark) Color(0xFF2ED1A2) else Color(0xFF1E8A6B)

    fun content(isDark: Boolean): Color =
        if (isDark) Color(0xFF003829) else Color(0xFFFFFFFF)
}

/** Same luminance heuristic as the auth screen for light vs dark UI. */
fun Color.isLightBackground(): Boolean {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return luminance > 0.5f
}
