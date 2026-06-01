package com.safarparmar.app.ui.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.ui.theme.*

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
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
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

    Scaffold(
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = scheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.shadow(1.dp, spotColor = Color.Black.copy(alpha = 0.06f)),
            ) {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Your identity and study",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = scheme.primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onHome) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = scheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onToggleDarkTheme) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Nightlight,
                                contentDescription = "Theme",
                                tint = scheme.onSurfaceVariant,
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
            ProfileHeaderCard(uiState = uiState)

            PersonalInfoSection(uiState = uiState, viewModel = viewModel)

            ExamFocusSection(uiState = uiState, viewModel = viewModel)

            AccountStatusSection()

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

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = scheme.error) },
            title = { Text("Confirm Logout", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("Are you sure you want to logout? You will need to sign in again to access your sanctuary.") },
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

@Composable
private fun ProfileHeaderCard(uiState: ProfileUiState) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
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
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = uiState.userName.ifEmpty { "User" },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = uiState.userEmail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(scheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(scheme.primary),
                    )
                    Text(
                        text = "ONLINE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    GlassCard {
        Column(modifier = Modifier.padding(24.dp)) {
            SectionHeader(Icons.Default.Person, "Personal Information")
            Spacer(Modifier.height(24.dp))
            CustomTextField(
                label = "FULL NAME",
                value = uiState.editName,
                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
                errorText = uiState.nameError,
            )
            Spacer(Modifier.height(24.dp))
            CustomTextField(
                label = "EMAIL ADDRESS",
                value = uiState.userEmail,
                onValueChange = {},
                enabled = false,
                helperText = "Contact support to update your primary email address.",
            )
            Spacer(Modifier.height(24.dp))
            CustomDropdownMenu(
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
    GlassCard {
        Column(modifier = Modifier.padding(24.dp)) {
            SectionHeader(Icons.Default.School, "Exam Focus")
            Spacer(Modifier.height(24.dp))
            CustomDropdownMenu(
                label = "TARGET EXAM",
                options = examOptions,
                selectedOption = uiState.editExamType.ifEmpty { "Select exam" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
            )
            Spacer(Modifier.height(24.dp))
            CustomDropdownMenu(
                label = "PREPARATION STAGE",
                options = stageOptions,
                selectedOption = uiState.editStage.ifEmpty { "Select stage" },
                onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
            )
        }
    }
}

@Composable
private fun AccountStatusSection() {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
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
                        .background(scheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Account Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Your account is verified and secured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "VERIFIED",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(scheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
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
                .heightIn(min = 56.dp),
            shape = ButtonDefaults.outlinedShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
            border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.5f)),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onSaveClick,
            enabled = !isSaving,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .shadow(8.dp, ButtonDefaults.shape, spotColor = scheme.primary.copy(alpha = 0.35f)),
            shape = ButtonDefaults.shape,
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
                    color = scheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    helperText: String? = null,
    errorText: String? = null,
) {
    val hasError = !errorText.isNullOrBlank()
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = false,
            enabled = enabled,
            isError = hasError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = OutlinedTextFieldDefaults.shape,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = scheme.onSurface,
                unfocusedTextColor = scheme.onSurface,
                disabledTextColor = scheme.onSurfaceVariant,
                cursorColor = scheme.primary,
                focusedBorderColor = scheme.primary,
                unfocusedBorderColor = scheme.outline,
                errorBorderColor = scheme.error,
                disabledBorderColor = scheme.outline.copy(alpha = 0.38f),
                focusedContainerColor = scheme.surface,
                unfocusedContainerColor = scheme.surface,
                disabledContainerColor = scheme.surfaceVariant,
            ),
        )
        if (hasError) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = errorText!!,
                color = scheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.heightIn(min = 16.dp)
            )
        }
        if (helperText != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.outline,
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
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.primary
        )
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
                shape = OutlinedTextFieldDefaults.shape,
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = scheme.onSurface,
                    unfocusedTextColor = scheme.onSurface,
                    cursorColor = scheme.primary,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outline,
                    focusedContainerColor = scheme.surface,
                    unfocusedContainerColor = scheme.surface,
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(scheme.surface),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
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
