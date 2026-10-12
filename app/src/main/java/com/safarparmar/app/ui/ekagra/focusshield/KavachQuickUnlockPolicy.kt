package com.safarparmar.app.ui.ekagra.focusshield

/** The same exception must apply to the app gate and to background-media enforcement. */
internal fun allowsKavachQuickUnlock(
    strict: Boolean,
    packageUnlocked: Boolean,
    studyModeOrigin: Boolean,
): Boolean = !strict && packageUnlocked && !studyModeOrigin
