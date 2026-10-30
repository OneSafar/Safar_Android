package com.safarparmar.app.ui.profile

import androidx.compose.ui.platform.LocalResources

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.safarparmar.app.ui.components.popups.SafarDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

// Composables UI imports
import com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog as ComposablesAlertDialog
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.safarparmar.app.ui.components.popups.SafarComposablesDropdownMenu as DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon as ComposablesIcon
import com.composables.ui.components.IconButton as ComposablesIconButton
import com.composables.ui.components.Text as ComposablesText
import com.safarparmar.app.ui.components.popups.SafarComposablesTooltip as Tooltip
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
    val localizedResources by rememberUpdatedState(LocalResources.current)
    val scheme = MaterialTheme.colorScheme
    val profileColors = remember(isDarkTheme) { compactProfileColors(isDarkTheme) }

    var showAvatarPreview by rememberSaveable { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.UploadAvatar(it)) }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, localizedResources.getString(R.string.profile_saved_success), Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(uiState.avatarUploadSuccess) {
        if (uiState.avatarUploadSuccess) {
            Toast.makeText(context, localizedResources.getString(R.string.profile_photo_updated), Toast.LENGTH_SHORT).show()
            viewModel.onEvent(ProfileEvent.ClearAvatarUploadSuccess)
        }
    }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme,
        LocalCompactProfileColors provides profileColors,
        LocalSafarComposablesAccent provides profileColors.action) {
      MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
          background = profileColors.canvas, surface = profileColors.paper,
          onBackground = profileColors.ink, onSurface = profileColors.ink,
          onSurfaceVariant = profileColors.muted, primary = profileColors.action,
          onPrimary = profileColors.onAction, outlineVariant = profileColors.border)) {
       SafarComposablesTheme {
        SafarDrawerScaffold(
            title = stringResource(R.string.profile_title),
            subtitle = null,
            showTutorialButton = false,
            topBarActions = {
                val canSave = !uiState.isLoading && !uiState.isAvatarUploading && !uiState.isDeletingAccount && !uiState.isLoggingOut
                val tickColor = if (isDarkTheme) Color(0xFFF0BEDC) else profileColors.plum
                ComposablesIconButton(
                    onClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                    enabled = canSave && !uiState.isSaving,
                    style = ButtonStyle.Ghost,
                    buttonSize = ButtonSize.Small,
                    contentColor = tickColor,
                ) {
                    if (uiState.isSaving) {
                        SafarCircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = tickColor,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        ComposablesIcon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.profile_save_content_description),
                            tint = tickColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            },
            topBarContentColor = profileColors.ink,
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            containerColor = profileColors.canvas,
        ) { paddingValues ->
            CompactProfileLayout(
                modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues),
                identity = { vertical ->
                    CompactIdentityCard(uiState, premiumStatus.hasAnyPaidAccess, vertical,
                        onAvatarClick = {
                            if (uiState.userAvatar.isNullOrBlank()) imagePicker.launch("image/*")
                            else showAvatarPreview = true
                        }, onEditAvatar = { imagePicker.launch("image/*") })
                },
                form = {
                    CompactProfileSurface {
                        CompactSectionLabel(stringResource(R.string.profile_personal_information))
                        PersonalInfoFields(uiState, viewModel)
                        Spacer(Modifier.height(14.dp))
                        CompactSectionLabel(stringResource(R.string.profile_academic_focus))
                        ExamFocusFields(uiState, viewModel)
                        uiState.error?.let {
                            ComposablesText(it, color = profileColors.danger, fontSize = 13.sp,
                                lineHeight = 21.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                },
                membership = { CompactProfileMembership(premiumStatus.hasAnyPaidAccess, onPremium) },
                save = { CompactProfileSave(uiState.isSaving,
                    !uiState.isLoading && !uiState.isAvatarUploading && !uiState.isDeletingAccount && !uiState.isLoggingOut,
                    onSave = { viewModel.onEvent(ProfileEvent.SaveProfile) }) },
                accountActions = { CompactProfileAccountActions(
                    enabled = !uiState.isSaving && !uiState.isLoggingOut && !uiState.isDeletingAccount,
                    onLogout = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                    onDelete = { viewModel.onEvent(ProfileEvent.ShowDeleteAccountDialog) }) },
                footer = { ProfileFooterSection() },
            )

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

} // Scoped Profile color and component themes.
}

@Composable
private fun CompactSectionLabel(label: String) {
    ComposablesText(label.uppercase(), color = LocalCompactProfileColors.current.muted,
        fontSize = 11.sp, lineHeight = 18.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun CompactIdentityCard(state: ProfileUiState, premium: Boolean, vertical: Boolean,
    onAvatarClick: () -> Unit, onEditAvatar: () -> Unit) {
    val c = LocalCompactProfileColors.current
    CompactProfileSurface {
        val details: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = if (vertical) Alignment.CenterHorizontally else Alignment.Start) {
                ComposablesText(state.userName.ifBlank { stringResource(R.string.profile_full_name_placeholder) },
                    color = c.ink, fontFamily = PoppinsFontFamily, fontSize = 20.sp, lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold)
                ComposablesText(state.userEmail.ifBlank { "—" }, color = c.muted, fontSize = 13.sp,
                    lineHeight = 20.sp, textAlign = if (vertical) TextAlign.Center else TextAlign.Start)
                ComposablesText(stringResource(R.string.profile_email_linked), color = c.muted, fontSize = 11.sp, lineHeight = 18.sp)
                ComposablesText(stringResource(if (premium) R.string.profile_safar_premium else R.string.profile_safar_plus),
                    color = c.onMint, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                    modifier = Modifier.clip(CircleShape).background(c.mint).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        BoxWithConstraints {
            if (vertical || maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileAvatarSection(state, onAvatarClick, onEditAvatar)
                    details()
                }
            } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileAvatarSection(state, onAvatarClick, onEditAvatar)
                Box(Modifier.weight(1f)) { details() }
            }
        }
    }
}

@Composable
private fun TabletProfileContent(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    isPremiumActive: Boolean,
    onPremium: () -> Unit,
    onAvatarClick: () -> Unit,
    onEditAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val divider = scheme.outlineVariant.copy(alpha = 0.6f)
    BoxWithConstraints(modifier.imePadding()) {
    val workspaceHeight = (maxHeight - 120.dp).coerceAtLeast(440.dp)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = scheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, divider),
        ) {
            Row {
                Column(
                    Modifier.weight(0.62f).heightIn(min = workspaceHeight).padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    ComposablesText(stringResource(R.string.profile_title), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ProfileAvatarSection(uiState, onAvatarClick, onEditAvatar)
                        Column(Modifier.weight(1f)) {
                            TabletProfileLabel(stringResource(R.string.profile_full_name_placeholder))
                            PersonalInfoFields(uiState, viewModel, nameOnly = true)
                        }
                    }
                    HorizontalSeparator(color = divider)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(1f)) {
                            TabletProfileLabel(stringResource(R.string.profile_personal_information))
                            Spacer(Modifier.height(16.dp))
                            PersonalInfoFields(uiState, viewModel, detailsOnly = true)
                        }
                        Column(Modifier.weight(1f).drawBehind { drawLine(divider, Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) }.padding(start = 20.dp)) {
                            TabletProfileLabel(stringResource(R.string.profile_academic_focus))
                            Spacer(Modifier.height(16.dp))
                            ExamFocusFields(uiState, viewModel)
                        }
                    }
                    uiState.error?.let {
                        ComposablesText(it, color = scheme.error, fontSize = 13.sp)
                    }
                }
                Column(
                    Modifier.weight(0.38f).heightIn(min = workspaceHeight).drawBehind { drawLine(divider, Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) }.padding(24.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TabletProfileLabel(stringResource(R.string.profile_account_subscription))
                    ComposablesIcon(imageVector = Icons.Default.WorkspacePremium, contentDescription = null, tint = SafarSemanticColors.brandPurple(), modifier = Modifier.size(40.dp))
                    ComposablesText(
                        stringResource(if (isPremiumActive) R.string.profile_safar_premium else R.string.profile_safar_plus),
                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    )
                    ComposablesText(
                        stringResource(if (isPremiumActive) R.string.profile_premium_active else R.string.profile_free_plan),
                        color = SafarSemanticColors.brandPurple(), fontSize = 14.sp,
                    )
                    Button(onClick = onPremium, style = ButtonStyle.Outlined) {
                        ComposablesText(stringResource(if (isPremiumActive) R.string.profile_manage_plan else R.string.profile_explore_premium))
                    }
                    HorizontalSeparator(color = divider)
                    Button(
                        onClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                        style = ButtonStyle.Outlined,
                        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                    ) {
                        ComposablesIcon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        ComposablesText(stringResource(R.string.profile_logout), color = scheme.error)
                    }
                    Button(
                        onClick = { viewModel.onEvent(ProfileEvent.ShowDeleteAccountDialog) },
                        style = ButtonStyle.Outlined,
                        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                    ) {
                        ComposablesIcon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        ComposablesText(stringResource(R.string.profile_delete_account), color = scheme.error)
                    }
                }
            }
        }
        ProfileFooterSection()
    }
}
    }

@Composable
private fun TabletProfileLabel(text: String) {
    ComposablesText(text.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

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
        color = LocalCompactProfileColors.current.paper,
        contentColor = LocalCompactProfileColors.current.ink,
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
            color = LocalCompactProfileColors.current.ink,
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
                .background(LocalCompactProfileColors.current.peach)
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

        // Keep the badge and icon on the same light/dark palette as the cards.
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LocalCompactProfileColors.current.paper)
                        .border(1.dp, LocalCompactProfileColors.current.border, CircleShape)
                        .clickable(enabled = !uiState.isAvatarUploading) { onEditAvatarClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    ComposablesIcon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.profile_change_photo),
                        tint = LocalCompactProfileColors.current.ink,
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
        fontFamily = PoppinsFontFamily,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = LocalCompactProfileColors.current.plum,
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
        fullScreen = true,
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
private fun PersonalInfoFields(uiState: ProfileUiState, viewModel: ProfileViewModel, nameOnly: Boolean = false, detailsOnly: Boolean = false) {
    val c = LocalCompactProfileColors.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val resources by rememberUpdatedState(LocalResources.current)
    val locked = uiState.isSaving || uiState.isLoading
    val nameLabel = stringResource(R.string.profile_full_name_placeholder)
    if (!detailsOnly) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ComposablesText(stringResource(R.string.profile_full_name_placeholder), color = c.muted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = uiState.editName, onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
                    enabled = !locked, singleLine = true,
                    textStyle = TextStyle(fontFamily = PoppinsFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.ink),
                    cursorBrush = SolidColor(c.action), modifier = Modifier.weight(1f).semantics { contentDescription = nameLabel }.heightIn(min = 48.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (uiState.editName.isBlank()) ComposablesText(stringResource(R.string.profile_full_name_placeholder), color = c.muted, fontSize = 16.sp)
                            field()
                        }
                    })
                ComposablesIcon(Icons.Default.Edit, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
            }
            uiState.nameError?.let { ComposablesText(it, color = c.danger, fontSize = 12.sp, lineHeight = 18.sp) }
        }
        HorizontalSeparator(color = c.border)
    }
    if (nameOnly) return
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ComposablesText(stringResource(R.string.profile_email_address), color = c.muted, fontSize = 12.sp)
            ComposablesText(uiState.userEmail.ifBlank { "—" }, color = c.ink, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
            ComposablesText(stringResource(R.string.profile_email_linked), color = c.muted, fontSize = 11.sp, lineHeight = 18.sp)
        }
        ComposablesIconButton(onClick = {
            clipboardManager.setText(AnnotatedString(uiState.userEmail))
            Toast.makeText(context, resources.getString(R.string.profile_email_copied), Toast.LENGTH_SHORT).show()
        }, enabled = uiState.userEmail.isNotBlank(), style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
            ComposablesIcon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.profile_copy_email), tint = c.ink,
                modifier = Modifier.size(21.dp))
        }
    }
    HorizontalSeparator(color = c.border)
    ProfileDropdownInlineRow(options = genderOptions, selectedOption = uiState.editGender,
        onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) }, label = stringResource(R.string.profile_gender), enabled = !locked,
        optionLabel = { when (it) {
            "Male" -> stringResource(R.string.profile_option_male)
            "Female" -> stringResource(R.string.profile_option_female)
            "Other" -> stringResource(R.string.profile_option_other)
            "Prefer not to say" -> stringResource(R.string.profile_option_private)
            else -> it.ifBlank { stringResource(R.string.profile_select_gender) }
        } })
}

@Composable
private fun ExamFocusFields(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val enabled = !uiState.isLoading && !uiState.isSaving
    ProfileDropdownInlineRow(options = examOptions, selectedOption = uiState.editExamType,
        onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) }, label = stringResource(R.string.profile_target_exam), enabled = enabled,
        optionLabel = { if (it == "Other") stringResource(R.string.profile_option_other_exam)
            else it.ifBlank { stringResource(R.string.profile_select_target_exam) } })
    HorizontalSeparator(color = LocalCompactProfileColors.current.border)
    ProfileDropdownInlineRow(options = stageOptions, selectedOption = uiState.editStage,
        onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) }, label = stringResource(R.string.profile_preparation_stage), enabled = enabled,
        optionLabel = { when (it) {
            "Beginner" -> stringResource(R.string.profile_option_beginner)
            "Intermediate" -> stringResource(R.string.profile_option_intermediate)
            "Advanced" -> stringResource(R.string.profile_option_advanced)
            "Revision" -> stringResource(R.string.profile_option_revision)
            "Mock Tests" -> stringResource(R.string.profile_option_mock_tests)
            else -> it.ifBlank { stringResource(R.string.profile_select_preparation_stage) }
        } })
}

/** Existing Composables UI anchored menu, with the approved label/value row trigger. */
@Composable
private fun ProfileDropdownInlineRow(options: List<String>, selectedOption: String, onSelect: (String) -> Unit,
    label: String, enabled: Boolean = true, optionLabel: @Composable (String) -> String = { it }) {
    val c = LocalCompactProfileColors.current
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var anchorWidthPx by remember { mutableStateOf(0) }
    val menuWidth = with(density) { anchorWidthPx.toDp() }.coerceIn(160.dp, 340.dp)
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    DropdownMenu(expanded = expanded, onExpandedChange = { expanded = it && enabled }, alignment = DropdownMenuAlignment.Start,
        panel = {
            DropdownMenuPanel(modifier = Modifier.width(menuWidth), minWidth = menuWidth, maxWidth = menuWidth) {
                options.forEach { opt ->
                    DropdownMenuItem(onClick = { onSelect(opt); expanded = false }, leading = {
                        if (opt == selectedOption) ComposablesIcon(Icons.Default.Check, contentDescription = null,
                            tint = c.action, modifier = Modifier.size(18.dp)) else Spacer(Modifier.width(18.dp))
                    }) { ComposablesText(optionLabel(opt), modifier = Modifier.weight(1f)) }
                }
            }
        }) {
        Row(Modifier.fillMaxWidth().onSizeChanged { anchorWidthPx = it.width }.clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button) { expanded = !expanded }
            .heightIn(min = 76.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                ComposablesText(label, color = c.muted, fontSize = 12.sp, lineHeight = 18.sp)
                ComposablesText(optionLabel(selectedOption), color = c.ink, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            ComposablesIcon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = c.ink, modifier = Modifier.size(22.dp))
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
                tint = if (isPremiumActive) SafarSemanticColors.brandPurple() else LocalCompactProfileColors.current.ink,
                modifier = Modifier.size(24.dp)
            )
            Column {
                ComposablesText(
                    text = statusTitle,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCompactProfileColors.current.ink,
                )
                ComposablesText(
                    text = statusText,
                    fontSize = 12.5.sp,
                    color = if (isPremiumActive) SafarSemanticColors.brandPurple() else LocalCompactProfileColors.current.muted,
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
            color = LocalCompactProfileColors.current.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        ComposablesText(
            text = stringResource(R.string.profile_footer_version),
            fontSize = 11.sp,
            color = LocalCompactProfileColors.current.muted.copy(alpha = 0.7f),
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
