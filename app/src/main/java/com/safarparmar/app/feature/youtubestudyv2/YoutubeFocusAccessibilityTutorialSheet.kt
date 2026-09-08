package com.safarparmar.app.feature.youtubestudyv2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R

/**
 * Native Compose illustrated guide to enabling
 * SAFAR YouTube Focus in Android Accessibility settings.
 *
 * Pointer motion reads the master clock in graphicsLayer, keeping the settings
 * layout out of frame-by-frame recomposition. System settings may vary by device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeFocusAccessibilityTutorialSheet(
    onDismiss: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    isLight: Boolean = true,
    onContinueGuide: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.90f).widthIn(max = 520.dp).fillMaxHeight(0.88f),
            shape = RoundedCornerShape(26.dp),
            color = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A),
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("YouTube Focus", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = if (isLight) YTCMColors.RoyalPurple else Color(0xFFC084FC))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PhoneMockup(isLight, Modifier.fillMaxWidth().height(430.dp))
                    Text("Settings may look different on your phone.",
                        fontSize = 12.sp, color = Color(0xFF64748B))
                }
                Text("The guide will float beside Settings when supported.",
                    fontSize = 12.sp, color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8))
                Button(
                    onClick = {
                        if (onContinueGuide != null) {
                            onContinueGuide()
                        } else {
                            onDismiss()
                            onOpenAccessibilitySettings()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YTCMColors.ctaFill(isLight),
                        contentColor = if (isLight) Color.White else Color(0xFF1E1033),
                    ),
                ) {
                    Text("Got it", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Phone Mockup Screen ───────────────────────────────────────────────────────

@Composable
internal fun PhoneMockup(
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val phoneFrameBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val phoneBorder = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)

    // A single clock drives the gesture in the draw phase. Only step/contact
    // boundaries invalidate composition; the static settings UI skips each frame.
    val progress = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                progress.snapTo(0f)
                // Stop issuing animation frames during reading/completion holds.
                progress.animateTo(0.19f, tween(3000, easing = LinearEasing))
                delay(600)
                progress.animateTo(0.39f, tween(3000, easing = LinearEasing))
                delay(500)
                progress.animateTo(0.45f, tween(500, easing = LinearEasing))
                delay(1800)
                progress.animateTo(0.79f, tween(3600, easing = LinearEasing))
                delay(500)
                progress.animateTo(0.82f, tween(450, easing = LinearEasing))
                delay(2500)
            }
        }
    }
    val effectiveStep by remember {
        derivedStateOf {
            when {
                progress.value < 0.20f -> 1
                progress.value < 0.40f -> 2
                progress.value < 0.80f -> 3
                else -> 4
            }
        }
    }
    val step1Progress = remember { { (progress.value / 0.20f).coerceIn(0f, 1f) } }
    val step2Progress = remember { { ((progress.value - 0.20f) / 0.20f).coerceIn(0f, 1f) } }
    val step3Progress = remember { { ((progress.value - 0.40f) / 0.40f).coerceIn(0f, 1f) } }
    val stillProgress = remember { { 0f } }

    // Phone Shell
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(phoneFrameBg)
            .border(2.dp, phoneBorder, RoundedCornerShape(22.dp)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated Android Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (effectiveStep) {
                        1 -> "1 of 4 · Open YouTube Focus"
                        2 -> "2 of 4 · Tap the main switch"
                        3 -> "3 of 4 · Review and allow"
                        else -> "4 of 4 · YouTube Focus is on"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                )
                // Small camera notch / pill
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B))
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 6.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isLight) Color(0xFF475569) else Color(0xFF94A3B8))
                    )
                }
            }

            // Screen Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF030712)),
            ) {
                AnimatedContent(
                    targetState = effectiveStep,
                    transitionSpec = {
                        if (initialState == 1 && targetState == 2) {
                            (slideInHorizontally(tween(450)) { it / 5 } + fadeIn(tween(350)))
                                .togetherWith(slideOutHorizontally(tween(450)) { -it / 5 } + fadeOut(tween(250)))
                        } else {
                            fadeIn(tween(450)).togetherWith(fadeOut(tween(350)))
                        }
                    },
                    label = "ScreenStepSlideTransition",
                ) { targetStep ->
                    when (targetStep) {
                        1 -> MockAccessibilityScreen(
                            tapProgress = step1Progress,
                            isLight = isLight,
                        )
                        else -> Box(Modifier.fillMaxSize()) {
                            MockSafarServiceScreen(
                                switchChecked = targetStep == 4,
                                tapProgress = if (targetStep == 2) step2Progress else stillProgress,
                                isLight = isLight,
                                showPointer = targetStep == 2,
                            )
                            if (targetStep == 3) MockPermissionConfirmation(
                                tapProgress = step3Progress,
                                isLight = isLight,
                            )
                        }
                    }
                }
            }
            Surface(color = if (isLight) Color(0xFFF3E8FF) else Color(0xFF3B0764)) {
                Text(
                    text = when (effectiveStep) {
                        1 -> "1. Open “SAFAR YouTube Focus”."
                        2 -> "2. Turn on the main switch. Leave Shortcut off."
                        3 -> "3. Read the permission details, then tap Allow."
                        else -> "4. It’s on. Return to Safar to continue."
                    },
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    fontSize = 13.sp, lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLight) YTCMColors.RoyalPurple else Color(0xFFE9D5FF),
                )
            }
        }
    }
}

// ── Screen 1: Android Accessibility List ─────────────────────────────────────

@Composable
private fun MockAccessibilityScreen(
    tapProgress: () -> Float,
    isLight: Boolean,
) {
    // Tap choreography:
    // 0.00 .. 0.50 -> Finger approaches target
    // 0.50 .. 0.75 -> Tap contact & pulse
    // 0.75 .. 1.00 -> Transition / hold
    val isTapping by remember(tapProgress) {
        derivedStateOf { tapProgress() in 0.60f..0.69f }
    }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    var targetCenter by remember { mutableStateOf(Offset.Zero) }
    val rowPressScale by animateFloatAsState(
        targetValue = if (isTapping) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "RowScaleSpring",
    )

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned {
        screenOrigin = it.positionInRoot()
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Screen App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (isLight) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Accessibility",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                )
            }

            // Section Label
            Text(
                text = "General  ·  More",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
            )

            // Highlighted Target Row: SAFAR YouTube Focus
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLight) Color.White else Color(0xFF1E293B),
                border = BorderStroke(
                    1.5.dp,
                    if (isTapping) Color(0xFF6366F1) else Color(0xFF6366F1).copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = rowPressScale
                        scaleY = rowPressScale
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Safar App Logo / Icon
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)),
                        modifier = Modifier.size(32.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_safar_launcher_foreground),
                            contentDescription = "SAFAR",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SAFAR YouTube Focus",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        )
                        Text(
                            text = "Off",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        )
                    }

                    // Measure the actual contact point so the gesture follows text/window sizing.
                    Surface(
                        modifier = Modifier.onGloballyPositioned {
                            targetCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF6366F1),
                    ) {
                        Text(
                            text = "TAP HERE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            // Dummy background items for context realism
            Text(
                text = "Other services",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp),
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isLight) Color.White.copy(alpha = 0.7f) else Color(0xFF1E293B).copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("T", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    }
                    Column {
                        Text(
                            text = "Switch Access",
                            fontSize = 11.sp,
                            color = if (isLight) Color(0xFF334155) else Color(0xFFCBD5E1),
                        )
                        Text(
                            text = "Off",
                            fontSize = 9.sp,
                            color = if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B),
                        )
                    }
                }
            }
        }

        AnimatedGesturePointer(
            target = { targetCenter - screenOrigin },
            progress = tapProgress,
            approachEnd = 0.54f,
            tapStart = 0.60f,
            isPressed = isTapping,
        )
    }
}

// ── Screen 2: SAFAR YouTube Focus Subscreen ───────────────────────────────────

@Composable
private fun MockSafarServiceScreen(
    switchChecked: Boolean,
    tapProgress: () -> Float,
    isLight: Boolean,
    showPointer: Boolean = true,
) {
    // Tap choreography:
    // 0.00 .. 0.40 -> Finger moves toward switch
    // 0.40 .. 0.65 -> Finger taps switch
    // 0.65 .. 1.00 -> Comprehension hold with switch ON
    val isTapping by remember(tapProgress) {
        derivedStateOf { tapProgress() in 0.44f..0.50f }
    }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    var targetCenter by remember { mutableStateOf(Offset.Zero) }

    val cardBgColor by animateColorAsState(
        targetValue = if (switchChecked) {
            if (isLight) Color(0xFFDCFCE7) else Color(0xFF064E3B)
        } else {
            if (isLight) Color(0xFFE8EEFC) else Color(0xFF1E293B)
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "CardBgColor",
    )

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned {
        screenOrigin = it.positionInRoot()
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Screen App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (isLight) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "SAFAR YouTube Focus",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLight) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                )
            }

            // Primary Toggle Card (Matches Android Settings Pill from screenshot)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(
                    1.5.dp,
                    if (switchChecked) Color(0xFF10B981) else Color(0xFF6366F1).copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SAFAR YouTube Focus",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (switchChecked) {
                                if (isLight) Color(0xFF065F46) else Color(0xFF6EE7B7)
                            } else {
                                if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            },
                        )
                        Text(
                            text = if (switchChecked) "On · Return to Safar" else "Off",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (switchChecked) {
                                if (isLight) Color(0xFF047857) else Color(0xFFA7F3D0)
                            } else {
                                if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                            },
                        )
                    }

                    // Simulated Animated Switch
                    MockMaterialSwitch(
                        checked = switchChecked,
                        isTapping = isTapping,
                        modifier = Modifier.onGloballyPositioned {
                            targetCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        },
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLight) Color.White else Color(0xFF1E293B),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Shortcut", fontSize = 12.sp)
                        Text("Off · Leave this off", fontSize = 10.sp,
                            color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8))
                    }
                    MockMaterialSwitch(checked = false, isTapping = false)
                }
            }

            // Info Card below
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isLight) Color.White.copy(alpha = 0.8f) else Color(0xFF1E293B).copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "About this function",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                    )
                    Text(
                        text = "YouTube Focus checks video channels to block distractions while allowing your chosen study channels.",
                        fontSize = 9.5.sp,
                        color = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                        lineHeight = 13.sp,
                    )
                }
            }
        }

        if (showPointer) AnimatedGesturePointer(
            target = { targetCenter - screenOrigin },
            progress = tapProgress,
            approachEnd = 0.38f,
            tapStart = 0.44f,
            isPressed = isTapping,
        )
    }
}

/**
 * Illustrated system consent prompt, not an interactive permission dialog.
 * Keep the scope of the permission visible before demonstrating Allow.
 */
@Composable
private fun MockPermissionConfirmation(tapProgress: () -> Float, isLight: Boolean) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var allowCenter by remember { mutableStateOf(Offset.Zero) }
    val pressed by remember(tapProgress) {
        derivedStateOf { tapProgress() in 0.78f..0.83f }
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f))
            .onGloballyPositioned { origin = it.positionInRoot() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            shape = RoundedCornerShape(22.dp),
            color = if (isLight) Color(0xFFFAFAFA) else Color(0xFF1E293B),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Allow “SAFAR YouTube Focus” to have full control of your device?",
                    fontSize = 13.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                )
                Text(
                    "Read the system permission details before allowing access.",
                    fontSize = 10.sp, lineHeight = 14.sp,
                    color = if (isLight) Color(0xFF64748B) else Color(0xFFCBD5E1),
                )
                Text("View and control screen", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Read screen content and display content over other apps.",
                    fontSize = 10.sp, lineHeight = 13.sp)
                Text("View and perform actions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Track interactions and perform actions on your behalf.",
                    fontSize = 10.sp, lineHeight = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text("Deny", fontSize = 12.sp, color = Color(0xFF64748B),
                        modifier = Modifier.padding(10.dp))
                    Text(
                        "Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.onGloballyPositioned {
                            allowCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        }.padding(10.dp),
                    )
                }
            }
        }
        AnimatedGesturePointer(
            target = { allowCenter - origin },
            progress = tapProgress,
            approachEnd = 0.73f,
            tapStart = 0.78f,
            isPressed = pressed,
        )
    }
}

// ── Mock Material You Switch ──────────────────────────────────────────────────

@Composable
private fun MockMaterialSwitch(
    checked: Boolean,
    isTapping: Boolean,
    modifier: Modifier = Modifier,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF6366F1) else Color(0xFF78716C).copy(alpha = 0.35f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "SwitchTrackColor",
    )
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 18f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "SwitchThumbOffset",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isTapping) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "SwitchThumbScale",
    )

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    translationX = thumbOffset * density
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(Color.White)
                .shadow(2.dp, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(12.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFF78716C),
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

// ── Animated Gesture Pointer (Touch Cursor) ───────────────────────────────────

@Composable
private fun AnimatedGesturePointer(
    target: () -> Offset,
    progress: () -> Float,
    approachEnd: Float,
    tapStart: Float,
    isPressed: Boolean,
    modifier: Modifier = Modifier,
) {
    val cursorScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "CursorScale",
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                val p = progress()
                val approach = FastOutSlowInEasing.transform(
                    ((p - 0.12f) / (approachEnd - 0.12f)).coerceIn(0f, 1f)
                )
                val contact = target()
                translationX = contact.x - size.width / 2 - (1 - approach) * 40.dp.toPx()
                translationY = contact.y - size.height / 2 + (1 - approach) * 64.dp.toPx()
                alpha = if (contact == Offset.Zero) 0f else
                    (p / 0.10f).coerceIn(0f, 1f) * ((0.94f - p) / 0.14f).coerceIn(0f, 1f)
                scaleX = cursorScale
                scaleY = cursorScale
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(44.dp).graphicsLayer {
                val ripple = ((progress() - tapStart) / 0.14f).coerceIn(0f, 1f)
                scaleX = 0.4f + ripple * 1.2f
                scaleY = scaleX
                alpha = if (ripple > 0f && ripple < 1f) (1 - ripple) * 0.35f else 0f
            }.background(Color(0xFF6366F1), CircleShape)
        )

        // Touch Cursor Outer Glow & Body
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, Color(0xFF6366F1), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Hand/Touch Icon indicator
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
