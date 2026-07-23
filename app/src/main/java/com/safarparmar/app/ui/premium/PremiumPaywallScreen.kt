package com.safarparmar.app.ui.premium

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.razorpay.Checkout
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.ui.theme.SafarTheme
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

@Composable
private fun PremiumSectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            color = PlannerFlatColors.TextDark,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    currentRoute: String = Routes.PREMIUM,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val premiumStatus by viewModel.premiumStatus.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

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

    SafarTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
            if (uiState is PremiumUiState.PaymentSuccess) {
                PremiumUnlockedDialog(
                    state = uiState as PremiumUiState.PaymentSuccess,
                    onDismiss = viewModel::resetState,
                )
            }

            if (showTrialConfirmation) {
                StartTrialConfirmationDialog(
                    onDismiss = { showTrialConfirmation = false },
                    onConfirm = {
                        showTrialConfirmation = false
                        viewModel.startFreeTrial()
                    },
                )
            }

            SafarDrawerScaffold(
                title = "Safar Premium",
                subtitle = "Unlimited Access & Analytics",
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onNavigate = onNavigate,
                onToggleDarkTheme = onToggleDarkTheme,
                containerColor = SafarSemanticColors.plannerBackground(),
            ) { paddingValues ->
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        PremiumBottomBar(
                            selectedPlan = selectedPlan,
                            isPremiumActive = isPremiumActive,
                            isLoading = isLoading,
                            onPurchase = {
                                viewModel.createOrder(
                                    duration = selectedPlan.durationMonths,
                                )
                            },
                        )
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(paddingValues)
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Main Title Banner
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Safar Premium",
                                fontFamily = LoraFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal,
                                color = PlannerFlatColors.TextDark,
                            )
                            Text(
                                text = "Unlock AI study planning, Ekagra focus reports, and live sessions",
                                fontSize = 13.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }

                        if (isPremiumActive) {
                            // Card 1: Active Subscription Summary
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            ) {
                                PremiumActiveSummaryCard(
                                    planLabel = planLabel,
                                    expiryText = formattedExpiry,
                                )
                            }
                        } else {
                            // Card 1: 7-Day Free Trial Banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            ) {
                                SevenDayTrialBanner(
                                    isLoading = isLoading,
                                    onStartTrial = { showTrialConfirmation = true },
                                )
                            }
                        }

                        // Card 2: Features Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PremiumSectionHeader(icon = Icons.Default.Star, title = "Premium Features")
                                PlanHairline(alpha = 0.5f)
                                PremiumBenefitsCard()
                            }
                        }

                        // Card 3: Subscription Plan Options
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PremiumSectionHeader(
                                    icon = Icons.Default.WorkspacePremium,
                                    title = if (isPremiumActive) "Extend Your Plan" else "Select Plan"
                                )
                                Text(
                                    text = "Purchasing extra time adds directly onto your existing plan without losing days.",
                                    fontSize = 12.5.sp,
                                    color = PlannerFlatColors.TextMuted,
                                )
                                PlanHairline(alpha = 0.5f)
                                PremiumPricingPanel(
                                    plans = plans,
                                    selectedPlanId = selectedPlanId,
                                    selectedPlan = selectedPlan,
                                    currentExpiryText = formattedExpiry,
                                    newExpiryText = formattedNewExpiry,
                                    onSelectPlan = updateSelectedPlan,
                                )
                            }
                        }

                        UiStateMessage(uiState = uiState)

                        PaywallFooter(
                            isLoading = isLoading,
                            onRestore = { viewModel.refreshPremiumStatus() },
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StartTrialConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafarSemanticColors.plannerBackground(),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "7",
                    fontFamily = LoraFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = scheme.onPrimary,
                )
            }
        },
        title = {
            Text(
                text = "Start 7-Day Free Trial",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = "Unlock all Safar Premium features instantly for 7 days. No charge today.",
                fontSize = 14.sp,
                color = PlannerFlatColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
            ) {
                Text("Start Free Trial", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PlannerFlatColors.TextMuted, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
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
        containerColor = SafarSemanticColors.plannerBackground(),
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
                text = "Plan Extended!",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
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
                        text = "Your Safar Premium plan is now active until $it",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center,
                    )
                } ?: Text(
                    text = "$dialogPlanLabel is active.",
                    fontSize = 14.sp,
                    color = PlannerFlatColors.TextDark,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Enjoy unlimited access to all AI study planning and Ekagra analytics features.",
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun PremiumBenefitsCard() {
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        benefits.forEach { benefit ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = benefit,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlannerFlatColors.TextDark,
                )
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
    onSelectPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioPlanSelector(
            plans = plans,
            selectedPlanId = selectedPlanId,
            onSelectPlan = onSelectPlan,
        )
        SelectedPlanCard(
            plan = selectedPlan,
            currentExpiryText = currentExpiryText,
            newExpiryText = newExpiryText,
        )
    }
}

@Composable
private fun RadioPlanSelector(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    onSelectPlan: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { plan ->
            val selected = selectedPlanId == plan.id

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) scheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) scheme.primary else PlannerFlatColors.BorderSoft,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectPlan(plan.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
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
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = scheme.primary,
                                unselectedColor = PlannerFlatColors.TextMuted,
                            ),
                        )
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = plan.label,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlannerFlatColors.TextDark,
                                )
                                if (plan.badge != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFF9500))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = plan.badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = plan.subtitle,
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                            )
                        }
                    }

                    Text(
                        text = "\u20B9${plan.price}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) scheme.primary else PlannerFlatColors.TextDark,
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
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.primary.copy(alpha = 0.05f))
            .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Selected Plan: ${plan.label}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlannerFlatColors.TextDark,
                    )
                    Text(
                        text = "Full access for ${plan.durationLabel}",
                        fontSize = 12.sp,
                        color = PlannerFlatColors.TextMuted,
                    )
                }
                Text(
                    text = "\u20B9${plan.price}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.primary,
                )
            }

            PlanHairline(alpha = 0.5f)

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Current expiry: ${currentExpiryText ?: "No active subscription"}",
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted,
                )
                Text(
                    text = "New expiry after purchase: ${newExpiryText ?: "Calculating..."}",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SevenDayTrialBanner(
    isLoading: Boolean,
    onStartTrial: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onStartTrial)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "7",
                fontFamily = LoraFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = scheme.onPrimary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Start 7-Day Free Trial",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.TextDark,
            )
            Text(
                text = "Instant access to all premium features • No payment today",
                fontSize = 12.sp,
                color = PlannerFlatColors.TextMuted,
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = scheme.primary,
            )
        } else {
            Text(
                text = "Try Free",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.primary
            )
        }
    }
}

@Composable
private fun UiStateMessage(
    uiState: PremiumUiState,
) {
    if (uiState !is PremiumUiState.Error) return

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = uiState.message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun PaywallFooter(
    isLoading: Boolean,
    onRestore: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(
            onClick = onRestore,
            enabled = !isLoading,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Text(
                text = "Restore Safar Premium",
                color = scheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Text(
            text = "Cancel anytime • 256-bit Secure Razorpay Checkout",
            fontSize = 11.sp,
            color = PlannerFlatColors.TextMuted,
        )
    }
}

@Composable
private fun PremiumBottomBar(
    selectedPlan: PremiumPlanOption,
    isPremiumActive: Boolean,
    isLoading: Boolean,
    onPurchase: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        color = SafarSemanticColors.plannerBackground(),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            PlannerFlatColors.BorderSoft
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "\u20B9${selectedPlan.price}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlannerFlatColors.TextDark,
                )
                Text(
                    text = selectedPlan.durationLabel,
                    fontSize = 11.5.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLoading) scheme.primary.copy(alpha = 0.5f) else scheme.primary)
                    .clickable(enabled = !isLoading, onClick = onPurchase)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = scheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Add ${selectedPlan.durationMonths} Months",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumActiveSummaryCard(
    planLabel: String,
    expiryText: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Safar Premium Active",
                    fontFamily = LoraFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = expiryText?.let { "Valid until $it" } ?: "$planLabel is active",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        Text(
            text = "Safar Premium is unlocked. Manage or extend your subscription plan below.",
            fontSize = 12.5.sp,
            color = PlannerFlatColors.TextMuted,
        )
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
    val instant = rawExpiryToInstant(expiresAt) ?: return expiresAt.take(10)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

private fun rawExpiryToInstant(expiresAt: String): Instant? {
    return runCatching { Instant.parse(expiresAt) }.getOrNull()
}
