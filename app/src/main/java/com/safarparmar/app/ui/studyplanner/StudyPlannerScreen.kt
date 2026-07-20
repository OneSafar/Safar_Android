package com.safarparmar.app.ui.studyplanner

import androidx.compose.runtime.Composable
import com.safarparmar.app.ui.navigation.Routes

@Composable
fun StudyPlannerScreen(
    currentRoute: String = Routes.STUDY_PLANNER,
    isDarkTheme: Boolean = false,
    planId: String? = null,
    showDailyTodoSetup: Boolean = false,
    openTab: String? = null,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: StudyPlannerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    com.safarparmar.app.ui.studyplanner.screens.StudyPlannerScreen(
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        planId = planId,
        showDailyTodoSetup = showDailyTodoSetup,
        openTab = openTab,
        onNavigate = onNavigate,
        onBack = onBack,
        onToggleDarkTheme = onToggleDarkTheme,
        viewModel = viewModel,
    )
}
