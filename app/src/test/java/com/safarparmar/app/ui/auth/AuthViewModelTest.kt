package com.safarparmar.app.ui.auth

import com.safarparmar.app.domain.model.User
import com.safarparmar.app.domain.model.UserProfile
import com.safarparmar.app.domain.model.ForgotPasswordResult
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login requires email and password`() {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onEvent(AuthEvent.Login)

        assertEquals("Email is required", viewModel.uiState.value.emailError)
        assertEquals("Password is required", viewModel.uiState.value.passwordError)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `signup rejects unsupported email domain`() {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onEvent(AuthEvent.SwitchMode)
        viewModel.onEvent(AuthEvent.NameChanged("Kumar"))
        viewModel.onEvent(AuthEvent.EmailChanged("kumar@example.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("password123"))
        viewModel.onEvent(AuthEvent.ConfirmPasswordChanged("password123"))
        viewModel.onEvent(AuthEvent.GenderChanged("Male"))
        viewModel.onEvent(AuthEvent.Signup)

        assertEquals("Please use a valid email (gmail / outlook)", viewModel.uiState.value.emailError)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `login success marks success and stops loading`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository(loginResult = Resource.Success(User(id = "1"))))

        viewModel.onEvent(AuthEvent.EmailChanged("kumar@gmail.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("password123"))
        viewModel.onEvent(AuthEvent.Login)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `login error exposes repository message`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository(loginResult = Resource.Error("Nope")))

        viewModel.onEvent(AuthEvent.EmailChanged("kumar@gmail.com"))
        viewModel.onEvent(AuthEvent.PasswordChanged("password123"))
        viewModel.onEvent(AuthEvent.Login)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Nope", viewModel.uiState.value.error)
    }

    @Test
    fun `forgot password clicked transitions to email step`() {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onEvent(AuthEvent.ForgotPassword)

        assertTrue(viewModel.uiState.value.isForgotPasswordMode)
        assertEquals(ForgotPasswordStep.EMAIL, viewModel.uiState.value.forgotPasswordStep)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `submit forgot password email success transitions to reset step`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onEvent(AuthEvent.ForgotPassword)
        viewModel.onEvent(AuthEvent.EmailChanged("kumar@gmail.com"))
        viewModel.onEvent(AuthEvent.SubmitForgotPasswordRequest)
        advanceUntilIdle()

        assertEquals(ForgotPasswordStep.RESET, viewModel.uiState.value.forgotPasswordStep)
        assertEquals("fake_token", viewModel.uiState.value.forgotPasswordToken)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `submit reset password confirm success resets to login mode`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.onEvent(AuthEvent.ForgotPassword)
        viewModel.onEvent(AuthEvent.EmailChanged("kumar@gmail.com"))
        viewModel.onEvent(AuthEvent.SubmitForgotPasswordRequest)
        advanceUntilIdle()

        viewModel.onEvent(AuthEvent.ResetNewPasswordChanged("newpassword123"))
        viewModel.onEvent(AuthEvent.ResetConfirmPasswordChanged("newpassword123"))
        viewModel.onEvent(AuthEvent.SubmitResetPasswordConfirm)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isForgotPasswordMode)
        assertEquals("Password reset successfully. Please log in.", viewModel.uiState.value.error)
    }

    private class FakeAuthRepository(
        private val loginResult: Resource<User> = Resource.Error("unused"),
    ) : AuthRepository {
        override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)

        override suspend fun login(email: String, password: String): Resource<User> = loginResult

        override suspend fun register(
            name: String,
            email: String,
            password: String,
            exam: String?,
            stage: String?,
            gender: String?,
            photoUrl: String?,
        ): Resource<User> = Resource.Success(User(id = "new"))

        override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult> =
            Resource.Success(ForgotPasswordResult("ok", "fake_token"))

        override suspend fun resetPasswordConfirm(token: String, newPassword: String): Resource<Unit> =
            Resource.Success(Unit)

        override suspend fun logout(): Resource<Unit> = Resource.Success(Unit)

        override suspend fun refreshToken(): Resource<Unit> = Resource.Success(Unit)

        override suspend fun getMe(): Resource<UserProfile> = Resource.Success(UserProfile(id = "1"))

        override suspend fun updateProfile(
            name: String?,
            examType: String?,
            preparationStage: String?,
            gender: String?,
            avatar: String?,
        ): Resource<UserProfile> = Resource.Success(UserProfile(id = "1"))

        override suspend fun uploadAvatar(avatar: MultipartBody.Part): Resource<String> = Resource.Success("avatar")
    }
}
