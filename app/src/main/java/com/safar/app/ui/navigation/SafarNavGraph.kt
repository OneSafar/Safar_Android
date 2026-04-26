package com.safar.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.safar.app.MainActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.achievements.AchievementsScreen
import com.safar.app.ui.auth.AuthScreen
import com.safar.app.ui.dashboard.DashboardScreen
import com.safar.app.ui.dhyan.DhyanScreen
import com.safar.app.ui.ekagra.EkagraScreen
import com.safar.app.ui.home.HomeScreen
import com.safar.app.ui.mehfil.DmChatScreen
import com.safar.app.ui.mehfil.MehfilScreen
import com.safar.app.ui.nishtha.NishthaScreen
import com.safar.app.ui.ekagra.LocalTimerService
import com.safar.app.ui.profile.ProfileScreen
import com.safar.app.ui.splash.SplashScreen

@Composable
fun SafarNavGraph(
    dataStore: SafarDataStore,
    isDarkTheme       : Boolean = false,
    isNightMode       : Boolean = false,
    onToggleDarkTheme : () -> Unit = {},
    onToggleNightMode : () -> Unit = {},
    onLanguageToggle  : () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: Routes.SPLASH
    val isLoggedIn by dataStore.isLoggedIn.collectAsState(initial = null)

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
                navController.navigate(Routes.EKAGRA) { launchSingleTop = true; restoreState = true }
            }
            activity?.resetNavigateToEkagra() // always reset, even if we skipped navigation
        }
    }

    fun navigate(route: String) {
        if (route == Routes.DM_CHAT) {
            navController.navigate(route) {
                launchSingleTop = true
                popUpTo(Routes.MEHFIL) { inclusive = false }
            }
        } else {
            navController.navigate(route) { launchSingleTop = true; restoreState = true }
        }
    }
    fun navigateAndClear(route: String) { navController.navigate(route) { popUpTo(0) { inclusive = true } } }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) { SplashScreen(onNavigateToAuth = { navigateAndClear(Routes.AUTH) }, onNavigateToHome = { navigateAndClear(Routes.HOME) }) }

        composable(Routes.AUTH) { AuthScreen(onNavigateToHome = { navigateAndClear(Routes.HOME) }) }

        composable(Routes.HOME) { HomeScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onLanguageClick = onLanguageToggle, onNavigateToAuth = { navigateAndClear(Routes.AUTH) }, dataStore = dataStore) }

        composable(Routes.DASHBOARD) {
            DashboardScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onLanguageClick = onLanguageToggle, onProfileClick = { navigate(Routes.PROFILE) })
        }

        composable(Routes.NISHTHA) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleNightMode, onLanguageClick = onLanguageToggle, onProfileClick = { navigate(Routes.PROFILE) })
        }

        composable(Routes.NISHTHA_CHECKIN) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleNightMode, onLanguageClick = onLanguageToggle, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 0)
        }

        composable(Routes.NISHTHA_GOALS) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleNightMode, onLanguageClick = onLanguageToggle, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 2)
        }

        composable(Routes.NISHTHA_STREAKS) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleNightMode, onLanguageClick = onLanguageToggle, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 3)
        }

        composable(Routes.NISHTHA_ANALYTICS) {
            NishthaScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleNightMode, onLanguageClick = onLanguageToggle, onProfileClick = { navigate(Routes.PROFILE) }, initialTab = 4)
        }

        composable(Routes.EKAGRA) {
            EkagraScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleNightMode = onToggleNightMode, onLanguageClick = onLanguageToggle)
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
                onToggleNightMode = onToggleNightMode,
                onLanguageClick = onLanguageToggle,
                linkedGoalId = entry.arguments?.getString("goalId"),
                linkedGoalTitle = entry.arguments?.getString("goalTitle"),
                initialView = entry.arguments?.getString("view"),
            )
        }

        composable(Routes.MEHFIL) {
            MehfilScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onLanguageClick = onLanguageToggle)
        }

        composable(Routes.DM_CHAT) {
            // Scope ViewModel to MEHFIL so state is shared; single popBackStack goes straight back
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Routes.MEHFIL)
            }
            val mehfilVm = androidx.hilt.navigation.compose.hiltViewModel<com.safar.app.ui.mehfil.MehfilViewModel>(parentEntry)
            DmChatScreen(
                viewModel = mehfilVm,
                onBack = {
                    navController.popBackStack(Routes.MEHFIL, inclusive = false)
                },
            )
        }

        composable(Routes.DHYAN) {
            DhyanScreen(currentRoute = currentRoute, isDarkTheme = isDarkTheme, onNavigate = ::navigate, onToggleDarkTheme = onToggleDarkTheme, onLanguageClick = onLanguageToggle)
        }

        composable(Routes.APP_PICKER) {
            com.safar.app.ui.ekagra.focusshield.AppPickerScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ACHIEVEMENTS) {
            val parentEntry = remember(currentEntry) { navController.getBackStackEntry(Routes.DASHBOARD) }
            val dashVm = androidx.hilt.navigation.compose.hiltViewModel<com.safar.app.ui.dashboard.DashboardViewModel>(parentEntry)
            val uiState by dashVm.uiState.collectAsState()
            AchievementsScreen(achievements = uiState.allAchievements, onBack = { navController.popBackStack() })
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
                onToggleDarkTheme = onToggleDarkTheme
            )
        }
    }
}
