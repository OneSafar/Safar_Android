package com.safar.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Tailwind-aligned tokens from the Profile Settings design reference. */
internal object ProfileDesignColors {
    val PrimaryContainer = Color(0xFF2D3E9F)
    val PrimaryFixed = Color(0xFFDEE0FF)
    val Primary = Color(0xFF102488)
    val OnSurfaceVariant = Color(0xFF454652)
    val SurfaceVariant = Color(0xFFE3E2E0)
    val SurfaceContainerLow = Color(0xFFF4F3F1)
    val OnBackground = Color(0xFF1A1C1B)
    val Background = Color(0xFFFAF9F7)
    val SurfaceBright = Color(0xFFFAF9F7)
    val Outline = Color(0xFF757684)
    val ErrorContainer = Color(0xFFFFDAD6)
    val Error = Color(0xFFBA1A1A)
    val Success = Color(0xFF22C55E)
    val SuccessBackground = Color(0xFFF0FDF4)
    val SuccessLabel = Color(0xFF15803D)
}

internal object ProfileDesignTypography {
    val HeadlineXl = androidx.compose.ui.text.TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        letterSpacing = (-0.64).sp,
    )
    val HeadlineLg = androidx.compose.ui.text.TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
    )
    val HeadlineMd = androidx.compose.ui.text.TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
    )
    val BodyLg = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    )
    val BodyMd = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    )
    val LabelCaps = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    )
    val LabelSm = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    )
}

private val examOptions = listOf("UPSC", "SSC", "IBPS", "RRB", "NEET", "JEE", "12th Boards", "State PSC", "CAT", "GATE", "Other")
private val stageOptions = listOf("Beginner", "Intermediate", "Advanced", "Revision", "Mock Tests")
private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

@Composable
private fun lightBg(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.background

@Composable
private fun glassSurface(isDarkTheme: Boolean): Color =
    if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.85f)

@Composable
private fun labelCapsColor(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.primary

@Composable
private fun iconCircleBg(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.primaryContainer

@Composable
private fun iconTint(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.primary

@Composable
private fun borderUnfocused(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

@Composable
private fun borderDisabled(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

@Composable
private fun subtleText(isDarkTheme: Boolean): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun footerMuted(isDarkTheme: Boolean): Color =
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)

@Composable
private fun GlassCard(modifier: Modifier = Modifier, isDarkTheme: Boolean, content: @Composable () -> Unit) {
    val borderCol = if (isDarkTheme) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            )
            .clip(RoundedCornerShape(24.dp))
            .background(glassSurface(isDarkTheme))
            .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconCircleBg(isDarkTheme)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint(isDarkTheme), modifier = Modifier.size(22.dp))
        }
        Text(
            title,
            style = ProfileDesignTypography.HeadlineMd,
            color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
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
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pageBg = lightBg(isDarkTheme)

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

    Scaffold(
        containerColor = pageBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier = Modifier.shadow(1.dp, spotColor = Color.Black.copy(alpha = 0.06f)),
            ) {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                "Profile",
                                style = ProfileDesignTypography.HeadlineMd,
                                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Your identity and study",
                                style = ProfileDesignTypography.BodyMd,
                                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onHome) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = navIconTint(isDarkTheme))
                        }
                        IconButton(onClick = onToggleDarkTheme) {
                            Icon(
                                if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Nightlight,
                                contentDescription = "Theme",
                                tint = navIconTint(isDarkTheme),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }
        },
        bottomBar = {
            ProfileBottomNavigation(
                isDarkTheme = isDarkTheme,
                onHome = onHome,
                onLibrary = onLibrary,
                onProgress = onProgress,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ProfileHeaderCard(uiState = uiState, isDarkTheme = isDarkTheme)

            PersonalInfoSection(uiState = uiState, viewModel = viewModel, isDarkTheme = isDarkTheme)

            ExamFocusSection(uiState = uiState, viewModel = viewModel, isDarkTheme = isDarkTheme)

            AccountStatusSection(isDarkTheme = isDarkTheme)

            if (uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = ProfileDesignTypography.BodyMd,
                        modifier = Modifier.padding(14.dp),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ActionsSection(
                isSaving = uiState.isSaving,
                isDarkTheme = isDarkTheme,
                onLogoutClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                onSaveClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
            )

            FooterSection(isDarkTheme = isDarkTheme)

            Spacer(Modifier.height(8.dp))
        }
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout? You will need to sign in again to access your sanctuary.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.logout { onLogout() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) }) {
                    Text("Stay Here", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }
}

@Composable
private fun navIconTint(isDarkTheme: Boolean): Color =
    if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun ProfileHeaderCard(uiState: ProfileUiState, isDarkTheme: Boolean) {
    GlassCard(isDarkTheme = isDarkTheme) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatarRing = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg(isDarkTheme))
                        .border(4.dp, avatarRing, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        uiState.userName.firstOrNull()?.uppercase() ?: "U",
                        style = ProfileDesignTypography.HeadlineXl,
                        color = iconTint(isDarkTheme),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary)
                        .border(2.dp, avatarRing, CircleShape)
                        .clickable { /* photo picker TBD */ },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    uiState.userName.ifEmpty { "User" },
                    style = ProfileDesignTypography.HeadlineLg,
                    color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    uiState.userEmail,
                    style = ProfileDesignTypography.BodyLg,
                    color = subtleText(isDarkTheme),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                val onlineBg =
                    if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.primaryContainer
                val onlineDot =
                    if (isDarkTheme) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    else com.safar.app.ui.theme.Emerald600
                val onlineFg =
                    if (isDarkTheme) MaterialTheme.colorScheme.onPrimaryContainer
                    else com.safar.app.ui.theme.Emerald600
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(onlineBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(onlineDot),
                    )
                    Text("ONLINE", style = ProfileDesignTypography.LabelCaps, color = onlineFg)
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(uiState: ProfileUiState, viewModel: ProfileViewModel, isDarkTheme: Boolean) {
    GlassCard(isDarkTheme = isDarkTheme) {
        Column(modifier = Modifier.padding(24.dp)) {
            SectionHeader(Icons.Default.Person, "Personal Information", isDarkTheme)
            Spacer(Modifier.height(24.dp))
            CustomTextField(
                label = "FULL NAME",
                value = uiState.editName,
                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
                errorText = uiState.nameError,
                isDarkTheme = isDarkTheme,
            )
            Spacer(Modifier.height(24.dp))
            CustomTextField(
                label = "EMAIL ADDRESS",
                value = uiState.userEmail,
                onValueChange = {},
                enabled = false,
                helperText = "Contact support to update your primary email address.",
                isDarkTheme = isDarkTheme,
            )
            Spacer(Modifier.height(24.dp))
            CustomDropdownMenu(
                label = "GENDER",
                options = genderOptions,
                selectedOption = uiState.editGender.ifEmpty { "Select gender" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) },
                isDarkTheme = isDarkTheme,
            )
        }
    }
}

@Composable
private fun ExamFocusSection(uiState: ProfileUiState, viewModel: ProfileViewModel, isDarkTheme: Boolean) {
    GlassCard(isDarkTheme = isDarkTheme) {
        Column(modifier = Modifier.padding(24.dp)) {
            SectionHeader(Icons.Default.School, "Exam Focus", isDarkTheme)
            Spacer(Modifier.height(24.dp))
            CustomDropdownMenu(
                label = "TARGET EXAM",
                options = examOptions,
                selectedOption = uiState.editExamType.ifEmpty { "Select exam" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
                isDarkTheme = isDarkTheme,
            )
            Spacer(Modifier.height(24.dp))
            CustomDropdownMenu(
                label = "PREPARATION STAGE",
                options = stageOptions,
                selectedOption = uiState.editStage.ifEmpty { "Select stage" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
                isDarkTheme = isDarkTheme,
            )
        }
    }
}

@Composable
private fun AccountStatusSection(isDarkTheme: Boolean) {
    GlassCard(isDarkTheme = isDarkTheme) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconCircleBg(isDarkTheme)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = iconTint(isDarkTheme),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Account Status",
                        style = ProfileDesignTypography.HeadlineMd,
                        color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Your account is verified and secured.",
                        style = ProfileDesignTypography.BodyMd,
                        color = subtleText(isDarkTheme),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "VERIFIED",
                style = ProfileDesignTypography.LabelCaps,
                color = iconTint(isDarkTheme),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconCircleBg(isDarkTheme))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ActionsSection(
    isSaving: Boolean,
    isDarkTheme: Boolean,
    onLogoutClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val primaryBtn = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDarkTheme) MaterialTheme.colorScheme.error.copy(alpha = 0.65f) else ProfileDesignColors.ErrorContainer,
            ),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Logout",
                style = ProfileDesignTypography.HeadlineMd.copy(fontSize = 16.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryBtn,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Save Changes",
                    style = ProfileDesignTypography.HeadlineMd.copy(fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun FooterSection(isDarkTheme: Boolean) {
    val outline = footerMuted(isDarkTheme)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "For any technical or app related queries mail at\nonesafar@gmail.com • safarparmar0@gmail.com",
            style = ProfileDesignTypography.BodyMd,
            color = outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "© 2026 SAFAR • Version 1.0.4",
            style = ProfileDesignTypography.BodyMd,
            color = outline.copy(alpha = 0.6f),
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
    val container = if (isDarkTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.92f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    val items = listOf(
        Triple("Home", Icons.Default.Home, onHome),
        Triple("Library", Icons.AutoMirrored.Filled.MenuBook, onLibrary),
        Triple("Progress", Icons.Default.BarChart, onProgress),
        Triple("Profile", Icons.Default.Person, { }),
    )
    NavigationBar(
        containerColor = container,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
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
                        tint = if (selected) {
                            if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                        } else {
                            navIconTint(isDarkTheme)
                        },
                    )
                },
                label = {
                    Text(
                        text = label.uppercase(),
                        style = ProfileDesignTypography.LabelSm.copy(fontSize = 10.sp, letterSpacing = 0.3.sp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ProfileDesignColors.PrimaryFixed.copy(alpha = if (isDarkTheme) 0.35f else 0.5f),
                    selectedIconColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    selectedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    unselectedIconColor = navIconTint(isDarkTheme),
                    unselectedTextColor = navIconTint(isDarkTheme),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    helperText: String? = null,
    errorText: String? = null,
    isDarkTheme: Boolean,
) {
    val hasError = !errorText.isNullOrBlank()
    Column {
        Text(text = label, style = ProfileDesignTypography.LabelCaps, color = labelCapsColor(isDarkTheme))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = false,
            enabled = enabled,
            isError = hasError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = ProfileDesignTypography.BodyLg,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                disabledTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = borderUnfocused(isDarkTheme),
                errorBorderColor = MaterialTheme.colorScheme.error,
                disabledBorderColor = borderDisabled(isDarkTheme),
                focusedContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface,
                disabledContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        if (hasError) {
            Spacer(Modifier.height(6.dp))
            Text(errorText!!, color = MaterialTheme.colorScheme.error, style = ProfileDesignTypography.BodyMd, modifier = Modifier.heightIn(min = 16.dp))
        }
        if (helperText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = helperText,
                style = ProfileDesignTypography.BodyMd.copy(fontSize = 12.sp),
                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f) else ProfileDesignColors.Outline,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    isDarkTheme: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = label, style = ProfileDesignTypography.LabelCaps, color = labelCapsColor(isDarkTheme))
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                shape = RoundedCornerShape(16.dp),
                textStyle = ProfileDesignTypography.BodyLg,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = borderUnfocused(isDarkTheme),
                    focusedContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface,
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                style = ProfileDesignTypography.BodyLg,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
