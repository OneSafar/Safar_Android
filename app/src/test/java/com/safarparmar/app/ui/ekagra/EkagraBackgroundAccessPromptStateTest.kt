package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class EkagraBackgroundAccessPromptStateTest {
    private val initial = EkagraBackgroundAccessPromptState()

    @Test fun existingInstallWithoutGrantIsPromptedOnItsFirstStudyStart() {
        val state = initial.sessionChanged("existing-install-new-session", true, false)
        assertTrue(state.visible)
        assertEquals("existing-install-new-session", state.promptedSessionId)
    }
    @Test fun grantAlreadyEnabledThroughKavachSkipsPrompt() {
        assertFalse(initial.sessionChanged("session", true, true).visible)
    }
    @Test fun browsingAndBreaksDoNotConsumeFirstStudyPrompt() {
        val browsing = initial.sessionChanged("session", false, false)
        assertEquals(initial, browsing)
        assertTrue(browsing.sessionChanged("session", true, false).visible)
    }
    @Test fun noPromptWithoutDurableSessionIdentity() {
        assertEquals(initial, initial.sessionChanged(null, true, false))
        assertEquals(initial, initial.sessionChanged("", true, false))
    }
    @Test fun pauseResumeDoesNotNagAfterDismissal() {
        val dismissed = initial.sessionChanged("session", true, false).dismiss()
        val paused = dismissed.sessionChanged("session", false, false)
        assertFalse(paused.sessionChanged("session", true, false).visible)
    }
    @Test fun missingOrDeniedGrantIsRequestedAgainOnNextSession() {
        val dismissed = initial.sessionChanged("old", true, false).dismiss()
        assertTrue(dismissed.sessionChanged("new", true, false).visible)
    }
    @Test fun openingSettingsNeverPretendsPermissionWasGranted() {
        val waiting = initial.sessionChanged("session", true, false).openSettings()
        assertFalse(waiting.visible)
        assertTrue(waiting.waitingForSettings)
        assertTrue(waiting.settingsReturned(false).visible)
        assertFalse(waiting.settingsReturned(false).waitingForSettings)
    }
    @Test fun actualGrantClosesPromptAfterSettingsReturn() {
        val waiting = initial.sessionChanged("session", true, false).openSettings()
        assertFalse(waiting.settingsReturned(true).visible)
        assertFalse(waiting.settingsReturned(true).waitingForSettings)
    }
    @Test fun ordinaryResumeKeepsUnansweredPromptVisible() {
        val showing = initial.sessionChanged("session", true, false)
        assertEquals(showing, showing.settingsReturned(false))
    }
    @Test fun stoppingWhileInSettingsDoesNotLeavePermissionDialogForBreak() {
        val waiting = initial.sessionChanged("session", true, false).openSettings()
        val stopped = waiting.sessionChanged("session", false, false)
        assertFalse(stopped.settingsReturned(false).visible)
    }
    @Test fun revokedSharedGrantIsDetectedOnNextStudyStart() {
        val granted = initial.sessionChanged("old", true, true)
        assertTrue(granted.sessionChanged("new", true, false).visible)
    }
    @Test fun recreationCanRetainThePendingSettingsRequestAndSessionIdentity() {
        val waiting = initial.sessionChanged("session", true, false).openSettings()
        val restored = EkagraBackgroundAccessPromptState(waiting.promptedSessionId, waiting.visible, waiting.waitingForSettings)
        assertTrue(restored.settingsReturned(false).visible)
        assertFalse(restored.settingsReturned(true).sessionChanged("session", true, true).visible)
    }
}
