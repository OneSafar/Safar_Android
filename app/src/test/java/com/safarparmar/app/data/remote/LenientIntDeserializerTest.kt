package com.safarparmar.app.data.remote

import com.google.gson.GsonBuilder
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression: a plan whose dailyGoal was stored as 6.1 threw
 * NumberFormatException and aborted parsing of the whole plan-list response,
 * taking the Plan/Calendar screens down on published apps.
 */
class LenientIntDeserializerTest {

    private val gson = GsonBuilder().create()

    @Test
    fun `a fractional dailyGoal parses instead of crashing`() {
        val plans = gson.fromJson(
            """[{"id":"p1","title":"Exam","dailyGoal":6.1}]""",
            Array<StudyPlan>::class.java,
        )
        assertEquals(6, plans[0].dailyGoal)
    }

    @Test
    fun `a whole number is unaffected`() {
        val plans = gson.fromJson(
            """[{"id":"p1","title":"Exam","dailyGoal":4}]""",
            Array<StudyPlan>::class.java,
        )
        assertEquals(4, plans[0].dailyGoal)
    }

    @Test
    fun `rounds to the nearest whole goal`() {
        val plan = gson.fromJson("""{"id":"p","dailyGoal":6.6}""", StudyPlan::class.java)
        assertEquals(7, plan.dailyGoal)
    }

    @Test
    fun `missing and null stay null rather than defaulting`() {
        assertNull(gson.fromJson("""{"id":"p"}""", StudyPlan::class.java).dailyGoal)
        assertNull(gson.fromJson("""{"id":"p","dailyGoal":null}""", StudyPlan::class.java).dailyGoal)
    }
}
