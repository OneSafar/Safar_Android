package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KavachActivationReadinessTest {

    @Test
    fun `requires at least one selected app`() {
        assertEquals(
            "Select at least one app for Kavach to block.",
            KavachActivationReadiness.warning(emptySet(), true, true),
        )
    }

    @Test
    fun `identifies each missing permission`() {
        val apps = setOf("com.example.blocked")

        assertEquals(
            "Turn on Usage Access for Kavach.",
            KavachActivationReadiness.warning(apps, false, true),
        )
        assertEquals(
            "Allow Kavach to display over other apps.",
            KavachActivationReadiness.warning(apps, true, false),
        )
    }

    @Test
    fun `returns no warning when Kavach is ready`() {
        assertNull(
            KavachActivationReadiness.warning(
                blockedPackages = setOf("com.example.blocked"),
                hasUsageAccess = true,
                hasOverlayPermission = true,
            ),
        )
    }
}
