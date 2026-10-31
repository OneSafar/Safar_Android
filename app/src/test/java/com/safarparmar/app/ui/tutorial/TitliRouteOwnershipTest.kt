package com.safarparmar.app.ui.tutorial

import com.safarparmar.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TitliRouteOwnershipTest {
    @Test fun everyDirectoryTutorialHasAnOwner() {
        val routes = listOf(Routes.EKAGRA, Routes.NISHTHA, Routes.ANALYTICS,
            Routes.YOUTUBE_STUDY_MODE_V2, Routes.HABIT_TRACKER, Routes.TOPPERS_BATCH,
            Routes.MEHFIL, Routes.DHYAN, Routes.STUDY_CIRCLES, Routes.STUDY_PLANNER)
        assertEquals(TitliFeature.entries.toSet(), routes.flatMap(::titliFeaturesForRoute).toSet())
    }

    @Test fun unsupportedAndDetailScreensDoNotInheritTutorials() {
        listOf(Routes.LEADERBOARD, Routes.PROFILE, Routes.PREMIUM, Routes.SETTINGS,
            Routes.SUPPORT, Routes.DASHBOARD, Routes.HABIT_INSIGHTS, Routes.CREATE_PLAN,
            Routes.FOCUS_SHIELD, Routes.TITLI_QUERIES, "mehfil/dm_chat", "live/session/123")
            .forEach { assertTrue(it, titliFeaturesForRoute(it).isEmpty()) }
    }

    @Test fun parametersRetainFeatureOwnership() {
        assertEquals(setOf(TitliFeature.EKAGRA), titliFeaturesForRoute("ekagra?goalId=123"))
        assertEquals(setOf(TitliFeature.EXAM_PLANNER), titliFeaturesForRoute(Routes.studyPlannerRoot()))
        assertEquals(setOf(TitliFeature.ANALYTICS), titliFeaturesForRoute(Routes.nishthaAnalytics()))
    }
}
