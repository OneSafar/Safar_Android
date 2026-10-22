package com.safarparmar.app.ui.tutorial

import androidx.compose.ui.graphics.Color

/** Four arrangements of the approved Soft Geometry palette. */
internal enum class TutorialMenuArtwork { ARCHES, WAVES, STEPS, LEAVES }
internal fun TitliFeature.menuArtwork(): TutorialMenuArtwork = when (this) {
    TitliFeature.EKAGRA, TitliFeature.CHECK_IN, TitliFeature.ANALYTICS -> TutorialMenuArtwork.ARCHES
    TitliFeature.GOALS, TitliFeature.DHYAN, TitliFeature.YOUTUBE -> TutorialMenuArtwork.WAVES
    TitliFeature.TOPPERS_BATCH, TitliFeature.STREAKS, TitliFeature.EXAM_PLANNER -> TutorialMenuArtwork.STEPS
    TitliFeature.HABITS, TitliFeature.JOURNAL, TitliFeature.MEHFIL, TitliFeature.STUDY_CIRCLE -> TutorialMenuArtwork.LEAVES
}
internal data class TutorialArtPalette(val coral: Color, val yellow: Color, val lilac: Color, val sage: Color, val pink: Color)
internal fun tutorialArtPalette(dark: Boolean) = if (dark) TutorialArtPalette(
    Color(0xFFD78378), Color(0xFFE6BF7C), Color(0xFFB09ACF), Color(0xFF91AC81), Color(0xFFEB9EA8))
else TutorialArtPalette(Color(0xFFED8A7D), Color(0xFFFFDFA0), Color(0xFFCDBBE9), Color(0xFF8EAD7C), Color(0xFFF2A1A6))
