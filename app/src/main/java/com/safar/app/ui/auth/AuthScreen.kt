package com.safar.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.ui.theme.*

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = !MaterialTheme.colorScheme.background.isLight()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateToHome()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "OK"
            )
            viewModel.onEvent(AuthEvent.ClearError)
        }
    }

    Scaffold(
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
        containerColor = if (isDark) BrandMidnight else BgLight
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SafarLogoHeader(showSignup = uiState.isSignupMode, isDark = isDark)
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (isDark) BrandMidnightLight.copy(alpha = 0.85f)
                        else Color.White.copy(alpha = 0.92f)
                    )
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                if (isDark) BrandTeal.copy(alpha = 0.3f) else PrimaryLight.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                AnimatedContent(
                    targetState = uiState.isSignupMode,
                    transitionSpec = {
                        slideInHorizontally { if (targetState) it else -it } + fadeIn() togetherWith
                                slideOutHorizontally { if (targetState) -it else it } + fadeOut()
                    },
                    label = "auth_mode"
                ) { isSignup ->
                    if (isSignup) {
                        SignupForm(uiState, isDark, viewModel::onEvent) { viewModel.onEvent(AuthEvent.SwitchMode) }
                    } else {
                        LoginForm(uiState, isDark, viewModel::onEvent) { viewModel.onEvent(AuthEvent.SwitchMode) }
                    }
                }
            }
        }
    }

}

@Composable
private fun SafarLogoHeader(showSignup: Boolean, isDark: Boolean) {
    AsyncImage(
        model = "https://safar.parmarssc.in/assets/safar-logo.png-DJjwaiml.jpeg",
        contentDescription = "SAFAR Logo",
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(1.dp, BrandTeal.copy(alpha = 0.25f), CircleShape)
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = "SAFAR",
        color = if (isDark) BrandMint else BrandMidnight,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = stringResource(if (showSignup) R.string.create_account_tagline else R.string.app_tagline),
        color = if (isDark) BrandTeal.copy(alpha = 0.8f) else PrimaryLightDim.copy(alpha = 0.8f),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp
    )
}

@Composable
private fun LoginForm(
    uiState: AuthUiState,
    isDark: Boolean,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToSignup: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Sign in to continue", isDark)
        Spacer(Modifier.height(8.dp))

        AuthLabel(stringResource(R.string.label_email), isDark)
        AuthField(
            value = uiState.email, isDark = isDark,
            onChange = { onEvent(AuthEvent.EmailChanged(it)) },
            placeholder = stringResource(R.string.hint_email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_password), isDark)
        AuthField(
            value = uiState.password, isDark = isDark,
            onChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            placeholder = stringResource(R.string.hint_password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onEvent(AuthEvent.Login) }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isDark) BrandTeal.copy(alpha = 0.7f) else PrimaryLightDim.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(2.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.rememberMe,
                    onCheckedChange = { onEvent(AuthEvent.RememberMeToggled) },
                    colors = CheckboxDefaults.colors(
                        checkedColor   = if (isDark) PrimaryDark else PrimaryLight,
                        uncheckedColor = if (isDark) BrandTeal.copy(alpha = 0.4f) else PrimaryLightDim.copy(alpha = 0.4f)
                    )
                )
                Text(
                    stringResource(R.string.check_remember_me),
                    color = if (isDark) BrandMint.copy(alpha = 0.7f) else BrandMidnight.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            TextButton(onClick = { onEvent(AuthEvent.ForgotPassword) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(
                    stringResource(R.string.btn_forgot_password),
                    color = if (isDark) PrimaryDark else PrimaryLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        GradientButton(
            text = stringResource(if (uiState.isLoading) R.string.btn_signing_in else R.string.btn_sign_in),
            enabled = !uiState.isLoading, isDark = isDark,
            onClick = { onEvent(AuthEvent.Login) }
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = if (isDark) BrandTeal.copy(alpha = 0.1f) else PrimaryLightDim.copy(alpha = 0.1f))
        Spacer(Modifier.height(16.dp))

        SwitchModeRow(
            prompt = stringResource(R.string.no_account),
            link   = stringResource(R.string.sign_up_here),
            isDark = isDark, onClick = onSwitchToSignup
        )

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.footer),
            color = if (isDark) BrandTeal.copy(alpha = 0.3f) else PrimaryLightDim.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun SignupForm(
    uiState: AuthUiState,
    isDark: Boolean,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val examOptions      = stringArrayResource(R.array.exam_options).toList()
    val prepStageOptions = stringArrayResource(R.array.prep_stage_options).toList()
    val genderOptions    = stringArrayResource(R.array.gender_options).toList()

    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SectionLabel("Create your account", isDark)
        Spacer(Modifier.height(8.dp))

        AuthLabel(stringResource(R.string.label_full_name), isDark)
        AuthField(value = uiState.name, isDark = isDark, onChange = { onEvent(AuthEvent.NameChanged(it)) }, placeholder = stringResource(R.string.hint_full_name), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_email), isDark)
        AuthField(value = uiState.email, isDark = isDark, onChange = { onEvent(AuthEvent.EmailChanged(it)) }, placeholder = stringResource(R.string.hint_email), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_password), isDark)
        AuthField(
            value = uiState.password, isDark = isDark,
            onChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            placeholder = stringResource(R.string.hint_password_signup),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if (isDark) BrandTeal.copy(alpha = 0.7f) else PrimaryLightDim.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        )

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_confirm_password), isDark)
        AuthField(
            value = uiState.confirmPassword, isDark = isDark,
            onChange = { onEvent(AuthEvent.ConfirmPasswordChanged(it)) },
            placeholder = stringResource(R.string.hint_confirm_password),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if (isDark) BrandTeal.copy(alpha = 0.7f) else PrimaryLightDim.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        )

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_exam_type), isDark)
        AuthDropdown(value = uiState.examType, isDark = isDark, placeholder = stringResource(R.string.hint_exam_type), options = examOptions, onSelect = { onEvent(AuthEvent.ExamTypeChanged(it)) })

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_prep_stage), isDark)
        AuthDropdown(value = uiState.preparationStage, isDark = isDark, placeholder = stringResource(R.string.hint_prep_stage), options = prepStageOptions, onSelect = { onEvent(AuthEvent.PreparationStageChanged(it)) })

        Spacer(Modifier.height(4.dp))
        AuthLabel(stringResource(R.string.label_gender), isDark)
        AuthDropdown(value = uiState.gender, isDark = isDark, placeholder = stringResource(R.string.hint_gender), options = genderOptions, onSelect = { onEvent(AuthEvent.GenderChanged(it)) })

        Spacer(Modifier.height(12.dp))
        GradientButton(
            text = stringResource(if (uiState.isLoading) R.string.btn_creating_account else R.string.btn_create_account),
            enabled = !uiState.isLoading, isDark = isDark,
            onClick = { onEvent(AuthEvent.Signup) }
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = if (isDark) BrandTeal.copy(alpha = 0.1f) else PrimaryLightDim.copy(alpha = 0.1f))
        Spacer(Modifier.height(16.dp))

        SwitchModeRow(
            prompt = stringResource(R.string.have_account),
            link   = stringResource(R.string.sign_in_here),
            isDark = isDark, onClick = onSwitchToLogin
        )
    }
}

@Composable
private fun SectionLabel(text: String, isDark: Boolean) {
    Text(
        text,
        color = if (isDark) BrandMint else BrandMidnight,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp
    )
}

@Composable
private fun AuthLabel(text: String, isDark: Boolean) {
    Text(
        text,
        color = if (isDark) BrandTeal.copy(alpha = 0.9f) else PrimaryLightDim,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, bottom = 4.dp)
    )
}

@Composable
private fun AuthField(
    value: String, isDark: Boolean, onChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(placeholder, color = if (isDark) BrandTeal.copy(alpha = 0.35f) else PrimaryLightDim.copy(alpha = 0.4f), fontSize = 14.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(14.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = if (isDark) BrandPlumDark.copy(alpha = 0.6f) else BrandMint.copy(alpha = 0.5f),
            unfocusedContainerColor = if (isDark) BrandPlumDark.copy(alpha = 0.3f) else BrandMint.copy(alpha = 0.3f),
            focusedBorderColor      = if (isDark) PrimaryDark.copy(alpha = 0.8f)   else PrimaryLight,
            unfocusedBorderColor    = if (isDark) BrandTeal.copy(alpha = 0.12f)    else PrimaryLightDim.copy(alpha = 0.15f),
            focusedTextColor        = if (isDark) BrandMint   else BrandMidnight,
            unfocusedTextColor      = if (isDark) BrandMint   else BrandMidnight,
            cursorColor             = if (isDark) PrimaryDark else PrimaryLight
        )
    )
}

@Composable
private fun AuthDropdown(
    value: String, isDark: Boolean, placeholder: String,
    options: List<String>, onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value, onValueChange = {}, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = if (isDark) BrandTeal.copy(alpha = 0.35f) else PrimaryLightDim.copy(alpha = 0.4f), fontSize = 14.sp) },
            readOnly = true, enabled = false,
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    tint = if (isDark) BrandTeal.copy(alpha = 0.6f) else PrimaryLightDim.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(14.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor    = if (isDark) BrandPlumDark.copy(alpha = 0.3f) else BrandMint.copy(alpha = 0.3f),
                disabledBorderColor       = if (isDark) BrandTeal.copy(alpha = 0.12f)    else PrimaryLightDim.copy(alpha = 0.15f),
                disabledTextColor         = if (isDark) BrandMint   else BrandMidnight,
                disabledPlaceholderColor  = if (isDark) BrandTeal.copy(alpha = 0.35f) else PrimaryLightDim.copy(alpha = 0.4f),
                disabledTrailingIconColor = if (isDark) BrandTeal.copy(alpha = 0.6f)  else PrimaryLightDim.copy(alpha = 0.6f)
            )
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDark) BrandMidnightLight else Color.White)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, color = if (isDark) BrandMint else BrandMidnight, fontSize = 14.sp) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun GradientButton(text: String, isDark: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.horizontalGradient(
                    listOf(
                        if (isDark) PrimaryDark else PrimaryLight,
                        if (isDark) Color(0xFF1A9E8F) else Color(0xFF005C4E)
                    )
                ) else Brush.horizontalGradient(
                    listOf(BrandMidnightLight, BrandMidnight)
                )
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
        Text(prompt, color = if (isDark) BrandTeal.copy(alpha = 0.6f) else PrimaryLightDim.copy(alpha = 0.7f), fontSize = 13.sp)
        Text(link, color = if (isDark) PrimaryDark else PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onClick))
    }
}

private fun Color.isLight(): Boolean {
    val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return luminance > 0.5f
}
