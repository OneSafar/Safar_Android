package com.safarparmar.app.ui.auth

import com.safarparmar.app.ui.onboarding.editorialCornerArtwork
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.safarparmar.app.ui.components.popups.SafarExposedDropdownMenu


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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.safarparmar.app.ui.theme.PoppinsFontFamily as LoraFontFamily
import com.composables.ui.components.Text
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.TabList
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.DropdownMenuItem as UiDropdownMenuItem
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlinx.coroutines.flow.distinctUntilChanged
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent

private val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val BgCream: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF211921) else Color(0xFFFFF5F0)
private val CardWhite: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF342735) else Color(0xFFFFFDFC)
private val TextDark: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFFF0F6) else Color(0xFF39132E)
private val TextMuted: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFCFBECB) else Color(0xFF75616F)
private val BorderSoft: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF654D66) else Color(0xFFEADFE6)
private val ShadowSoft: Color @Composable get() = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.2f) else Color(0xFF1E1B4B).copy(alpha = 0.04f)
private val PrimaryAccent: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF0BEDC) else Color(0xFF491438)
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
    val rootView = LocalView.current.rootView
    val density = LocalDensity.current
    val navigationBottom = WindowInsets.navigationBars.getBottom(density)
    var artworkBottomCorrection by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val localizedResources by rememberUpdatedState(LocalResources.current)
    val logoRes = if (isDark) R.drawable.ic_safar_logo_brand_dark else R.drawable.ic_safar_logo_brand_light
    val backgroundRes = if (isDark) R.drawable.auth_bg_dark else R.drawable.auth_bg

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateToHome()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, actionLabel = localizedResources.getString(R.string.common_ok))
            viewModel.onEvent(AuthEvent.ClearError)
        }
    }

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
        AuthWarmTheme {
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
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
                    .onGloballyPositioned { coordinates ->
                        val localBottomInWindow = coordinates.positionInWindow().y + coordinates.size.height
                        artworkBottomCorrection = (rootView.height - navigationBottom - localBottomInWindow).coerceAtLeast(0f)
                    }
                    .editorialCornerArtwork(artworkBottomCorrection),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = when {
                        uiState.isForgotPasswordMode -> 2
                        uiState.isSignupMode -> 1
                        else -> 0
                    },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentAlignment = Alignment.Center,
                    label = "authMode",
                    modifier = Modifier.fillMaxSize()
                ) { mode ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
    } // AuthWarmTheme
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
    if (keyboardOptions.keyboardType != KeyboardType.Password) {
        AuthLibraryInput(value, onValueChange, placeholder, leadingIcon, modifier, isError, keyboardOptions, keyboardActions)
        return
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(placeholder, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 0.dp,
                shape = cardShape, 
                spotColor = ShadowSoft, 
                ambientColor = ShadowSoft
            )
            .clip(cardShape)
            .background(CardWhite)
            .border(width = 1.dp, color = if (isError) MaterialTheme.colorScheme.error else BorderSoft, shape = cardShape)
            .heightIn(min = 54.dp).padding(horizontal = 14.dp, vertical = 4.dp),
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
    } // field label
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
            IconButton(onClick = onToggleVisibility, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
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
fun HtmlDropdownField(value: String, placeholder: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(placeholder, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        com.safarparmar.app.ui.components.popups.SafarComposablesDropdownMenu(
            expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.fillMaxWidth(),
            panel = {
                DropdownMenuPanel {
                    options.forEach { option ->
                        UiDropdownMenuItem(onClick = { onSelect(option); expanded = false }) {
                            Text(option, fontSize = 14.sp)
                        }
                    }
                }
            },
            anchor = {
                UiButton(onClick = { expanded = !expanded }, style = ButtonStyle.Outlined,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), contentColor = TextDark) {
                    Text(value.ifBlank { placeholder }, modifier = Modifier.weight(1f), fontSize = 14.sp,
                        color = if (value.isBlank()) TextMuted else TextDark)
                    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp), tint = TextMuted)
                }
            })
    }
}

@Composable
fun HtmlPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    UiButton(onClick = onClick, enabled = enabled && !isLoading, modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = CircleShape, style = ButtonStyle.Primary) {
        if (isLoading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = if (LocalIsDarkTheme.current) BgCream else Color.White); Spacer(Modifier.width(10.dp)) }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    UiButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        style = ButtonStyle.Outlined, shape = RoundedCornerShape(16.dp), contentColor = TextDark) {
        Image(painterResource(R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp)); Text(stringResource(R.string.auth_continue_google), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
    com.safarparmar.app.ui.onboarding.EditorialAuthPage(
        title = stringResource(R.string.auth_welcome_back), showLogo = true,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Email Input
        HtmlTextField(
            value = uiState.email,
            onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
            placeholder = stringResource(R.string.auth_label_email),
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
            placeholder = stringResource(R.string.auth_label_password),
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
                text = stringResource(R.string.auth_forgot_password),
                color = palette.link,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onEvent(AuthEvent.ForgotPassword) }
            )
        }

        // Sign In Button
        HtmlPrimaryButton(
            text = stringResource(if (uiState.isLoading) R.string.auth_signing_in else R.string.auth_sign_in),
            onClick = { onEvent(AuthEvent.Login) },
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // A real layout gap separates the prompt from the signup action; no reliance on trailing spaces.
        Row(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.auth_no_account).trim(), modifier = Modifier.weight(1f),
                color = palette.supportingText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            UiButton(onClick = onSwitchToSignup, enabled = !uiState.isLoading,
                style = ButtonStyle.Ghost, contentColor = palette.accent,
                contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.auth_sign_up_here), color = palette.accent,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = palette.accent, modifier = Modifier.padding(start = 4.dp).size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // AuthFormCard
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

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth = if (screenWidth > 520.dp) 440.dp else screenWidth

    com.safarparmar.app.ui.onboarding.EditorialAuthPage(
        title = stringResource(R.string.auth_create_account), showLogo = true,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Form Fields
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Full Name
            Column {
                HtmlTextField(
                    value = uiState.name,
                    onValueChange = { onEvent(AuthEvent.NameChanged(it)) },
                    placeholder = stringResource(R.string.auth_hint_full_name),
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
                    placeholder = stringResource(R.string.auth_hint_email),
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
                    placeholder = stringResource(R.string.auth_hint_password_signup),
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
                    placeholder = stringResource(R.string.auth_hint_confirm_password),
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
                    placeholder = stringResource(R.string.auth_select_target_exam),
                    options = examOptions,
                    onSelect = { onEvent(AuthEvent.ExamTypeChanged(it)) }
                )
                // "Other" alone told the backend nothing useful — this is where
                // the user's actual exam gets typed and stored instead.
                if (uiState.examType == "Other") {
                    HtmlTextField(
                        value = uiState.customExamType,
                        onValueChange = { onEvent(AuthEvent.CustomExamTypeChanged(it)) },
                        placeholder = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_exam_exact_hint),
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
                placeholder = stringResource(R.string.auth_select_preparation_stage),
                options = prepStageOptions,
                onSelect = { onEvent(AuthEvent.PreparationStageChanged(it)) }
            )

            Column {
                HtmlDropdownField(
                    value = uiState.gender,
                    placeholder = stringResource(R.string.auth_hint_gender),
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
            text = stringResource(if (uiState.isLoading) R.string.auth_creating_account else R.string.auth_create_account_btn),
            onClick = { onEvent(AuthEvent.Signup) },
            enabled = !uiState.isLoading,
            isLoading = uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.auth_have_account).trim(), modifier = Modifier.weight(1f),
                color = palette.supportingText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            UiButton(onClick = onSwitchToLogin, enabled = !uiState.isLoading,
                style = ButtonStyle.Ghost, contentColor = palette.accent,
                contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.auth_sign_in), color = palette.accent,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = palette.accent, modifier = Modifier.padding(start = 4.dp).size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // AuthFormCard
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

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth = if (screenWidth > 520.dp) 440.dp else screenWidth

    val pageTitle = when (uiState.forgotPasswordStep) {
        ForgotPasswordStep.EMAIL -> "Reset password"
        ForgotPasswordStep.EMAIL_SENT -> "Check your email"
        ForgotPasswordStep.RESET -> "New password"
    }
    com.safarparmar.app.ui.onboarding.EditorialAuthPage(title = pageTitle,
        onBack = onBackToLogin.takeUnless { uiState.forgotPasswordStep == ForgotPasswordStep.EMAIL_SENT },
        showLogo = false,
    ) {
        if (uiState.forgotPasswordStep != ForgotPasswordStep.RESET) {
            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFB96E61),
                modifier = Modifier.padding(vertical = 16.dp).size(64.dp)
                    .background(Color(0xFFFFCBBE), RoundedCornerShape(16.dp)).padding(12.dp))
            Text(pageTitle, color = palette.heading, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Text(
            text = when (uiState.forgotPasswordStep) {
                ForgotPasswordStep.EMAIL -> "Enter your email to request a reset link."
                ForgotPasswordStep.EMAIL_SENT -> "We've sent a password reset link to your email. Please check your inbox and click the link to set a new password."
                ForgotPasswordStep.RESET -> "Create a new password below."
            },
            color = palette.supportingText,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        when (uiState.forgotPasswordStep) {
            ForgotPasswordStep.EMAIL -> {
                // Email Input
                HtmlTextField(
                    value = uiState.email,
                    onValueChange = { onEvent(AuthEvent.EmailChanged(it)) },
                    placeholder = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_email_address),
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
                    text = if (uiState.isLoading) "Sending link..." else "Send Reset Link",
                    onClick = { onEvent(AuthEvent.SubmitForgotPasswordRequest) },
                    enabled = !uiState.isLoading,
                    isLoading = uiState.isLoading,
                )
            }
            ForgotPasswordStep.EMAIL_SENT -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HtmlPrimaryButton(
                        text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_email_retry),
                        onClick = { onEvent(AuthEvent.ForgotPassword) },
                        enabled = true,
                    )
                    TextButton(
                        onClick = onBackToLogin,
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_back_sign_in),
                            color = palette.link,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            ForgotPasswordStep.RESET -> {
                // Reset fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // New Password Input
                    Column {
                        HtmlPasswordField(
                            value = uiState.resetNewPassword,
                            onValueChange = { onEvent(AuthEvent.ResetNewPasswordChanged(it)) },
                            placeholder = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_new_password_hint),
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
                            placeholder = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_confirm_password),
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
                    isLoading = uiState.isLoading,
                )
            }
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
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.auth_back_sign_in),
                color = palette.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AuthWarmTheme(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme.copy(background = BgCream, surface = CardWhite,
        surfaceContainerHigh = CardWhite, onBackground = TextDark, onSurface = TextDark,
        onSurfaceVariant = TextMuted, primary = PrimaryAccent,
        outlineVariant = BorderSoft, secondaryContainer = AccentTint, onSecondaryContainer = TextDark)
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography.copy(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = PoppinsFontFamily))) {
        CompositionLocalProvider(LocalSafarComposablesAccent provides PrimaryAccent) {
            SafarComposablesTheme { content() }
        }
    }
}

@Composable
private fun AuthFormCard(signup: Boolean, enabled: Boolean, onSwitch: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(28.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        com.composables.ui.components.Tabs(selectedTab = signup, onSelectedTabChange = { if (it != signup && enabled) onSwitch() },
            orderedTabs = listOf(false, true)) {
            TabList(modifier = Modifier.fillMaxWidth().clip(CircleShape).background(AccentTint), dividerColor = Color.Transparent) {
                Row(Modifier.fillMaxWidth()) {
                    listOf(false, true).forEach { tab ->
                        com.composables.ui.components.Tab(key = tab, enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).background(if (signup == tab) PrimaryAccent else Color.Transparent, CircleShape),
                            text = { Text(stringResource(if (tab) R.string.auth_warm_sign_up else R.string.auth_sign_in),
                                color = if (signup == tab) (if (LocalIsDarkTheme.current) BgCream else Color.White) else TextDark,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold) })
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
private fun AuthOrDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        com.composables.ui.components.HorizontalSeparator(modifier = Modifier.weight(1f), color = BorderSoft)
        Text(stringResource(R.string.auth_warm_or), fontSize = 12.sp, color = TextMuted)
        com.composables.ui.components.HorizontalSeparator(modifier = Modifier.weight(1f), color = BorderSoft)
    }
}

@Composable
private fun AuthLibraryInput(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector,
    modifier: Modifier, error: Boolean, keyboardOptions: KeyboardOptions, keyboardActions: KeyboardActions) {
    val state = rememberTextFieldState(value)
    val callback by rememberUpdatedState(onValueChange)
    val current by rememberUpdatedState(value)
    LaunchedEffect(value) { if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value) }
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.distinctUntilChanged().collect { if (it != current) callback(it) } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        com.composables.ui.components.TextField(state = state, accessibilityLabel = label,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp),
            backgroundColor = CardWhite, contentColor = TextDark, borderColor = if (error) MaterialTheme.colorScheme.error else BorderSoft,
            fontFamily = PoppinsFontFamily, fontSize = 14.sp, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            leading = { Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
            keyboardOptions = keyboardOptions, onKeyboardAction = { performDefault ->
                val action = when (keyboardOptions.imeAction) {
                    ImeAction.Next -> keyboardActions.onNext
                    ImeAction.Done -> keyboardActions.onDone
                    ImeAction.Go -> keyboardActions.onGo
                    ImeAction.Search -> keyboardActions.onSearch
                    ImeAction.Send -> keyboardActions.onSend
                    ImeAction.Previous -> keyboardActions.onPrevious
                    else -> null
                }
                if (action == null) performDefault() else action.invoke(object : androidx.compose.foundation.text.KeyboardActionScope {
                    override fun defaultKeyboardAction(imeAction: ImeAction) { performDefault() }
                })
            })
    }
}
