package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context

/** The same exception must apply to the app gate and every media-enforcement path. */
internal fun allowsKavachQuickUnlock(
    strict: Boolean,
    packageUnlocked: Boolean,
    studyModeOrigin: Boolean,
): Boolean = !strict && packageUnlocked && !studyModeOrigin

internal fun allowsKavachQuickUnlock(context: Context, packageName: String, strict: Boolean): Boolean =
    allowsKavachQuickUnlock(
        strict = strict,
        packageUnlocked = FocusShieldRepository.ShieldPrefs.isInGracePeriodForPackage(context, packageName),
        studyModeOrigin = FocusShieldRepository.ShieldPrefs.quickUnlockOrigin(context) ==
            FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
    )
