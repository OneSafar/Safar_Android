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
import androidx.compose.material.icons.filled.DeleteForever
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.safarparmar.app.ui.components.DeleteAccountDialog
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.R

private val examOptions = listOf("UPSC", "SSC", "IBPS", "RRB", "NEET", "JEE", "12th Boards", "State PSC", "CAT", "GATE", "Other")
private val stageOptions = listOf("Beginner", "Intermediate", "Advanced", "Revision", "Mock Tests")
private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

@Composable
private fun ProfileSheetSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextDark,
        )
        content()
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
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.UploadAvatar(it)) }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, context.getString(R.string.profile_saved_success), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.avatarUploadSuccess) {
        if (uiState.avatarUploadSuccess) {
            Toast.makeText(context, context.getString(R.string.profile_photo_updated), Toast.LENGTH_SHORT).show()
            viewModel.onEvent(ProfileEvent.ClearAvatarUploadSuccess)
        }
    }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
        SafarDrawerScaffold(
            title = stringResource(R.string.profile_title),
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
                            color = SafarSemanticColors.brandPurple()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.profile_save_content_description),
                            tint = SafarSemanticColors.brandPurple()
                        )
                    }
                }
            },
        ) { paddingValues ->
            var profileVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                profileVisible = true
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.profile_update_details),
                            fontSize = 13.sp,
                            color = PlannerFlatColors.TextMuted,
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profile_language_title),
                                    fontWeight = FontWeight.Bold,
                                    color = PlannerFlatColors.TextDark,
                                )
                                Text(
                                    text = stringResource(R.string.profile_language_subtitle),
                                    fontSize = 12.sp,
                                    color = PlannerFlatColors.TextMuted,
                                )
                            }
                            Text(
                                text = when (AppCompatDelegate.getApplicationLocales().toLanguageTags()) {
                                    "hi" -> stringResource(R.string.profile_language_hindi)
                                    "hi-Latn" -> stringResource(R.string.profile_language_hinglish)
                                    else -> stringResource(R.string.profile_language_english)
                                },
                                color = SafarSemanticColors.brandPurple(),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    StaggeredProfileEntranceBox(index = 0, isVisible = profileVisible) {
                        ProfileHeaderSection(
                            uiState = uiState,
                            onAvatarClick = {
                                if (uiState.userAvatar.isNullOrBlank()) imagePicker.launch("image/*")
                                else showAvatarPreview = true
                            },
                            onEditAvatarClick = { imagePicker.launch("image/*") },
                        )
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredProfileEntranceBox(index = 1, isVisible = profileVisible) {
                        ProfileSheetSection(title = stringResource(R.string.profile_personal_information)) {
                            PersonalInfoFields(uiState = uiState, viewModel = viewModel)
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredProfileEntranceBox(index = 2, isVisible = profileVisible) {
                        ProfileSheetSection(title = stringResource(R.string.profile_academic_focus)) {
                            ExamFocusFields(uiState = uiState, viewModel = viewModel)
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredProfileEntranceBox(index = 3, isVisible = profileVisible) {
                        ProfileSheetSection(title = stringResource(R.string.profile_account_subscription)) {
                            AccountStatusRow(
                                isPremiumActive = premiumStatus.hasAnyPaidAccess,
                                onPremiumClick = onPremium,
                            )
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = scheme.error,
                            fontSize = 13.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredProfileEntranceBox(index = 4, isVisible = profileVisible) {
                        ActionsRow(
                            isSaving = uiState.isSaving,
                            onLogoutClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                            onDeleteAccountClick = { viewModel.onEvent(ProfileEvent.ShowDeleteAccountDialog) },
                            onSaveClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                        )
                    }

                    FooterSection()

                    Spacer(Modifier.height(16.dp))
                }
            }

            if (uiState.showDeleteAccountDialog) {
                DeleteAccountDialog(
                    userEmail = uiState.userEmail,
                    isDeleting = uiState.isDeletingAccount,
                    errorMessage = uiState.deleteAccountError,
                    onDismiss = { viewModel.onEvent(ProfileEvent.DismissDeleteAccountDialog) },
                    onConfirmDelete = { password ->
                        viewModel.onEvent(ProfileEvent.DeleteAccount(password))
                    },
                )
            }

            if (showLanguageDialog) {
                val choices = listOf(
                    "en" to stringResource(R.string.profile_language_english),
                    "hi" to stringResource(R.string.profile_language_hindi),
                    "hi-Latn" to stringResource(R.string.profile_language_hinglish),
                )
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.profile_language_dialog_title)) },
                    text = {
                        Column {
                            choices.forEach { (languageTag, label) ->
                                TextButton(
                                    onClick = {
                                        showLanguageDialog = false
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(languageTag)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {},
                )
            }

            if (uiState.showLogoutDialog || uiState.isLoggingOut) {
                AlertDialog(
                    onDismissRequest = {
                        if (!uiState.isLoggingOut) viewModel.onEvent(ProfileEvent.DismissLogoutDialog)
                    },
                    containerColor = SafarSemanticColors.plannerBackground(),
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = scheme.error) },
                    title = { Text(stringResource(R.string.profile_confirm_logout), fontFamily = LoraFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Normal, color = PlannerFlatColors.TextDark) },
                    text = { Text(stringResource(R.string.profile_logout_message), fontSize = 14.sp, color = PlannerFlatColors.TextMuted) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.logout { onLogout() } },
                            enabled = !uiState.isLoggingOut,
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (uiState.isLoggingOut) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.profile_logging_out), fontWeight = FontWeight.Bold)
                            } else {
                                Text(stringResource(R.string.profile_logout), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        if (!uiState.isLoggingOut) {
                            TextButton(onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) }) {
                                Text(stringResource(R.string.profile_cancel), fontWeight = FontWeight.Bold, color = PlannerFlatColors.TextMuted)
                            }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(SafarSemanticColors.brandPurple().copy(alpha = 0.18f))
                    .border(2.5.dp, SafarSemanticColors.brandPurple().copy(alpha = 0.45f), CircleShape)
                    .clickable(enabled = !uiState.isAvatarUploading) { onAvatarClick() },
                contentAlignment = Alignment.Center,
            ) {
                val avatarUrl = uiState.userAvatar?.takeIf { it.isNotBlank() }
                if (avatarUrl != null) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = stringResource(R.string.profile_photo),
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
                    .background(SafarSemanticColors.brandPurple())
                    .clickable(enabled = !uiState.isAvatarUploading) { onEditAvatarClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = stringResource(R.string.profile_change_photo),
                    tint = SafarSemanticColors.brandOnPurple(),
                    modifier = Modifier.size(16.dp),
                )
            }
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
                contentDescription = stringResource(R.string.profile_photo_fullscreen, userName),
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
                    Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.profile_change_photo), tint = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.profile_close_photo), tint = Color.White)
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
            label = stringResource(R.string.profile_full_name),
            value = uiState.editName,
            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
            leadingIcon = Icons.Default.Badge,
            errorText = uiState.nameError,
            placeholder = stringResource(R.string.profile_full_name_placeholder)
        )

        ProfileTextField(
            label = stringResource(R.string.profile_email_address),
            value = uiState.userEmail,
            onValueChange = {},
            enabled = false,
            leadingIcon = Icons.Default.Email,
            trailingIcon = {
                if (uiState.userEmail.isNotBlank()) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(uiState.userEmail))
                        Toast.makeText(context, context.getString(R.string.profile_email_copied), Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.profile_copy_email),
                            tint = PlannerFlatColors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.profile_primary_email),
                        tint = PlannerFlatColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            helperText = stringResource(R.string.profile_email_linked),
        )

        ProfileDropdownMenu(
            label = stringResource(R.string.profile_gender),
            options = genderOptions,
            selectedOption = uiState.editGender.ifEmpty { stringResource(R.string.profile_select_gender) },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) },
            leadingIcon = Icons.Default.Person,
            optionLabel = { value ->
                when (value) {
                    "Male" -> stringResource(R.string.profile_option_male)
                    "Female" -> stringResource(R.string.profile_option_female)
                    "Other" -> stringResource(R.string.profile_option_other)
                    "Prefer not to say" -> stringResource(R.string.profile_option_private)
                    else -> value
                }
            },
        )
    }
}

@Composable
private fun ExamFocusFields(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileDropdownMenu(
            label = stringResource(R.string.profile_target_exam),
            options = examOptions,
            selectedOption = uiState.editExamType.ifEmpty { stringResource(R.string.profile_select_target_exam) },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
            leadingIcon = Icons.Default.School,
            optionLabel = { value -> if (value == "Other") stringResource(R.string.profile_option_other_exam) else value },
        )

        ProfileDropdownMenu(
            label = stringResource(R.string.profile_preparation_stage),
            options = stageOptions,
            selectedOption = uiState.editStage.ifEmpty { stringResource(R.string.profile_select_preparation_stage) },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
            leadingIcon = Icons.Default.BarChart,
            optionLabel = { value ->
                when (value) {
                    "Beginner" -> stringResource(R.string.profile_option_beginner)
                    "Intermediate" -> stringResource(R.string.profile_option_intermediate)
                    "Advanced" -> stringResource(R.string.profile_option_advanced)
                    "Revision" -> stringResource(R.string.profile_option_revision)
                    "Mock Tests" -> stringResource(R.string.profile_option_mock_tests)
                    else -> value
                }
            },
        )
    }
}

@Composable
private fun AccountStatusRow(
    isPremiumActive: Boolean,
    onPremiumClick: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val statusTitle = stringResource(if (isPremiumActive) R.string.profile_safar_premium else R.string.profile_safar_plus)
    val statusText = stringResource(if (isPremiumActive) R.string.profile_premium_active else R.string.profile_free_plan)
    val buttonText = stringResource(if (isPremiumActive) R.string.profile_manage_plan else R.string.profile_explore_premium)

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
                    .background(if (isPremiumActive) SafarSemanticColors.brandPurple().copy(alpha = 0.12f) else PlannerFlatColors.TextMuted.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremiumActive) SafarSemanticColors.brandPurple() else PlannerFlatColors.TextMuted,
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
                    color = if (isPremiumActive) SafarSemanticColors.brandPurple() else PlannerFlatColors.TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SafarSemanticColors.brandPurple().copy(alpha = 0.08f))
                .border(1.dp, SafarSemanticColors.brandPurple().copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable(onClick = onPremiumClick)
                .padding(vertical = 8.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SafarSemanticColors.brandPurple()
            )
        }
    }
}

@Composable
private fun ActionsRow(
    isSaving: Boolean,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.isLightBackground()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MacOSPrimaryActionButton(
            text = stringResource(R.string.profile_save_profile),
            onClick = onSaveClick,
            enabled = !isSaving,
            isLoading = isSaving,
            isLight = isLight,
            icon = Icons.Default.Check,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Logout Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, scheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogoutClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = scheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_logout),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.error
                    )
                }
            }

            // Delete Account Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE11D48).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onDeleteAccountClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_delete_account),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48)
                    )
                }
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
                { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp), tint = SafarSemanticColors.brandPurple()) }
            },
            trailingIcon = trailingIcon,
            isError = errorText != null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SafarSemanticColors.brandPurple(),
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
    optionLabel: @Composable (String) -> String = { it },
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
                value = optionLabel(selectedOption),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                shape = RoundedCornerShape(10.dp),
                leadingIcon = leadingIcon?.let {
                    { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp), tint = SafarSemanticColors.brandPurple()) }
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SafarSemanticColors.brandPurple(),
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
                        text = { Text(text = optionLabel(opt), fontSize = 15.sp, color = PlannerFlatColors.TextDark) },
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
            text = stringResource(R.string.profile_support_contact),
            fontSize = 11.sp,
            color = PlannerFlatColors.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_footer_version),
            fontSize = 11.sp,
            color = PlannerFlatColors.TextMuted.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun StaggeredProfileEntranceBox(
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit,
) {
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else (20 + index * 12).dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 320,
            delayMillis = index * 40,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "profileStaggeredOffset",
    )
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 280,
            delayMillis = index * 40,
        ),
        label = "profileStaggeredAlpha",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = slideOffset.toPx()
                alpha = alphaAnim
            }
    ) {
        content()
    }
}
