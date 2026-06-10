package com.safarparmar.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.isLightBackground
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val snackbarHostState = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme
    val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateToHome()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, actionLabel = "OK")
            viewModel.onEvent(AuthEvent.ClearError)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = scheme.inverseSurface,
                    contentColor = scheme.inverseOnSurface,
                    actionColor = scheme.inversePrimary,
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
        containerColor = scheme.background,
    ) { padding ->
        AuthStitchScaffold(
            isDark = isDark,
            isSignupMode = uiState.isSignupMode,
            logoRes = logoRes,
            modifier = Modifier.padding(padding),
        ) {
            AuthStitchModeCrossfade(
                isSignupMode = uiState.isSignupMode,
                loginContent = {
                    LoginForm(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onSwitchToSignup = { viewModel.onEvent(AuthEvent.SwitchMode) },
                    )
                },
                signupContent = {
                    SignupForm(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onSwitchToLogin = { viewModel.onEvent(AuthEvent.SwitchMode) },
                    )
                },
            )
        }
    }
}

@Composable
private fun LoginForm(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToSignup: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AuthStitchFormCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuthStitchFieldLabel("EMAIL")
                AuthStitchTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    placeholder = "you@gmail.com",
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )

                AuthStitchFieldLabel("PASSWORD")
                AuthStitchPasswordField(
                    value = uiState.password,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    placeholder = "Enter your password",
                    passwordVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onEvent(AuthEvent.Login)
                    }),
                )

                AuthStitchRememberRow(
                    checked = uiState.rememberMe,
                    onToggle = { onEvent(AuthEvent.RememberMeToggled) },
                    onForgotPassword = { onEvent(AuthEvent.ForgotPassword) },
                )

                AuthStitchPrimaryButton(
                    text = if (uiState.isLoading) "Signing in..." else "Sign In",
                    onClick = { onEvent(AuthEvent.Login) },
                    enabled = !uiState.isLoading,
                )
            }
        }

        AuthStitchModeSwitch(
            prompt = "Don't have an account?",
            actionLabel = "Sign Up",
            onAction = onSwitchToSignup,
        )
        AuthStitchFooterTagline()
    }
}

@Composable
private fun SignupForm(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val examOptions = listOf("SSC CGL", "SSC CHSL", "SSC MTS", "SSC CPO", "Other")
    val prepStageOptions = listOf("Just Started", "1-3 Months", "3-6 Months", "6+ Months", "Final Stage")
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AuthStitchFormCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuthStitchFieldLabel("FULL NAME")
                AuthStitchTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
                    placeholder = "John Doe",
                    leadingIcon = Icons.Default.Person,
                    isError = !uiState.nameError.isNullOrBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
                AuthFieldError(uiState.nameError)

                AuthStitchFieldLabel("EMAIL")
                AuthStitchTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    placeholder = "you@gmail.com",
                    leadingIcon = Icons.Default.Email,
                    isError = !uiState.emailError.isNullOrBlank(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
                AuthFieldError(uiState.emailError)

                AuthStitchFieldLabel("PASSWORD")
                AuthStitchPasswordField(
                    value = uiState.password,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    placeholder = "At least 8 characters",
                    passwordVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                    isError = !uiState.passwordError.isNullOrBlank(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
                AuthFieldError(uiState.passwordError)

                AuthStitchFieldLabel("CONFIRM PASSWORD")
                AuthStitchPasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
                    placeholder = "Re-enter password",
                    passwordVisible = confirmPasswordVisible,
                    onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                    isError = !uiState.confirmPasswordError.isNullOrBlank(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
                AuthFieldError(uiState.confirmPasswordError)

                AuthStitchFieldLabel("TARGET EXAM")
                AuthDropdownM3(
                    value = uiState.examType,
                    placeholder = "Select Target Exam",
                    options = examOptions,
                    onSelect = { onEvent(AuthEvent.ExamTypeChanged(it)) },
                )

                AuthStitchFieldLabel("PREPARATION STAGE")
                AuthDropdownM3(
                    value = uiState.preparationStage,
                    placeholder = "Select Preparation Stage",
                    options = prepStageOptions,
                    onSelect = { onEvent(AuthEvent.PreparationStageChanged(it)) },
                )

                AuthStitchFieldLabel("GENDER")
                AuthDropdownM3(
                    value = uiState.gender,
                    placeholder = "Select Gender",
                    options = genderOptions,
                    onSelect = { onEvent(AuthEvent.GenderChanged(it)) },
                )
                AuthFieldError(uiState.genderError)

                Spacer(Modifier.height(4.dp))

                AuthStitchPrimaryButton(
                    text = if (uiState.isLoading) "Creating account..." else "Create Account",
                    onClick = { onEvent(AuthEvent.Signup) },
                    enabled = !uiState.isLoading,
                )
            }
        }

        AuthStitchModeSwitch(
            prompt = "Already have an account?",
            actionLabel = "Sign In",
            onAction = onSwitchToLogin,
        )
        AuthStitchFooterTagline()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthDropdownM3(
    value: String,
    placeholder: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val fieldShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Box(Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                placeholder = { Text(placeholder) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = fieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = scheme.onSurface,
                    unfocusedTextColor = scheme.onSurface,
                    cursorColor = scheme.primary,
                    focusedBorderColor = scheme.primary.copy(alpha = 0.55f),
                    unfocusedBorderColor = scheme.outline.copy(alpha = 0.45f),
                    focusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = scheme.surfaceVariant.copy(alpha = 0.35f),
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(scheme.surface),
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                opt,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                        },
                        onClick = {
                            onSelect(opt)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
