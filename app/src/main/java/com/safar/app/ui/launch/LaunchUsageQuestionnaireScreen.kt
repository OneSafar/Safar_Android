package com.safar.app.ui.launch

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.data.local.SafarDataStore
import com.safar.app.notifications.rememberNotificationPermissionRequester
import com.safar.app.ui.ekagra.focusshield.FocusShieldConsentDialog
import com.safar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchUsageQuestionnaireScreen(
    dataStore: SafarDataStore,
    onNavigateHome: () -> Unit,
    onUnauthorized: () -> Unit,
    viewModel: LaunchUsageQuestionnaireViewModel = hiltViewModel(),
) {
    val isLoggedIn by dataStore.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false) onUnauthorized()
    }

    if (isLoggedIn != true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reasons = listOf(
        "I want to study without distractions",
        "I want to track my goals and stay consistent",
        "I want to journal my thoughts and feelings",
        "I want to manage my stress and mental wellness",
        "All of the above"
    )

    // Indices for focus setup: 0, 1, 4 (Study, Goals, All)
    val needsFocusSetup = remember(uiState.selectedReasons) {
        uiState.selectedReasons.any { it in setOf(0, 1, 4) }
    }

    var page by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<String?>(null) }

    var hasUsage by remember { mutableStateOf(FocusShieldPermissionHelper.hasUsageStatsPermission(context)) }
    var hasA11y by remember { mutableStateOf(FocusShieldPermissionHelper.hasAccessibilityService(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasA11y = FocusShieldPermissionHelper.hasAccessibilityService(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showShieldConsent by remember { mutableStateOf(false) }
    var showUsageConsent by remember { mutableStateOf(false) }
    var showA11yConsent by remember { mutableStateOf(false) }
    var awaitingUsageGrant by remember { mutableStateOf(false) }
    var awaitingA11yGrant by remember { mutableStateOf(false) }

    var strictPipelineAfterNotif by remember { mutableStateOf(false) }

    fun finishStandard() {
        viewModel.markQuestionnaireFinished(AppUsageMode.STANDARD, onNavigateHome)
    }

    fun finishFocused() {
        viewModel.markQuestionnaireFinished(AppUsageMode.FOCUSED, onNavigateHome)
    }

    fun enableShieldAndFinishFocused() {
        viewModel.setFocusShieldEnabled(true)
        finishFocused()
    }

    val requestNotification = rememberNotificationPermissionRequester { _ ->
        if (strictPipelineAfterNotif) {
            strictPipelineAfterNotif = false
            showShieldConsent = true
        } else {
            finishStandard()
        }
    }

    fun onFinishQuestionnaire() {
        val mode = selectedMode ?: (if (needsFocusSetup) AppUsageMode.FOCUSED else AppUsageMode.STANDARD)
        if (mode == AppUsageMode.STANDARD) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotification()
            } else {
                finishStandard()
            }
            return
        }
        strictPipelineAfterNotif = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotification()
        } else {
            strictPipelineAfterNotif = false
            showShieldConsent = true
        }
    }

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF1A1C1F) else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) Color(0xFF2D3139) else Color(0xFFD6DEE8)

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Welcome", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (page > 0) {
                        IconButton(onClick = { page-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = scheme.onBackground,
                    navigationIconContentColor = scheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val totalPages = 3
                repeat(totalPages) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (i <= page) scheme.primary else scheme.outline.copy(alpha = 0.35f)),
                    )
                }
            }

            when (page) {
                0 -> {
                    Text(
                        "What brings you to SAFAR today?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onBackground,
                    )
                    Text(
                        "Select all that apply to personalize your experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    reasons.forEachIndexed { index, reason ->
                        ReasonCard(
                            text = reason,
                            selected = index in uiState.selectedReasons,
                            cardBg = cardBg,
                            border = cardBorder,
                            accent = scheme.primary,
                            onClick = { viewModel.toggleReason(index) }
                        )
                    }
                }
                1 -> {
                    Text(
                        "How would you like to use SAFAR?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onBackground,
                    )
                    Text(
                        "Beast mode shows Kavach setup and asks you before opening every required permission screen. Standard mode only asks for notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ModeCard(
                        title = "Beast Mode",
                        subtitle = "Kavach with clear App Usage Permission, Ekagra Mode Accessibility, and notification prompts. You decide what to grant.",
                        icon = Icons.Default.Shield,
                        selected = selectedMode == AppUsageMode.FOCUSED,
                        accent = scheme.primary,
                        cardBg = cardBg,
                        border = cardBorder,
                        onClick = { selectedMode = AppUsageMode.FOCUSED },
                    )
                    ModeCard(
                        title = "Normal",
                        subtitle = "Notifications only. You can enable Kavach later from the menu.",
                        icon = Icons.Default.Notifications,
                        selected = selectedMode == AppUsageMode.STANDARD,
                        accent = scheme.tertiary,
                        cardBg = cardBg,
                        border = cardBorder,
                        onClick = { selectedMode = AppUsageMode.STANDARD },
                    )
                }
                2 -> {
                    PermissionRationalePage(
                        onSetItUp = { onFinishQuestionnaire() },
                        onSkip = { 
                            selectedMode = AppUsageMode.STANDARD
                            onFinishQuestionnaire()
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            if (page < 2) {
                Button(
                    onClick = { 
                        if (page == 0) {
                            page = 1
                        } else if (page == 1) {
                            if (selectedMode == AppUsageMode.FOCUSED || (selectedMode == null && needsFocusSetup)) {
                                if (selectedMode == null) selectedMode = AppUsageMode.FOCUSED
                                page = 2
                            } else {
                                onFinishQuestionnaire()
                            }
                        }
                    },
                    enabled = when(page) {
                        0 -> uiState.selectedReasons.isNotEmpty()
                        1 -> selectedMode != null
                        else -> true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Next", fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showShieldConsent) {
        FocusShieldConsentDialog(
            title = "Enable Kavach?",
            body = "Kavach is optional and user-controlled. SAFAR will not access App Usage Permission or Accessibility unless you grant those permissions in Android settings.\n\nIf you enable it and grant permissions, SAFAR uses Accessibility only while a focus timer or Study Session is running to detect whether an app you selected has opened. If a selected app opens, SAFAR shows its own block screen and a notification so you can return to your focus session.\n\nSAFAR does not read messages, passwords, typed text, contacts, photos, or screen content. It does not click buttons, change settings, prevent uninstall, or control your device. Your blocked app choices stay on this device. You can turn Kavach off at any time.",
            confirmText = "I Agree",
            onDismiss = {
                showShieldConsent = false
                finishFocused()
            },
            onConfirm = {
                showShieldConsent = false
                when {
                    !FocusShieldPermissionHelper.hasUsageStatsPermission(context) -> showUsageConsent = true
                    !FocusShieldPermissionHelper.hasAccessibilityService(context) -> showA11yConsent = true
                    else -> enableShieldAndFinishFocused()
                }
            },
        )
    }

    if (showUsageConsent) {
        FocusShieldConsentDialog(
            title = "App Usage Permission",
            body = "This opens Android's App Usage Permission settings for SAFAR. Grant it only if you want Kavach to be available.\n\nSAFAR uses this only for Kavach setup checks and does not activate blocking unless you also enable Kavach and grant the SAFAR Ekagra Mode Accessibility service. You can skip this and continue.",
            confirmText = "Open settings",
            onDismiss = {
                showUsageConsent = false
                if (!FocusShieldPermissionHelper.hasAccessibilityService(context)) showA11yConsent = true
                else finishFocused()
            },
            onConfirm = {
                showUsageConsent = false
                awaitingUsageGrant = true
                FocusShieldPermissionHelper.openUsageAccessSettings(context)
            },
        )
    }

    if (showA11yConsent) {
        FocusShieldConsentDialog(
            title = "Ekagra Mode Accessibility",
            body = "This opens Android Accessibility settings for the service named “SAFAR Ekagra Mode Accessibility”. Grant it only if you want SAFAR to block selected apps during focus timers and Study Sessions.\n\nWhat SAFAR uses it for: detecting opened app package names while a focus timer or Study Session is running.\n\nWhat SAFAR does not do: read messages, passwords, typed text, contacts, photos, or screen content; click buttons; change settings; prevent uninstall; or control your device. If you skip, Kavach stays off and you can enable it later from the menu.",
            confirmText = "Open Settings",
            onDismiss = {
                showA11yConsent = false
                finishFocused()
            },
            onConfirm = {
                showA11yConsent = false
                awaitingA11yGrant = true
                FocusShieldPermissionHelper.openAccessibilitySettings(context)
            },
        )
    }

    LaunchedEffect(hasUsage, awaitingUsageGrant) {
        if (awaitingUsageGrant && FocusShieldPermissionHelper.hasUsageStatsPermission(context)) {
            awaitingUsageGrant = false
            if (!FocusShieldPermissionHelper.hasAccessibilityService(context)) showA11yConsent = true
            else enableShieldAndFinishFocused()
        }
    }

    LaunchedEffect(hasA11y, awaitingA11yGrant) {
        if (awaitingA11yGrant && FocusShieldPermissionHelper.hasAccessibilityService(context)) {
            awaitingA11yGrant = false
            if (FocusShieldPermissionHelper.hasUsageStatsPermission(context)) {
                enableShieldAndFinishFocused()
            } else {
                finishFocused()
            }
        }
    }
}

@Composable
private fun ReasonCard(
    text: String,
    selected: Boolean,
    cardBg: Color,
    border: Color,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, if (selected) accent else border, RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.08f) else cardBg)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, if (selected) accent else border, RoundedCornerShape(6.dp))
                .background(if (selected) accent else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    cardBg: Color,
    border: Color,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, if (selected) accent else border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else cardBg)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = scheme.onBackground)
            Text(subtitle, fontSize = 13.sp, color = scheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PermissionRationalePage(
    onSetItUp: () -> Unit,
    onSkip: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Focus Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onBackground,
        )
        
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.primaryContainer.copy(alpha = 0.3f))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = scheme.primary)
                    Text(
                        "To help you stay focused, SAFAR needs to know which apps you're using. This helps us gently remind you if you drift away during study time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface
                    )
                }
                
                Text(
                    "Don't worry, we only use this to support your focus. Your data is never shared.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        
        Button(
            onClick = onSetItUp,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Okay, let's set it up", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'll do this later", color = scheme.onSurfaceVariant)
        }
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            "Note: You can change this anytime from Settings or SAFAR settings.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
