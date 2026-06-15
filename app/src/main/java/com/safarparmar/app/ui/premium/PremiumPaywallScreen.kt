package com.safarparmar.app.ui.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.hilt.navigation.compose.hiltViewModel
import com.razorpay.Checkout
import org.json.JSONObject

private data class PremiumPlanOption(
    val id: String,
    val label: String,
    val price: Int,
    val subtitle: String,
    val badge: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()
    
    // Dynamic theme colors
    val screenBgColor = if (isDark) Color(0xFF0B0E14) else Color(0xFFEFF1FE)
    val cardContainerColor = if (isDark) Color(0xFF111622) else Color(0xFFF6F8FE)
    val cardBorderColor = if (isDark) Color(0xFF1F2937) else Color(0xFFE2E8F0)
    
    val lockBgColor = if (isDark) Color(0xFFFFD700).copy(alpha = 0.1f) else Color(0xFFFCD34D).copy(alpha = 0.2f)
    val lockBorderColors = if (isDark) {
        listOf(Color(0xFFFFD700), Color(0xFFDAA520).copy(alpha = 0.3f))
    } else {
        listOf(Color(0xFFF59E0B), Color(0xFFD97706).copy(alpha = 0.4f))
    }
    val lockIconColor = if (isDark) Color(0xFFFFD700) else Color(0xFFD97706)
    
    val textPrimaryColor = if (isDark) Color.White else Color(0xFF1E293B)
    val textSecondaryColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF475569)
    
    // Plan colors
    val planUnselectedBgColor = if (isDark) Color.Transparent else Color.White
    val planSelectedBgColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color(0xFFEEF2F6)
    val planUnselectedBorderColor = if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
    val planSelectedBorderColor = if (isDark) Color(0xFFB0A8FF) else Color(0xFF4F46E5)
    
    // Premium details card colors
    val detailsCardBgColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White
    val detailsHeaderColor = if (isDark) Color(0xFFF59E0B) else Color(0xFF4F46E5)
    val detailsDividerColor = if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
    val checklistIconColor = if (isDark) Color(0xFF10B981) else Color(0xFF4F46E5)
    val checklistTextColor = if (isDark) Color.White else Color(0xFF334155)
    
    // Payment method colors
    val methodUnselectedBgColor = if (isDark) Color.Transparent else Color.White
    val methodSelectedBgColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color(0xFFEEF2F6)
    val methodUnselectedBorderColor = if (isDark) Color(0xFF374151) else Color(0xFFE2E8F0)
    val methodSelectedBorderColor = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)

    LaunchedEffect(uiState) {
        if (uiState is PremiumUiState.OrderCreated) {
            val state = uiState as PremiumUiState.OrderCreated
            try {
                val checkout = Checkout()
                checkout.setKeyID(state.keyId ?: "rzp_live_SWHJBT7AXadF8a")
                
                val options = JSONObject()
                options.put("name", "Safar")
                options.put("description", "Safar Premium")
                options.put("theme.color", "#0A4E70")
                options.put("currency", state.order.currency)
                options.put("amount", state.order.amount)
                options.put("order_id", state.order.id)
                
                val retryObj = JSONObject()
                retryObj.put("enabled", true)
                retryObj.put("max_count", 4)
                options.put("retry", retryObj)

                checkout.open(activity, options)
                viewModel.resetState()
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.notifyPaymentFailed(e.message ?: "Error launching checkout")
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lockGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Shimmer effect animation on price
    val shimmerTranslateX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslateX"
    )

    val baseColor = if (isDark) Color.White else Color(0xFF0F172A)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            baseColor,
            if (isDark) Color(0xFFC7D2FE) else Color(0xFF818CF8), // Lavender shimmer shine matching new mockup colors
            baseColor,
            baseColor,
        ),
        start = Offset(shimmerTranslateX, 0f),
        end = Offset(shimmerTranslateX + 150f, 150f)
    )

    val plans = listOf(
        PremiumPlanOption(
            id = "3month",
            label = "3 Months Access",
            price = 69,
            subtitle = "Start small for your next target",
        ),
        PremiumPlanOption(
            id = "6month",
            label = "6 Months Access",
            price = 99,
            subtitle = "Good for one exam cycle",
            badge = "Popular",
        ),
        PremiumPlanOption(
            id = "yearly",
            label = "Yearly Access",
            price = 149,
            subtitle = "Best value for serious prep",
            badge = "Best value",
        ),
    )
    var selectedPlanId by remember { mutableStateOf("3month") }
    var selectedMethod by remember { mutableStateOf("upi") } // "upi", "card", "netbanking"

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBgColor)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                border = BorderStroke(1.dp, cardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Gold Lock Icon
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Animated Aura Glow
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = glowScale
                                    scaleY = glowScale
                                }
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            if (isDark) Color(0xFFFFD700).copy(alpha = 0.35f * glowAlpha) else Color(0xFFF59E0B).copy(alpha = 0.25f * glowAlpha),
                                            if (isDark) Color(0xFFDAA520).copy(alpha = 0.12f * glowAlpha) else Color(0xFFD97706).copy(alpha = 0.08f * glowAlpha),
                                            Color.Transparent,
                                        )
                                    ),
                                    androidx.compose.foundation.shape.CircleShape,
                                )
                        )
                        
                        // Golden Lock Container (now Purple/Indigo)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = lockBgColor,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(lockBorderColors),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = lockIconColor,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Unlock Safar Premium",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Plan better. Stay regular. Prepare with clarity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondaryColor
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Plan Selection
                    Text(
                        text = "Choose a Plan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    plans.forEach { plan ->
                        val isPlanSelected = selectedPlanId == plan.id
                        val planBgColor = if (isPlanSelected) planSelectedBgColor else planUnselectedBgColor
                        val planBorderColor = if (isPlanSelected) planSelectedBorderColor else planUnselectedBorderColor
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = planBgColor),
                            border = BorderStroke(1.dp, planBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedPlanId = plan.id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    // Custom Radio Button
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(
                                                width = if (isPlanSelected) 0.dp else 1.5.dp,
                                                color = if (isPlanSelected) Color.Transparent else (if (isDark) Color(0xFF9CA3AF) else Color(0xFF94A3B8)),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                            .background(
                                                color = if (isPlanSelected) planSelectedBorderColor else Color.Transparent,
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlanSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(if (isDark) Color.Black else Color.White, androidx.compose.foundation.shape.CircleShape)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = plan.label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textPrimaryColor,
                                            )
                                            plan.badge?.let { badge ->
                                                Text(
                                                    text = badge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = planSelectedBorderColor,
                                                )
                                            }
                                        }
                                        Text(
                                            text = plan.subtitle,
                                            fontSize = 11.sp,
                                            color = textSecondaryColor,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "\u20B9${plan.price}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlanSelected) planSelectedBorderColor else textPrimaryColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium Access card with gradient border
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = detailsCardBgColor),
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5))
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "WHAT YOU GET",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = detailsHeaderColor,
                                    letterSpacing = 0.5.sp
                                )
                                val currentPlan = plans.first { it.id == selectedPlanId }
                                val summaryPriceSuffix = when (selectedPlanId) {
                                    "3month" -> " / 3 months"
                                    "6month" -> " / 6 months"
                                    else -> " / year"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "\u20B9${currentPlan.price}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            brush = shimmerBrush
                                        )
                                    )
                                    Text(
                                        text = summaryPriceSuffix,
                                        fontSize = 12.sp,
                                        color = textSecondaryColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = detailsDividerColor)
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            val features = listOf(
                                "Know if your exam plan is on track",
                                "See completed work, pending topics, and overdue chapters",
                                "Paste syllabus and let AI arrange it",
                                "Missed topics get adjusted in your schedule",
                                "Detailed Ekagra study reports and history",
                                "Private Mehfil Connect with other students",
                                "Dhyan audio and guided learning sessions",
                                "Live Vartalap sessions with Parmar Sir",
                            )
                            features.forEach { feature ->
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = checklistIconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = feature,
                                        fontSize = 14.sp,
                                        color = checklistTextColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val paymentMethods = listOf(
                        Triple("upi", "UPI", Icons.Default.QrCode),
                        Triple("card", "Credit / Debit Card", Icons.Default.CreditCard),
                        Triple("netbanking", "Net Banking", Icons.Default.AccountBalance)
                    )
                    
                    paymentMethods.forEach { (id, label, icon) ->
                        val isMethodSelected = selectedMethod == id
                        val rowBgColor = if (isMethodSelected) methodSelectedBgColor else methodUnselectedBgColor
                        val rowBorderColor = if (isMethodSelected) methodSelectedBorderColor else methodUnselectedBorderColor
                        
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = rowBgColor),
                            border = BorderStroke(1.dp, rowBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { selectedMethod = id }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Custom Radio Button
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(
                                            width = if (isMethodSelected) 0.dp else 1.5.dp,
                                            color = if (isMethodSelected) Color.Transparent else (if (isDark) Color(0xFF9CA3AF) else Color(0xFF94A3B8)),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .background(
                                            color = if (isMethodSelected) methodSelectedBorderColor else Color.Transparent,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isMethodSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isMethodSelected) methodSelectedBorderColor else (if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (uiState is PremiumUiState.Error) {
                        Text(
                            text = (uiState as PremiumUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState is PremiumUiState.PaymentSuccess) {
                        val status = (uiState as PremiumUiState.PaymentSuccess).status
                        Text(
                            text = "Safar Premium is active${status.expiresAt?.let { " until $it" }.orEmpty()}.",
                            color = Color(0xFF4CAF50),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            val activePlan = plans.first { it.id == selectedPlanId }
                            val courseId = when (selectedPlanId) {
                                "3month" -> "study-planner-pro-3month"
                                "6month" -> "study-planner-pro-6month"
                                else -> "study-planner-pro-yearly"
                            }
                            viewModel.createOrder(amount = activePlan.price, courseId = courseId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5),
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState is PremiumUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Pay Securely",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDark) Color.Transparent else Color(0xFFEEF2F6),
                            contentColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4F46E5)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF374151) else Color(0xFFC7D2FE)
                        )
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    TextButton(
                        onClick = { viewModel.refreshPremiumStatus() },
                        enabled = uiState !is PremiumUiState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Restore Safar Premium")
                    }
                }
            }
        }
    }
}
