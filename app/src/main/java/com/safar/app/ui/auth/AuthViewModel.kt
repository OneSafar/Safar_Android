package com.safar.app.ui.auth

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.R
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SwitchMode              -> _uiState.update { it.copy(isSignupMode = !it.isSignupMode, error = null) }
            is AuthEvent.EmailChanged            -> _uiState.update { it.copy(email = event.value) }
            is AuthEvent.PasswordChanged         -> _uiState.update { it.copy(password = event.value) }
            is AuthEvent.NameChanged             -> _uiState.update { it.copy(name = event.value) }
            is AuthEvent.ConfirmPasswordChanged  -> _uiState.update { it.copy(confirmPassword = event.value) }
            is AuthEvent.ExamTypeChanged         -> _uiState.update { it.copy(examType = event.value) }
            is AuthEvent.PreparationStageChanged -> _uiState.update { it.copy(preparationStage = event.value) }
            is AuthEvent.GenderChanged           -> _uiState.update { it.copy(gender = event.value) }
            is AuthEvent.RememberMeToggled       -> _uiState.update { it.copy(rememberMe = !it.rememberMe) }
            is AuthEvent.ClearError              -> _uiState.update { it.copy(error = null) }
            is AuthEvent.ForgotPassword          -> handleForgotPassword()
            is AuthEvent.Login                   -> handleLogin()
            is AuthEvent.Signup                  -> handleSignup()
        }
    }

    private fun str(@StringRes id: Int) = context.getString(id)

    private fun handleLogin() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = str(R.string.error_email_required)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun handleSignup() {
        val state = _uiState.value
        when {
            state.name.isBlank()                    -> { _uiState.update { it.copy(error = str(R.string.error_name_required)) }; return }
            state.email.isBlank()                   -> { _uiState.update { it.copy(error = str(R.string.error_email_required)) }; return }
            !isValidEmail(state.email)              -> { _uiState.update { it.copy(error = str(R.string.error_invalid_domain)) }; return }
            state.password.length < 8               -> { _uiState.update { it.copy(error = str(R.string.error_password_length)) }; return }
            state.password != state.confirmPassword -> { _uiState.update { it.copy(error = str(R.string.error_password_mismatch)) }; return }
            state.gender.isBlank()                  -> { _uiState.update { it.copy(error = str(R.string.error_gender_required)) }; return }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(
                name     = state.name.trim(),
                email    = state.email.trim(),
                password = state.password,
                exam     = state.examType.ifBlank { null },
                stage    = state.preparationStage.ifBlank { null },
                gender   = state.gender.ifBlank { null },
                photoUrl = null
            )) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun handleForgotPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(error = str(R.string.error_enter_email_first)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.forgotPassword(email)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.error_reset_email_sent, email)) }
                is Resource.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val domain = email.substringAfterLast("@", "")
        return domain == "gmail.com" || domain == "outlook.com"
    }
}