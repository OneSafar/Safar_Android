package com.safarparmar.app.ui.maintenance

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.data.remote.maintenance.MaintenanceInfo
import com.safarparmar.app.ui.theme.isLightBackground

@Composable
fun MaintenanceScreen(
    info: MaintenanceInfo,
    isChecking: Boolean,
    onCheckStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "maintenance_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val primaryAccent = if (isLight) Color(0xFF6D28D9) else Color(0xFFA78BFA)
    val accentBg = if (isLight) Color(0xFFF5F3FF) else Color(0xFF2E1065).copy(alpha = 0.6f)
    val cardBorder = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155).copy(alpha = 0.6f)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E293B).copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.height(16.dp))

                // Status Pill Badge
                Surface(
                    color = primaryAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryAccent)
                                .scale(pulseScale)
                                .alpha(glowAlpha),
                        )
                        Text(
                            text = "SYSTEM & DATABASE UPGRADE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = primaryAccent,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Hero Visual / Icon Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp),
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(primaryAccent.copy(alpha = 0.10f * glowAlpha)),
                    )
                    // Middle ring
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(primaryAccent.copy(alpha = 0.18f)),
                    )
                    // Core Icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(primaryAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Main Title & Subtitle as requested
                Text(
                    text = info.title.ifBlank { "App Under Maintenance !" },
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = info.message.ifBlank { "Check Back Soon......" },
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = primaryAccent,
                )

                Spacer(Modifier.height(24.dp))

                // Information & Reassurance Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MaintenanceInfoCard(
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF10B981),
                        title = "Your Data is 100% Safe",
                        description = "Your study streak, focus logs, syllabus progress, and journal notes are securely preserved.",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                    )

                    MaintenanceInfoCard(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = primaryAccent,
                        title = "Database & Server Optimization",
                        description = info.detail ?: "We are performing scheduled server and database enhancements to make Safar faster and more reliable.",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                    )

                    MaintenanceInfoCard(
                        icon = Icons.Default.CheckCircle,
                        iconTint = Color(0xFF3B82F6),
                        title = "Automatic Reconnection",
                        description = "This screen will automatically dismiss and take you back to your study space as soon as maintenance finishes.",
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Bottom Actions & Live Status Polling
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onCheckStatus,
                    enabled = !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLight) Color(0xFF581C87) else Color(0xFF6D28D9),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Checking server status...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Check Status Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }

                Text(
                    text = "Auto-checking in background every 15 seconds",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MaintenanceInfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    cardBg: Color,
    cardBorder: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
