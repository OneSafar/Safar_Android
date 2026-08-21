package com.safarparmar.app.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakAndCircleEligibilityDtosTest {
    private val gson = Gson()

    @Test
    fun `parses Study Circle creation eligibility from shared backend response`() {
        val response = gson.fromJson(
            """{
                "circles": [],
                "creationEligibility": {
                    "allowed": false,
                    "currentStreak": 12,
                    "requiredStreak": 21,
                    "adminBypass": false
                }
            }""".trimIndent(),
            StudyCirclesResponse::class.java,
        )

        assertFalse(response.creationEligibility.allowed)
        assertEquals(12, response.creationEligibility.currentStreak)
        assertEquals(21, response.creationEligibility.requiredStreak)
    }

    @Test
    fun `parses check-in restore availability and projected streak`() {
        val response = gson.fromJson(
            """{
                "loginStreak": 3,
                "checkInStreak": 1,
                "goalCompletionStreak": 2,
                "checkInRestore": {
                    "available": true,
                    "missedDate": "2026-08-19",
                    "projectedStreak": 7,
                    "activeProtectedDate": null
                }
            }""".trimIndent(),
            StreaksDto::class.java,
        )

        assertTrue(response.checkInRestore?.available == true)
        assertEquals("2026-08-19", response.checkInRestore?.missedDate)
        assertEquals(7, response.checkInRestore?.projectedStreak)
    }
}
