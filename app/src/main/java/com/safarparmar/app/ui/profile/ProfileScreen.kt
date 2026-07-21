package com.safarparmar.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.theme.*
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

val LocalIsDarkTheme = staticCompositionLocalOf { false }

val BgCream: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF131316) else Color(0xFFFFF9F0)
val CardWhite: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
val TextDark: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
val TextMuted: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)
val BorderSoft: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF33333D) else Color(0xFFE2DDF0)
val ShadowSoft: Color @Composable get() = if (LocalIsDarkTheme.current) Color.Black.copy(alpha = 0.2f) else Color(0xFF1E1B4B).copy(alpha = 0.04f)
val PrimaryAccent: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF60A5FA) else Color(0xFF3B82F6)
val AccentShadow: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF60A5FA).copy(alpha = 0.3f) else Color(0xFF3B82F6).copy(alpha = 0.3f)
val AccentTint: Color @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF60A5FA).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.1f)

@Composable
private fun FlatCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
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
    ) {
        content()
    }
}

@Composable
fun FlatTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    errorText: String? = null,
    helperText: String? = null,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp, 
                    shape = cardShape, 
                    spotColor = ShadowSoft, 
                    ambientColor = ShadowSoft
                )
                .clip(cardShape)
                .background(if (enabled) CardWhite else Color(0xFFF8FAFC))
                .border(width = 1.5.dp, color = BorderSoft, shape = cardShape)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (errorText != null) {
            Text(text = errorText, color = Color.Red, fontSize = 12.sp)
        } else if (helperText != null) {
            Text(text = helperText, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatDropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
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
                    text = selectedOption,
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(CardWhite)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = opt,
                                color = TextDark,
                                fontSize = 16.sp
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
}

private val examOptions = listOf("UPSC", "SSC", "IBPS", "RRB", "NEET", "JEE", "12th Boards", "State PSC", "CAT", "GATE", "Other")
private val stageOptions = listOf("Beginner", "Intermediate", "Advanced", "Revision", "Mock Tests")
private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")



@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onHome: () -> Unit = {},
    onToggleDarkTheme: () -> Unit,
    onLibrary: () -> Unit = {},
    onProgress: () -> Unit = {},
    onPremium: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var showAvatarPreview by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.UploadAvatar(it)) }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.avatarUploadSuccess) {
        if (uiState.avatarUploadSuccess) {
            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
            viewModel.onEvent(ProfileEvent.ClearAvatarUploadSuccess)
        }
    }

    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        ),
        LocalIsDarkTheme provides isDarkTheme
    ) {
        Scaffold(
            containerColor = BgCream,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    TopAppBar(
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = "Profile",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                    ),
                                    color = TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Your identity and study",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.sp,
                                    ),
                                    color = TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.padding(start = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = PrimaryAccent,
                                )
                            }
                        },
                        actions = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { onToggleDarkTheme() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryAccent,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = BorderSoft,
                                ),
                                modifier = Modifier.padding(end = 16.dp),
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    )
                }
            },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ProfileHeaderCard(
                        uiState = uiState,
                        onAvatarClick = {
                            if (uiState.userAvatar.isNullOrBlank()) imagePicker.launch("image/*")
                            else showAvatarPreview = true
                        },
                        onEditAvatarClick = { imagePicker.launch("image/*") },
                    )

                    PersonalInfoSection(uiState = uiState, viewModel = viewModel)

                    ExamFocusSection(uiState = uiState, viewModel = viewModel)

                    AccountStatusSection(
                        isPremiumActive = premiumStatus.hasAnyPaidAccess,
                        onPremiumClick = onPremium,
                    )

                    if (uiState.error != null) {
                        Surface(
                            color = scheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = scheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    ActionsSection(
                        isSaving = uiState.isSaving,
                        onLogoutClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                        onSaveClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                    )

                    FooterSection()

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (uiState.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = scheme.error) },
                title = { Text("Confirm Logout", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                text = { Text("Are you sure you want to logout ? You will need to sign in again to access Safar Features . ") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.logout { onLogout() } },
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                    ) {
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) }) {
                        Text("Stay Here", fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
            )
        }

        if (showAvatarPreview && !uiState.userAvatar.isNullOrBlank()) {
            ProfilePhotoPreview(
                avatarUrl = uiState.userAvatar!!,
                userName = uiState.userName,
                onDismiss = { showAvatarPreview = false },
                onEdit = {
                    showAvatarPreview = false
                    imagePicker.launch("image/*")
                },
            )
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    uiState: ProfileUiState,
    onAvatarClick: () -> Unit,
    onEditAvatarClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    FlatCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatarRing = BorderSoft
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(AccentTint)
                        .border(4.dp, avatarRing, CircleShape)
                        .clickable(enabled = !uiState.isAvatarUploading) { onAvatarClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUrl = uiState.userAvatar?.takeIf { it.isNotBlank() }
                    if (avatarUrl != null) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        ) {
                            when (painter.state) {
                                is coil.compose.AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                                is coil.compose.AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                                )
                                else -> ProfileInitial(uiState.userName)
                            }
                        }
                    } else {
                        ProfileInitial(uiState.userName)
                    }
                    if (uiState.isAvatarUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryAccent)
                        .border(2.dp, avatarRing, CircleShape)
                        .clickable(enabled = !uiState.isAvatarUploading) { onEditAvatarClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = uiState.userName.ifEmpty { "User" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = uiState.userEmail,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileInitial(userName: String) {
    Text(
        text = userName.firstOrNull()?.uppercase() ?: "U",
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ProfilePhotoPreview(
    avatarUrl: String,
    userName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$userName profile photo, full screen",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change profile photo", tint = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close profile photo", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = scheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            FlatTextField(
                label = "FULL NAME",
                value = uiState.editName,
                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
                errorText = uiState.nameError,
            )
            FlatTextField(
                label = "EMAIL ADDRESS",
                value = uiState.userEmail,
                onValueChange = {},
                enabled = false,
                helperText = "Contact support to update your primary email address.",
            )
            FlatDropdownMenu(
                label = "GENDER",
                options = genderOptions,
                selectedOption = uiState.editGender.ifEmpty { "Select gender" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) },
            )
        }
    }
}

@Composable
private fun ExamFocusSection(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Exam Ekagra",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = scheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            FlatDropdownMenu(
                label = "TARGET EXAM",
                options = examOptions,
                selectedOption = uiState.editExamType.ifEmpty { "Select exam" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
            )
            FlatDropdownMenu(
                label = "PREPARATION STAGE",
                options = stageOptions,
                selectedOption = uiState.editStage.ifEmpty { "Select stage" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
            )
        }
    }
}

@Composable
private fun AccountStatusSection(
    isPremiumActive: Boolean,
    onPremiumClick: () -> Unit = {},
) {
    val statusTitle = if (isPremiumActive) "Safar Premium" else "Safar Plus"
    val statusText = if (isPremiumActive) "Premium access active" else "Normal Safar access"
    val buttonText = if (isPremiumActive) "Manage Plan" else "View Plans"
    FlatCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF15803D)
                    )
                }
            }
            
            val btnShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = btnShape, spotColor = ShadowSoft)
                    .clip(btnShape)
                    .background(AccentTint)
                    .border(1.dp, PrimaryAccent, btnShape)
                    .clickable(onClick = onPremiumClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(buttonText, color = PrimaryAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActionsSection(
    isSaving: Boolean,
    onLogoutClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    FlatCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Logout Button
            val outlineShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(outlineShape)
                    .background(CardWhite)
                    .border(1.5.dp, Color.Red.copy(alpha = 0.5f), outlineShape)
                    .clickable(onClick = onLogoutClick),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            // Save Button
            val buttonShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(elevation = 6.dp, shape = buttonShape, spotColor = AccentShadow)
                    .clip(buttonShape)
                    .background(if (!isSaving) PrimaryAccent else PrimaryAccent.copy(alpha = 0.5f))
                    .clickable(enabled = !isSaving, onClick = onSaveClick),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "For any technical or app related queries mail at\nonesafar@gmail.com • safarparmar0@gmail.com",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "© 2026 SAFAR • Version 1.0.4",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ProfileBottomNavigation(
    isDarkTheme: Boolean,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onProgress: () -> Unit,
) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, onHome),
        Triple("Library", Icons.AutoMirrored.Filled.MenuBook, onLibrary),
        Triple("Progress", Icons.Default.BarChart, onProgress),
        Triple("Profile", Icons.Default.Person, { }),
    )
    NavigationBar(
        containerColor = CardWhite,
        tonalElevation = 4.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.5.dp, BorderSoft, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        items.forEach { (label, icon, onClick) ->
            val selected = label == "Profile"
            NavigationBarItem(
                selected = selected,
                onClick = onClick,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) PrimaryAccent else TextMuted,
                    )
                },
                label = {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AccentTint,
                    selectedIconColor = PrimaryAccent,
                    selectedTextColor = PrimaryAccent,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                ),
            )
        }
    }
}
