package com.safarparmar.app.ui.premium

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
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
    val surface: Color,
    val elevatedSurface: Color,
    val border: Color,
    val primary: Color,
    val primarySoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val danger: Color,
    val cta: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val muted: Color,
    val success: Color,
    val isDark: Boolean,
)

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
                subtitle = "Good for one exam cycle",
                durationLabel = "6 months",
                durationMonths = 6,
                courseId = "study-planner-pro-6month",
                badge = "Popular",
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
                        fallbackError = "Payment returned from PhonePe/Razorpay, but Safar Premium is not active yet. Please tap Restore Safar Premium in a moment."
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
                options.put("theme.color", "#F04438")
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
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
                    onSelectPlan = updateSelectedPlan,
                    onStartTrial = { showTrialConfirmation = true },
                    onRestore = { viewModel.refreshPremiumStatus() },
                )
            }
            Spacer(modifier = Modifier.height(88.dp))
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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.primarySoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "7",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.primary,
                )
            }
        },
        title = {
            Text(
                text = "Start 7-day free trial?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "This will unlock Safar Premium features for 7 days on this account. No payment is needed today.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.cta,
                    contentColor = Color.White,
                ),
            ) {
                Text("Start free trial", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
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
                tint = Color(0xFF10B981),
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
                text = "Plan extended",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Your plan has been extended to $it",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF15803D),
                        textAlign = TextAlign.Center,
                    )
                } ?: Text(
                    text = "$dialogPlanLabel is active.",
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "You can keep using every Premium feature across SAFAR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue")
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
    onSelectPlan: (String) -> Unit,
    onStartTrial: () -> Unit,
    onRestore: () -> Unit,
) {
    SevenDayTrialBanner(
        colors = colors,
        isLoading = isLoading,
        onStartTrial = onStartTrial,
    )

    PremiumBenefitsCard(colors = colors)

    PremiumHero(colors = colors)

    PremiumPricingPanel(
        plans = plans,
        selectedPlanId = selectedPlanId,
        selectedPlan = selectedPlan,
        currentExpiryText = currentExpiryText,
        newExpiryText = newExpiryText,
        colors = colors,
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
    onSelectPlan: (String) -> Unit,
    onRestore: () -> Unit,
) {
    PremiumActiveSummaryCard(
        planLabel = planLabel,
        expiryText = expiryText,
        colors = colors,
    )

    PremiumBenefitsCard(colors = colors)

    SectionHeader(
        title = "Add more time to your plan",
        subtitle = "You won’t lose your current time. This purchase adds more time to your plan.",
        colors = colors,
    )

    PremiumPricingPanel(
        plans = plans,
        selectedPlanId = selectedPlanId,
        selectedPlan = selectedPlan,
        currentExpiryText = currentExpiryText,
        newExpiryText = newExpiryText,
        colors = colors,
        onSelectPlan = onSelectPlan,
    )

    PaywallFooter(
        isLoading = isLoading,
        colors = colors,
        onRestore = onRestore,
    )
}

@Composable
private fun PremiumHero(colors: PremiumColors) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.elevatedSurface, colors.surface)))
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.accent.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Turn exam prep",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = "into clarity",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = "AI planning, focus reports, premium community,\nand guided learning in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun PremiumBenefitsCard(colors: PremiumColors) {
    val benefits = remember {
        listOf(
            "Know if your exam plan is on track",
            "See completed work, pending topics, and overdue chapters",
            "Paste syllabus and let AI arrange it",
            "Missed topics get adjusted in your schedule",
            "Detailed Ekagra study reports and history",
            "Private Mehfil Connect with other students",
            "Dhyan audio and guided learning sessions",
            "Live Vartalap sessions with Parmar Sir",
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WHAT YOU GET",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.accent,
                )
            }

            HorizontalDivider(color = colors.border)

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = benefit,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = colors.textSecondary,
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
    onSelectPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        RadioPlanSelector(
            plans = plans,
            selectedPlanId = selectedPlanId,
            colors = colors,
            onSelectPlan = onSelectPlan,
        )
        SelectedPlanCard(
            plan = selectedPlan,
            currentExpiryText = currentExpiryText,
            newExpiryText = newExpiryText,
            colors = colors,
        )
    }
}

@Composable
private fun RadioPlanSelector(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    colors: PremiumColors,
    onSelectPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { plan ->
            val selected = selectedPlanId == plan.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        onClick = { onSelectPlan(plan.id) },
                        role = Role.RadioButton,
                    ),
                shape = RoundedCornerShape(18.dp),
                color = if (selected) colors.accentSoft else colors.elevatedSurface,
                border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.accent,
                            unselectedColor = colors.muted,
                        ),
                    )
                    Text(
                        text = "${plan.label} – ₹${plan.price}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(22.dp),
            color = colors.accentSoft,
            border = BorderStroke(1.4.dp, colors.accent.copy(alpha = 0.78f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = plan.label,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = colors.textPrimary,
                        )
                        Text(
                            text = plan.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        text = "\u20B9${plan.price}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (colors.isDark) colors.accent else colors.textPrimary,
                    )
                }
                Text(
                    text = "Safar Premium for ${plan.durationLabel}. Secure payment by Razorpay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                HorizontalDivider(color = colors.border)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Your current expiry: ${currentExpiryText ?: "No current active plan"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "New expiry after purchase: ${newExpiryText ?: "Calculating..."}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SevenDayTrialBanner(
    colors: PremiumColors,
    isLoading: Boolean,
    onStartTrial: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = !isLoading, onClick = onStartTrial),
        shape = RoundedCornerShape(18.dp),
        color = colors.cta,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
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
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "Try Premium for Free",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Text(
                    text = "Start your 7 Days Free trial",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
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
        shape = RoundedCornerShape(14.dp),
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Cancel anytime",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        TextButton(
            onClick = onRestore,
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Text(
                text = "Restore Safar Premium",
                color = colors.textPrimary,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = "Secure payment by Razorpay",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
    }
}

@Composable
private fun PremiumBottomBar(
    selectedPlan: PremiumPlanOption,
    isPremiumActive: Boolean,
    isLoading: Boolean,
    colors: PremiumColors,
    onPurchase: () -> Unit,
) {
    Surface(
        color = colors.background,
        shadowElevation = if (colors.isDark) 0.dp else 12.dp,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.72f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
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
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.cta,
                    contentColor = Color.White,
                    disabledContainerColor = colors.cta.copy(alpha = 0.42f),
                    disabledContentColor = Color.White.copy(alpha = 0.82f),
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 15.dp),
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
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
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.elevatedSurface),
        border = BorderStroke(1.dp, colors.success.copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(colors.success.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Premium Active",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.textPrimary,
                    )
                    Text(
                        text = expiryText?.let { "Valid until $it" } ?: "$planLabel is active on this account.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.success,
                    )
                }
            }
            Text(
                text = "$planLabel is unlocked. Manage your plan here or jump straight into Premium features.",
                style = MaterialTheme.typography.bodyMedium,
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
                background = Color(0xFF0D0D0F),
                surface = Color(0xFF141416),
                elevatedSurface = Color(0xFF19191D),
                border = Color(0xFF2B2B31),
                primary = Color(0xFFFFB020),
                primarySoft = Color(0xFF1E1E22),
                accent = Color(0xFFFFB020),
                accentSoft = Color(0xFF251B0D),
                danger = Color(0xFFFF5148),
                cta = Color(0xFFF04438),
                textPrimary = Color.White,
                textSecondary = Color(0xFFB6B7BE),
                muted = Color(0xFF787B84),
                success = Color(0xFF34D399),
                isDark = true,
            )
        } else {
            PremiumColors(
                background = Color(0xFFFAFAFA),
                surface = Color.White,
                elevatedSurface = Color(0xFFF7F7F8),
                border = Color(0xFFD8D8DD),
                primary = Color(0xFF111111),
                primarySoft = Color(0xFFF3F3F4),
                accent = Color(0xFFF59E0B),
                accentSoft = Color(0xFFFFFBF0),
                danger = Color(0xFFEF4444),
                cta = Color(0xFF050505),
                textPrimary = Color(0xFF111111),
                textSecondary = Color(0xFF5C5F66),
                muted = Color(0xFF9CA0AA),
                success = Color(0xFF059669),
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
