package com.safarparmar.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.R
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.exceptions.GetCredentialException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.launch
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import com.safarparmar.app.ui.theme.LoraFontFamily

private val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val BgCream: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF131316) else Color(0xFFFFF9F0)
private val CardWhite: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
private val TextDark: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
private val TextMuted: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)
private val BorderSoft: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF33333D) else Color(0xFFE2DDF0)
private val ShadowSoft: Color @Composable get() = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.2f) else Color(0xFF1E1B4B).copy(alpha = 0.04f)
private val PrimaryAccent: Color @Composable get() = SafarSemanticColors.brandPurple(LocalIsDarkTheme.current)
private val AccentShadow: Color @Composable get() = PrimaryAccent.copy(alpha = 0.3f)
private val AccentTint: Color @Composable get() = PrimaryAccent.copy(alpha = if (LocalIsDarkTheme.current) 0.15f else 0.12f)

private data class AuthPalette(
    val heading: Color,
    val supportingText: Color,
    val inputText: Color,
    val inputIcon: Color,
    val link: Color,
    val primaryButton: Color,
    val accent: Color,
)

@Composable
private fun authPalette(): AuthPalette {
    return AuthPalette(
        heading = TextDark,
        supportingText = TextMuted,
        inputText = TextDark,
        inputIcon = TextMuted,
        link = PrimaryAccent,
        primaryButton = PrimaryAccent,
        accent = PrimaryAccent,
    )
}

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
    val snackbarHostState = remember { SnackbarHostState() }
    val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light
    val backgroundRes = if (isDark) R.drawable.auth_bg_dark else R.drawable.auth_bg

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateToHome()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, actionLabel = "OK")
            viewModel.onEvent(AuthEvent.ClearError)
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        CredentialManager.create(context)
    }

    fun startGoogleSignIn() {
        coroutineScope.launch {
            try {
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    viewModel.onEvent(AuthEvent.GoogleLogin(idToken))
                } else {
                    viewModel.onEvent(AuthEvent.Error("Invalid Google credential"))
                }
            } catch (e: GetCredentialException) {
                viewModel.onEvent(AuthEvent.Error(e.message ?: "Google sign-in failed"))
            } catch (e: Exception) {
                viewModel.onEvent(AuthEvent.Error(e.message ?: "Something went wrong"))
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary,
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgCream)
                    .padding(padding)
            ) {
                AnimatedContent(
                    targetState = when {
                        uiState.isForgotPasswordMode -> 2
                        uiState.isSignupMode -> 1
                        else -> 0
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "authMode",
                    modifier = Modifier.fillMaxSize()
                ) { mode ->
                    when (mode) {
                        2 -> {
                            ForgotPasswordContent(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onBackToLogin = { viewModel.onEvent(AuthEvent.BackToLoginClicked) }
                            )
                        }
                        1 -> {
                            SignupContent(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onSwitchToLogin = { viewModel.onEvent(AuthEvent.SwitchMode) },
                                onGoogleClick = { startGoogleSignIn() }
                            )
                        }
                        else -> {
                            LoginContent(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onSwitchToSignup = { viewModel.onEvent(AuthEvent.SwitchMode) },
                                onGoogleClick = { startGoogleSignIn() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HtmlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val palette = authPalette()
    val cardShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp, 
                shape = cardShape, 
                spotColor = ShadowSoft, 
                ambientColor = ShadowSoft
            )
            .clip(cardShape)
            .background(CardWhite)
            .border(width = 1.5.dp, color = BorderSoft, shape = cardShape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = leadingIcon, contentDescription = null, tint = palette.inputIcon, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = palette.inputText, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Medium,
                    fontFamily = PoppinsFontFamily
                ),
                cursorBrush = SolidColor(palette.accent),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(text = placeholder, color = palette.supportingText, fontSize = 15.sp)
            }
        }
        if (trailingIcon != null) {
            Box(modifier = Modifier.padding(start = 8.dp)) {
                trailingIcon()
            }
        }
    }
}

@Composable
fun HtmlPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    HtmlTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        leadingIcon = Icons.Default.Lock,
        modifier = modifier,
        isError = isError,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlDropdownField(
    value: String,
    placeholder: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = authPalette()
    var expanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                .shadow(
                    elevation = 4.dp, 
                    shape = cardShape, 
                    spotColor = ShadowSoft, 
                    ambientColor = ShadowSoft
                )
                .clip(cardShape)
                .background(CardWhite)
                .border(width = 1.5.dp, color = BorderSoft, shape = cardShape)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                color = if (value.isEmpty()) palette.supportingText else palette.inputText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HtmlPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(20.dp)

    val buttonColor = if (enabled) PrimaryAccent else PrimaryAccent.copy(alpha = 0.5f)
    val borderBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }

    val shadowElevation = if (isDarkTheme) 12.dp else 4.dp
    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .clip(shape)
            .background(buttonColor)
            .border(
                width = 0.5.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(20.dp)

    val bodyColor = if (isDarkTheme) Color(0xFF2C2C2E).copy(alpha = 0.65f) else Color(0xFFF9F9FB)
    val borderBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }

    val shadowElevation = if (isDarkTheme) 12.dp else 4.dp
    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .clip(shape)
            .background(bodyColor)
            .border(
                width = 0.5.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                color = TextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun LoginContent(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToSignup: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    val palette = authPalette()
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
        val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light
        AsyncImage(
            model = logoRes,
            contentDescription = "SAFAR Logo",
            modifier = Modifier
                .size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Welcome Text
        Text(
            text = "Welcome back",
            fontFamily = LoraFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = palette.heading,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Glad to see you again.",
            color = palette.supportingText,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Email Input
        HtmlTextField(
            value = uiState.email,
            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
            placeholder = "Email address",
            leadingIcon = Icons.Default.Email,
            isError = !uiState.emailError.isNullOrBlank(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Error handling
        if (!uiState.emailError.isNullOrBlank()) {
            Text(text = uiState.emailError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Password Input
        HtmlPasswordField(
            value = uiState.password,
            onValueChange = { onEvent(AuthEvent.PasswordChanged(it)) },
            placeholder = "Password",
            passwordVisible = passwordVisible,
            onToggleVisibility = { passwordVisible = !passwordVisible },
            isError = !uiState.passwordError.isNullOrBlank(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onEvent(AuthEvent.Login)
            }),
        )
        if (!uiState.passwordError.isNullOrBlank()) {
            Text(text = uiState.passwordError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp))
        }

        // Forgot password
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Forgot password?",
                color = palette.link,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onEvent(AuthEvent.ForgotPassword) }
            )
        }

        // Sign In Button
        HtmlPrimaryButton(
            text = if (uiState.isLoading) "Signing in..." else "Sign In",
            onClick = { onEvent(AuthEvent.Login) },
            enabled = !uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Footer Links
        Row(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                color = palette.supportingText,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier.clickable { onSwitchToSignup() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sign Up",
                    color = palette.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.padding(start = 4.dp).size(14.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SignupContent(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onSwitchToLogin: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    val palette = authPalette()
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val examOptions = listOf("SSC CGL", "SSC CHSL", "SSC MTS", "SSC CPO", "Other")
    val prepStageOptions = listOf("Just Started", "1-3 Months", "3-6 Months", "6+ Months", "Final Stage")
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
        val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light
        AsyncImage(
            model = logoRes,
            contentDescription = "SAFAR Logo",
            modifier = Modifier
                .size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Welcome Text
        Text(
            text = "Create account",
            fontFamily = LoraFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = palette.heading,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Start your study journey with SAFAR",
            color = palette.supportingText,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Form Fields
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Full Name
            Column {
                HtmlTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
                    placeholder = "John Doe",
                    leadingIcon = Icons.Default.Person,
                    isError = !uiState.nameError.isNullOrBlank(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                )
                if (!uiState.nameError.isNullOrBlank()) {
                    Text(text = uiState.nameError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }
            
            // Email
            Column {
                HtmlTextField(
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
                if (!uiState.emailError.isNullOrBlank()) {
                    Text(text = uiState.emailError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }

            // Password
            Column {
                HtmlPasswordField(
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
                if (!uiState.passwordError.isNullOrBlank()) {
                    Text(text = uiState.passwordError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }

            // Confirm Password
            Column {
                HtmlPasswordField(
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
                if (!uiState.confirmPasswordError.isNullOrBlank()) {
                    Text(text = uiState.confirmPasswordError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }

            // Dropdowns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HtmlDropdownField(
                    value = uiState.examType,
                    placeholder = "Select Target Exam",
                    options = examOptions,
                    onSelect = { onEvent(AuthEvent.ExamTypeChanged(it)) }
                )
                // "Other" alone told the backend nothing useful — this is where
                // the user's actual exam gets typed and stored instead.
                if (uiState.examType == "Other") {
                    HtmlTextField(
                        value = uiState.customExamType,
                        onValueChange = { onEvent(AuthEvent.CustomExamTypeChanged(it)) },
                        placeholder = "Enter your exact exam (e.g., RRB NTPC)",
                        leadingIcon = Icons.Default.School,
                        isError = !uiState.customExamTypeError.isNullOrBlank(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )
                    if (!uiState.customExamTypeError.isNullOrBlank()) {
                        Text(text = uiState.customExamTypeError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
            }

            HtmlDropdownField(
                value = uiState.preparationStage,
                placeholder = "Select Preparation Stage",
                options = prepStageOptions,
                onSelect = { onEvent(AuthEvent.PreparationStageChanged(it)) }
            )

            Column {
                HtmlDropdownField(
                    value = uiState.gender,
                    placeholder = "Select Gender",
                    options = genderOptions,
                    onSelect = { onEvent(AuthEvent.GenderChanged(it)) }
                )
                if (!uiState.genderError.isNullOrBlank()) {
                    Text(text = uiState.genderError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Up Button
        HtmlPrimaryButton(
            text = if (uiState.isLoading) "Creating account..." else "Create Account",
            onClick = { onEvent(AuthEvent.Signup) },
            enabled = !uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Footer Links
        Row(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                color = palette.supportingText,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier.clickable { onSwitchToLogin() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sign In",
                    color = palette.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.padding(start = 4.dp).size(14.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ForgotPasswordContent(
    uiState: AuthUiState,
    onEvent: (AuthEvent) -> Unit,
    onBackToLogin: () -> Unit,
) {
    val palette = authPalette()
    val focusManager = LocalFocusManager.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        val isDark = !MaterialTheme.colorScheme.background.isLightBackground()
        val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light
        AsyncImage(
            model = logoRes,
            contentDescription = "SAFAR Logo",
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Reset password",
            fontFamily = LoraFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = palette.heading,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = if (uiState.forgotPasswordStep == ForgotPasswordStep.EMAIL) {
                "Enter your email to request a reset link."
            } else {
                "Create a new password below."
            },
            color = palette.supportingText,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.forgotPasswordStep == ForgotPasswordStep.EMAIL) {
            // Email Input
            HtmlTextField(
                value = uiState.email,
                onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                placeholder = "Email address",
                leadingIcon = Icons.Default.Email,
                isError = !uiState.emailError.isNullOrBlank(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onEvent(AuthEvent.SubmitForgotPasswordRequest)
                }),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (!uiState.emailError.isNullOrBlank()) {
                Text(
                    text = uiState.emailError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Email Button
            HtmlPrimaryButton(
                text = if (uiState.isLoading) "Sending request..." else "Send Request",
                onClick = { onEvent(AuthEvent.SubmitForgotPasswordRequest) },
                enabled = !uiState.isLoading,
            )
        } else {
            // Reset fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // New Password Input
                Column {
                    HtmlPasswordField(
                        value = uiState.resetNewPassword,
                        onValueChange = { onEvent(AuthEvent.ResetNewPasswordChanged(it)) },
                        placeholder = "New Password (min 8 chars)",
                        passwordVisible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        isError = !uiState.resetNewPasswordError.isNullOrBlank(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )
                    if (!uiState.resetNewPasswordError.isNullOrBlank()) {
                        Text(
                            text = uiState.resetNewPasswordError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                // Confirm Password Input
                Column {
                    HtmlPasswordField(
                        value = uiState.resetConfirmPassword,
                        onValueChange = { onEvent(AuthEvent.ResetConfirmPasswordChanged(it)) },
                        placeholder = "Confirm Password",
                        passwordVisible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        isError = !uiState.resetConfirmPasswordError.isNullOrBlank(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            onEvent(AuthEvent.SubmitResetPasswordConfirm)
                        }),
                    )
                    if (!uiState.resetConfirmPasswordError.isNullOrBlank()) {
                        Text(
                            text = uiState.resetConfirmPasswordError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Password Button
            HtmlPrimaryButton(
                text = if (uiState.isLoading) "Saving password..." else "Save Password",
                onClick = { onEvent(AuthEvent.SubmitResetPasswordConfirm) },
                enabled = !uiState.isLoading,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footer back to login
        Row(
            modifier = Modifier.clickable { onBackToLogin() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.padding(end = 4.dp).size(14.dp)
            )
            Text(
                text = "Back to Sign In",
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
