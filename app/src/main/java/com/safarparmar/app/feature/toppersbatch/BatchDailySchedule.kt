package com.safarparmar.app.feature.toppersbatch

/** Admin release metadata overrides generated dates and saved local row copies. */
internal fun BatchOverview.applyBackendControls(): BatchOverview {
    if (lectureControls.isEmpty()) return this
    val controls = lectureControls.associateBy { it.subjectKey to it.lectureNumber }
    fun apply(row: BatchLecture): BatchLecture {
        val control = controls[row.subjectKey to row.controlSlotNumber] ?: return row
        val officialTopic = control.topic ?: row.originalTopic
        return row.copy(scheduledFor = control.scheduledDate, classTime = control.classTime,
            displayLectureNumber = control.displayLectureNumber ?: row.displayLectureNumber,
            originalTopic = officialTopic,
            displayTopic = if (row.displayTopic == row.originalTopic) officialTopic else row.displayTopic,
            releaseState = control.releaseState, isAvailable = control.isAvailable)
    }
    val controlled = copy(lectures = lectures.map(::apply), removedLectures = removedLectures.map(::apply))
    return controlled.lectures.firstOrNull()?.let {
        controlled.withLecture(it, serverDay ?: ToppersBatchViewModel.indiaDay())
    } ?: controlled
}
