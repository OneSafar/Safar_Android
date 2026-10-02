package com.safarparmar.app.ui.ekagra.focusshield

/** A fresh day gets a first break; all subsequent breaks retain the reminder. */
internal fun repeatQuickUnlockMinutes(previousDate: String?, minutes: Int, today: String): Int =
    if (previousDate == today) minutes.coerceAtLeast(0) else 0
