package com.safarparmar.app.ui.premium

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.razorpay.Checkout
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private data class PremiumPlanOption(
    val id: String,
    val label: String,
    val price: Int,
    val subtitle: String,
    val durationLabel: String,
    val durationMonths: Int,
    val courseId: String,
    val badge: String? = null,
    val discountLabel: String? = null,
)

private data class PremiumColors(
    val background: Color,
    val cardBody: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderBrush: Brush,
    val macBlue: Color,
    val macGold: Color,
    val macGreen: Color,
    val danger: Color,
    val isDark: Boolean,
)

// --- macOS Control Center Card Component ---
@Composable
private fun MacOSControlCard(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    isSelected: Boolean = false,
    selectedAccentColor: Color = Color(0xFF0A84FF),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val bodyColor = if (isSelected) {
        if (isDarkTheme) selectedAccentColor.copy(alpha = 0.20f) else selectedAccentColor.copy(alpha = 0.08f)
    } else {
        if (isDarkTheme) Color(0xFF2C2C2E).copy(alpha = 0.65f) else Color(0xFFF9F9FB)
    }

    val borderBrush = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(selectedAccentColor, selectedAccentColor.copy(alpha = 0.5f))
        )
    } else if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }

    val shadowElevation = if (isDarkTheme) 12.dp else 4.dp
    val shadowColor = if (isDarkTheme) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)
    val borderWidth = if (isSelected) 1.5.dp else 0.5.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor
            )
            .clip(shape)
            .background(bodyColor)
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val premiumStatus by viewModel.premiumStatus.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val colors = rememberPremiumColors(isDarkTheme)
    var refreshAfterPaymentReturn by remember { mutableStateOf(false) }
    var showTrialConfirmation by remember { mutableStateOf(false) }

    val plans = remember {
        listOf(
            PremiumPlanOption(
                id = "3month",
                label = "3 Months",
                price = 79,
                subtitle = "Start small for your next target",
                durationLabel = "3 months",
                durationMonths = 3,
                courseId = "study-planner-pro-3month",
                discountLabel = "Starter",
            ),
            PremiumPlanOption(
                id = "6month",
                label = "6 Months",
                price = 99,
                subtitle = "Best value for full exam cycle",
                durationLabel = "6 months",
                durationMonths = 6,
                courseId = "study-planner-pro-6month",
                badge = "POPULAR",
                discountLabel = "Popular",
            ),
        )
    }
    var selectedPlanId by remember { mutableStateOf("6month") }
    var selectedPlanDuration by remember { mutableStateOf(6) }
    var currentExpiryDate by remember { mutableStateOf<String?>(null) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId } ?: plans.last()
    val isLoading = uiState is PremiumUiState.Loading

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && refreshAfterPaymentReturn) {
                refreshAfterPaymentReturn = false
                scope.launch {
                    delay(1_500)
                    viewModel.refreshPremiumStatus(
                        showLoading = true,
                        fallbackError = "Payment returned, but Safar Premium is not active yet. Please tap Restore Safar Premium."
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState) {
        if (uiState is PremiumUiState.OrderCreated) {
            val state = uiState as PremiumUiState.OrderCreated
            try {
                if (activity == null) {
                    viewModel.notifyPaymentFailed("Checkout needs an active screen. Please try again.")
                    return@LaunchedEffect
                }

                val checkout = Checkout()
                checkout.setKeyID(state.keyId ?: "rzp_live_SWHJBT7AXadF8a")

                val options = JSONObject()
                options.put("name", "Safar")
                options.put("description", "Safar Premium")
                options.put("theme.color", "#0A84FF")
                options.put("currency", state.order.currency)
                options.put("amount", state.order.amount)
                options.put("order_id", state.order.id)

                val retryObj = JSONObject()
                retryObj.put("enabled", true)
                retryObj.put("max_count", 4)
                options.put("retry", retryObj)

                refreshAfterPaymentReturn = true
                checkout.open(activity, options)
                viewModel.resetState()
            } catch (e: Exception) {
                e.printStackTrace()
                refreshAfterPaymentReturn = false
                viewModel.notifyPaymentFailed(e.message ?: "Error launching checkout")
            }
        }
    }

    val activeStatus = (uiState as? PremiumUiState.PaymentSuccess)?.status ?: premiumStatus
    val isPremiumActive = activeStatus.hasAnyPaidAccess
    val formattedExpiry = remember(activeStatus.expiresAt) { formatPremiumExpiry(activeStatus.expiresAt) }
    val planLabel = remember(activeStatus.planType) { premiumPlanLabel(activeStatus.planType) }
    val newExpiryAfterPurchase = remember(currentExpiryDate, selectedPlanDuration) {
        calculatePremiumExtensionExpiry(currentExpiryDate, selectedPlanDuration)
    }
    val formattedNewExpiry = remember(newExpiryAfterPurchase) { formatPremiumExpiry(newExpiryAfterPurchase) }
    val updateSelectedPlan: (String) -> Unit = { planId ->
        selectedPlanId = planId
        selectedPlanDuration = plans.firstOrNull { it.id == planId }?.durationMonths ?: selectedPlanDuration
    }

    LaunchedEffect(activeStatus.expiresAt) {
        currentExpiryDate = activeStatus.expiresAt
    }

    if (uiState is PremiumUiState.PaymentSuccess) {
        PremiumUnlockedDialog(
            state = uiState as PremiumUiState.PaymentSuccess,
            onDismiss = viewModel::resetState,
        )
    }

    if (showTrialConfirmation) {
        StartTrialConfirmationDialog(
            colors = colors,
            isDarkTheme = isDarkTheme,
            onDismiss = { showTrialConfirmation = false },
            onConfirm = {
                showTrialConfirmation = false
                viewModel.startFreeTrial()
            },
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            PremiumTopBar(
                colors = colors,
                onBack = onBack,
            )
        },
        bottomBar = {
            PremiumBottomBar(
                selectedPlan = selectedPlan,
                isPremiumActive = isPremiumActive,
                isLoading = isLoading,
                colors = colors,
                isDarkTheme = isDarkTheme,
                onPurchase = {
                    viewModel.createOrder(
                        duration = selectedPlan.durationMonths,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (isPremiumActive) {
                PremiumActiveContent(
                    planLabel = planLabel,
                    expiryText = formattedExpiry,
                    plans = plans,
                    selectedPlanId = selectedPlanId,
                    selectedPlan = selectedPlan,
                    currentExpiryText = formattedExpiry,
                    newExpiryText = formattedNewExpiry,
                    isLoading = isLoading,
                    colors = colors,
                    isDarkTheme = isDarkTheme,
                    onSelectPlan = updateSelectedPlan,
                    onRestore = { viewModel.refreshPremiumStatus() },
                )
            } else {
                PremiumUpgradeContent(
                    plans = plans,
                    selectedPlanId = selectedPlanId,
                    selectedPlan = selectedPlan,
                    currentExpiryText = formattedExpiry,
                    newExpiryText = formattedNewExpiry,
                    uiState = uiState,
                    isLoading = isLoading,
                    colors = colors,
                    isDarkTheme = isDarkTheme,
                    onSelectPlan = updateSelectedPlan,
                    onStartTrial = { showTrialConfirmation = true },
                    onRestore = { viewModel.refreshPremiumStatus() },
                )
            }
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumTopBar(
    colors: PremiumColors,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "Safar Premium",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textPrimary,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.background,
            scrolledContainerColor = colors.background,
        ),
    )
}

@Composable
private fun StartTrialConfirmationDialog(
    colors: PremiumColors,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.macBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "7",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
            }
        },
        title = {
            Text(
                text = "Start 7-Day Free Trial",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Unlock all Safar Premium features instantly for 7 days. No charge today.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.macBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text("Start Free Trial", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary, fontWeight = FontWeight.Medium)
            }
        },
        containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

@Composable
private fun PremiumUnlockedDialog(
    state: PremiumUiState.PaymentSuccess,
    onDismiss: () -> Unit,
) {
    val dialogExpiry = formatPremiumExpiry(state.status.expiresAt)
    val dialogPlanLabel = premiumPlanLabel(state.status.planType)
    var unlockTargetScale by remember { mutableStateOf(0.72f) }
    LaunchedEffect(Unit) {
        unlockTargetScale = 1f
    }
    val unlockScale by animateFloatAsState(
        targetValue = unlockTargetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "premiumUnlockScale",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF34C759),
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = unlockScale
                        scaleY = unlockScale
                    },
            )
        },
        title = {
            Text(
                text = "Plan Extended!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dialogExpiry?.let {
                    Text(
                        text = "Your Safar Premium plan is now active until $it",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF34C759),
                        textAlign = TextAlign.Center,
                    )
                } ?: Text(
                    text = "$dialogPlanLabel is active.",
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Enjoy unlimited access to all AI study planning and Ekagra analytics features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
    )
}

@Composable
private fun PremiumUpgradeContent(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    selectedPlan: PremiumPlanOption,
    currentExpiryText: String?,
    newExpiryText: String?,
    uiState: PremiumUiState,
    isLoading: Boolean,
    colors: PremiumColors,
    isDarkTheme: Boolean,
    onSelectPlan: (String) -> Unit,
    onStartTrial: () -> Unit,
    onRestore: () -> Unit,
) {
    SevenDayTrialBanner(
        colors = colors,
        isDarkTheme = isDarkTheme,
        isLoading = isLoading,
        onStartTrial = onStartTrial,
    )

    PremiumHero(colors = colors, isDarkTheme = isDarkTheme)

    PremiumBenefitsCard(colors = colors, isDarkTheme = isDarkTheme)

    PremiumPricingPanel(
        plans = plans,
        selectedPlanId = selectedPlanId,
        selectedPlan = selectedPlan,
        currentExpiryText = currentExpiryText,
        newExpiryText = newExpiryText,
        colors = colors,
        isDarkTheme = isDarkTheme,
        onSelectPlan = onSelectPlan,
    )

    UiStateMessage(uiState = uiState)

    PaywallFooter(
        isLoading = isLoading,
        colors = colors,
        onRestore = onRestore,
    )
}

@Composable
private fun PremiumActiveContent(
    planLabel: String,
    expiryText: String?,
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    selectedPlan: PremiumPlanOption,
    currentExpiryText: String?,
    newExpiryText: String?,
    isLoading: Boolean,
    colors: PremiumColors,
    isDarkTheme: Boolean,
    onSelectPlan: (String) -> Unit,
    onRestore: () -> Unit,
) {
    PremiumActiveSummaryCard(
        planLabel = planLabel,
        expiryText = expiryText,
        colors = colors,
        isDarkTheme = isDarkTheme,
    )

    PremiumBenefitsCard(colors = colors, isDarkTheme = isDarkTheme)

    SectionHeader(
        title = "Extend Your Plan",
        subtitle = "Purchasing extra time adds directly onto your existing active plan without losing days.",
        colors = colors,
    )

    PremiumPricingPanel(
        plans = plans,
        selectedPlanId = selectedPlanId,
        selectedPlan = selectedPlan,
        currentExpiryText = currentExpiryText,
        newExpiryText = newExpiryText,
        colors = colors,
        isDarkTheme = isDarkTheme,
        onSelectPlan = onSelectPlan,
    )

    PaywallFooter(
        isLoading = isLoading,
        colors = colors,
        onRestore = onRestore,
    )
}

@Composable
private fun PremiumHero(colors: PremiumColors, isDarkTheme: Boolean) {
    MacOSControlCard(isDarkTheme = isDarkTheme) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.macGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Turn exam prep into clarity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "AI planning, focus reports, community & guided study in one place.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PremiumBenefitsCard(colors: PremiumColors, isDarkTheme: Boolean) {
    val benefits = remember {
        listOf(
            "Track exam readiness with real-time indicators",
            "Automatic AI schedule adjustments for missed topics",
            "Detailed Ekagra study reports & analytics",
            "Private Mehfil Connect student community",
            "Dhyan audio & guided focus sessions",
            "Live Vartalap sessions with Parmar Sir",
        )
    }

    MacOSControlCard(isDarkTheme = isDarkTheme) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.macBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "PREMIUM FEATURES",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    ),
                    color = colors.textPrimary,
                )
            }

            HorizontalDivider(
                color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E5EA)
            )

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.macGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = colors.macGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = benefit,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumPricingPanel(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    selectedPlan: PremiumPlanOption,
    currentExpiryText: String?,
    newExpiryText: String?,
    colors: PremiumColors,
    isDarkTheme: Boolean,
    onSelectPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RadioPlanSelector(
            plans = plans,
            selectedPlanId = selectedPlanId,
            colors = colors,
            isDarkTheme = isDarkTheme,
            onSelectPlan = onSelectPlan,
        )
        SelectedPlanCard(
            plan = selectedPlan,
            currentExpiryText = currentExpiryText,
            newExpiryText = newExpiryText,
            colors = colors,
            isDarkTheme = isDarkTheme,
        )
    }
}

@Composable
private fun RadioPlanSelector(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    colors: PremiumColors,
    isDarkTheme: Boolean,
    onSelectPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        plans.forEach { plan ->
            val selected = selectedPlanId == plan.id

            MacOSControlCard(
                isDarkTheme = isDarkTheme,
                isSelected = selected,
                selectedAccentColor = colors.macBlue,
                onClick = { onSelectPlan(plan.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.macBlue,
                                unselectedColor = colors.textSecondary.copy(alpha = 0.5f),
                            ),
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = plan.label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary,
                                )
                                if (plan.badge != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.macGold)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = plan.badge,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = plan.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = colors.textSecondary,
                            )
                        }
                    }

                    Text(
                        text = "\u20B9${plan.price}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (selected) colors.macBlue else colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPlanCard(
    plan: PremiumPlanOption,
    currentExpiryText: String?,
    newExpiryText: String?,
    colors: PremiumColors,
    isDarkTheme: Boolean,
) {
    MacOSControlCard(
        isDarkTheme = isDarkTheme,
        isSelected = true,
        selectedAccentColor = colors.macBlue
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Selected Plan: ${plan.label}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Full access for ${plan.durationLabel}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = colors.textSecondary,
                    )
                }
                Text(
                    text = "\u20B9${plan.price}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.macBlue,
                )
            }

            HorizontalDivider(
                color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFE5E5EA)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Current expiry: ${currentExpiryText ?: "No active subscription"}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                )
                Text(
                    text = "New expiry after purchase: ${newExpiryText ?: "Calculating..."}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = colors.macBlue,
                )
            }
        }
    }
}

@Composable
private fun SevenDayTrialBanner(
    colors: PremiumColors,
    isDarkTheme: Boolean,
    isLoading: Boolean,
    onStartTrial: () -> Unit,
) {
    MacOSControlCard(
        isDarkTheme = isDarkTheme,
        onClick = if (!isLoading) onStartTrial else null,
        selectedAccentColor = colors.macBlue,
        isSelected = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.macBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "7",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Start 7-Day Free Trial",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
                Text(
                    text = "Instant access to all premium features • No payment today",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary,
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = colors.macBlue,
                )
            } else {
                Text(
                    text = "Try Free",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.macBlue
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    colors: PremiumColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun UiStateMessage(
    uiState: PremiumUiState,
) {
    if (uiState !is PremiumUiState.Error) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = uiState.message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun PaywallFooter(
    isLoading: Boolean,
    colors: PremiumColors,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextButton(
            onClick = onRestore,
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Text(
                text = "Restore Safar Premium",
                color = colors.macBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Text(
            text = "Cancel anytime • 256-bit Secure Razorpay Checkout",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PremiumBottomBar(
    selectedPlan: PremiumPlanOption,
    isPremiumActive: Boolean,
    isLoading: Boolean,
    colors: PremiumColors,
    isDarkTheme: Boolean,
    onPurchase: () -> Unit,
) {
    Surface(
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFF9F9FB),
        shadowElevation = if (isDarkTheme) 12.dp else 6.dp,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color(0xFFE5E5EA)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "\u20B9${selectedPlan.price}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                )
                Text(
                    text = selectedPlan.durationLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }

            Button(
                onClick = onPurchase,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.macBlue,
                    contentColor = Color.White,
                    disabledContainerColor = colors.macBlue.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f),
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add ${selectedPlan.durationMonths} Months",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumActiveSummaryCard(
    planLabel: String,
    expiryText: String?,
    colors: PremiumColors,
    isDarkTheme: Boolean,
) {
    MacOSControlCard(
        isDarkTheme = isDarkTheme,
        isSelected = true,
        selectedAccentColor = colors.macGreen
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.macGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Safar Premium Active",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                    Text(
                        text = expiryText?.let { "Valid until $it" } ?: "$planLabel is active",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.macGreen,
                    )
                }
            }
            Text(
                text = "$planLabel is unlocked. Manage or extend your subscription plan below.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun rememberPremiumColors(isDark: Boolean): PremiumColors {
    return remember(isDark) {
        if (isDark) {
            PremiumColors(
                background = Color(0xFF000000),
                cardBody = Color(0xFF2C2C2E).copy(alpha = 0.65f),
                textPrimary = Color.White,
                textSecondary = Color.White.copy(alpha = 0.55f),
                borderBrush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
                ),
                macBlue = Color(0xFF0A84FF),
                macGold = Color(0xFFFF9500),
                macGreen = Color(0xFF34C759),
                danger = Color(0xFFFF453A),
                isDark = true,
            )
        } else {
            PremiumColors(
                background = Color(0xFFF2F2F7),
                cardBody = Color(0xFFF9F9FB),
                textPrimary = Color.Black,
                textSecondary = Color.Black.copy(alpha = 0.55f),
                borderBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
                ),
                macBlue = Color(0xFF0A84FF),
                macGold = Color(0xFFFF9500),
                macGreen = Color(0xFF34C759),
                danger = Color(0xFFFF3B30),
                isDark = false,
            )
        }
    }
}

private fun premiumPlanLabel(planType: String?): String {
    val normalized = planType.orEmpty().lowercase(Locale.US)
    return when {
        "trial" in normalized -> "7-day free trial"
        "3month" in normalized || "3-month" in normalized -> "3-month Premium plan"
        "6month" in normalized || "6-month" in normalized -> "6-month Premium plan"
        normalized.isNotBlank() -> "Safar Premium plan"
        else -> "Safar Premium"
    }
}

private fun calculatePremiumExtensionExpiry(
    currentExpiryDate: String?,
    selectedPlanDuration: Int,
    now: Instant = Instant.now(),
): String {
    val currentExpiry = currentExpiryDate?.let { raw ->
        runCatching { Instant.parse(raw) }.getOrNull()
    }
    val startsFrom = currentExpiry?.takeIf { it.isAfter(now) } ?: now
    return ZonedDateTime
        .ofInstant(startsFrom, ZoneOffset.UTC)
        .plusMonths(selectedPlanDuration.toLong())
        .toInstant()
        .toString()
}

private fun formatPremiumExpiry(expiresAt: String?): String? {
    if (expiresAt.isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(expiresAt) }.getOrNull() ?: return expiresAt.take(10)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}
