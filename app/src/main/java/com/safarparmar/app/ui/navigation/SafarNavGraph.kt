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
import com.safarparmar.app.ui.components.ExitConfirmationHandler
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

    // ── Navigation helper ────────────────────────────────────────────────────
    //
    // All feature roots (drawer items) navigate using saveState = true so their
    // internal back-stack is preserved when the user returns to them.
    // Sub-screens push normally on top of their feature's own graph.

    fun navigate(route: String) {
        val routeBase = route.substringBefore("?")
        val currentRouteBase = currentRoute.substringBefore("?")

        // Feature root routes that live in the drawer — they each have their own
        // nested nav graph so they independently save/restore back-stack state.
        val featureGraphRoots = setOf(
            Routes.HOME,
            Routes.DASHBOARD,
            Routes.STUDY_PLANNER,
            Routes.FOCUS_SHIELD,
            Routes.NISHTHA,
            Routes.EKAGRA,
            Routes.MEHFIL,
            Routes.DHYAN,
            Routes.COURSES,
        )

        // Navigate to a feature root, saving the current feature's back-stack and
        // restoring the target's. popUpTo(HOME, inclusive=false) keeps Home pinned at
        // the base so every feature collapses to [Home, Feature] and Back is one hop.
        fun openFeatureRoot(featureRoute: String) {
            navController.navigate(featureRoute) {
                popUpTo(Routes.HOME) { saveState = true; inclusive = false }
                launchSingleTop = true
                restoreState = true
            }
        }

        // Push a sub-screen, first ensuring its parent feature root is on the back
        // stack beneath it. This is what makes the sub-screen's
        // getBackStackEntry(parent) safe and guarantees Back returns through the
        // parent → Home. When the parent is already present we skip the hop.
        fun ensureParentThenPush(childRoute: String, parentRoute: String, parentAlreadyPresent: Boolean) {
            if (!parentAlreadyPresent) openFeatureRoot(parentRoute)
            navController.navigate(childRoute) { launchSingleTop = true }
        }

        // A route can arrive from external input (a push-notification deep link, a
        // PiP restore, a legacy link) that no longer matches any registered
        // destination. NavController.navigate() throws IllegalArgumentException for an
        // unknown route; swallow it so a stale/malformed deep link is a no-op instead
        // of crashing the app.
        try {
        when {
            // DM Chat must sit on top of Mehfil (its ViewModel is scoped there).
            route == Routes.DM_CHAT ->
                ensureParentThenPush(route, Routes.MEHFIL,
                    parentAlreadyPresent = currentRouteBase == Routes.MEHFIL)

            // Live session detail sits on top of Courses.
            route.startsWith("live/session/") ->
                ensureParentThenPush(route, Routes.COURSES,
                    parentAlreadyPresent = currentRouteBase in setOf(Routes.COURSES, Routes.LIVE_SESSIONS_ROOT))

            // Syllabus detail sits on top of Study Planner.
            route.startsWith("syllabus/") ->
                ensureParentThenPush(route, Routes.STUDY_PLANNER,
                    parentAlreadyPresent = currentRouteBase == Routes.STUDY_PLANNER || currentRouteBase.startsWith("syllabus/"))

            // App Picker / Kavach About sit on top of Focus Shield (or Ekagra).
            route == Routes.APP_PICKER || route == Routes.KAVACH_ABOUT ->
                ensureParentThenPush(route, Routes.FOCUS_SHIELD,
                    parentAlreadyPresent = currentRouteBase in setOf(Routes.FOCUS_SHIELD, Routes.EKAGRA))

            // Achievements sits on top of Dashboard.
            route == Routes.ACHIEVEMENTS ->
                ensureParentThenPush(route, Routes.DASHBOARD,
                    parentAlreadyPresent = currentRouteBase == Routes.DASHBOARD)

            // Feature root (drawer item) — preserve each feature's back-stack independently.
            routeBase in featureGraphRoots ->
                openFeatureRoot(route)

            // All other sub-screens push on top of the current feature's stack.
            else ->
                navController.navigate(route) { launchSingleTop = true }
        }
        } catch (e: IllegalArgumentException) {
            // Unknown/unregistered route (usually a stale deep link). Ignore rather than crash.
            android.util.Log.w("SafarNavGraph", "Ignoring navigation to unknown route: $route", e)
        }
    }

    fun navigateAndClear(route: String) {
        navController.navigate(route) { popUpTo(0) { inclusive = true } }
    }

    // Safe back — if there's no previous entry to pop to (e.g. user opened a
    // feature directly from the drawer with nothing behind it), go Home instead
    // of letting the system close the app.
    fun safeBack() {
        val popped = navController.popBackStack()
        if (!popped) navigate(Routes.HOME)
    }

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
            // currentRoute may carry the EKAGRA_ROUTE query pattern, so compare the base.
            if (isLoggedIn != false && currentRoute.substringBefore("?") != Routes.EKAGRA) {
                navigate(Routes.EKAGRA)
            }
            activity?.resetNavigateToEkagra()
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

        // ── Auth / onboarding ─────────────────────────────────────────────────

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToAuth = { navigateAndClear(Routes.AUTH) },
                onNavigateToHome = { navigateTowardHomeAfterLogin() },
                isDarkTheme = isDarkTheme
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(onNavigateToHome = { navigateTowardHomeAfterLogin() })
        }

        composable(Routes.LAUNCH_USAGE_QUESTIONNAIRE) {
            LaunchUsageQuestionnaireScreen(
                dataStore = dataStore,
                onNavigateHome = {
                    navigateAndClear(Routes.HOME)
                    navigate(Routes.PREMIUM)
                },
                onNavigateKavach = {
                    // Seed Home as the base first, then open Focus Shield, so the rest
                    // of the session's popUpTo(HOME) navigation keeps working and Back
                    // from Focus Shield returns to Home instead of exiting.
                    navigateAndClear(Routes.HOME)
                    navigate(Routes.FOCUS_SHIELD)
                },
                onUnauthorized = { navigateAndClear(Routes.AUTH) },
            )
        }

        // ── Home (start destination after login) ──────────────────────────────

        composable(Routes.HOME) {
            // Home is the base destination — intercept Back here to confirm before
            // exiting, instead of the app closing on a single stray Back press.
            ExitConfirmationHandler(onConfirmExit = { activity?.finish() })
            HomeScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
                onNavigateToAuth = { navigateAndClear(Routes.AUTH) },
                dataStore = dataStore,
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────────────

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            // getBackStackEntry(DASHBOARD) throws IllegalArgumentException if the parent
            // isn't on the back stack (stack cleared by an auth redirect, a direct deep
            // link, or a final recompose during the exit animation). Bail out of rendering
            // rather than crashing — the screen is on its way out in that state anyway.
            val parentEntry = remember(currentEntry) {
                runCatching { navController.getBackStackEntry(Routes.DASHBOARD) }.getOrNull()
            } ?: return@composable
            val dashVm = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.dashboard.DashboardViewModel>(parentEntry)
            val uiState by dashVm.uiState.collectAsStateWithLifecycle()
            AchievementsScreen(
                achievements = uiState.allAchievements,
                selectedAchievementId = uiState.activeTitleId,
                onSelectAchievement = dashVm::selectAchievement,
                onBack = ::safeBack,
            )
        }

        // ── Nishtha (single composable — tab selected via initialTab arg) ─────
        // Sub-tabs are managed by FeatureTabBackStack inside NishthaScreen.
        // They are NOT separate nav destinations — this is intentional to avoid
        // the back-stack pollution that previously occurred.

        composable(
            route = Routes.NISHTHA_ROUTE,
            arguments = listOf(
                navArgument("tab") { type = NavType.IntType; defaultValue = 0 },
                navArgument("section") { type = NavType.StringType; nullable = true; defaultValue = "overview" },
            )
        ) { entry ->
            NishthaScreen(
                currentRoute = Routes.NISHTHA,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
                initialTab = entry.arguments?.getInt("tab") ?: 0,
                analyticsInitialSection = entry.arguments?.getString("section") ?: "overview",
            )
        }

        // ── Ekagra ───────────────────────────────────────────────────────────

        composable(
            route = Routes.EKAGRA_ROUTE,
            arguments = listOf(
                navArgument("goalId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("goalTitle") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("view") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("topicId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("topicTitle") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("planId") { type = NavType.StringType; nullable = true; defaultValue = null },
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
                linkedTopicId = entry.arguments?.getString("topicId"),
                linkedTopicTitle = entry.arguments?.getString("topicTitle"),
                linkedPlanId = entry.arguments?.getString("planId"),
            )
        }

        composable(Routes.FOCUS_SHIELD) {
            FocusShieldStandaloneScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onBack = ::safeBack,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(Routes.APP_PICKER) {
            com.safarparmar.app.ui.ekagra.focusshield.AppPickerScreen(
                onBack = ::safeBack,
            )
        }

        composable(Routes.KAVACH_ABOUT) {
            KavachAboutScreen(onBack = ::safeBack)
        }

        // ── Study Planner ─────────────────────────────────────────────────────

        composable(
            route = Routes.STUDY_PLANNER_ROUTE,
            arguments = listOf(
                navArgument("planId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("showDailyTodoSetup") { type = NavType.BoolType; defaultValue = false },
                navArgument("openTab") { type = NavType.StringType; nullable = true; defaultValue = null },
            )
        ) { entry ->
            StudyPlannerScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                planId = entry.arguments?.getString("planId"),
                showDailyTodoSetup = entry.arguments?.getBoolean("showDailyTodoSetup") ?: false,
                openTab = entry.arguments?.getString("openTab"),
                onNavigate = ::navigate,
                onBack = ::safeBack,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(route = Routes.CREATE_PLAN) {
            val premiumViewModel2: PremiumViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val premiumStatus2 by premiumViewModel2.premiumStatus.collectAsStateWithLifecycle()
            val canUsePremiumPlannerFeatures = premiumStatus2.hasAnyPaidAccess || premiumStatus2.canUseStudyPlannerInsights
            com.safarparmar.app.ui.studyplanner.create.CreatePlanScreen(
                canUsePremiumPlannerFeatures = canUsePremiumPlannerFeatures,
                onUpgrade = { navigate(Routes.PREMIUM) },
                onBack = ::safeBack,
                onPlanConfirmed = { confirmedPlanId ->
                    navController.navigate("${Routes.STUDY_PLANNER}?planId=$confirmedPlanId&showDailyTodoSetup=false") {
                        popUpTo(Routes.CREATE_PLAN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.ROUTE_SYLLABUS_SUBJECTS,
            enterTransition = { slideInHorizontally { it } + fadeIn(tween(220)) },
            exitTransition = { slideOutHorizontally { -it } + fadeOut(tween(220)) },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn(tween(220)) },
            popExitTransition = { slideOutHorizontally { it } + fadeOut(tween(220)) },
        ) { entry ->
            if (!studyPlannerPremiumUnlocked) {
                LaunchedEffect(Unit) { navigate(Routes.PREMIUM) }
            } else {
                val parentEntry = remember(entry) {
                    runCatching { navController.getBackStackEntry(Routes.STUDY_PLANNER_ROUTE) }.getOrNull()
                } ?: return@composable
                val viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.studyplanner.StudyPlannerViewModel>(parentEntry)
                val planId = entry.arguments?.getString("planId") ?: ""

                SyllabusSubjectsScreen(
                    viewModel = viewModel,
                    planId = planId,
                    isDarkTheme = isDarkTheme,
                    onNavigate = ::navigate,
                    onBack = {
                        viewModel.setSection(com.safarparmar.app.domain.model.studyplanner.PlannerSection.PLAN)
                        safeBack()
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

        // ── Mehfil ───────────────────────────────────────────────────────────

        composable(Routes.MEHFIL) {
            MehfilScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(Routes.DM_CHAT) {
            val parentEntry = remember(it) {
                runCatching { navController.getBackStackEntry(Routes.MEHFIL) }.getOrNull()
            } ?: return@composable
            val mehfilVm = androidx.hilt.navigation.compose.hiltViewModel<com.safarparmar.app.ui.mehfil.MehfilViewModel>(parentEntry)
            DmChatScreen(
                viewModel = mehfilVm,
                onBack = ::safeBack,
            )
        }

        // ── Dhyan / Courses / Live ────────────────────────────────────────────

        composable(Routes.DHYAN) {
            DhyanScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        composable(Routes.COURSES) {
            DhyanCoursesScreen(
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
            )
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
                onBack = ::safeBack,
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = ::navigate,
                onToggleDarkTheme = onToggleDarkTheme,
            )
        }

        // ── Global / utility screens ─────────────────────────────────────────

        composable(Routes.PROFILE) {
            val timerService = LocalTimerService.current
            ProfileScreen(
                isDarkTheme = isDarkTheme,
                onBack = ::safeBack,
                onLogout = {
                    timerService?.reset()
                    activity?.onLogout()
                    navigateAndClear(Routes.AUTH)
                },
                onHome = { navigate(Routes.HOME) },
                onToggleDarkTheme = onToggleDarkTheme,
                onLibrary = { navigate(Routes.DASHBOARD) },
                onProgress = { navigate(Routes.nishthaTab(4)) },
                onPremium = { navigate(Routes.PREMIUM) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onBack = ::safeBack,
                onHome = { navigate(Routes.HOME) },
                onToggleDarkTheme = onToggleDarkTheme,
                dataStore = dataStore,
                canAccessAdminComposer = canAccessAdminComposer,
                onOpenAdminNotificationComposer = {
                    if (canAccessAdminComposer) navigate(Routes.ADMIN_NOTIFICATIONS)
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
                onBack = ::safeBack,
                onNavigate = ::navigate,
            )
        }
    }
}
