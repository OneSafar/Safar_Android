package com.safarparmar.app.ui.ekagra.focusshield

import android.graphics.drawable.Drawable

/**
 * Represents an installed app that can be blocked during focus sessions.
 */
data class BlockedAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean = false,
)
