package com.safarparmar.app.ui.ekagra.focusshield

/** Kavach strictness controls its own breaks; YouTube Focus owns study-mode breaks. */
internal fun shouldClearKavachQuickUnlockForStrictMode(
    strict: Boolean,
    quickUnlockOrigin: String?,
): Boolean = strict && quickUnlockOrigin !=
    FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY
