package com.safarparmar.app.ui.profile

import android.widget.Toast
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.ui.components.*

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
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

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

    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Your identity and study",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 12.sp
                                ),
                                color = scheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = scheme.primary,
                            )
                        }
                    },
                    actions = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = scheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = scheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background)
            ) {
                val gradientStart = if (isDarkTheme) Color(0xFF1B212D) else Color(0xFFD6E9FF)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(gradientStart, Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ProfileHeaderCard(uiState = uiState)

                    PersonalInfoSection(uiState = uiState, viewModel = viewModel)

                    ExamFocusSection(uiState = uiState, viewModel = viewModel)

                    AccountStatusSection(onPremiumClick = onPremium)

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
    }
}

@Composable
private fun ProfileHeaderCard(uiState: ProfileUiState) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatarRing = scheme.surface
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(scheme.primaryContainer)
                        .border(4.dp, avatarRing, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.userName.firstOrNull()?.uppercase() ?: "U",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = scheme.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .border(2.dp, avatarRing, CircleShape)
                        .clickable { /* photo picker TBD */ },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = uiState.userEmail,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp
                    ),
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = scheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SafarCustomTextField(
            label = "FULL NAME",
            value = uiState.editName,
            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
            errorText = uiState.nameError,
        )
        SafarCustomTextField(
            label = "EMAIL ADDRESS",
            value = uiState.userEmail,
            onValueChange = {},
            enabled = false,
            helperText = "Contact support to update your primary email address.",
        )
        SafarCustomDropdownMenu(
            label = "GENDER",
            options = genderOptions,
            selectedOption = uiState.editGender.ifEmpty { "Select gender" },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) },
        )
    }
}

@Composable
private fun ExamFocusSection(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Exam Focus",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = scheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SafarCustomDropdownMenu(
            label = "TARGET EXAM",
            options = examOptions,
            selectedOption = uiState.editExamType.ifEmpty { "Select exam" },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
        )
        SafarCustomDropdownMenu(
            label = "PREPARATION STAGE",
            options = stageOptions,
            selectedOption = uiState.editStage.ifEmpty { "Select stage" },
            onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
        )
    }
}

@Composable
private fun AccountStatusSection(onPremiumClick: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Account Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
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
                        text = "Verified Student",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF15803D)
                    )
                }
            }
            Button(
                onClick = onPremiumClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Plans", fontWeight = FontWeight.Bold)
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
    val scheme = MaterialTheme.colorScheme
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
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
            border = BorderStroke(1.5.dp, scheme.error),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = scheme.surfaceVariant,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun FooterSection() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "For any technical or app related queries mail at\nonesafar@gmail.com • safarparmar0@gmail.com",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "© 2026 SAFAR • Version 1.0.4",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
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
    val scheme = MaterialTheme.colorScheme
    val items = listOf(
        Triple("Home", Icons.Default.Home, onHome),
        Triple("Library", Icons.AutoMirrored.Filled.MenuBook, onLibrary),
        Triple("Progress", Icons.Default.BarChart, onProgress),
        Triple("Profile", Icons.Default.Person, { }),
    )
    NavigationBar(
        containerColor = scheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .clip(MaterialTheme.shapes.large),
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
                        tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
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
                    indicatorColor = scheme.primaryContainer,
                    selectedIconColor = scheme.primary,
                    selectedTextColor = scheme.primary,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant,
                ),
            )
        }
    }
}
