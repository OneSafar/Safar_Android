package com.safar.app.ui.auth

import com.safar.app.domain.model.User
import com.safar.app.domain.model.UserProfile
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

        assertEquals("Email and password are required", viewModel.uiState.value.error)
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

        assertEquals("Please use a valid email (gmail / outlook)", viewModel.uiState.value.error)
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

        override suspend fun forgotPassword(email: String): Resource<String> = Resource.Success("ok")

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
    }
}
