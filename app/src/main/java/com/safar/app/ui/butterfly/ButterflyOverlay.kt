package com.safar.app.ui.butterfly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.ui.theme.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.delay

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * Full-screen butterfly guided-tour overlay.
 *
 * Drop this anywhere inside your root composable (e.g. inside [Surface] in MainActivity). It is
 * transparent / click-through when [state].isVisible == false.
 *
 * Example:
 * ```
 * val steps = listOf(
 *     ButterflyTourStep("Welcome", "Tap here to explore Mehfil.", 0.15f, 0.20f),
 *     ButterflyTourStep("Profile", "Your profile lives here.", 0.85f, 0.12f),
 * )
 * val tourState = rememberButterflyTourState(steps)
 *
 * Box(Modifier.fillMaxSize()) {
 *     SafarNavGraph()
 *     ButterflyOverlay(state = tourState)
 * }
 *
 * // Trigger anywhere:
 * LaunchedEffect(Unit) { tourState.start() }
 * ```
 */
@Composable
fun ButterflyOverlay(
        state: ButterflyTourState,
        wingColor: Color = Color(0xFFF082DC), // pinkish purple — Fay's butterfly
        bodyColor: Color = Color(0xFF500F50), // dark purple
        butterflySize: Dp = 88.dp,
        dimColor: Color = Color(0x55000000),
        autoAdvanceMs: Long = 3200L,
) {
    if (!state.isVisible) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val bfPx = with(density) { butterflySize.toPx() }

        // ── Animatable butterfly position ──────────────────────────
        val bfX = remember { Animatable(W * (state.currentStep?.anchorX ?: 0.5f)) }
        val bfY = remember { Animatable(H * (state.currentStep?.anchorY ?: 0.5f)) }

        // ── Glitter system ─────────────────────────────────────────
        val glitter = remember { GlitterSystem() }
        var showTooltip by remember { mutableStateOf(false) }
        var trailMs by remember { mutableLongStateOf(0L) }

        // Trail: ring buffer of recent positions for tapering ribbon
        val trailHistory = remember { mutableStateListOf<Offset>() }
        val maxTrail = 50

        // ── Tick loop: glitter physics + trail spawn ───────────────
        LaunchedEffect(state.isVisible) {
            var prevX = bfX.value
            var prevY = bfY.value
            while (state.isVisible) {
                withFrameMillis { ms ->
                    glitter.tick()
                    val dx = bfX.value - prevX
                    val dy = bfY.value - prevY
                    val speed = sqrt(dx * dx + dy * dy)
                    val moving = speed > 0.5f
                    if (ms - trailMs > 22L) {
                        if (moving) {
                            val pos = Offset(bfX.value, bfY.value)
                            trailHistory.add(pos)
                            if (trailHistory.size > maxTrail) trailHistory.removeAt(0)
                            // Ambient sparkles around butterfly
                            glitter.spawn(bfX.value, bfY.value, 5)
                            // Glitter tail drifting behind
                            glitter.spawnTail(prevX, prevY, dx, dy, 4)
                        } else {
                            // Idle sparkle — fewer, just ambient twinkle
                            glitter.spawn(bfX.value, bfY.value, 2)
                        }
                        prevX = bfX.value
                        prevY = bfY.value
                        trailMs = ms
                    }
                }
            }
        }

        // ── Step change: fly butterfly to new anchor ───────────────
        LaunchedEffect(state.currentStepIndex, state.isVisible) {
            val step = state.currentStep ?: return@LaunchedEffect
            showTooltip = false

            // Capture start ONCE before the loop — bezier needs a fixed origin
            val sx = bfX.value
            val sy = bfY.value
            val tx = W * step.anchorX
            val ty = H * step.anchorY

            // Control point: midpoint offset perpendicular to the flight vector
            // giving a natural arcing bow rather than a straight line
            val midX = (sx + tx) / 2f
            val midY = (sy + ty) / 2f
            val perpX = -(ty - sy) // perpendicular to flight direction
            val perpY = (tx - sx)
            val bowStrength = 0.22f // how much arc — tune 0.1 (subtle) to 0.4 (big arc)
            val cpx = midX + perpX * bowStrength
            val cpy = midY + perpY * bowStrength

            // Fly along the quadratic bezier B(t) = (1-t)²·S + 2(1-t)t·CP + t²·T
            val dur = 1400L
            val steps = 80 // more steps → smoother
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                val nt = EaseInOut.transform(t)
                val nx = (1 - nt) * (1 - nt) * sx + 2 * (1 - nt) * nt * cpx + nt * nt * tx
                val ny = (1 - nt) * (1 - nt) * sy + 2 * (1 - nt) * nt * cpy + nt * nt * ty
                bfX.snapTo(nx)
                bfY.snapTo(ny)
                glitter.spawn(nx, ny, 3)
                delay(dur / steps)
            }
            bfX.snapTo(tx)
            bfY.snapTo(ty)
            showTooltip = true

            // Auto-advance
            if (!state.isLastStep) {
                delay(autoAdvanceMs)
                state.next()
            }
        }

        // ── Dim background ─────────────────────────────────────────
        Box(
                modifier =
                        Modifier.fillMaxSize().background(dimColor).clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                ) { /* consume touches */}
        )

        // ── Trail ribbon + sparkle canvas ─────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {

            // 1. Tapering ribbon — thicker near butterfly, tapers to nothing at tail
            if (trailHistory.size > 1) {
                for (i in 1 until trailHistory.size) {
                    val pt = trailHistory[i]
                    val prevPt = trailHistory[i - 1]
                    val progress = i.toFloat() / trailHistory.size.toFloat()
                    // Inner bright core
                    drawLine(
                            color = Color(0xFFFFB0F8).copy(alpha = progress * 0.55f),
                            start = Offset(prevPt.x, prevPt.y),
                            end = Offset(pt.x, pt.y),
                            strokeWidth = progress * 5f,
                    )
                    // Outer soft glow
                    drawLine(
                            color = Color(0xFFF082DC).copy(alpha = progress * 0.22f),
                            start = Offset(prevPt.x, prevPt.y),
                            end = Offset(pt.x, pt.y),
                            strokeWidth = progress * 14f,
                    )
                }
            }

            // 2. Sparkles — pink/gold circles + star crosses
            for (p in glitter.particles) {
                val alpha = p.life.coerceIn(0f, 1f)
                val currentSize = p.size * alpha.coerceAtLeast(0f)
                if (currentSize <= 0f) continue

                val baseColor =
                        when {
                            p.isGold -> Color(1f, 0.88f, 0.30f, alpha)
                            p.isTail -> Color(1f, 0.70f, 0.98f, alpha)
                            else -> Color(1f, 0.59f, 0.98f, alpha)
                        }

                drawCircle(color = baseColor, radius = currentSize, center = Offset(p.x, p.y))

                // Star cross on star-flagged particles
                if (p.isStar && currentSize > 1.2f) {
                    val arm = currentSize * 2.0f
                    val starAlpha = alpha * 0.75f
                    drawLine(
                            color = baseColor.copy(alpha = starAlpha),
                            start = Offset(p.x - arm, p.y),
                            end = Offset(p.x + arm, p.y),
                            strokeWidth = 0.9f,
                    )
                    drawLine(
                            color = baseColor.copy(alpha = starAlpha),
                            start = Offset(p.x, p.y - arm),
                            end = Offset(p.x, p.y + arm),
                            strokeWidth = 0.9f,
                    )
                    // Diagonal arms for 8-point star on tail sparkles
                    if (p.isTail) {
                        val diag = arm * 0.65f
                        drawLine(
                                color = baseColor.copy(alpha = starAlpha * 0.6f),
                                start = Offset(p.x - diag, p.y - diag),
                                end = Offset(p.x + diag, p.y + diag),
                                strokeWidth = 0.7f,
                        )
                        drawLine(
                                color = baseColor.copy(alpha = starAlpha * 0.6f),
                                start = Offset(p.x + diag, p.y - diag),
                                end = Offset(p.x - diag, p.y + diag),
                                strokeWidth = 0.7f,
                        )
                    }
                }
            }
        }

        // ── Butterfly ──────────────────────────────────────────────
        val bfOffX = (bfX.value - bfPx / 2f).roundToInt()
        val bfOffY = (bfY.value - bfPx / 2f).roundToInt()
        ButterflyDrawing(
                wingColor = wingColor,
                bodyColor = bodyColor,
                modifier = Modifier.size(butterflySize).offset { IntOffset(bfOffX, bfOffY) },
        )

        // ── Tooltip card ───────────────────────────────────────────
        AnimatedVisibility(
                visible = showTooltip,
                enter = fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.88f),
                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.88f),
        ) {
            val step = state.currentStep
            if (step != null) {
                val cardW = 220.dp
                val cardH = 160.dp
                val cardWPx = with(density) { cardW.toPx() }
                val cardHPx = with(density) { cardH.toPx() }
                val margin = with(density) { 12.dp.toPx() }
                val navBarH = with(density) { 80.dp.toPx() } // reserve space for bottom nav

                // Start tooltip to the right of butterfly, prefer above center
                var tx = bfX.value + 64f
                var ty = bfY.value - 60f

                // Push left if overflowing right edge
                if (tx + cardWPx > W - margin) tx = bfX.value - cardWPx - 18f
                // Push right if overflowing left edge
                if (tx < margin) tx = margin
                // Push down if overflowing top
                if (ty < margin) ty = bfY.value + 64f
                // Push up if overflowing bottom nav
                if (ty + cardHPx > H - navBarH - margin) ty = H - navBarH - cardHPx - margin
                // Final clamp
                tx = tx.coerceIn(margin, W - cardWPx - margin)
                ty = ty.coerceIn(margin, H - navBarH - cardHPx - margin)

                TooltipCard(
                        step = step,
                        stepIndex = state.currentStepIndex,
                        totalSteps = state.steps.size,
                        isLast = state.isLastStep,
                        offsetX = tx.roundToInt(),
                        offsetY = ty.roundToInt(),
                        onNext = { state.next() },
                        onSkip = { state.dismiss() },
                )
            }
        }
    }
}

@Composable
private fun TooltipCard(
        step: ButterflyTourStep,
        stepIndex: Int,
        totalSteps: Int,
        isLast: Boolean,
        offsetX: Int,
        offsetY: Int,
        onNext: () -> Unit,
        onSkip: () -> Unit,
) {
    Surface(
            modifier =
                    Modifier.offset { IntOffset(offsetX, offsetY) }
                            .widthIn(min = 180.dp, max = 240.dp),
            shape = RoundedCornerShape(16.dp),
            color = ButterflyCardBg,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Step indicator dots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = if (isLast) "✦ done!" else "stop ${stepIndex + 1} / $totalSteps",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ButterflyGoldDark,
                        letterSpacing = 0.08.sp,
                        modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                    text = step.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ButterflyTextDark,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                    text = step.message,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = ButterflyTextDark,
                    lineHeight = 17.sp,
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                        onClick = onSkip,
                        modifier = Modifier.height(30.dp),
                        contentPadding =
                                androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 0.dp
                                ),
                ) {
                    Text(
                            "Skip",
                            fontSize = 11.sp,
                            color = ButterflyGold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                        onClick = onNext,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = ButterflyGold,
                                        contentColor = Color.White,
                                ),
                        contentPadding =
                                androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 0.dp
                                ),
                ) {
                    Text(
                            if (isLast) "Done 🎨" else "Next →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
