package com.safar.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    object Home : Screen("home")

    object NishthaCheckin : Screen("nishtha_checkin")
    object Mehfil : Screen("mehfil")
    object Ekagra : Screen("ekagra")

    object Dhyan : Screen("dhyan")

    object NightMode : Screen("night_mode")
    object Profile : Screen("profile")
}
