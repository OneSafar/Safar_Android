package com.safar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safar.app.ui.auth.AuthScreen
import com.safar.app.ui.dhyan.DhyanScreen
import com.safar.app.ui.ekagra.EkagraScreen
import com.safar.app.ui.home.HomeScreen
import com.safar.app.ui.mehfil.MehfilScreen
import com.safar.app.ui.nightmode.NightModeScreen
import com.safar.app.ui.splash.SplashScreen
import com.safar.app.ui.onboarding.OnboardingScreen

@Composable
fun SafarNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } } }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = { navController.navigate(Screen.Auth.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Auth.route) { inclusive = true } } }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.NightMode.route) {
            NightModeScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Mehfil.route) {
            MehfilScreen(
                navController = navController,
            )
        }

        composable(Screen.Dhyan.route) {
            DhyanScreen(
                navController = navController
            )
        }

        composable(Screen.Ekagra.route) {
            EkagraScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.NishthaCheckin.route) {
            EkagraScreen(onBack = { navController.popBackStack() })
        }
    }
}
