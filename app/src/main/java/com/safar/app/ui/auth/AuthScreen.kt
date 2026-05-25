package com.safar.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.ui.theme.*

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val snackbarHostState = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme

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
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        containerColor = scheme.background
    ) { padding ->
        if (uiState.isSignupMode) {
            SignupScreenFromDesign(
                padding = padding,
                isDark = isDark,
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onSwitchToLogin = { viewModel.onEvent(AuthEvent.SwitchMode) }
            )
        } else {
            LoginScreenFromDesign(
                padding = padding,
                isDark = isDark,
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onSwitchToSignup = { viewModel.onEvent(AuthEvent.SwitchMode) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoginScreenFromDesign(
    padding: PaddingValues,
    isDark: Boolean,
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToSignup: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    val scheme = MaterialTheme.colorScheme
    val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxHeight()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo Section
            AsyncImage(
                model = logoRes,
                contentDescription = "SAFAR Logo",
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .size(80.dp),
                contentScale = ContentScale.Fit,
            )
            
            Text(
                text = "Welcome back",
                color = scheme.onSurface,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Sign in to continue your journey",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Sign In Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surface)
                    .padding(24.dp),
            ) {
                Text(
                    text = "Sign in to continue",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Email Field
                Text(
                    text = "EMAIL",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    placeholder = { Text("you@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = OutlinedTextFieldDefaults.shape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Password Field
                Text(
                    text = "PASSWORD",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    placeholder = { Text("••••••••") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = OutlinedTextFieldDefaults.shape,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onEvent(AuthEvent.Login)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )

                // Forgot Password
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEvent(AuthEvent.ForgotPassword) }, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            text = "Forgot password?",
                            color = scheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = { onEvent(AuthEvent.Login) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = ButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
                    )
                ) {
                    Text(
                        text = if (uiState.isLoading) "Signing in..." else "Sign In",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Secondary Actions
            FlowRow(
                modifier = Modifier.padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Sign Up",
                    color = scheme.secondary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable(onClick = onSwitchToSignup)
                )
            }

            Text(
                text = "KAVACH • WELLNESS FOR EVERY ASPIRANT",
                color = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignupScreenFromDesign(
    padding: PaddingValues,
    isDark: Boolean,
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val examOptions      = listOf("SSC CGL", "SSC CHSL", "SSC MTS", "SSC CPO", "Other")
    val prepStageOptions = listOf("Just Started", "1-3 Months", "3-6 Months", "6+ Months", "Final Stage")
    val genderOptions    = listOf("Male", "Female", "Other", "Prefer not to say")
    val scroll = rememberScrollState()

    val scheme = MaterialTheme.colorScheme
    val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxHeight()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo Section
            AsyncImage(
                model = logoRes,
                contentDescription = "SAFAR Logo",
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .size(80.dp),
                contentScale = ContentScale.Fit,
            )
            
            Text(
                text = "Create account",
                color = scheme.onSurface,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Start your study journey with SAFAR",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Sign Up Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surface)
                    .padding(24.dp),
            ) {
                Text(
                    text = "Sign up to begin",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Full Name
                Text(
                    text = "FULL NAME",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
                    placeholder = { Text("John Doe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !uiState.nameError.isNullOrBlank(),
                    shape = OutlinedTextFieldDefaults.shape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        errorBorderColor = scheme.error,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )
                if (!uiState.nameError.isNullOrBlank()) {
                    Text(
                        text = uiState.nameError,
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Email Field
                Text(
                    text = "EMAIL",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    placeholder = { Text("you@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !uiState.emailError.isNullOrBlank(),
                    shape = OutlinedTextFieldDefaults.shape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        errorBorderColor = scheme.error,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )
                if (!uiState.emailError.isNullOrBlank()) {
                    Text(
                        text = uiState.emailError,
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Password Field
                Text(
                    text = "PASSWORD",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    placeholder = { Text("••••••••") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !uiState.passwordError.isNullOrBlank(),
                    shape = OutlinedTextFieldDefaults.shape,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        errorBorderColor = scheme.error,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )
                if (!uiState.passwordError.isNullOrBlank()) {
                    Text(
                        text = uiState.passwordError,
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Confirm Password Field
                Text(
                    text = "CONFIRM PASSWORD",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
                    placeholder = { Text("••••••••") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !uiState.confirmPasswordError.isNullOrBlank(),
                    shape = OutlinedTextFieldDefaults.shape,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline,
                        errorBorderColor = scheme.error,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                        focusedContainerColor = scheme.surface,
                        unfocusedContainerColor = scheme.surface,
                    )
                )
                if (!uiState.confirmPasswordError.isNullOrBlank()) {
                    Text(
                        text = uiState.confirmPasswordError,
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Target Exam
                Text(
                    text = "TARGET EXAM",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                AuthDropdownM3(
                    value = uiState.examType,
                    placeholder = "Select Target Exam",
                    options = examOptions,
                    onSelect = { onEvent(AuthEvent.ExamTypeChanged(it)) }
                )

                Spacer(Modifier.height(16.dp))

                // Prep Stage
                Text(
                    text = "PREPARATION STAGE",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                AuthDropdownM3(
                    value = uiState.preparationStage,
                    placeholder = "Select Preparation Stage",
                    options = prepStageOptions,
                    onSelect = { onEvent(AuthEvent.PreparationStageChanged(it)) }
                )

                Spacer(Modifier.height(16.dp))

                // Gender
                Text(
                    text = "GENDER",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                AuthDropdownM3(
                    value = uiState.gender,
                    placeholder = "Select Gender",
                    options = genderOptions,
                    onSelect = { onEvent(AuthEvent.GenderChanged(it)) }
                )
                if (!uiState.genderError.isNullOrBlank()) {
                    Text(
                        text = uiState.genderError,
                        color = scheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = { onEvent(AuthEvent.Signup) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = ButtonDefaults.shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
                    )
                ) {
                    Text(
                        text = if (uiState.isLoading) "Creating account..." else "Create Account",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Secondary Actions
            FlowRow(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account? ",
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Sign In",
                    color = scheme.secondary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable(onClick = onSwitchToLogin)
                )
            }

            Text(
                text = "KAVACH • WELLNESS FOR EVERY ASPIRANT",
                color = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthDropdownM3(
    value: String,
    placeholder: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
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
                shape = OutlinedTextFieldDefaults.shape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = scheme.onSurface,
                    unfocusedTextColor = scheme.onSurface,
                    cursorColor = scheme.primary,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outline,
                    focusedContainerColor = scheme.surface,
                    unfocusedContainerColor = scheme.surface,
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(scheme.surface)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface) },
                        onClick = {
                            onSelect(opt)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
