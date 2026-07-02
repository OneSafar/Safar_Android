package com.safarparmar.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import com.safarparmar.app.MainActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.achievements.AchievementsScreen
import com.safarparmar.app.ui.admin.AdminNotificationComposerScreen
import com.safarparmar.app.ui.auth.AuthScreen
import com.safarparmar.app.ui.dashboard.DashboardScreen
import com.safarparmar.app.ui.dhyan.DhyanCoursesScreen
import com.safarparmar.app.ui.dhyan.CoursesHubTab
import com.safarparmar.app.ui.dhyan.DhyanScreen
import com.safarparmar.app.ui.ekagra.EkagraScreen
import com.safarparmar.app.ui.home.HomeScreen
import com.safarparmar.app.ui.mehfil.DmChatScreen
import com.safarparmar.app.ui.mehfil.MehfilScreen
import com.safarparmar.app.ui.nishtha.NishthaScreen
import com.safarparmar.app.ui.ekagra.LocalTimerService
import com.safarparmar.app.ui.profile.ProfileScreen
import com.safarparmar.app.ui.settings.SettingsScreen
import com.safarparmar.app.ui.splash.SplashScreen
import com.safarparmar.app.ui.studyplanner.StudyPlannerScreen
import com.safarparmar.app.ui.launch.LaunchUsageQuestionnaireScreen
import com.safarparmar.app.ui.studyplanner.screens.SyllabusSubjectsScreen
import com.safarparmar.app.ui.studyplanner.screens.SyllabusChaptersScreen
import com.safarparmar.app.ui.studyplanner.screens.SyllabusTopicsScreen
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldStandaloneScreen
import com.safarparmar.app.ui.ekagra.focusshield.KavachAboutScreen
import com.safarparmar.app.feature.live.presentation.LiveSessionScreen
import com.safarparmar.app.ui.premium.PremiumPaywallScreen
import com.safarparmar.app.ui.premium.PremiumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ADMIN_NOTIFICATION_ALLOWED_EMAILS = setOf(
    "safarparmar0@gmail.com",
    "steve123@example.com",
    "thatkindchic@gmail.com",
)

@Composable
fun SafarNavGraph(
    dataStore: SafarDataStore,
    isDarkTheme       : Boolean = false,
    onToggleDarkTheme : () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: Routes.SPLASH
    val isLoggedIn by dataStore.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)
    val isAdmin by dataStore.isAdmin.collectAsStateWithLifecycle(initialValue = false)
    val currentUserEmail by dataStore.userEmail.collectAsStateWithLifecycle(initialValue = null)
    val premiumViewModel = androidx.hilt.navigation.compose.hiltViewModel<PremiumViewModel>()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val studyPlannerPremiumUnlocked = premiumStatus.hasAnyPaidAccess || premiumStatus.canUseStudyPlannerInsights
    val scope = rememberCoroutineScope()
    val canAccessAdminComposer = remember(isAdmin, currentUserEmail) {
        isAdmin || (currentUserEmail?.trim()?.lowercase() in ADMIN_NOTIFICATION_ALLOWED_EMAILS)
    }

    // Drawer destinations are feature roots. Keep only Home beneath a root so the
    // system Back gesture exits a feature to Home instead of reopening an older one.
    val featureRootRoutes = remember {
        setOf(
            Routes.HOME,
            Routes.DASHBOARD,
            Routes.STUDY_PLANNER,
            Routes.FOCUS_SHIELD,
            Routes.NISHTHA,
            Routes.NISHTHA_CHECKIN,
            Routes.NISHTHA_GOALS,
            Routes.NISHTHA_STREAKS,
            Routes.NISHTHA_ANALYTICS,
            Routes.EKAGRA,
            Routes.MEHFIL,
            Routes.DHYAN,
            Routes.COURSES,
            Routes.LIVE_SESSIONS_ROOT,
        )
    }

    fun navigate(route: String) {
        val routeBase = route.substringBefore("?")
        val currentRouteBase = currentRoute.substringBefore("?")
        val keepKavachInsideEkagra = route == Routes.FOCUS_SHIELD && currentRouteBase == Routes.EKAGRA
        val isLiveSessionDetail = route.startsWith("live/session/")
        val isSyllabusDetail = route.startsWith("syllabus/")
        val needsFocusShieldParent = route == Routes.APP_PICKER || route == Routes.KAVACH_ABOUT
        val needsDashboardParent = route == Routes.ACHIEVEMENTS

        fun navigateFeatureRoot(route: String) {
            navController.navigate(route) {
                popUpTo(Routes.HOME) { inclusive = false }
                launchSingleTop = true
                restoreState = false
            }
        }

        if (route == Routes.DM_CHAT) {
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo(Routes.MEHFIL) { inclusive = false }
            }
        } else if (isLiveSessionDetail && currentRouteBase !in setOf(Routes.COURSES, Routes.LIVE_SESSIONS_ROOT)) {
            navigateFeatureRoot(Routes.COURSES)
            navController.navigate(route) { launchSingleTop = true; restoreState = true }
        } else if (isSyllabusDetail && currentRouteBase != Routes.STUDY_PLANNER && !currentRouteBase.startsWith("syllabus/")) {
            navigateFeatureRoot(Routes.STUDY_PLANNER)
            navController.navigate(route) { launchSingleTop = true; restoreState = true }
        } else if (needsFocusShieldParent && currentRouteBase !in setOf(Routes.FOCUS_SHIELD, Routes.EKAGRA)) {
            navigateFeatureRoot(Routes.FOCUS_SHIELD)
            navController.navigate(route) { launchSingleTop = true; restoreState = true }
        } else if (needsDashboardParent && currentRouteBase != Routes.DASHBOARD) {
            navigateFeatureRoot(Routes.DASHBOARD)
            navController.navigate(route) { launchSingleTop = true; restoreState = true }
        } else if (keepKavachInsideEkagra) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        } else if (routeBase in featureRootRoutes) {
            // Feature roots never inherit prior feature history. Detail screens
            // intentionally use the branch below so their Back path stays local.
            navigateFeatureRoot(route)
        } else {
            navController.navigate(route) { launchSingleTop = true; restoreState = true }
        }
    }
    fun navigateAndClear(route: String) { navController.navigate(route) { popUpTo(0) { inclusive = true } } }

    fun navigateTowardHomeAfterLogin() {
        scope.launch {
            val done = withContext(Dispatchers.IO) {
                dataStore.launchUsageQuestionnaireCompleted.first()
            }
            navigateAndClear(if (done) Routes.HOME else Routes.LAUNCH_USAGE_QUESTIONNAIRE)
        }
    }

    LaunchedEffect(isLoggedIn) {
        // null = DataStore not yet loaded, don't redirect yet
        if (isLoggedIn == false && currentRoute != Routes.SPLASH && currentRoute != Routes.AUTH) {
            navController.navigate(Routes.AUTH) { popUpTo(0) { inclusive = true } }
        }
    }

    // ── PiP restore: navigate to Ekagra when user taps the PiP window ────────
    val activity = LocalContext.current as? MainActivity
    val navigateToEkagra = activity?.navigateToEkagra ?: false
    LaunchedEffect(navigateToEkagra) {
        if (navigateToEkagra) {
            // Only navigate to Ekagra if the user is still logged in — prevents
            // a tap on a stale PiP window from bypassing the AUTH screen after logout
            if (isLoggedIn != false && currentRoute != Routes.EKAGRA) {
                navigate(Routes.EKAGRA)
            }
            activity?.resetNavigateToEkagra() // always reset, even if we skipped navigation
        }
    }

    val notificationRoute = activity?.notificationRoute
    LaunchedEffect(notificationRoute, isLoggedIn) {
        val route = notificationRoute ?: return@LaunchedEffect
        if (isLoggedIn != true) return@LaunchedEffect
        if (currentRoute != route) {
            navigate(route)
        }
        activity.resetNotificationRoute()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(240)) { it / 5 } + fadeIn(animationSpec = tween(180))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(220)) { -it / 8 } + fadeOut(animationSpec = tween(160))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(240)) { -it / 5 } + fadeIn(animationSpec = tween(180))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(220)) { it / 8 } + fadeOut(animationSpec = tween(160))
        },
    ) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToAuth = { navigateAndClear(Routes.AUTH) },
                onNavigateToHome = { navigateTowardHomeAfterLogin() },
                isDarkTheme = isDarkTheme
            )
        }

        composable(Routes.AUTH) { AuthScreen(onNavigateToHome = { navigateTowardHomeAfterLogin() }) }

        composable(Routes.LAUNCH_USAGE_QUESTIONNAIRE) {
            LaunchUsageQuestionnaireScreen(
                dataStore = dataStore,
                onNavigateHome = { 
                    navigateAndClear(Routes.HOME)
                    navigate(Routes.PREMIUM)
                },
                onNavigateKavach = {
                    navigateAndClear(Routes.FOCUS_SHIELD)
                },
                onUnauthorized = { navigateAndClear(Routes.AUTH) },
            )
        }

        composable(Routes.HOME) { HomeScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onNavigateToAuth = { navigateAndClear(Routes.AUTH) }, dataStore = dataStore) }

        composable(Routes.DASHBOARD) {
            DashboardScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onProfileClick = { navigate(Routes.PROFILE) })
        }

        composable(Routes.NISHTHA) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onProfileClick = { navigate(Routes.PROFILE) })
        }

        composable(Routes.NISHTHA_CHECKIN) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 0)
        }

        composable(Routes.NISHTHA_GOALS) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 2)
        }

        composable(Routes.NISHTHA_STREAKS) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 3)
        }

        composable(Routes.NISHTHA_ANALYTICS) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 4)
        }

        composable(
            route = Routes.NISHTHA_ANALYTICS_SECTION,
            arguments = listOf(
                navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = "overview" },
            )
        ) { entry ->
            NishthaScreen(
                currentRoute = Routes.NISHTHA_ANALYTICS,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
                onProfileClick = { navigate(Routes.PROFILE) },
                initialTab = 4,
                analyticsInitialSection = entry.arguments?.getString("section") ?: "overview",
            )
        }

        composable(Routes.EKAGRA) {
            EkagraScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleDarkTheme)
        }

        composable(
            route = Routes.EKAGRA_LINKED,
            arguments = listOf(
                navArgument("goalId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("goalTitle") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("view") { type = NavType.StringType; nullable = true; defaultValue = null },
            )
        ) { entry ->
            EkagraScreen(
                currentRoute = Routes.EKAGRA,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleNightMode = onToggleDarkTheme,
                linkedGoalId = entry.arguments?.getString("goalId"),
                linkedGoalTitle = entry.arguments?.getString("goalTitle"),
                initialView = entry.arguments?.getString("view"),
            )
        }

        composable(
            route = Routes.STUDY_PLANNER_ROUTE,
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            StudyPlannerScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onBack = { navController.popBackStack() },
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(
            route = Routes.ROUTE_SYLLABUS_SUBJECTS,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(220)) },
            exitTransition = { slideOutHorizontally { -it } + fadeOut(tween(220)) },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) }
        ) { entry ->
            if (!studyPlannerPremiumUnlocked) {
                LaunchedEffect(Unit) { navigate(Routes.PREMIUM) }
            } else {
                val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.STUDY_PLANNER_ROUTE) }
                val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel>(parentEntry)
                val planId = entry.arguments?.getString("planId") ?: ""
                
                SyllabusSubjectsScreen(
                    viewModel = viewModel,
                    planId = planId,
                    onNavigate = ::navigate,
                    onBack = {
                        viewModel.setSection(com.safarparmar.app.domain.model.studyplanner.PlannerSection.PLAN)
                        navController.popBackStack()
                    },
                    onPlannerSectionSelect = { section ->
                        viewModel.setSection(section)
                        if (section != com.safarparmar.app.domain.model.studyplanner.PlannerSection.SYLLABUS) {
                            navController.popBackStack(Routes.STUDY_PLANNER_ROUTE, false)
                        }
                    },
                )
            }
        }

        composable(
            route = Routes.ROUTE_SYLLABUS_CHAPTERS,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(220)) },
            exitTransition = { slideOutHorizontally { -it } + fadeOut(tween(220)) },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) }
        ) { entry ->
            if (!studyPlannerPremiumUnlocked) {
                LaunchedEffect(Unit) { navigate(Routes.PREMIUM) }
            } else {
                val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.STUDY_PLANNER_ROUTE) }
                val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel>(parentEntry)
                val planId = entry.arguments?.getString("planId") ?: ""
                val subjectId = entry.arguments?.getString("subjectId") ?: ""
                
                SyllabusChaptersScreen(
                    viewModel = viewModel,
                    planId = planId,
                    subjectId = subjectId,
                    onNavigate = ::navigate,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Routes.ROUTE_SYLLABUS_TOPICS,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(220)) },
            exitTransition = { slideOutHorizontally { -it } + fadeOut(tween(220)) },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) }
        ) { entry ->
            if (!studyPlannerPremiumUnlocked) {
                LaunchedEffect(Unit) { navigate(Routes.PREMIUM) }
            } else {
                val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.STUDY_PLANNER_ROUTE) }
                val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel>(parentEntry)
                val planId = entry.arguments?.getString("planId") ?: ""
                val subjectId = entry.arguments?.getString("subjectId") ?: ""
                val chapterId = entry.arguments?.getString("chapterId") ?: ""
                
                SyllabusTopicsScreen(
                    viewModel = viewModel,
                    planId = planId,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    onNavigate = ::navigate,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.MEHFIL) {
            MehfilScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme)
        }

        composable(Routes.DM_CHAT) {
            // Scope ViewModel to MEHFIL so state is shared; single popBackStack goes straight back
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Routes.MEHFIL)
            }
            val mehfilVm = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.mehfil.MehfilViewModel>(parentEntry)
            DmChatScreen(
                viewModel = mehfilVm,
                onBack = {
                    navController.popBackStack(Routes.MEHFIL, inclusive = false)
                },
            )
        }

        composable(Routes.DHYAN) {
            DhyanScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme)
        }

        composable(Routes.COURSES) {
            DhyanCoursesScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme)
        }

        composable(Routes.FOCUS_SHIELD) {
            FocusShieldStandaloneScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onBack = { navController.popBackStack() },
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(Routes.APP_PICKER) {
            com.safarparmar.app.ui.ekagra.focusshield.AppPickerScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.KAVACH_ABOUT) {
            KavachAboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.LIVE_SESSIONS,
            arguments = listOf(
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { entry ->
            val courseId = entry.arguments?.getString("courseId").orEmpty()
            DhyanCoursesScreen(
                currentRoute = Routes.COURSES,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
                initialTab = CoursesHubTab.LIVE_CLASSES,
                liveCourseId = courseId,
            )
        }

        composable(
            route = Routes.LIVE_SESSION,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { entry ->
            val sessionId = entry.arguments?.getString("sessionId").orEmpty()
            LiveSessionScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            val parentEntry = remember(currentEntry) { navController.getBackStackEntry(Routes.DASHBOARD) }
            val dashVm = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.dashboard.DashboardViewModel>(parentEntry)
            val uiState by dashVm.uiState.collectAsStateWithLifecycle()
            AchievementsScreen(
                achievements = uiState.allAchievements,
                selectedAchievementId = uiState.activeTitleId,
                onSelectAchievement = dashVm::selectAchievement,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            val timerService = LocalTimerService.current
            ProfileScreen(
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() },
                onLogout = {
                    timerService?.reset()      // stop timer + clear SharedPrefs theme
                    activity?.onLogout()       // dismiss PiP, reset nav flag, disable auto-enter
                    navigateAndClear(Routes.AUTH)
                },
                onHome = { navigate(Routes.HOME) },
                onToggleDarkTheme = onToggleDarkTheme,
                onLibrary = { navigate(Routes.DASHBOARD) },
                onProgress = { navigate(Routes.NISHTHA_ANALYTICS) },
                onPremium = { navigate(Routes.PREMIUM) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() },
                onHome = { navigate(Routes.HOME) },
                onToggleDarkTheme = onToggleDarkTheme,
                dataStore = dataStore,
                canAccessAdminComposer = canAccessAdminComposer,
                onOpenAdminNotificationComposer = {
                    if (canAccessAdminComposer) {
                        navigate(Routes.ADMIN_NOTIFICATIONS)
                    }
                },
                onPremium = { navigate(Routes.PREMIUM) },
            )
        }

        composable(Routes.ADMIN_NOTIFICATIONS) {
            if (!canAccessAdminComposer) {
                LaunchedEffect(Unit) { navigate(Routes.HOME) }
            } else {
                AdminNotificationComposerScreen(
                    currentRoute = currentRoute,
                    isDarkTheme = isDarkTheme,
                    onNavigate = ::navigate,
                    onToggleDarkTheme = onToggleDarkTheme,
                )
            }
        }

        composable(Routes.PREMIUM) {
            PremiumPaywallScreen(
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() },
                onNavigate = ::navigate
            )
        }
    }
}
