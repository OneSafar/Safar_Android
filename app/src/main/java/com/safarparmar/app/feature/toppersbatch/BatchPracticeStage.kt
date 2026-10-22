package com.safarparmar.app.feature.toppersbatch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.*
import java.time.LocalDate

/** LectureCardContent receives no ViewModel or link launcher in practice. */
@Composable
internal fun BatchPracticeStage(scene: PracticeScene) {
    val today = LocalDate.now(com.safarparmar.app.util.IstDateUtils.zone)
    val subject = BatchSubject(id = "practice-subject", key = "history", name = stringResource(R.string.practice_sample_subject))
    val done = scene in setOf(PracticeScene.BATCH_DONE, PracticeScene.BATCH_REVISION, PracticeScene.BATCH_PROGRESS)
    val lecture = BatchLecture(id = "practice-lecture", subjectId = subject.id, subjectKey = subject.key,
        lectureNumber = 1, displayTopic = stringResource(R.string.practice_sample_lecture),
        originalTopic = stringResource(R.string.practice_sample_lecture), scheduledFor = today.toString(),
        completedAt = if (done) today.toString() else null,
        revisionDate = if (scene == PracticeScene.BATCH_CALENDAR) today.toString() else null,
        isAvailable = true)
    val overview = BatchOverview(subjects = listOf(subject), lectures = listOf(lecture))
    val state = BatchUiState(gate = BatchGate.READY, overview = overview, selectedDay = today.toString(), month = today.toString().take(7))
    BatchClampedContent {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Text(stringResource(R.string.practice_batch), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
            Box(Modifier.weight(1f)) {
                if (scene == PracticeScene.BATCH_CALENDAR) Box(Modifier.tutorialTarget(scene.target)) {
                    BatchStudentCalendarContent(state, {}, {}, {}, {}, {}, today.toString())
                } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (scene == PracticeScene.BATCH_MODES) Row(Modifier.tutorialTarget(scene.target), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BatchStudyMode.entries.forEach { mode -> CourseSwitch(stringResource(mode.titleRes), mode == BatchStudyMode.OFFICIAL, Modifier.weight(1f), {}) }
                    }
                    Box(Modifier.tutorialTarget(if (scene == PracticeScene.BATCH_PROGRESS) scene.target else "batch.subject")) {
                        SubjectRow(subject, SubjectProgress(subjectId = subject.id, total = 7, completed = if (done) 1 else 0), {})
                    }
                    Box(Modifier.tutorialTarget("batch.lecture")) { LectureWeekBrowser(listOf(listOf(lecture)), 0, true, {}) }
                    LectureCardContent(lecture, subject, state)
                }
            }
            BatchBottomNav(when (scene) { PracticeScene.BATCH_CALENDAR -> BatchSection.CALENDAR; PracticeScene.BATCH_PROGRESS -> BatchSection.PROGRESS; else -> BatchSection.COURSES }, {})
        }
    }
}
