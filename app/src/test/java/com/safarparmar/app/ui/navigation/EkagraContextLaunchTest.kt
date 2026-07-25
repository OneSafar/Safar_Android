package com.safarparmar.app.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EkagraContextLaunchTest {
    @Test
    fun `different planner topics create different Ekagra routes`() {
        val first = "ekagra?topicId=topic-1&topicTitle=Trigonometric%20Ratios&planId=plan-1"
        val second = "ekagra?topicId=topic-2&topicTitle=Divisibility%20Rules&planId=plan-1"

        assertNotEquals(first, second)
        assertTrue(Routes.isContextualEkagraLaunch(first))
        assertTrue(Routes.isContextualEkagraLaunch(second))
    }

    @Test
    fun `plain Ekagra launch may restore its own screen state`() {
        assertFalse(Routes.isContextualEkagraLaunch(Routes.EKAGRA))
        assertFalse(Routes.isContextualEkagraLaunch("nishtha?tab=4&section=ekagra"))
    }

    @Test
    fun `plain Nishtha route normalizes to tabbed destination`() {
        val normalized = Routes.normalizeFeatureRoute(Routes.NISHTHA)

        assertTrue(normalized.startsWith("nishtha?tab=0"))
        assertTrue(normalized.contains("section=overview"))
        assertNotEquals(Routes.PROFILE, normalized)
    }

    @Test
    fun `plain Study Planner route normalizes to registered destination`() {
        val normalized = Routes.normalizeFeatureRoute(Routes.STUDY_PLANNER)

        assertTrue(normalized.startsWith("study_planner?"))
        assertTrue(normalized.contains("showDailyTodoSetup=false"))
        assertNotEquals(Routes.HOME, normalized)
    }
}
