package com.safarparmar.app.ui.studyplanner.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlannerReferenceKeyTest {
    @Test
    fun `same collection instance keeps the cache key stable`() {
        val topics = listOf("topic")
        assertEquals(PlannerReferenceKey(topics), PlannerReferenceKey(topics))
    }

    @Test
    fun `equal replacement collection invalidates the cache`() {
        val original = listOf("topic")
        val replacement = listOf("topic")
        assertEquals(original, replacement)
        assertNotEquals(PlannerReferenceKey(original), PlannerReferenceKey(replacement))
    }
}
