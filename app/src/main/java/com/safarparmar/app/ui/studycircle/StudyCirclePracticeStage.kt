package com.safarparmar.app.ui.studycircle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.data.remote.dto.*
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.tutorial.*

/** Native circle controls with local samples; no membership, chat, or clipboard access. */
@Composable
internal fun StudyCirclePracticeStage(scene: PracticeScene) {
    val name = stringResource(R.string.practice_next_sample_circle)
    val person = stringResource(R.string.practice_next_sample_person)
    Column(Modifier.fillMaxSize().background(PlannerFlatColors.BgCream).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.study_circle_title), color = PlannerFlatColors.TextDark)
        when (scene) {
            PracticeScene.CIRCLE_CODE, PracticeScene.CIRCLE_PRIVACY -> StudyCircleInputContent(
                isCreate = scene == PracticeScene.CIRCLE_PRIVACY,
                value = if (scene == PracticeScene.CIRCLE_CODE) "DEMO12" else name,
                visibility = "private", busy = false, onValueChange = {}, onVisibilityChange = {}, onDismiss = {}, onSubmit = { _, _ -> })
            PracticeScene.CIRCLE_JOINED -> Box(Modifier.tutorialTarget(scene.target)) {
                MyCircleRow(StudyCircleSummaryDto(id = "practice", name = name, memberCount = 4, focusingCount = 2), {})
            }
            PracticeScene.CIRCLE_MEMBERS, PracticeScene.CIRCLE_FOCUS -> Box(Modifier.tutorialTarget(scene.target)) {
                LeaderboardMemberRow(StudyCircleMemberDto(userId = "practice", name = person, isFocusing = scene == PracticeScene.CIRCLE_FOCUS),
                    StudyCircleLeaderboardEntryDto(rank = 1, userId = "practice", name = person, totalFocusMinutes = 90, sessionCount = 3),
                    "practice", false, false, false, {}, {}, { _, _ -> }, {})
            }
            else -> {
                StudyCircleTopButtons(false, true, {}, {})
                Box(Modifier.tutorialTarget("circle.public")) {
                    PublicCircleRow(PublicStudyCircleDto(id = "practice", name = name, memberCount = 4, focusingCount = 2), 1, true, false, {})
                }
            }
        }
    }
}
