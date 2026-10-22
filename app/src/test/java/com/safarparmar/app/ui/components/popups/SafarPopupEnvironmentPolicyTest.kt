package com.safarparmar.app.ui.components.popups

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SafarPopupEnvironmentPolicyTest {
    @Test fun `resource scale represents the app density without changing its font limit`() {
        assertEquals(PopupResourceScale(256, 1.45f), popupResourceScale(1.6f, 1.45f))
        assertEquals(PopupResourceScale(384, 1.0f), popupResourceScale(2.4f, 1.0f))
    }

    @Test fun `Android resource density rounding stays within half a density dpi`() {
        listOf(0.85f, 1.2345f, 1.8f, 2.718f, 3.5f).forEach { density ->
            val scale = popupResourceScale(density, 1.15f)
            assertEquals(density, scale.densityDpi / 160f, 0.5f / 160f)
            assertEquals(1.15f, scale.fontScale, 0f)
        }
    }

    @Test fun `context preserves Activity unwrapping and services with isolated resources`() {
        val host = mockk<Context>()
        val configured = mockk<Context>()
        val originalResources = mockk<Resources>(relaxed = true)
        val isolatedResources = mockk<Resources>(relaxed = true)
        val assets = mockk<AssetManager>()
        val configuration = mockk<Configuration>()
        val windowService = Any()
        every { host.resources } returns originalResources
        every { host.createConfigurationContext(configuration) } returns configured
        every { configured.resources } returns isolatedResources
        every { isolatedResources.assets } returns assets
        every { host.getSystemService(Context.WINDOW_SERVICE) } returns windowService

        val popup = SafarPopupContext(host, configuration)

        assertSame(host, popup.baseContext)
        assertSame(isolatedResources, popup.resources)
        assertSame(originalResources, host.resources)
        assertSame(assets, popup.assets)
        assertSame(windowService, popup.getSystemService(Context.WINDOW_SERVICE))
        verify(exactly = 1) { host.createConfigurationContext(configuration) }
        @Suppress("DEPRECATION")
        verify(exactly = 0) { originalResources.updateConfiguration(any(), any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid display density cannot create popup resources`() {
        popupResourceScale(Float.NaN, 1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid font scale cannot create popup resources`() {
        popupResourceScale(2f, 0f)
    }
}
