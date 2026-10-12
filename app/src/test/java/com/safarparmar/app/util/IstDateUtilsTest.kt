package com.safarparmar.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IstDateUtilsTest {
    @Test
    fun `date prefix is returned without parsing an instant`() {
        assertEquals("2026-10-20", IstDateUtils.getDateKey("2026-10-20"))
        assertEquals("2026-10-20", IstDateUtils.getDateKey("2026-10-20T23:59:59Z"))
    }

    @Test
    fun `invalid dates do not produce keys`() {
        assertEquals("2026-10-21", IstDateUtils.getDateKey("2026-10-21T00:00:00Z"))
        assertNull(IstDateUtils.getDateKey("not-a-date"))
        assertNull(IstDateUtils.getDateKey(""))
    }
}
