package com.safarparmar.app.ui.nishtha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.*
import com.safarparmar.app.ui.nishtha.analytics.NishthaAnalyticsContent
import com.safarparmar.app.ui.nishtha.checkin.CheckInScreenContent
import com.safarparmar.app.ui.nishtha.journal.*
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.tutorial.*
import java.time.LocalDate

/** Presentation snapshots only. Each step resets its own local draft, never real entries. */
@Composable
internal fun NishthaPracticeStage(feature: TitliFeature, scene: PracticeScene, dark: Boolean) {
    val today = LocalDate.now(com.safarparmar.app.util.IstDateUtils.zone)
    val timestamp = today.toString() + "T09:00:00+05:30"
    val sample = NishthaUiState(
        goals = (0L..4L).map { Goal(id = "practice-$it", title = stringResource(R.string.practice_next_sample_topic), scheduledDate = today.minusDays(it).toString(), completed = true, status = "completed", studiedMinutes = 25) },
        moods = listOf(Mood(id = "practice", mood = "happy", intensity = 7, timestamp = timestamp)),
        streaks = Streaks(loginStreak = 5, checkInStreak = 4, goalCompletionStreak = 3),
        loginHistory = (0L..4L).map { LoginHistoryEntry(today.minusDays(it).toString() + "T09:00:00+05:30") },
        ekagraAnalytics = EkagraAnalyticsStats(totalFocusMinutes = 125, totalSessions = 5, completedSessions = 4, timerUsageCount = 5, averageTimerMinutes = 25, weeklyData = listOf(0,25,0,25,25,25,25)),
        monthlyReport = MonthlyReport(month = today.toString().take(7), consistencyScore = 75.0, completionRate = 60.0, daysLoggedIn = 12, totalFocusMinutes = 125, goalsCreated = 5, goalsCompleted = 3))
    Column(Modifier.fillMaxSize().background(PlannerFlatColors.BgCream)) {
        Box(Modifier.weight(1f)) {
            key(scene) {
                when (feature) {
                    TitliFeature.CHECK_IN -> CheckInScreenContent(sample, !dark,
                        initialMoodIndex = if (scene == PracticeScene.CHECKIN_DETAILS || scene == PracticeScene.CHECKIN_SAVE) 0 else null,
                        initialHistory = scene == PracticeScene.CHECKIN_HISTORY)
                    TitliFeature.STREAKS -> StreaksScreenContent(sample, !dark)
                    TitliFeature.ANALYTICS -> NishthaAnalyticsContent(sample, initialSection = when (scene) {
                        PracticeScene.ANALYTICS_GOALS -> "goals"
                        PracticeScene.ANALYTICS_FOCUS -> "ekagra"
                        PracticeScene.ANALYTICS_MONTH -> "monthly"
                        else -> "overview"
                    })
                    TitliFeature.JOURNAL -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val accent = PlannerFlatColors.PrimaryAccent
                        Text(stringResource(R.string.practice_next_journal), color = PlannerFlatColors.TextDark)
                        when (scene) {
                            PracticeScene.JOURNAL_PROMPT -> Box(Modifier.tutorialTarget(scene.target)) { PromptGlassCard(stringResource(R.string.practice_next_sample_prompt), 0, 1, !dark, accent, {}) }
                            PracticeScene.JOURNAL_WRITE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FlatJournalField(stringResource(R.string.practice_next_sample_journal_title), {}, stringResource(R.string.practice_next_journal_title_label), accent, true)
                                Box(Modifier.tutorialTarget(scene.target)) { FlatJournalField(stringResource(R.string.practice_next_sample_journal_body), {}, stringResource(R.string.practice_next_journal_body_label), accent, false, minHeight = 120.dp) }
                                FlatPrimaryButton(stringResource(R.string.journal_save_entry), true, false, accent, {}, Modifier.fillMaxWidth())
                            }
                            else -> Box(Modifier.tutorialTarget(scene.target)) { JournalGlassCard(JournalEntry(id = "practice", content = stringResource(R.string.practice_next_sample_journal_body), timestamp = timestamp), !dark, accent, {}) }
                        }
                    }
                    else -> Unit
                }
            }
        }
        if (feature != TitliFeature.ANALYTICS) NishthaBottomBar(when (feature) {
            TitliFeature.JOURNAL -> NishthaTab.JOURNAL
            TitliFeature.STREAKS -> NishthaTab.STREAKS
            else -> NishthaTab.CHECK_IN
        }, {})
    }
}
