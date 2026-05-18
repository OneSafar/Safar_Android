package com.safar.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

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
            is AuthEvent.ExamTypeChanged -> _uiState.update { it.copy(examType = event.value) }
            is AuthEvent.PreparationStageChanged -> _uiState.update { it.copy(preparationStage = event.value) }
            is AuthEvent.GenderChanged -> _uiState.update {
                it.copy(gender = event.value, genderError = null, error = null)
            }
            is AuthEvent.RememberMeToggled -> _uiState.update { it.copy(rememberMe = !it.rememberMe) }
            is AuthEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is AuthEvent.ForgotPassword -> handleForgotPassword()
            is AuthEvent.Login -> handleLogin()
            is AuthEvent.Signup -> handleSignup()
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
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
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

        if (nameError != null || emailError != null || passwordError != null ||
            confirmPasswordError != null || genderError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    genderError = genderError,
                    error = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(
                name = state.name.trim(),
                email = state.email.trim(),
                password = state.password,
                exam = state.examType.ifBlank { null },
                stage = state.preparationStage.ifBlank { null },
                gender = state.gender.ifBlank { null },
                photoUrl = null,
            )) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun handleForgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Please enter your email first", error = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.forgotPassword(email)) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, error = "Password reset email sent to $email")
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val domain = email.substringAfterLast("@", "")
        return domain == "gmail.com" || domain == "outlook.com"
    }
}
