package com.safarparmar.app.ui.ekagra.focusshield

data class FocusShieldBlockPrompt(
    val packageName: String,
    val appName: String,
    val strict: Boolean,
    val alwaysOn: Boolean = false,
    val eventId: Long = System.currentTimeMillis(),
)
