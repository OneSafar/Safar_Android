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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
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
    onBack: () -> Unit = {},
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
                refreshAfterPaymentReturn = true
                val checkout = Checkout()
                checkout.setKeyID(state.keyId ?: "rzp_live_SWHJBT7AXadF8a")
                val options = JSONObject().apply {
                    put("name", "Safar")
                    put("description", "Safar Premium")
                    put("order_id", state.order.id)
                    put("currency", state.order.currency)
                    put("amount", state.order.amount)
                    put("theme.color", if (isDarkTheme) "#3B0764" else "#581C87")
                    put("modal", JSONObject().apply {
                        put("ondismiss", "function(){}")
                    })
                }
                checkout.open(activity, options)
                viewModel.resetState()
            } catch (e: Exception) {
                e.printStackTrace()
                refreshAfterPaymentReturn = false
                viewModel.notifyPaymentFailed(e.message ?: "Error launching checkout")
            }
        }
    }

    val isPremiumActive = premiumStatus.hasAnyPaidAccess
    val formattedExpiry = remember(premiumStatus.expiresAt) { formatPremiumExpiry(premiumStatus.expiresAt) }
    val formattedNewExpiry = remember(premiumStatus.expiresAt, selectedPlanDuration) {
        calculatePremiumExtensionExpiry(premiumStatus.expiresAt, selectedPlanDuration)
    }
    val planLabel = if (isPremiumActive) premiumPlanLabel(premiumStatus.planType) else "Free Plan"

    val updateSelectedPlan: (String) -> Unit = { planId ->
        selectedPlanId = planId
        val plan = plans.firstOrNull { it.id == planId }
        if (plan != null) {
            selectedPlanDuration = plan.durationMonths
        }
    }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
        if (uiState is PremiumUiState.PaymentSuccess) {
            PremiumUnlockedDialog(
                state = uiState as PremiumUiState.PaymentSuccess,
                isDarkTheme = isDarkTheme,
                onDismiss = viewModel::resetState,
            )
        }

        if (showTrialConfirmation) {
            StartTrialConfirmationDialog(
                isDarkTheme = isDarkTheme,
                onDismiss = { showTrialConfirmation = false },
                onConfirm = {
                    showTrialConfirmation = false
                    viewModel.startFreeTrial()
                },
            )
        }

        Scaffold(
            containerColor = SafarSemanticColors.plannerBackground(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Safar Premium",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = LoraFontFamily,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SafarSemanticColors.plannerBackground(),
                    ),
                )
            },
            bottomBar = {
                PremiumBottomBar(
                    selectedPlan = selectedPlan,
                    isPremiumActive = isPremiumActive,
                    isLoading = isLoading,
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
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)).copy(alpha = 0.3f)),
                        ) {
                            SevenDayTrialBanner(
                                isLoading = isLoading,
                                isDarkTheme = isDarkTheme,
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
                            PremiumSectionHeader(icon = Icons.Default.Star, title = "Premium Features", isDarkTheme = isDarkTheme)
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
                                title = if (isPremiumActive) "Extend Your Plan" else "Select Plan",
                                isDarkTheme = isDarkTheme,
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
                                isDarkTheme = isDarkTheme,
                                onSelectPlan = updateSelectedPlan,
                            )
                        }
                    }

                    UiStateMessage(uiState = uiState)

                    PaywallFooter(
                        isLoading = isLoading,
                        isDarkTheme = isDarkTheme,
                        onRestore = { viewModel.refreshPremiumStatus() },
                    )
                }
            }
        }
    }

@Composable
private fun StartTrialConfirmationDialog(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val buttonBg = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFF581C87)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafarSemanticColors.plannerBackground(),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(buttonBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "7",
                    fontFamily = LoraFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
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
                    containerColor = buttonBg,
                    contentColor = Color.White,
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
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
) {
    val buttonBg = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFF581C87)
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
                colors = ButtonDefaults.buttonColors(containerColor = buttonBg, contentColor = Color.White)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun PremiumBenefitsCard() {
    val benefits = remember {
        listOf(
            "Track Exam Readiness with Real-time Progress Analytics",
            "Automatic schedule adjustment for missed topics",
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
    isDarkTheme: Boolean = false,
    onSelectPlan: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioPlanSelector(
            plans = plans,
            selectedPlanId = selectedPlanId,
            isDarkTheme = isDarkTheme,
            onSelectPlan = onSelectPlan,
        )
        SelectedPlanCard(
            plan = selectedPlan,
            currentExpiryText = currentExpiryText,
            newExpiryText = newExpiryText,
            isDarkTheme = isDarkTheme,
        )
    }
}

@Composable
private fun RadioPlanSelector(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    isDarkTheme: Boolean = false,
    onSelectPlan: (String) -> Unit,
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { plan ->
            val selected = selectedPlanId == plan.id

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) accent.copy(alpha = 0.08f) else Color.Transparent)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) accent else PlannerFlatColors.BorderSoft,
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
                                selectedColor = accent,
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
                        text = "₹${plan.price}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) accent else PlannerFlatColors.TextDark,
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
    isDarkTheme: Boolean = false,
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.05f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
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
                    text = "₹${plan.price}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
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
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun SevenDayTrialBanner(
    isLoading: Boolean,
    isDarkTheme: Boolean = false,
    onStartTrial: () -> Unit,
) {
    val buttonBg = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFF581C87)
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)

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
                .background(buttonBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "7",
                fontFamily = LoraFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
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
                color = accent,
            )
        } else {
            Text(
                text = "Try Free",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
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
    isDarkTheme: Boolean = false,
    onRestore: () -> Unit,
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)

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
                color = accent,
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
    isDarkTheme: Boolean = false,
    onPurchase: () -> Unit,
) {
    val buttonBg = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFF581C87)

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
                    text = "₹${selectedPlan.price}",
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
                    .background(if (isLoading) buttonBg.copy(alpha = 0.5f) else buttonBg)
                    .clickable(enabled = !isLoading, onClick = onPurchase)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
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
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Add ${selectedPlan.durationMonths} Months",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
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
