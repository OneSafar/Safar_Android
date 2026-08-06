package com.safarparmar.app.ui.ekagra

/**
 * Linking study time and finishing a goal are separate student choices.
 * Keeping this as an explicit value prevents the save sheet from silently
 * turning every linked focus session into a completed goal again.
 */
internal enum class GoalSessionSaveChoice(
    val marksGoalDone: Boolean,
) {
    KEEP_GOAL_OPEN(marksGoalDone = false),
    MARK_GOAL_DONE(marksGoalDone = true),
}
