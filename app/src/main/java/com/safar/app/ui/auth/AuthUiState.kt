package com.safar.app.ui.auth

data class AuthUiState(
    val isSignupMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val name: String = "",
    val confirmPassword: String = "",
    val examType: String = "",
    val preparationStage: String = "",
    val gender: String = ""
)

sealed class AuthEvent {
    object SwitchMode : AuthEvent()
    object Login : AuthEvent()
    object Signup : AuthEvent()
    object ForgotPassword : AuthEvent()
    object RememberMeToggled : AuthEvent()
    object ClearError : AuthEvent()
    data class EmailChanged(val value: String) : AuthEvent()
    data class PasswordChanged(val value: String) : AuthEvent()
    data class NameChanged(val value: String) : AuthEvent()
    data class ConfirmPasswordChanged(val value: String) : AuthEvent()
    data class ExamTypeChanged(val value: String) : AuthEvent()
    data class PreparationStageChanged(val value: String) : AuthEvent()
    data class GenderChanged(val value: String) : AuthEvent()
}
