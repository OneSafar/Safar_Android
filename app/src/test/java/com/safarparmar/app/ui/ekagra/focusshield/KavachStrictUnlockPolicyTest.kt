package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KavachStrictUnlockPolicyTest {
    @Test fun `strict Kavach preserves a YouTube Focus break`() {
        assertFalse(shouldClearKavachQuickUnlockForStrictMode(
            strict = true,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
        ))
    }

    @Test fun `strict Kavach still cancels its own app unlock`() {
        assertTrue(shouldClearKavachQuickUnlockForStrictMode(
            strict = true,
            quickUnlockOrigin = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH,
        ))
    }

    @Test fun `strict Kavach does not let legacy or unknown unlocks bypass protection`() {
        listOf(null, "", "legacy_unlock").forEach { origin ->
            assertTrue(shouldClearKavachQuickUnlockForStrictMode(
                strict = true,
                quickUnlockOrigin = origin,
            ))
        }
    }

    @Test fun `normal Kavach preserves breaks for both features and older installations`() {
        listOf(
            FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY,
            FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH,
            null,
        ).forEach { origin ->
            assertFalse(shouldClearKavachQuickUnlockForStrictMode(
                strict = false,
                quickUnlockOrigin = origin,
            ))
        }
    }
}
