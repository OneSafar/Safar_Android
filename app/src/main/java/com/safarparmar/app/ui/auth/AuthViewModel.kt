package com.safarparmar.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    private companion object {
        const val AUTH_REQUEST_TIMEOUT_MS = 35_000L
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SwitchMode -> _uiState.update {
                it.copy(
                    isSignupMode = !it.isSignupMode,
                    error = null,
                    emailError = null,
                    passwordError = null,
                    nameError = null,
                    confirmPasswordError = null,
                    genderError = null,
                )
            }
            is AuthEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailError = null, error = null)
            }
            is AuthEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.value, passwordError = null, error = null)
            }
            is AuthEvent.NameChanged -> _uiState.update {
                it.copy(name = event.value, nameError = null, error = null)
            }
            is AuthEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordError = null, error = null)
            }
            is AuthEvent.ExamTypeChanged -> _uiState.update {
                it.copy(examType = event.value, customExamTypeError = null, error = null)
            }
            is AuthEvent.CustomExamTypeChanged -> _uiState.update {
                it.copy(customExamType = event.value, customExamTypeError = null, error = null)
            }
            is AuthEvent.PreparationStageChanged -> _uiState.update { it.copy(preparationStage = event.value) }
            is AuthEvent.GenderChanged -> _uiState.update {
                it.copy(gender = event.value, genderError = null, error = null)
            }
            is AuthEvent.RememberMeToggled -> _uiState.update { it.copy(rememberMe = !it.rememberMe) }
            is AuthEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is AuthEvent.ForgotPassword -> _uiState.update {
                it.copy(
                    isForgotPasswordMode = true,
                    forgotPasswordStep = ForgotPasswordStep.EMAIL,
                    error = null,
                    emailError = null
                )
            }
            is AuthEvent.BackToLoginClicked -> _uiState.update {
                it.copy(
                    isForgotPasswordMode = false,
                    isSignupMode = false,
                    error = null,
                    emailError = null
                )
            }
            is AuthEvent.ResetNewPasswordChanged -> _uiState.update {
                it.copy(resetNewPassword = event.value, resetNewPasswordError = null, error = null)
            }
            is AuthEvent.ResetConfirmPasswordChanged -> _uiState.update {
                it.copy(resetConfirmPassword = event.value, resetConfirmPasswordError = null, error = null)
            }
            is AuthEvent.SubmitForgotPasswordRequest -> handleForgotPassword()
            is AuthEvent.SubmitResetPasswordConfirm -> handleResetPasswordConfirm()
            is AuthEvent.Login -> handleLogin()
            is AuthEvent.Signup -> handleSignup()
            is AuthEvent.GoogleLogin -> handleGoogleLogin(event.idToken)
            is AuthEvent.Error -> _uiState.update { it.copy(error = event.message) }
        }
    }

    private fun handleLogin() {
        val state = _uiState.value
        val emailError = when {
            state.email.isBlank() -> "Email is required"
            else -> null
        }
        val passwordError = when {
            state.password.isBlank() -> "Password is required"
            else -> null
        }
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError, error = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = withTimeout(AUTH_REQUEST_TIMEOUT_MS) {
                    authRepository.login(state.email.trim(), state.password)
                }) {
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = false, error = "Sign-in is taking too long. Please try again.") }
                }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(isLoading = false, error = "Sign-in timed out. Check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign-in failed. Please try again.") }
            }
        }
    }

    private fun handleSignup() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) "Name is required" else null
        val emailError = when {
            state.email.isBlank() -> "Email is required"
            !isValidEmail(state.email) -> "Please use a valid email (gmail / outlook)"
            else -> null
        }
        val passwordError = when {
            state.password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
        val confirmPasswordError = when {
            state.password != state.confirmPassword -> "Passwords do not match"
            else -> null
        }
        val genderError = if (state.gender.isBlank()) "Please select a gender" else null
        // When "Other" is picked, the exact exam name typed into the follow-up
        // field is what actually gets stored — the literal word "Other" is never
        // sent to the backend.
        val isCustomExam = state.examType == "Other"
        val customExamTypeError = if (isCustomExam && state.customExamType.isBlank()) {
            "Please enter your exact exam"
        } else null

        if (nameError != null || emailError != null || passwordError != null ||
            confirmPasswordError != null || genderError != null || customExamTypeError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    genderError = genderError,
                    customExamTypeError = customExamTypeError,
                    error = null,
                )
            }
            return
        }
        val resolvedExam = (if (isCustomExam) state.customExamType else state.examType).trim()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = withTimeout(AUTH_REQUEST_TIMEOUT_MS) {
                    authRepository.register(
                        name = state.name.trim(),
                        email = state.email.trim(),
                        password = state.password,
                        exam = resolvedExam.ifBlank { null },
                        stage = state.preparationStage.ifBlank { null },
                        gender = state.gender.ifBlank { null },
                        photoUrl = null,
                    )
                }) {
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = false, error = "Signup is taking too long. Please try again.") }
                }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(isLoading = false, error = "Signup timed out. Check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Signup failed. Please try again.") }
            }
        }
    }

    private fun handleForgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email is required", error = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = withTimeout(AUTH_REQUEST_TIMEOUT_MS) {
                    authRepository.forgotPassword(email)
                }) {
                    is Resource.Success -> {
                        val token = result.data?.resetToken
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                forgotPasswordToken = token ?: "",
                                forgotPasswordStep = if (!token.isNullOrBlank()) ForgotPasswordStep.RESET else ForgotPasswordStep.EMAIL_SENT,
                                error = null,
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = false, error = "Request is taking too long. Please try again.") }
                }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(isLoading = false, error = "Request timed out. Check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Password reset failed. Please try again.") }
            }
        }
    }

    private fun handleResetPasswordConfirm() {
        val state = _uiState.value
        val newPasswordError = when {
            state.resetNewPassword.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
        val confirmPasswordError = when {
            state.resetNewPassword != state.resetConfirmPassword -> "Passwords do not match"
            else -> null
        }
        if (newPasswordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    resetNewPasswordError = newPasswordError,
                    resetConfirmPasswordError = confirmPasswordError,
                    error = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = withTimeout(AUTH_REQUEST_TIMEOUT_MS) {
                    authRepository.resetPasswordConfirm(state.forgotPasswordToken, state.resetNewPassword)
                }) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isForgotPasswordMode = false,
                                isSignupMode = false,
                                error = "Password reset successfully. Please log in.",
                                resetNewPassword = "",
                                resetConfirmPassword = "",
                                forgotPasswordToken = "",
                                forgotPasswordStep = ForgotPasswordStep.EMAIL,
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = false, error = "Reset is taking too long. Please try again.") }
                }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(isLoading = false, error = "Reset timed out. Check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to reset password. Please try again.") }
            }
        }
    }

    private fun handleGoogleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (val result = withTimeout(AUTH_REQUEST_TIMEOUT_MS) {
                    authRepository.googleLogin(idToken)
                }) {
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = false, error = "Google Sign-in is taking too long. Please try again.") }
                }
            } catch (_: TimeoutCancellationException) {
                _uiState.update { it.copy(isLoading = false, error = "Google Sign-in timed out. Check your connection and try again.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Google Sign-in failed. Please try again.") }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val domain = email.substringAfterLast("@", "")
        return domain == "gmail.com" || domain == "outlook.com"
    }
}
