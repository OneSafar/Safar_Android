package com.safar.app.ui.studyplanner

import androidx.compose.runtime.Composable
import com.safar.app.ui.navigation.Routes

@Composable
fun StudyPlannerScreen(
    currentRoute: String = Routes.STUDY_PLANNER,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    viewModel: StudyPlannerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    com.safar.app.ui.studyplanner.screens.StudyPlannerScreen(
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onBack = onBack,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        viewModel = viewModel,
    )
}
