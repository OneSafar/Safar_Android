package com.safarparmar.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
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

// Composables UI imports
import com.composables.ui.components.AlertDialog as ComposablesAlertDialog
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon as ComposablesIcon
import com.composables.ui.components.IconButton as ComposablesIconButton
import com.composables.ui.components.Text as ComposablesText
import com.composables.ui.components.Tooltip
import com.composables.ui.components.TooltipPanel
import com.composables.ui.components.TooltipSide
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composeunstyled.theme.Theme

import com.safarparmar.app.R
import com.safarparmar.app.ui.components.DeleteAccountDialog
import com.safarparmar.app.ui.components.LanguageSelectionDialog
import com.safarparmar.app.ui.components.SafarCircularProgressIndicator
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors

// ─── Constants ─────────────────────────────────────────────────────────────────
private val examOptions = listOf("UPSC", "SSC", "IBPS", "RRB", "NEET", "JEE", "12th Boards", "State PSC", "CAT", "GATE", "Other")
private val stageOptions = listOf("Beginner", "Intermediate", "Advanced", "Revision", "Mock Tests")
private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

// Exact Option 1 Mockup Colors & Shapes
private val ProfileBg = Color(0xFFF5F4FF)
private val CardBg = Color.White
private val CardShape = RoundedCornerShape(18.dp)

// ─── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
            containerColor = if (isDarkTheme) MaterialTheme.colorScheme.background else ProfileBg,
            topBarActions = {
                // Top-right Action: Solid purple circle with white checkmark (as in Option 1 Mockup)
                Tooltip(
                    side = TooltipSide.Bottom,
                    panel = {
                        TooltipPanel {
                            ComposablesText(text = stringResource(R.string.profile_save_content_description))
                        }
                    },
                    anchor = {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SafarSemanticColors.brandPurple())
                                .clickable(enabled = !uiState.isSaving) {
                                    viewModel.onEvent(ProfileEvent.SaveProfile)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.isSaving) {
                                SafarCircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                ComposablesIcon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.profile_save_content_description),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                )
            },
        ) { paddingValues ->
            var profileVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { profileVisible = true }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Centered Avatar Section ───────────────────────────────────
                ProfileStaggeredBox(index = 0, isVisible = profileVisible) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProfileAvatarSection(
                            uiState = uiState,
                            onAvatarClick = {
                                if (uiState.userAvatar.isNullOrBlank()) imagePicker.launch("image/*")
                                else showAvatarPreview = true
                            },
                            onEditAvatarClick = { imagePicker.launch("image/*") },
                        )
                    }
                }

                // Thin hairline horizontal separator below avatar (as in Option 1 Mockup)
                HorizontalSeparator(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.Black.copy(alpha = 0.08f),
                )

                // ── Personal Information Section ───────────────────────────────
                ProfileStaggeredBox(index = 2, isVisible = profileVisible) {
                    ProfileSectionBlock(
                        title = stringResource(R.string.profile_personal_information),
                    ) {
                        PersonalInfoFields(uiState = uiState, viewModel = viewModel)
                    }
                }

                // ── Academic & Exam Focus Section ──────────────────────────────
                ProfileStaggeredBox(index = 3, isVisible = profileVisible) {
                    ProfileSectionBlock(
                        title = stringResource(R.string.profile_academic_focus),
                    ) {
                        ExamFocusFields(uiState = uiState, viewModel = viewModel)
                    }
                }

                // ── Account & Subscription Section ─────────────────────────────
                ProfileStaggeredBox(index = 4, isVisible = profileVisible) {
                    ProfileSectionBlock(
                        title = stringResource(R.string.profile_account_subscription),
                    ) {
                        AccountStatusRow(
                            isPremiumActive = premiumStatus.hasAnyPaidAccess,
                            onPremiumClick = onPremium,
                        )
                    }
                }

                // Error text
                if (uiState.error != null) {
                    ComposablesText(
                        text = uiState.error!!,
                        color = scheme.error,
                        fontSize = 13.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalSeparator(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.Black.copy(alpha = 0.08f),
                )

                // ── Danger Zone / Logout Actions ───────────────────────────────
                ProfileStaggeredBox(index = 5, isVisible = profileVisible) {
                    ActionsRow(
                        onLogoutClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                        onDeleteAccountClick = { viewModel.onEvent(ProfileEvent.ShowDeleteAccountDialog) },
                    )
                }

                ProfileFooterSection()

                Spacer(Modifier.height(16.dp))
            }

            // ── Dialogs ───────────────────────────────────────────────────────
            if (uiState.showDeleteAccountDialog) {
                DeleteAccountDialog(
                    userEmail = uiState.userEmail,
                    isDeleting = uiState.isDeletingAccount,
                    errorMessage = uiState.deleteAccountError,
                    onDismiss = { viewModel.onEvent(ProfileEvent.DismissDeleteAccountDialog) },
                    onConfirmDelete = { password -> viewModel.onEvent(ProfileEvent.DeleteAccount(password)) },
                )
            }

            if (uiState.showLogoutDialog || uiState.isLoggingOut) {
                ComposablesAlertDialog(
                    visible = true,
                    onDismissRequest = {
                        if (!uiState.isLoggingOut) viewModel.onEvent(ProfileEvent.DismissLogoutDialog)
                    },
                    icon = {
                        ComposablesIcon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = scheme.error,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    title = {
                        ComposablesText(
                            text = stringResource(R.string.profile_confirm_logout),
                            fontFamily = LoraFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    },
                    text = {
                        ComposablesText(
                            text = stringResource(R.string.profile_logout_message),
                            fontSize = 14.sp,
                            color = Theme[colors][mutedColor],
                            textAlign = TextAlign.Center,
                        )
                    },
                    positiveButton = {
                        Button(
                            onClick = { viewModel.logout { onLogout() } },
                            enabled = !uiState.isLoggingOut,
                            style = ButtonStyle.Destructive,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (uiState.isLoggingOut) {
                                SafarCircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            ComposablesText(
                                text = if (uiState.isLoggingOut)
                                    stringResource(R.string.profile_logging_out)
                                else
                                    stringResource(R.string.profile_logout),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    negativeButton = {
                        if (!uiState.isLoggingOut) {
                            Button(
                                onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
                                style = ButtonStyle.Secondary,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ComposablesText(
                                    text = stringResource(R.string.profile_cancel),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    },
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

// ─── Option-1 Card Wrapper ─────────────────────────────────────────────────────
@Composable
private fun ProfileCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = CardShape, ambientColor = Color.Black.copy(alpha = 0.05f)),
        shape = CardShape,
        color = CardBg,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ProfileSectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Section title — dark, bold, uppercase (as in Option 1 Mockup)
        ComposablesText(
            text = title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = PlannerFlatColors.TextDark,
        )
        ProfileCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                content = content,
            )
        }
    }
}

// ─── Avatar ────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileAvatarSection(
    uiState: ProfileUiState,
    onAvatarClick: () -> Unit,
    onEditAvatarClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(SafarSemanticColors.brandPurple().copy(alpha = 0.12f))
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
                        is coil.compose.AsyncImagePainter.State.Loading -> SafarCircularProgressIndicator(
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                        )
                        else -> ProfileInitialLetter(uiState.userName)
                    }
                }
            } else {
                ProfileInitialLetter(uiState.userName)
            }
            if (uiState.isAvatarUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SafarCircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        // Camera badge — White circle with black camera icon (as in Option 1 Mockup)
        Tooltip(
            side = TooltipSide.Bottom,
            panel = {
                TooltipPanel {
                    ComposablesText(text = stringResource(R.string.profile_change_photo))
                }
            },
            anchor = {
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = 2.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color.Black.copy(alpha = 0.10f), CircleShape)
                        .clickable(enabled = !uiState.isAvatarUploading) { onEditAvatarClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    ComposablesIcon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.profile_change_photo),
                        tint = PlannerFlatColors.TextDark,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        )
    }
}

@Composable
private fun ProfileInitialLetter(userName: String) {
    ComposablesText(
        text = userName.firstOrNull()?.uppercase() ?: "U",
        fontFamily = LoraFontFamily,
        fontSize = 36.sp,
        fontWeight = FontWeight.Normal,
        color = PlannerFlatColors.TextDark,
    )
}

// ─── Photo Preview Dialog ──────────────────────────────────────────────────────
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
                ComposablesIconButton(style = ButtonStyle.Ghost, onClick = onEdit) {
                    ComposablesIcon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.profile_change_photo),
                        tint = Color.White
                    )
                }
                ComposablesIconButton(style = ButtonStyle.Ghost, onClick = onDismiss) {
                    ComposablesIcon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.profile_close_photo),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ─── Personal Information Fields ───────────────────────────────────────────────
@Composable
private fun PersonalInfoFields(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Row 1: Full Name — Clean inline row with Person icon + editable text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComposablesIcon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = PlannerFlatColors.TextDark,
            modifier = Modifier.size(22.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.editName.isEmpty()) {
                ComposablesText(
                    text = stringResource(R.string.profile_full_name_placeholder),
                    fontSize = 15.5.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            BasicTextField(
                value = uiState.editName,
                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlannerFlatColors.TextDark,
                ),
                cursorBrush = SolidColor(SafarSemanticColors.brandPurple()),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    HorizontalSeparator(color = Color.Black.copy(alpha = 0.08f))

    // Row 2: Email Address — Inline row with Mail icon + email text + Copy icon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComposablesIcon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = PlannerFlatColors.TextDark,
            modifier = Modifier.size(22.dp),
        )
        ComposablesText(
            text = uiState.userEmail.ifBlank { "—" },
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            color = PlannerFlatColors.TextDark,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (uiState.userEmail.isNotBlank()) {
            ComposablesIcon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.profile_copy_email),
                tint = PlannerFlatColors.TextDark,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(uiState.userEmail))
                        Toast.makeText(context, context.getString(R.string.profile_email_copied), Toast.LENGTH_SHORT).show()
                    },
            )
        }
    }

    HorizontalSeparator(color = Color.Black.copy(alpha = 0.08f))

    // Row 3: Gender Dropdown — Inline row with Person icon + selection + down chevron
    ProfileDropdownInlineRow(
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

// ─── Exam Focus Fields ─────────────────────────────────────────────────────────
@Composable
private fun ExamFocusFields(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    // Row 1: Target Exam
    ProfileDropdownInlineRow(
        options = examOptions,
        selectedOption = uiState.editExamType.ifEmpty { stringResource(R.string.profile_select_target_exam) },
        onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
        leadingIcon = Icons.Default.School,
        optionLabel = { value -> if (value == "Other") stringResource(R.string.profile_option_other_exam) else value },
    )

    HorizontalSeparator(color = Color.Black.copy(alpha = 0.08f))

    // Row 2: Preparation Stage
    ProfileDropdownInlineRow(
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

// ─── Inline Dropdown Row (Option 1 Mockup Style) ───────────────────────────────
@Composable
private fun ProfileDropdownInlineRow(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    leadingIcon: ImageVector,
    optionLabel: @Composable (String) -> String = { it },
) {
    var expanded by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        alignment = DropdownMenuAlignment.Start,
        panel = {
            DropdownMenuPanel(
                modifier = Modifier.fillMaxWidth(),
                maxWidth = 340.dp,
            ) {
                options.forEach { opt ->
                    val isSelected = opt == selectedOption || optionLabel(opt) == selectedOption
                    DropdownMenuItem(
                        onClick = {
                            onSelect(opt)
                            expanded = false
                        },
                        leading = {
                            if (isSelected) {
                                ComposablesIcon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SafarSemanticColors.brandPurple(),
                                )
                            } else {
                                Spacer(Modifier.width(16.dp))
                            }
                        },
                    ) {
                        ComposablesText(
                            text = optionLabel(opt),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    ) {
        // Flat inline row trigger (as in Option 1 Mockup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ComposablesIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = PlannerFlatColors.TextDark,
                modifier = Modifier.size(22.dp),
            )
            ComposablesText(
                text = optionLabel(selectedOption),
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium,
                color = PlannerFlatColors.TextDark,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val chevronRotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "dropdown_chevron",
            )
            ComposablesIcon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PlannerFlatColors.TextDark,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
    }
}

// ─── Account Status Row ────────────────────────────────────────────────────────
@Composable
internal fun AccountStatusRow(
    isPremiumActive: Boolean,
    onPremiumClick: () -> Unit = {},
) {
    val statusTitle = stringResource(if (isPremiumActive) R.string.profile_safar_premium else R.string.profile_safar_plus)
    val statusText = stringResource(if (isPremiumActive) R.string.profile_premium_active else R.string.profile_free_plan)
    val buttonText = stringResource(if (isPremiumActive) R.string.profile_manage_plan else R.string.profile_explore_premium)

    com.safarparmar.app.ui.components.SafarAdaptiveRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        equalWidth = false,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ComposablesIcon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = if (isPremiumActive) SafarSemanticColors.brandPurple() else PlannerFlatColors.TextDark,
                modifier = Modifier.size(24.dp)
            )
            Column {
                ComposablesText(
                    text = statusTitle,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark,
                )
                ComposablesText(
                    text = statusText,
                    fontSize = 12.5.sp,
                    color = if (isPremiumActive) SafarSemanticColors.brandPurple() else PlannerFlatColors.TextMuted,
                )
            }
        }

        Button(
            onClick = onPremiumClick,
            style = if (isPremiumActive) ButtonStyle.Outlined else ButtonStyle.Primary,
            buttonSize = ButtonSize.Small,
        ) {
            ComposablesText(text = buttonText, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Danger Zone Actions ───────────────────────────────────────────────────────
@Composable
internal fun ActionsRow(
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    com.safarparmar.app.ui.components.SafarAdaptiveRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Logout — Outlined
        Button(
            onClick = onLogoutClick,
            style = ButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ComposablesIcon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = scheme.error,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            ComposablesText(
                text = stringResource(R.string.profile_logout),
                fontWeight = FontWeight.Bold,
                color = scheme.error,
            )
        }

        // Delete Account — Destructive
        Button(
            onClick = onDeleteAccountClick,
            style = ButtonStyle.Destructive,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ComposablesIcon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            ComposablesText(
                text = stringResource(R.string.profile_delete_account),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─── Footer ───────────────────────────────────────────────────────────────────
@Composable
private fun ProfileFooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ComposablesText(
            text = stringResource(R.string.profile_support_contact),
            fontSize = 11.sp,
            color = PlannerFlatColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        ComposablesText(
            text = stringResource(R.string.profile_footer_version),
            fontSize = 11.sp,
            color = PlannerFlatColors.TextMuted.copy(alpha = 0.7f),
        )
    }
}

// ─── Staggered Entrance Animation ─────────────────────────────────────────────
@Composable
private fun ProfileStaggeredBox(
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit,
) {
    val slideOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (20 + index * 12).dp,
        animationSpec = tween(
            durationMillis = 320,
            delayMillis = index * 40,
            easing = FastOutSlowInEasing,
        ),
        label = "profileStaggeredOffset_$index",
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            delayMillis = index * 40,
        ),
        label = "profileStaggeredAlpha_$index",
    )

    Box(
        modifier = Modifier.graphicsLayer {
            translationY = slideOffset.toPx()
            alpha = alphaAnim
        }
    ) {
        content()
    }
}
