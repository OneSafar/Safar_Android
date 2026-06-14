package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.safarparmar.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachOnboardingScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scheme = MaterialTheme.colorScheme

    val shieldState by viewModel.shieldState.collectAsStateWithLifecycle()
    var hasUsageStats by remember { mutableStateOf(shieldState.hasUsageStats) }
    var hasAccessibility by remember { mutableStateOf(shieldState.hasAccessibilityService) }
    var hasNotifications by remember { mutableStateOf(shieldState.hasNotifications) }
    val accessibilityRequired = FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()

    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    var selectedPermission by remember { mutableStateOf<PermissionTarget?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasAccessibility = FocusShieldPermissionHelper.hasAccessibilityService(context)
                hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-finish if all required permissions are granted
    LaunchedEffect(hasUsageStats, hasAccessibility) {
        if (hasUsageStats && (!accessibilityRequired || hasAccessibility)) {
            // Only set to enabled if they completed the flow
            viewModel.setEnabled(true)
            onFinished()
        }
    }

    val totalSteps = if (accessibilityRequired) 3 else 2
    var grantedCount = 0
    if (hasUsageStats) grantedCount++
    if (hasAccessibility && accessibilityRequired) grantedCount++
    if (hasNotifications) grantedCount++
    
    // Prevent over-filling progress bar if they grant notifications but it wasn't required
    val progress = (grantedCount.toFloat() / totalSteps).coerceAtMost(1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val isUsageNext = !hasUsageStats
    val isAccessibilityNext = hasUsageStats && accessibilityRequired && !hasAccessibility
    val isNotificationsNext = hasUsageStats && (!accessibilityRequired || hasAccessibility) && !hasNotifications

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF111111) // Match Regain's dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color(0xFF333333),
                strokeCap = StrokeCap.Round,
            )

            // Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                
                // Mascot & Chat Bubble
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mascot Avatar
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_safar_launcher_foreground), // Fallback to app icon
                            contentDescription = "Mascot",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    // Chat Bubble
                    Surface(
                        color = Color(0xFF222222),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Almost there, allow these permissions to focus alongside thousands of students now!",
                            color = Color(0xFFCCCCCC),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Permissions List
                KavachRegainPermissionRow(
                    title = "Usage permission",
                    subtitle = "This allows us to track your app usage.",
                    granted = hasUsageStats,
                    isNext = isUsageNext,
                    onClick = { if (isUsageNext) selectedPermission = PermissionTarget.USAGE_STATS }
                )
                
                HorizontalDivider(color = Color(0xFF2A2A2A))

                if (accessibilityRequired) {
                    KavachRegainPermissionRow(
                        title = "Block screen permission",
                        subtitle = "Lets us show the shield screen over blocked apps.",
                        granted = hasAccessibility,
                        isNext = isAccessibilityNext,
                        onClick = { if (isAccessibilityNext) selectedPermission = PermissionTarget.ACCESSIBILITY }
                    )
                    HorizontalDivider(color = Color(0xFF2A2A2A))
                }

                KavachRegainPermissionRow(
                    title = "Background permission",
                    subtitle = "Keeps KAVACH running smoothly in the background.",
                    granted = hasNotifications,
                    isNext = isNotificationsNext,
                    onClick = {
                        if (isNotificationsNext) {
                            requestNotificationPermission()
                        }
                    }
                )
            }
            
            // Fixed Bottom Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Why should I give this permission? Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A3311)) // Dark green
                        .clickable { selectedPermission = PermissionTarget.USAGE_STATS } // Show info for first
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HelpOutline, 
                        contentDescription = null, 
                        tint = Color(0xFF4ADE80), // Bright green
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Why should I give this permission?",
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight, 
                        contentDescription = null, 
                        tint = Color(0xFF4ADE80), 
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(Modifier.height(20.dp))
                
                Text(
                    text = "Trusted by 2M+ students ❤️",
                    color = Color(0xFF888888),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (selectedPermission != null) {
            KavachRegainExplanationSheet(
                permission = selectedPermission!!,
                onDismiss = { selectedPermission = null },
                onAllow = {
                    selectedPermission = null
                    scope.launch {
                        when (it) {
                            PermissionTarget.USAGE_STATS -> {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                            PermissionTarget.ACCESSIBILITY -> {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                            PermissionTarget.NOTIFICATIONS -> {
                                requestNotificationPermission()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun KavachRegainPermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    isNext: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (granted || isNext) 1f else 0.4f
    val titleColor = if (granted || isNext) Color.White else Color(0xFF888888)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isNext && !granted, onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            if (isNext && !granted) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    lineHeight = 20.sp
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        
        if (granted) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Granted",
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(24.dp)
            )
        } else if (isNext) {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(99.dp),
                color = Color.White,
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Allow", 
                        fontWeight = FontWeight.Medium, 
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KavachRegainExplanationSheet(
    permission: PermissionTarget,
    onDismiss: () -> Unit,
    onAllow: (PermissionTarget) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    val title = when (permission) {
        PermissionTarget.USAGE_STATS -> "Allow usage permission to find your phone usage"
        PermissionTarget.ACCESSIBILITY -> "Allow accessibility permission to block apps"
        PermissionTarget.NOTIFICATIONS -> "Allow notifications for status"
    }

    val bullets = when (permission) {
        PermissionTarget.USAGE_STATS -> listOf(
            "We never sell data we get from permissions.",
            "We collect & store Installed Application data to help reduce your screen time.",
            "We receive analytics data to improve the app for you."
        )
        PermissionTarget.ACCESSIBILITY -> listOf(
            "We do not read your private messages or screen content.",
            "We only monitor when you open an app you chose to block.",
            "This enables the full-screen shield overlay."
        )
        PermissionTarget.NOTIFICATIONS -> listOf(
            "Receive alerts when focus timer finishes.",
            "Keep KAVACH service alive in the background."
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E), // Dark gray match
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 30.sp
            )
            
            Spacer(Modifier.height(20.dp))

            // Safe badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A3311))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Is it safe to give this permission?",
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(28.dp))

            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "✦", // Sparkle char instead of icon
                        color = Color(0xFF555555),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = bullet,
                        color = Color(0xFFAAAAAA),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = { onAllow(permission) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(99.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Allow",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Trusted by 2M+ students ❤️",
                color = Color(0xFF888888),
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
