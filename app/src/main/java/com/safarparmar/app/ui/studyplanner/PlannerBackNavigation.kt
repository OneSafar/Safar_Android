package com.safarparmar.app.ui.studyplanner

import com.safarparmar.app.domain.model.studyplanner.PlannerSection

/** Null delegates Back to the enclosing navigation stack. */
internal fun plannerBackSection(section: PlannerSection): PlannerSection? =
    if (section == PlannerSection.YOUR_EXAMS) null else PlannerSection.YOUR_EXAMS
