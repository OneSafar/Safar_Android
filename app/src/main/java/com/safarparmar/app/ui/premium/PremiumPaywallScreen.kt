/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 */
package com.safarparmar.app.ui.premium

import androidx.compose.ui.platform.LocalResources

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.app.Activity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.safarparmar.app.ui.components.popups.SafarMaterialAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.composables.ui.components.Disclosure
import com.composables.ui.components.DisclosureButton
import com.composables.ui.components.DisclosurePanel
import com.composables.ui.components.Text as UiText
import com.composables.ui.components.Icon as UiIcon
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
private fun PremiumSectionHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    val c = LocalPremiumMembershipColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.selected), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.action, modifier = Modifier.size(20.dp))
        }
        Text(title, color = c.ink, fontFamily = PoppinsFontFamily, fontSize = 16.sp,
            lineHeight = 22.sp, fontWeight = FontWeight.Bold)
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
    val localizedResources by rememberUpdatedState(LocalResources.current)
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var refreshAfterPaymentReturn by remember { mutableStateOf(false) }
    var showTrialConfirmation by remember { mutableStateOf(false) }

    val allPlans = listOf(
            PremiumPlanOption(
                id = "3month",
                label = stringResource(R.string.premium_three_months),
                price = 99,
                subtitle = stringResource(R.string.premium_plus_dhyan),
                durationLabel = stringResource(R.string.premium_three_months_lower),
                durationMonths = 3,
                courseId = "study-planner-pro-3month",
                discountLabel = stringResource(R.string.premium_starter),
            ),
            PremiumPlanOption(
                id = "6month",
                label = stringResource(R.string.premium_six_months),
                price = 119,
                subtitle = stringResource(R.string.premium_plus_dhyan),
                durationLabel = stringResource(R.string.premium_six_months_lower),
                durationMonths = 6,
                courseId = "study-planner-pro-6month",
                badge = stringResource(R.string.premium_popular).uppercase(),
                discountLabel = stringResource(R.string.premium_popular),
            ),
            PremiumPlanOption(
                id = "dhyan",
                label = stringResource(R.string.premium_dhyan_live),
                price = if (dhyanPricing.couponEligible) dhyanPricing.premiumPrice else dhyanPricing.standardPrice,
                subtitle = stringResource(
                    if (dhyanPricing.couponEligible) R.string.premium_existing_member_price
                    else R.string.premium_meditation_only
                ),
                durationLabel = stringResource(R.string.premium_six_months_lower),
                durationMonths = 6,
                courseId = "safar-30",
                badge = null,
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
                        fallbackError = localizedResources.getString(R.string.premium_payment_returned_inactive)
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
                    viewModel.notifyPaymentFailed(localizedResources.getString(R.string.premium_checkout_active_screen))
                    return@LaunchedEffect
                }
                val razorpayKeyId = state.keyId?.trim()?.takeIf { it.isNotEmpty() }
                if (razorpayKeyId == null) {
                    viewModel.notifyPaymentFailed(localizedResources.getString(R.string.premium_gateway_not_configured))
                    return@LaunchedEffect
                }
                refreshAfterPaymentReturn = state.planType != "dhyan"
                val checkout = Checkout()
                checkout.setKeyID(razorpayKeyId)
                val options = JSONObject().apply {
                    put("name", "Safar")
                    put(
                        "description",
                        localizedResources.getString(
                            if (state.planType == "dhyan") R.string.premium_dhyan_six_months
                            else R.string.premium_plus_dhyan
                        )
                    )
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
                viewModel.notifyPaymentFailed(e.message ?: localizedResources.getString(R.string.premium_checkout_launch_error))
            }
        }
    }

    val isTrialActive = premiumStatus.planType.orEmpty().contains("trial", ignoreCase = true)
    val isPremiumActive = premiumStatus.hasAnyPaidAccess && !isTrialActive
    val formattedExpiry = remember(premiumStatus.expiresAt) { formatPremiumExpiry(premiumStatus.expiresAt) }
    val formattedNewExpiry = remember(premiumStatus.expiresAt, selectedPlanDuration) {
        calculatePremiumExtensionExpiry(premiumStatus.expiresAt, selectedPlanDuration)
    }
    val planLabel = if (isPremiumActive) premiumPlanLabel(premiumStatus.planType) else stringResource(R.string.premium_free_plan)

    val updateSelectedPlan: (String) -> Unit = { planId ->
        selectedPlanId = planId
        val plan = plans.firstOrNull { it.id == planId }
        if (plan != null) {
            selectedPlanDuration = plan.durationMonths
        }
    }

    val membershipColors = remember(isDarkTheme) { premiumMembershipColors(isDarkTheme) }
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
    }
    SideEffect { activity?.window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false } }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme,
        LocalPremiumMembershipColors provides membershipColors,
        LocalSafarComposablesAccent provides membershipColors.action) {
      SafarComposablesTheme {
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
                title = { Text(stringResource(R.string.premium_dhyan_unlocked)) },
                text = { Text(stringResource(R.string.premium_dhyan_access_active)) },
                confirmButton = { Button(onClick = viewModel::resetState) { Text(stringResource(R.string.common_continue)) } },
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
            containerColor = membershipColors.canvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.premium_safar_premium),
                        color = membershipColors.onHero, fontFamily = PoppinsFontFamily,
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back),
                                tint = membershipColors.onHero)
                        }
                    },
                    // Reuse the original black/pink Titli and its existing guide action verbatim.
                    actions = {
                        Box(Modifier.padding(end = 12.dp).clip(CircleShape).background(membershipColors.paper)) {
                            com.safarparmar.app.ui.tutorial.TitliGuideButton()
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = membershipColors.hero),
                )
            },
            bottomBar = {
                if (plans.isNotEmpty()) PremiumBottomBar(
                    selectedPlan = selectedPlan, isPremiumActive = isPremiumActive, isLoading = isLoading,
                    isDarkTheme = isDarkTheme,
                    onPurchase = {
                        if (selectedPlan.isDhyanOnly) viewModel.createDhyanOrder()
                        else viewModel.createOrder(duration = selectedPlan.durationMonths)
                    },
                )
            },
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().background(membershipColors.canvas), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 760.dp)) {
                        PremiumMembershipHero(status = stringResource(when {
                            isPremiumActive -> R.string.premium_active_title
                            isTrialActive -> R.string.premium_trial_active_summary
                            else -> R.string.premium_free_plan
                        })) {
                            if (isPremiumActive) PremiumHeroDetail(Icons.Default.CalendarMonth,
                                formattedExpiry?.let { stringResource(R.string.premium_valid_until, it) }
                                    ?: stringResource(R.string.premium_named_plan_active, planLabel),
                                stringResource(R.string.premium_unlocked_manage_help))
                            else if (isTrialActive) PremiumHeroDetail(Icons.Default.CalendarMonth,
                                formattedExpiry?.let { stringResource(R.string.premium_valid_until, it) }
                                    ?: stringResource(R.string.premium_trial_is_active),
                                stringResource(R.string.premium_trial_bundle_help))
                            if (dhyanPricing.accessState == "DHYAN_INCLUDED") DhyanAccessNotice(
                                isPremiumActive = isPremiumActive,
                                onOpenDhyan = { onNavigate(Routes.LIVE_SESSIONS_ROOT) })
                        }
                    }
                }
                Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (!isPremiumActive && !isTrialActive) SevenDayTrialBanner(
                        isLoading = isLoading, isDarkTheme = isDarkTheme,
                        onStartTrial = { showTrialConfirmation = true })
                    if (dhyanPricing.accessState == "DHYAN_SCHEDULED") DhyanScheduledNotice(
                        startsAt = dhyanPricing.dhyanStartsAt, expiresAt = dhyanPricing.dhyanExpiresAt,
                        isDarkTheme = isDarkTheme)

                    PremiumBenefitGroups()

                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(when {
                        dhyanPricing.accessState == "LEGACY_PREMIUM_DISCOUNT" -> R.string.premium_choose_what_to_add
                        isPremiumActive -> R.string.premium_extend_your_plan
                        else -> R.string.premium_choose_your_plan
                    }), color = membershipColors.ink, fontFamily = PoppinsFontFamily,
                        fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(when {
                        dhyanPricing.accessState == "LEGACY_PREMIUM_DISCOUNT" -> R.string.premium_legacy_choice_help
                        isPremiumActive -> R.string.premium_extend_help
                        else -> R.string.premium_choose_help
                    }), color = membershipColors.muted, fontFamily = PoppinsFontFamily,
                        fontSize = 14.sp, lineHeight = 22.sp)
                    when (dhyanPricing.accessState) {
                        "LOADING" -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = membershipColors.action)
                        }
                        "ERROR" -> Surface(color = membershipColors.paper, shape = RoundedCornerShape(20.dp)) {
                            Text(stringResource(R.string.premium_verify_plans_error), color = membershipColors.muted,
                                fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.padding(20.dp))
                        }
                        else -> if (plans.isNotEmpty()) PremiumPricingPanel(
                            plans = plans, selectedPlanId = selectedPlanId, selectedPlan = selectedPlan,
                            currentExpiryText = formattedExpiry, newExpiryText = formattedNewExpiry,
                            isDarkTheme = isDarkTheme, enabled = !isLoading, onSelectPlan = updateSelectedPlan)
                    }
                    UiStateMessage(uiState)
                    PaywallFooter(isLoading = isLoading, isDarkTheme = isDarkTheme,
                        onRestore = { viewModel.refreshPremiumStatus(isRestore = true) })
                    if (plans.isEmpty()) Text(stringResource(R.string.premium_secure_checkout),
                        color = membershipColors.muted, fontSize = 11.sp, lineHeight = 17.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
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
                    .size(43.dp)
                    .clip(CircleShape)
                    .background(buttonBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "7",
                    fontFamily = LoraFontFamily,
                    fontSize = 20.4.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.premium_start_trial_title),
                fontFamily = LoraFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.premium_trial_description),
                fontSize = 12.sp,
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
                Text(stringResource(R.string.premium_start_free_trial), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = PlannerFlatColors.TextMuted, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(12.dp),
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
        state.isRestore && isTrial -> stringResource(R.string.premium_trial_active_title)
        state.isRestore -> stringResource(R.string.premium_active_title)
        isTrial -> stringResource(R.string.premium_trial_started_title)
        else -> stringResource(R.string.premium_payment_success_title)
    }

    val subtitleText = when {
        isTrial && dialogExpiry != null -> stringResource(R.string.premium_trial_active_until, dialogExpiry)
        isTrial -> stringResource(R.string.premium_trial_is_active)
        dialogExpiry != null -> stringResource(R.string.premium_plan_active_until, dialogExpiry)
        else -> stringResource(R.string.premium_named_plan_active, dialogPlanLabel)
    }

    val bodyText = when {
        isTrial -> stringResource(R.string.premium_trial_access_body)
        else -> stringResource(R.string.premium_paid_access_body)
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
                    .size(46.dp)
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
                fontSize = 16.sp,
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = bodyText,
                    fontSize = 11.sp,
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
                Text(stringResource(R.string.common_continue), fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        shape = RoundedCornerShape(12.dp),
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
                modifier = Modifier.size(43.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.premium_no_active_plan_title),
                fontFamily = LoraFontFamily,
                fontSize = 16.sp,
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.premium_no_active_plan_body),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlannerFlatColors.TextDark,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.premium_choose_later_help),
                    fontSize = 11.sp,
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
                Text(stringResource(R.string.premium_view_plans), fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        shape = RoundedCornerShape(12.dp),
    )
}


@Composable
private fun DhyanAccessNotice(isPremiumActive: Boolean, onOpenDhyan: () -> Unit) {
    val c = LocalPremiumMembershipColors.current
    PremiumHeroDetail(Icons.Default.PlayArrow, stringResource(R.string.premium_dhyan_active),
        stringResource(if (isPremiumActive) R.string.premium_dhyan_included_current else R.string.premium_dhyan_active_add_premium),
        action = {
            TextButton(onClick = onOpenDhyan, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(stringResource(R.string.common_open), color = c.mint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        })
}


@Composable
private fun DhyanScheduledNotice(startsAt: String?, expiresAt: String?, isDarkTheme: Boolean) {
    val c = LocalPremiumMembershipColors.current
    Surface(color = c.paper, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, c.border)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSectionHeader(Icons.Default.CheckCircle, stringResource(R.string.premium_next_bundle_scheduled))
            Text(stringResource(R.string.premium_next_bundle_help), color = c.ink, fontSize = 14.sp, lineHeight = 22.sp)
            Text(stringResource(R.string.premium_dhyan_schedule_range,
                formatPremiumExpiry(startsAt) ?: stringResource(R.string.premium_scheduled_start),
                formatPremiumExpiry(expiresAt) ?: stringResource(R.string.premium_scheduled_end)),
                color = c.muted, fontSize = 13.sp, lineHeight = 21.sp)
        }
    }
}


@Composable
private fun PremiumPricingPanel(plans: List<PremiumPlanOption>, selectedPlanId: String,
    selectedPlan: PremiumPlanOption, currentExpiryText: String?, newExpiryText: String?,
    isDarkTheme: Boolean = false, enabled: Boolean = true, onSelectPlan: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RadioPlanSelector(plans, selectedPlanId, isDarkTheme, enabled, onSelectPlan)
        SelectedPlanCard(selectedPlan, currentExpiryText, newExpiryText, isDarkTheme)
    }
}


@Composable
private fun RadioPlanSelector(plans: List<PremiumPlanOption>, selectedPlanId: String,
    isDarkTheme: Boolean = false, enabled: Boolean = true, onSelectPlan: (String) -> Unit) {
    val c = LocalPremiumMembershipColors.current
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { plan ->
            val selected = plan.id == selectedPlanId
            Surface(shape = RoundedCornerShape(18.dp), color = if (selected) c.selected else c.paper,
                border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp,
                    if (selected) c.action else c.border), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled,
                    role = Role.RadioButton, onClick = { onSelectPlan(plan.id) }).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = selected, onClick = null, enabled = enabled,
                        colors = RadioButtonDefaults.colors(selectedColor = c.action, unselectedColor = c.muted,
                            disabledSelectedColor = c.action.copy(alpha = .5f), disabledUnselectedColor = c.muted.copy(alpha = .5f)))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically) {
                            Text(plan.label, color = c.ink, fontFamily = PoppinsFontFamily,
                                fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
                            plan.badge?.let { badge ->
                                Text(badge, color = c.onBadge, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.badge)
                                        .padding(horizontal = 7.dp, vertical = 4.dp))
                            }
                        }
                        Text(plan.subtitle, color = c.muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        plan.originalPrice?.let { Text("₹$it", color = c.muted, fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough) }
                        Text("₹${plan.price}", color = if (selected) c.action else c.ink,
                            fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}


@Composable
private fun SelectedPlanCard(plan: PremiumPlanOption, currentExpiryText: String?, newExpiryText: String?,
    isDarkTheme: Boolean = false) {
    val c = LocalPremiumMembershipColors.current
    Surface(color = c.selected, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, c.border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!plan.isDhyanOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(c.lilac), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CalendarMonth, null, tint = c.hero, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.premium_current_expiry,
                            currentExpiryText ?: stringResource(R.string.premium_no_active_subscription)),
                            color = c.muted, fontSize = 12.sp, lineHeight = 19.sp)
                        Text(stringResource(R.string.premium_new_expiry,
                            newExpiryText ?: stringResource(R.string.premium_calculating)),
                            color = c.action, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(if (currentExpiryText != null) R.string.premium_bundle_starts_after_current
                            else R.string.premium_bundle_starts_today), color = c.muted, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                }
            } else {
                Text(stringResource(if (plan.price == 29) R.string.premium_existing_member_price else R.string.premium_standalone_dhyan),
                    color = c.action, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
            }
            var expanded by rememberSaveable(plan.id) { mutableStateOf(false) }
            Disclosure(expanded = expanded, onExpandedChange = { expanded = it }) {
                DisclosureButton(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp),
                    indicator = { UiIcon(if (it) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null, tint = c.action, modifier = Modifier.size(20.dp)) }) {
                    UiText(stringResource(R.string.premium_selected_plan, plan.label), color = c.action,
                        fontSize = 12.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                DisclosurePanel(contentPadding = PaddingValues(top = 4.dp),
                    enter = androidx.compose.animation.EnterTransition.None, exit = androidx.compose.animation.ExitTransition.None) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        UiText(stringResource(if (plan.isDhyanOnly) R.string.premium_dhyan_for_duration else R.string.premium_bundle_for_duration,
                            plan.durationLabel), color = c.ink, fontSize = 13.sp, lineHeight = 20.sp)
                        plan.originalPrice?.let { UiText(stringResource(R.string.premium_regular_price, it),
                            color = c.muted, fontSize = 12.sp, style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough)) }
                        UiText("₹${plan.price}", color = c.action, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun SevenDayTrialBanner(isLoading: Boolean, isDarkTheme: Boolean = false, onStartTrial: () -> Unit) {
    val c = LocalPremiumMembershipColors.current
    Surface(color = c.paper, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, c.border)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.premium_start_trial_title), color = c.ink, fontSize = 17.sp,
                fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.premium_trial_short_description), color = c.muted, fontSize = 13.sp, lineHeight = 21.sp)
            TextButton(onClick = onStartTrial, enabled = !isLoading) {
                if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = c.action, strokeWidth = 2.dp)
                else Text(stringResource(R.string.premium_try_free), color = c.action, fontWeight = FontWeight.Bold)
            }
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
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(10.dp),
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
private fun PaywallFooter(isLoading: Boolean, isDarkTheme: Boolean = false, onRestore: () -> Unit) {
    val c = LocalPremiumMembershipColors.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onRestore, enabled = !isLoading) {
            Text(stringResource(R.string.premium_restore), color = c.action, fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}


@Composable
private fun PremiumBottomBar(selectedPlan: PremiumPlanOption, isPremiumActive: Boolean, isLoading: Boolean,
    isDarkTheme: Boolean = false, onPurchase: () -> Unit) {
    val c = LocalPremiumMembershipColors.current
    Surface(color = c.canvas, shadowElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPurchase, enabled = !isLoading, shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.action, contentColor = c.onAction,
                        disabledContainerColor = c.action.copy(alpha = .55f), disabledContentColor = c.onAction)) {
                    if (isLoading) {
                        CircularProgressIndicator(color = c.onAction, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(if (isPremiumActive && !selectedPlan.isDhyanOnly) R.string.premium_extend_your_plan
                            else R.string.premium_buy_now), fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
                        Text("${selectedPlan.durationLabel} · ₹${selectedPlan.price}", fontSize = 12.sp,
                            lineHeight = 18.sp, textAlign = TextAlign.Center)
                    }
                    if (!isLoading) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(22.dp))
                }
                Text(stringResource(R.string.premium_secure_checkout), color = c.muted, fontSize = 10.sp,
                    lineHeight = 16.sp, textAlign = TextAlign.Center)
            }
        }
    }
}




@Composable
private fun premiumPlanLabel(planType: String?): String {
    val normalized = planType.orEmpty().lowercase(Locale.US)
    return when {
        "trial" in normalized -> stringResource(R.string.premium_plan_trial_label)
        "3month" in normalized || "3-month" in normalized -> stringResource(R.string.premium_plan_three_month_label)
        "6month" in normalized || "6-month" in normalized -> stringResource(R.string.premium_plan_six_month_label)
        normalized.isNotBlank() -> stringResource(R.string.premium_plan_generic_label)
        else -> stringResource(R.string.premium_safar_premium)
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
    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(extendedExpiry)
}

private fun formatPremiumExpiry(expiresAt: String?): String? {
    if (expiresAt.isNullOrBlank()) return null
    val instant = rawExpiryToInstant(expiresAt) ?: return expiresAt.take(10)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

private fun rawExpiryToInstant(expiresAt: String): Instant? {
    return runCatching { Instant.parse(expiresAt) }.getOrNull()
}
