package com.safar.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.ui.theme.*
import com.safar.app.ui.theme.shimmer
import com.safar.app.util.bounceClick

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    containerColor = BrandPlumDark,
                    contentColor = BrandMint,
                    actionColor = BrandTeal,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        containerColor = if (isDark) BgDark else BgLight
    ) { padding ->
        if (uiState.isSignupMode) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 56.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SafarLogoHeader(showSignup = true, isDark = isDark)
                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(BrandMidnightLight.copy(alpha = 0.85f))
                        .border(
                            width = 0.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(BrandTeal.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    SignupForm(uiState, isDark, viewModel::onEvent) { viewModel.onEvent(AuthEvent.SwitchMode) }
                }
            }
        } else {
            LoginScreenFromDesign(
                padding = padding,
                isDark = isDark,
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onSwitchToSignup = { viewModel.onEvent(AuthEvent.SwitchMode) },
            )
        }
    }
}

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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Sign in to continue your journey",
                color = scheme.onSurfaceVariant,
                fontSize = 16.sp,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Email Field
                Text(
                    text = "EMAIL",
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    placeholder = { Text("you@gmail.com", color = scheme.outlineVariant) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = scheme.background,
                        unfocusedContainerColor = scheme.background,
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outlineVariant,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Password Field
                Text(
                    text = "PASSWORD",
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
                    placeholder = { Text("••••••••", color = scheme.outlineVariant) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = scheme.secondary,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onEvent(AuthEvent.Login)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = scheme.background,
                        unfocusedContainerColor = scheme.background,
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outlineVariant,
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface,
                        cursorColor = scheme.primary,
                    )
                )

                // Forgot Password
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onEvent(AuthEvent.ForgotPassword) }, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            text = "Forgot password?",
                            color = scheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = { onEvent(AuthEvent.Login) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick(),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                        disabledContainerColor = scheme.primary.copy(alpha = 0.5f),
                        disabledContentColor = scheme.onPrimary.copy(alpha = 0.5f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = if (uiState.isLoading) "Signing in..." else "Sign In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Secondary Actions
            Row(
                modifier = Modifier.padding(top = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Sign Up",
                    color = scheme.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onSwitchToSignup)
                )
            }

            Text(
                text = "KAVACH • WELLNESS FOR EVERY ASPIRANT",
                color = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}


@Composable
private fun LoginLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    )
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    accent: Color,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    minHeight: Dp = 50.dp,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        placeholder = { Text(text = placeholder, color = textColor.copy(alpha = 0.4f), fontSize = 15.sp) },
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            focusedBorderColor = accent,
            unfocusedBorderColor = borderColor,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = accent,
        ),
    )
}

@Composable
private fun SafarLogoHeader(showSignup: Boolean, isDark: Boolean) {
    AsyncImage(
        model = "https://safar.parmarssc.in/safar-logo.png.webp",
        contentDescription = "SAFAR Logo",
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.app_name),
        color = if (isDark) BrandMint else BrandMidnight,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(if (showSignup) R.string.auth_create_account_tagline else R.string.auth_tagline),
        color = if (isDark) BrandTeal.copy(alpha = 0.8f) else PrimaryLightDim.copy(alpha = 0.8f),
        fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp, letterSpacing = 0.2.sp
    )
}

@Composable
private fun LoginForm(
    uiState: AuthUiState, isDark: Boolean,
    onEvent: (AuthEvent) -> Unit, onSwitchToSignup: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel(stringResource(R.string.auth_sign_in_to_continue), isDark)
        Spacer(Modifier.height(8.dp))

        AuthLabel(stringResource(R.string.auth_label_email), isDark)
        AuthField(
            value = uiState.email, isDark = isDark,
            onChange = { onEvent(AuthEvent.EmailChanged(it)) },
            placeholder = stringResource(R.string.auth_hint_email),
            error = uiState.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_password), isDark)
        AuthField(
            value = uiState.password, isDark = isDark,
            onChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            placeholder = stringResource(R.string.auth_hint_password),
            error = uiState.passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onEvent(AuthEvent.Login) }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isDark) BrandTeal.copy(0.7f) else PrimaryLightDim.copy(0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onEvent(AuthEvent.ForgotPassword) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(R.string.auth_forgot_password), color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(8.dp))
        GradientButton(
            text = stringResource(if (uiState.isLoading) R.string.auth_signing_in else R.string.auth_sign_in),
            enabled = !uiState.isLoading, isDark = isDark,
            onClick = { onEvent(AuthEvent.Login) }
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = if (isDark) BrandTeal.copy(0.1f) else PrimaryLightDim.copy(0.1f))
        Spacer(Modifier.height(16.dp))

        SwitchModeRow(
            prompt = stringResource(R.string.auth_no_account),
            link   = stringResource(R.string.auth_sign_up_here),
            isDark = isDark, onClick = onSwitchToSignup
        )

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.auth_footer),
            color = if (isDark) BrandTeal.copy(0.3f) else PrimaryLightDim.copy(0.4f),
            fontSize = 11.sp, modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun SignupForm(
    uiState: AuthUiState, isDark: Boolean,
    onEvent: (AuthEvent) -> Unit, onSwitchToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val examOptions      = listOf("SSC CGL", "SSC CHSL", "SSC MTS", "SSC CPO", "Other")
    val prepStageOptions = listOf("Just Started", "1-3 Months", "3-6 Months", "6+ Months", "Final Stage")
    val genderOptions    = listOf("Male", "Female", "Other", "Prefer not to say")

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel(stringResource(R.string.auth_create_account), isDark)
        Spacer(Modifier.height(8.dp))

        AuthLabel(stringResource(R.string.auth_label_full_name), isDark)
        AuthField(value = uiState.name, isDark = isDark, onChange = { onEvent(AuthEvent.NameChanged(it)) }, placeholder = stringResource(R.string.auth_hint_full_name), error = uiState.nameError, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_email), isDark)
        AuthField(value = uiState.email, isDark = isDark, onChange = { onEvent(AuthEvent.EmailChanged(it)) }, placeholder = stringResource(R.string.auth_hint_email), error = uiState.emailError, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_password), isDark)
        AuthField(
            value = uiState.password, isDark = isDark,
            onChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            placeholder = stringResource(R.string.auth_hint_password_signup),
            error = uiState.passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if (isDark) BrandTeal.copy(0.7f) else PrimaryLightDim.copy(0.7f), modifier = Modifier.size(20.dp)) } }
        )

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_confirm_password), isDark)
        AuthField(
            value = uiState.confirmPassword, isDark = isDark,
            onChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
            placeholder = stringResource(R.string.auth_hint_confirm_password),
            error = uiState.confirmPasswordError,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            trailingIcon = { IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if (isDark) BrandTeal.copy(0.7f) else PrimaryLightDim.copy(0.7f), modifier = Modifier.size(20.dp)) } }
        )

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_exam_type), isDark)
        AuthDropdown(value = uiState.examType, isDark = isDark, placeholder = stringResource(R.string.auth_hint_exam_type), options = examOptions, onSelect = { onEvent(AuthEvent.ExamTypeChanged(it)) })

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_prep_stage), isDark)
        AuthDropdown(value = uiState.preparationStage, isDark = isDark, placeholder = stringResource(R.string.auth_hint_prep_stage), options = prepStageOptions, onSelect = { onEvent(AuthEvent.PreparationStageChanged(it)) })

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.auth_label_gender), isDark)
        AuthDropdown(value = uiState.gender, isDark = isDark, placeholder = stringResource(R.string.auth_hint_gender), options = genderOptions, onSelect = { onEvent(AuthEvent.GenderChanged(it)) })
        if (uiState.genderError != null) {
            Text(
                uiState.genderError,
                color = SafarError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 2.dp, top = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = stringResource(if (uiState.isLoading) R.string.auth_creating_account else R.string.auth_create_account),
            enabled = !uiState.isLoading, isDark = isDark,
            onClick = { onEvent(AuthEvent.Signup) }
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = if (isDark) BrandTeal.copy(0.1f) else PrimaryLightDim.copy(0.1f))
        Spacer(Modifier.height(16.dp))

        SwitchModeRow(
            prompt = stringResource(R.string.auth_have_account),
            link   = stringResource(R.string.auth_sign_in_here),
            isDark = isDark, onClick = onSwitchToLogin
        )
    }
}

@Composable
private fun SectionLabel(text: String, isDark: Boolean) {
    Text(text, color = if (isDark) BrandMint else BrandMidnight, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
}

@Composable
private fun AuthLabel(text: String, isDark: Boolean) {
    Text(text, color = if (isDark) BrandTeal.copy(0.9f) else PrimaryLightDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, modifier = Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 4.dp))
}

@Composable
private fun AuthField(
    value: String, isDark: Boolean, onChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val hasError = !error.isNullOrBlank()
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = onChange,
            placeholder = { Text(placeholder, color = if (isDark) BrandTeal.copy(0.35f) else PrimaryLightDim.copy(0.4f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            isError = hasError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = if (isDark) BrandPlumDark.copy(0.6f) else BrandMint.copy(0.5f),
                unfocusedContainerColor = if (isDark) BrandPlumDark.copy(0.3f) else BrandMint.copy(0.3f),
                focusedBorderColor      = if (isDark) PrimaryDark.copy(0.8f)   else PrimaryLight,
                unfocusedBorderColor    = if (isDark) BrandTeal.copy(0.12f)    else PrimaryLightDim.copy(0.15f),
                errorBorderColor        = SafarError,
                focusedTextColor        = if (isDark) BrandMint   else BrandMidnight,
                unfocusedTextColor      = if (isDark) BrandMint   else BrandMidnight,
                cursorColor             = if (isDark) PrimaryDark else PrimaryLight
            )
        )
        Text(
            text = error.orEmpty(),
            color = SafarError,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(start = 2.dp, top = 4.dp)
                .heightIn(min = 16.dp),
        )
    }
}

@Composable
private fun AuthDropdown(
    value: String, isDark: Boolean, placeholder: String, options: List<String>, onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = {}, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = if (isDark) BrandTeal.copy(0.35f) else PrimaryLightDim.copy(0.4f), fontSize = 14.sp) },
            readOnly = true, enabled = false,
            trailingIcon = { Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = if (isDark) BrandTeal.copy(0.6f) else PrimaryLightDim.copy(0.6f), modifier = Modifier.size(20.dp)) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor    = if (isDark) BrandPlumDark.copy(0.3f) else BrandMint.copy(0.3f),
                disabledBorderColor       = if (isDark) BrandTeal.copy(0.12f)    else PrimaryLightDim.copy(0.15f),
                disabledTextColor         = if (isDark) BrandMint else BrandMidnight,
                disabledPlaceholderColor  = if (isDark) BrandTeal.copy(0.35f) else PrimaryLightDim.copy(0.4f),
                disabledTrailingIconColor = if (isDark) BrandTeal.copy(0.6f) else PrimaryLightDim.copy(0.6f)
            )
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(if (isDark) BrandMidnightLight else Color.White)) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt, color = if (isDark) BrandMint else BrandMidnight, fontSize = 14.sp) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
private fun GradientButton(text: String, isDark: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(28.dp))
            .shimmer()
            .background(
                if (enabled) Brush.horizontalGradient(listOf(if (isDark) PrimaryDark else PrimaryLight, GradientMidDark))
                else Brush.horizontalGradient(listOf(BtnDisabled, BrandMidnight))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SwitchModeRow(prompt: String, link: String, isDark: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(prompt, color = if (isDark) BrandTeal.copy(0.6f) else PrimaryLightDim.copy(0.7f), fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(link, color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onClick))
    }
}

