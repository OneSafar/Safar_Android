/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 */
package com.safarparmar.app.ui.premium

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.style.TextDecoration
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
    val isDhyanOnly: Boolean = false,
    val originalPrice: Int? = null,
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val premiumStatus by viewModel.premiumStatus.collectAsStateWithLifecycle()
    val dhyanPricing by viewModel.dhyanPricing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var refreshAfterPaymentReturn by remember { mutableStateOf(false) }
    var showTrialConfirmation by remember { mutableStateOf(false) }

    val allPlans = listOf(
            PremiumPlanOption(
                id = "3month",
                label = "3 Months",
                price = 99,
                subtitle = "Premium + Dhyan Live",
                durationLabel = "3 months",
                durationMonths = 3,
                courseId = "study-planner-pro-3month",
                discountLabel = "Starter",
            ),
            PremiumPlanOption(
                id = "6month",
                label = "6 Months",
                price = 119,
                subtitle = "Premium + Dhyan Live",
                durationLabel = "6 months",
                durationMonths = 6,
                courseId = "study-planner-pro-6month",
                badge = "POPULAR",
                discountLabel = "Popular",
            ),
            PremiumPlanOption(
                id = "dhyan",
                label = "Dhyan Live",
                price = if (dhyanPricing.couponEligible) dhyanPricing.premiumPrice else dhyanPricing.standardPrice,
                subtitle = if (dhyanPricing.couponEligible) "Existing Premium member price" else "Meditation & yoga sessions only",
                durationLabel = "6 months",
                durationMonths = 6,
                courseId = "safar-30",
                badge = if (dhyanPricing.couponEligible) "41% REBATE" else null,
                isDhyanOnly = true,
                originalPrice = if (dhyanPricing.couponEligible) dhyanPricing.standardPrice else null,
            ),
        )
    val plans = when (dhyanPricing.accessState) {
        "LEGACY_PREMIUM_DISCOUNT" -> allPlans.sortedByDescending { it.isDhyanOnly }
        "STANDARD" -> allPlans
        "DHYAN_INCLUDED", "DHYAN_SCHEDULED" -> allPlans.filterNot { it.isDhyanOnly }
        else -> emptyList()
    }
    var selectedPlanId by remember { mutableStateOf("6month") }
    var selectedPlanDuration by remember { mutableStateOf(6) }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId } ?: plans.firstOrNull() ?: allPlans.first()
    val isLoading = uiState is PremiumUiState.Loading

    LaunchedEffect(dhyanPricing.accessState) {
        val preferredPlanId = when (dhyanPricing.accessState) {
            "LEGACY_PREMIUM_DISCOUNT" -> "dhyan"
            "STANDARD", "DHYAN_INCLUDED", "DHYAN_SCHEDULED" -> "6month"
            else -> null
        }
        preferredPlanId?.let { planId ->
            selectedPlanId = planId
            selectedPlanDuration = allPlans.first { it.id == planId }.durationMonths
        }
    }

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
                val razorpayKeyId = state.keyId?.trim()?.takeIf { it.isNotEmpty() }
                if (razorpayKeyId == null) {
                    viewModel.notifyPaymentFailed("The payment gateway is not configured on this server.")
                    return@LaunchedEffect
                }
                refreshAfterPaymentReturn = state.planType != "dhyan"
                val checkout = Checkout()
                checkout.setKeyID(razorpayKeyId)
                val options = JSONObject().apply {
                    put("name", "Safar")
                    put("description", if (state.planType == "dhyan") "Dhyan Live · 6 Months" else "Safar Premium + Dhyan Live")
                    put("order_id", state.order.id)
                    put("currency", state.order.currency)
                    put("amount", state.order.amount)
                    put("theme.color", if (isDarkTheme) "#3B0764" else "#581C87")
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

    val isTrialActive = premiumStatus.planType.orEmpty().contains("trial", ignoreCase = true)
    val isPremiumActive = premiumStatus.hasAnyPaidAccess && !isTrialActive
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

        if (uiState is PremiumUiState.NoActivePlan) {
            NoActivePlanDialog(
                isDarkTheme = isDarkTheme,
                onDismiss = viewModel::resetState,
            )
        }

        if (uiState is PremiumUiState.DhyanPaymentSuccess) {
            AlertDialog(
                onDismissRequest = viewModel::resetState,
                icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981)) },
                title = { Text("Dhyan Live unlocked") },
                text = { Text("Your six-month meditation and yoga access is now active.") },
                confirmButton = { Button(onClick = viewModel::resetState) { Text("Continue") } },
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
                if (plans.isNotEmpty()) {
                    PremiumBottomBar(
                        selectedPlan = selectedPlan,
                        isPremiumActive = isPremiumActive,
                        isLoading = isLoading,
                        isDarkTheme = isDarkTheme,
                        onPurchase = {
                            if (selectedPlan.isDhyanOnly) {
                                viewModel.createDhyanOrder()
                            } else {
                                viewModel.createOrder(duration = selectedPlan.durationMonths)
                            }
                        },
                    )
                }
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
                    } else if (isTrialActive) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        ) {
                            TrialActiveSummaryCard(expiryText = formattedExpiry)
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

                    when (dhyanPricing.accessState) {
                        "LEGACY_PREMIUM_DISCOUNT" -> LegacyPremiumNotice(isDarkTheme = isDarkTheme)
                        "DHYAN_INCLUDED" -> DhyanAccessNotice(
                            isPremiumActive = isPremiumActive,
                            onOpenDhyan = { onNavigate(Routes.LIVE_SESSIONS_ROOT) },
                        )
                        "DHYAN_SCHEDULED" -> DhyanScheduledNotice(
                            startsAt = dhyanPricing.dhyanStartsAt,
                            expiresAt = dhyanPricing.dhyanExpiresAt,
                            isDarkTheme = isDarkTheme,
                        )
                    }

                    if (dhyanPricing.accessState == "LOADING") {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (dhyanPricing.accessState == "ERROR") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                        ) {
                            Text("We could not verify your available plans. Please reopen this screen and try again.", modifier = Modifier.padding(20.dp), color = PlannerFlatColors.TextMuted)
                        }
                    } else if (plans.isNotEmpty()) {
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
                                title = when {
                                    dhyanPricing.accessState == "LEGACY_PREMIUM_DISCOUNT" -> "Choose what to add"
                                    isPremiumActive -> "Extend your plan"
                                    else -> "Choose your plan"
                                },
                                isDarkTheme = isDarkTheme,
                            )
                            Text(
                                text = when {
                                    dhyanPricing.accessState == "LEGACY_PREMIUM_DISCOUNT" -> "Add Dhyan Live at your member price, or extend Premium by 3 or 6 months. Extra time is added after your current plan."
                                    isPremiumActive -> "Choose 3 or 6 more months. The new time starts after your current plan ends."
                                    else -> "Choose Dhyan Live only, or get Premium with Dhyan Live included."
                                },
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
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            PremiumSectionHeader(icon = Icons.Default.Star, title = "What you get", isDarkTheme = isDarkTheme)
                            PlanHairline(alpha = 0.5f)
                            PremiumBenefitsCard()
                        }
                    }

                    UiStateMessage(uiState = uiState)

                    PaywallFooter(
                        isLoading = isLoading,
                        isDarkTheme = isDarkTheme,
                        onRestore = { viewModel.refreshPremiumStatus(isRestore = true) },
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
                text = "Use Safar's Premium study features for 7 days. Dhyan Live is not included in the trial.",
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
    val isTrial = state.status.planType.orEmpty().contains("trial", ignoreCase = true)
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

    val dialogTitle = when {
        state.isRestore && isTrial -> "Free Trial Active!"
        state.isRestore -> "Safar Premium Active!"
        isTrial -> "Free Trial Started!"
        else -> "Payment Successful!"
    }

    val subtitleText = when {
        isTrial && dialogExpiry != null -> "Your 7-day free trial is active until $dialogExpiry"
        isTrial -> "Your 7-day free trial is active."
        dialogExpiry != null -> "Your Safar Premium plan is active until $dialogExpiry"
        else -> "$dialogPlanLabel is active."
    }

    val bodyText = when {
        isTrial -> "Enjoy full access to all AI study planning and Ekagra analytics features during your trial."
        else -> "Enjoy unlimited access to all AI study planning and Ekagra analytics features."
    }

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
                text = dialogTitle,
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
                Text(
                    text = subtitleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = bodyText,
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
private fun NoActivePlanDialog(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
) {
    val buttonBg = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFF581C87)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafarSemanticColors.plannerBackground(),
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF7C3AED),
                modifier = Modifier.size(52.dp),
            )
        },
        title = {
            Text(
                text = "No Active Plan Found",
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
                Text(
                    text = "We couldn't find an active Safar Premium or trial plan for this account.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlannerFlatColors.TextDark,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "You can choose a plan below anytime to unlock all study tools and analytics.",
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
                Text("View Plans", fontWeight = FontWeight.Bold, color = Color.White)
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
            "Dhyan Live meditation & yoga sessions with Parmar Sir",
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
private fun LegacyPremiumNotice(
    isDarkTheme: Boolean,
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PremiumSectionHeader(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Existing member benefit",
                    isDarkTheme = isDarkTheme,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = "41% REBATE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                    )
                }
            }
            Text(
                text = "You joined Premium before Dhyan Live was added, so you can add 6 months of Dhyan Live for ₹29.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlannerFlatColors.TextDark,
            )
            Text(
                text = "This member price is available while your current Premium plan is active. Future Premium plans use the regular price: ₹99 for 3 months or ₹119 for 6 months.",
                fontSize = 12.sp,
                color = PlannerFlatColors.TextMuted,
            )
        }
    }
}

@Composable
private fun DhyanAccessNotice(
    isPremiumActive: Boolean,
    onOpenDhyan: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Dhyan Live is active", fontWeight = FontWeight.Bold, color = PlannerFlatColors.TextDark)
                Text(
                    text = if (isPremiumActive) "It is included with your current Premium plan." else "Your 6-month Dhyan Live plan is active. You can also add Premium below.",
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            TextButton(onClick = onOpenDhyan, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text("Open", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DhyanScheduledNotice(
    startsAt: String?,
    expiresAt: String?,
    isDarkTheme: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, PlannerFlatColors.BorderSoft),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PremiumSectionHeader(
                icon = Icons.Default.CheckCircle,
                title = "Next bundle scheduled",
                isDarkTheme = isDarkTheme,
            )
            Text(
                text = "Your new Premium + Dhyan Live period starts after your current Premium plan ends.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlannerFlatColors.TextDark,
            )
            Text(
                text = "Dhyan Live: ${formatPremiumExpiry(startsAt) ?: "scheduled start"} to ${formatPremiumExpiry(expiresAt) ?: "scheduled end"}. It does not use time from your current plan.",
                fontSize = 12.sp,
                color = PlannerFlatColors.TextMuted,
            )
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

                    Column(horizontalAlignment = Alignment.End) {
                        plan.originalPrice?.let { originalPrice ->
                            Text(
                                text = "₹$originalPrice",
                                fontSize = 12.sp,
                                color = PlannerFlatColors.TextMuted,
                                textDecoration = TextDecoration.LineThrough,
                            )
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
                        text = if (plan.isDhyanOnly) "Dhyan Live for ${plan.durationLabel}" else "Premium + Dhyan Live for ${plan.durationLabel}",
                        fontSize = 12.sp,
                        color = PlannerFlatColors.TextMuted,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    plan.originalPrice?.let { originalPrice ->
                        Text(
                            text = "Regular ₹$originalPrice",
                            fontSize = 11.sp,
                            color = PlannerFlatColors.TextMuted,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                    Text(
                        text = "₹${plan.price}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                    )
                }
            }

            if (!plan.isDhyanOnly) {
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
                Text(
                    text = if (currentExpiryText != null) {
                        "Premium and Dhyan Live start after your current Premium plan ends."
                    } else {
                        "Premium and Dhyan Live start today."
                    },
                    fontSize = 11.5.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            } else {
                Text(
                    text = if (plan.price == 29) "Existing member price · 41% rebate" else "Standalone Dhyan plan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
                text = "Premium study features for 7 days · Dhyan Live not included",
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
    if (uiState.message.isRawPaymentProviderPayload()) return

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

private fun String.isRawPaymentProviderPayload(): Boolean {
    val normalized = trim()
    return normalized.startsWith("{") ||
        normalized.contains("\"metadata\"", ignoreCase = true) ||
        normalized.contains("BAD_REQUEST_ERROR", ignoreCase = true) ||
        normalized.contains("payment_authentication", ignoreCase = true)
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
                            text = "Buy Now",
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

@Composable
private fun TrialActiveSummaryCard(
    expiryText: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Premium Trial Active",
                    fontFamily = LoraFontFamily,
                    fontSize = 18.sp,
                    color = PlannerFlatColors.TextDark,
                )
                Text(
                    text = expiryText?.let { "Valid until $it" } ?: "Your 7-day trial is active",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                )
            }
        }
        Text(
            text = "Your trial includes Premium study features. Dhyan Live needs a separate plan or a Premium bundle.",
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
    val extendedExpiry = ZonedDateTime
        .ofInstant(startsFrom, ZoneOffset.UTC)
        .plusMonths(selectedPlanDuration.toLong())
        .withZoneSameInstant(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).format(extendedExpiry)
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
