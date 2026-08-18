package com.safarparmar.app.ui.auth

enum class ForgotPasswordStep {
    EMAIL,
    EMAIL_SENT,
    RESET
}

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
    // Free-text exact exam name, entered when examType == "Other" so the actual
    // exam gets stored instead of the literal word "Other".
    val customExamType: String = "",
    val preparationStage: String = "",
    val gender: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null,
    val confirmPasswordError: String? = null,
    val genderError: String? = null,
    val customExamTypeError: String? = null,
    val isForgotPasswordMode: Boolean = false,
    val forgotPasswordStep: ForgotPasswordStep = ForgotPasswordStep.EMAIL,
    val forgotPasswordToken: String = "",
    val resetNewPassword: String = "",
    val resetConfirmPassword: String = "",
    val resetNewPasswordError: String? = null,
    val resetConfirmPasswordError: String? = null,
)

sealed class AuthEvent {
    object SwitchMode : AuthEvent()
    object Login : AuthEvent()
    object Signup : AuthEvent()
    data class GoogleLogin(val idToken: String) : AuthEvent()
    data class Error(val message: String) : AuthEvent()
    object ForgotPassword : AuthEvent()
    object BackToLoginClicked : AuthEvent()
    object RememberMeToggled : AuthEvent()
    object ClearError : AuthEvent()
    data class EmailChanged(val value: String) : AuthEvent()
    data class PasswordChanged(val value: String) : AuthEvent()
    data class NameChanged(val value: String) : AuthEvent()
    data class ConfirmPasswordChanged(val value: String) : AuthEvent()
    data class ExamTypeChanged(val value: String) : AuthEvent()
    data class CustomExamTypeChanged(val value: String) : AuthEvent()
    data class PreparationStageChanged(val value: String) : AuthEvent()
    data class GenderChanged(val value: String) : AuthEvent()
    data class ResetNewPasswordChanged(val value: String) : AuthEvent()
    data class ResetConfirmPasswordChanged(val value: String) : AuthEvent()
    object SubmitForgotPasswordRequest : AuthEvent()
    object SubmitResetPasswordConfirm : AuthEvent()
}

