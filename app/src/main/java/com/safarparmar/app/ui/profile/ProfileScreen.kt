package com.safarparmar.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors

private val examOptions = listOf("UPSC", "SSC", "IBPS", "RRB", "NEET", "JEE", "12th Boards", "State PSC", "CAT", "GATE", "Other")
private val stageOptions = listOf("Beginner", "Intermediate", "Advanced", "Revision", "Mock Tests")
private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

@Composable
private fun ProfileSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = PlannerFlatColors.TextDark,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentRoute: String = Routes.PROFILE,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLogout: () -> Unit = {},
    onHome: () -> Unit = {},
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
            Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
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

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
        SafarDrawerScaffold(
            title = "Profile",
            subtitle = null,
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            containerColor = SafarSemanticColors.plannerBackground(),
            topBarActions = {
                IconButton(
                    onClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = scheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Profile",
                            tint = scheme.primary
                        )
                    }
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Main Screen Title Banner
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "My Profile",
                            fontFamily = LoraFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = PlannerFlatColors.TextDark,
                        )
                        Text(
                            text = "Update your personal and exam details",
                            fontSize = 13.sp,
                            color = PlannerFlatColors.TextMuted,
                        )
                    }

                    // Hero Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                    ) {
                        ProfileHeaderSection(
                            uiState = uiState,
                            onAvatarClick = {
                                if (uiState.userAvatar.isNullOrBlank()) imagePicker.launch("image/*")
                                else showAvatarPreview = true
                            },
                            onEditAvatarClick = { imagePicker.launch("image/*") },
                        )
                    }

                    // Section Card 1: Personal Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ProfileSectionHeader(icon = Icons.Default.Person, title = "Personal Information")
                            PlanHairline(alpha = 0.5f)
                            PersonalInfoFields(uiState = uiState, viewModel = viewModel)
                        }
                    }

                    // Section Card 2: Academic & Exam Focus
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            ProfileSectionHeader(icon = Icons.Default.School, title = "Academic & Exam Focus")
                            PlanHairline(alpha = 0.5f)
                            ExamFocusFields(uiState = uiState, viewModel = viewModel)
                        }
                    }

                    // Section Card 3: Account & Subscription
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            ProfileSectionHeader(icon = Icons.Default.WorkspacePremium, title = "Account & Subscription")
                            PlanHairline(alpha = 0.5f)
                            AccountStatusRow(
                                isPremiumActive = premiumStatus.hasAnyPaidAccess,
                                onPremiumClick = onPremium,
                            )
                        }
                    }

                    if (uiState.error != null) {
                        Surface(
                            color = scheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = scheme.onErrorContainer,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Action Buttons Card
                    ActionsRow(
                        isSaving = uiState.isSaving,
                        onLogoutClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                        onSaveClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                    )

                    FooterSection()

                    Spacer(Modifier.height(16.dp))
                }
            }

            if (uiState.showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
                    containerColor = SafarSemanticColors.plannerBackground(),
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = scheme.error) },
                    title = { Text("Confirm Logout", fontFamily = LoraFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = PlannerFlatColors.TextDark) },
                    text = { Text("Are you sure you want to logout? You will need to sign in again to access SAFAR features.", fontSize = 14.sp, color = PlannerFlatColors.TextMuted) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.logout { onLogout() } },
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Logout", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) }) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = PlannerFlatColors.TextMuted)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
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
}

@Composable
private fun ProfileHeaderSection(
    uiState: ProfileUiState,
    onAvatarClick: () -> Unit,
    onEditAvatarClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer.copy(alpha = 0.4f))
                    .border(2.5.dp, scheme.primary.copy(alpha = 0.45f), CircleShape)
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
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = 2.dp, y = 2.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .clickable(enabled = !uiState.isAvatarUploading) { onEditAvatarClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change profile photo",
                    tint = scheme.onPrimary,
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
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiState.userEmail,
                fontSize = 14.sp,
                color = PlannerFlatColors.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileInitial(userName: String) {
    Text(
        text = userName.firstOrNull()?.uppercase() ?: "U",
        fontFamily = LoraFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Normal,
        color = PlannerFlatColors.TextDark,
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
private fun PersonalInfoFields(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileTextField(
            label = "FULL NAME",
            value = uiState.editName,
            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
            leadingIcon = Icons.Default.Badge,
            errorText = uiState.nameError,
            placeholder = "Enter your full name"
        )

        ProfileTextField(
            label = "EMAIL ADDRESS",
            value = uiState.userEmail,
            onValueChange = {},
            enabled = false,
            leadingIcon = Icons.Default.Email,
            trailingIcon = {
                if (uiState.userEmail.isNotBlank()) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(uiState.userEmail))
                        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Email",
                            tint = PlannerFlatColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Primary Email",
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            helperText = "Linked to your account.",
        )

        ProfileDropdownMenu(
            label = "GENDER",
            options = genderOptions,
            selectedOption = uiState.editGender.ifEmpty { "Select gender" },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) },
            leadingIcon = Icons.Default.Person,
        )
    }
}

@Composable
private fun ExamFocusFields(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileDropdownMenu(
            label = "TARGET EXAM",
            options = examOptions,
            selectedOption = uiState.editExamType.ifEmpty { "Select target exam" },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
            leadingIcon = Icons.Default.School,
        )

        ProfileDropdownMenu(
            label = "PREPARATION STAGE",
            options = stageOptions,
            selectedOption = uiState.editStage.ifEmpty { "Select preparation stage" },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
            leadingIcon = Icons.Default.BarChart,
        )
    }
}

@Composable
private fun AccountStatusRow(
    isPremiumActive: Boolean,
    onPremiumClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val statusTitle = if (isPremiumActive) "Safar Premium" else "Safar Plus"
    val statusText = if (isPremiumActive) "Premium subscription active" else "Free Access Plan"
    val buttonText = if (isPremiumActive) "Manage Plan" else "Explore Premium"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPremiumActive) scheme.primary.copy(alpha = 0.12f) else PlannerFlatColors.TextMuted.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremiumActive) scheme.primary else PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = statusTitle,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = statusText,
                    fontSize = 12.5.sp,
                    color = if (isPremiumActive) scheme.primary else PlannerFlatColors.TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.primary.copy(alpha = 0.08f))
                .border(1.dp, scheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable(onClick = onPremiumClick)
                .padding(vertical = 8.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.primary
            )
        }
    }
}

@Composable
private fun ActionsRow(
    isSaving: Boolean,
    onLogoutClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Save Button (Flat Action Pill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.primary)
                .clickable(enabled = !isSaving, onClick = onSaveClick)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = scheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Save Profile",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onPrimary
                    )
                }
            }
        }

        // Logout Button (Bare Outlined Action Pill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, scheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable(onClick = onLogoutClick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = scheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Logout",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.error
                )
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    helperText: String? = null,
    placeholder: String? = null,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = PlannerFlatColors.TextDark.copy(alpha = 0.9f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            placeholder = placeholder?.let { { Text(it, fontSize = 15.sp, color = PlannerFlatColors.TextMuted.copy(alpha = 0.6f)) } },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp), tint = scheme.primary) }
            },
            trailingIcon = trailingIcon,
            isError = errorText != null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = scheme.primary,
                unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                disabledBorderColor = PlannerFlatColors.BorderSoft.copy(alpha = 0.4f),
                disabledContainerColor = PlannerFlatColors.BorderSoft.copy(alpha = 0.1f),
                disabledTextColor = PlannerFlatColors.TextDark.copy(alpha = 0.8f),
                focusedTextColor = PlannerFlatColors.TextDark,
                unfocusedTextColor = PlannerFlatColors.TextDark,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        )
        if (errorText != null) {
            Text(text = errorText, color = scheme.error, fontSize = 12.5.sp)
        } else if (helperText != null) {
            Text(text = helperText, color = PlannerFlatColors.TextMuted, fontSize = 12.5.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = PlannerFlatColors.TextDark.copy(alpha = 0.9f),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(10.dp),
                leadingIcon = leadingIcon?.let {
                    { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp), tint = scheme.primary) }
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                    focusedTextColor = PlannerFlatColors.TextDark,
                    unfocusedTextColor = PlannerFlatColors.TextDark,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = SafarSemanticColors.plannerBackground(),
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(text = opt, fontSize = 15.sp, color = PlannerFlatColors.TextDark) },
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

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "For support or queries, reach us at:\nonesafar@gmail.com • safarparmar0@gmail.com",
            fontSize = 12.sp,
            color = PlannerFlatColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "© 2026 SAFAR • Version 1.0.4",
            fontSize = 11.sp,
            color = PlannerFlatColors.TextMuted.copy(alpha = 0.7f),
        )
    }
}
