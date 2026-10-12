package com.safarparmar.app.ui.components

import org.junit.Assert.*
import org.junit.Test

class PermissionWalkthroughTest {
    @Test fun everyPermissionHasActionableInstructionsAndDisclosure() {
        PermissionWalkthroughKind.entries.forEach { kind ->
            val content = permissionWalkthroughContent(kind)
            assertTrue(content.title.isNotBlank())
            assertTrue(content.reason.isNotBlank())
            assertTrue(content.warning.isNotBlank())
            assertTrue(content.steps.isNotEmpty())
            assertTrue(content.steps.size <= 3)
            content.steps.forEach {
                assertTrue(it.title.isNotBlank())
                assertTrue(it.instruction.isNotBlank())
                assertTrue(it.screen.isNotBlank())
                assertTrue(it.row.isNotBlank())
            }
        }
    }

    @Test fun accessibilityNamesTheRegisteredServiceAndDoesNotPromiseCompletedSetup() {
        val content = permissionWalkthroughContent(PermissionWalkthroughKind.ACCESSIBILITY)
        assertEquals("SAFAR YouTube Focus", content.steps.first().row)
        assertTrue(content.steps.last().instruction.contains("does not finish setup"))
    }

    @Test fun standardNotificationsDoNotRequireAMultiStepTutorial() {
        assertEquals(1, permissionWalkthroughContent(PermissionWalkthroughKind.NOTIFICATIONS).steps.size)
    }

    @Test fun sensitiveAccessIsNotDescribedAsOnlyAHarmlessToggle() {
        assertTrue(permissionWalkthroughContent(PermissionWalkthroughKind.NOTIFICATION_ACCESS).warning.contains("notification content"))
        assertTrue(permissionWalkthroughContent(PermissionWalkthroughKind.ACCESSIBILITY).warning.contains("screen content and actions"))
    }
}
