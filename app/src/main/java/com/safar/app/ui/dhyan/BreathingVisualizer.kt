package com.safar.app.ui.dhyan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.ui.theme.*
import kotlinx.coroutines.delay

// ─── Data ────────────────────────────────────────────────────────────────────

enum class BreathPhase(val label: String) {
    INHALE("inhale"),
    HOLD("hold"),
    EXHALE("exhale"),
    HOLD_EMPTY("hold-empty")
}

data class BreathCycle(
    val inhale: Int = 4,
    val holdIn: Int = 4,
    val exhale: Int = 4,
    val holdOut: Int = 4
)

// ─── Entry Point ─────────────────────────────────────────────────────────────

@Composable
fun BreathingVisualizer(
    sessionId: String,
    breathPhase: BreathPhase,
    isActive: Boolean,
    cycle: BreathCycle = BreathCycle(),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (sessionId) {
            "1" -> WavyPathViz(breathPhase, isActive)
            "2" -> GoldenOrbViz(breathPhase, isActive)
            "3" -> BoxTraceViz(breathPhase, isActive, cycle)
            "4", "5" -> ArcRingViz(breathPhase, isActive, cycle)
            "nostril" -> NostrilViz()
        }
    }
}

// ─── Shared easing ───────────────────────────────────────────────────────────

val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private fun phaseDurationMs(phase: BreathPhase, cycle: BreathCycle) = when (phase) {
    BreathPhase.INHALE -> cycle.inhale * 1000
    BreathPhase.HOLD -> cycle.holdIn * 1000
    BreathPhase.EXHALE -> cycle.exhale * 1000
    BreathPhase.HOLD_EMPTY -> cycle.holdOut * 1000
}

// ─── 1. Diaphragmatic / Belly Breathing (WavyPathViz) ────────────────────────

@Composable
fun WavyPathViz(breathPhase: BreathPhase, isActive: Boolean) {
    val isInhale = breathPhase == BreathPhase.INHALE
    val isExhale = breathPhase == BreathPhase.EXHALE
    val isHoldFull = breathPhase == BreathPhase.HOLD

    val orbDuration = when {
        isInhale -> 4000; isExhale -> 6000; else -> 500
    }

    val phaseColor = when (breathPhase) {
        BreathPhase.INHALE -> Emerald500
        BreathPhase.EXHALE -> Indigo500
        else -> Amber500
    }
    val gradStart = when (breathPhase) {
        BreathPhase.INHALE -> Emerald400
        BreathPhase.EXHALE -> Indigo400
        else -> Amber400
    }
    val gradEnd = when (breathPhase) {
        BreathPhase.INHALE -> Teal500
        BreathPhase.EXHALE -> Purple500
        else -> Orange500
    }

    val targetOrbScale = if (isInhale || isHoldFull) 1.35f else 0.75f
    val orbScale by animateFloatAsState(
        targetValue = if (isActive) targetOrbScale else 1f,
        animationSpec = tween(orbDuration, easing = EaseInOut), label = "orbScale"
    )
    val fillFraction by animateFloatAsState(
        targetValue = if (isActive) (if (isInhale || isHoldFull) 1f else 0.2f) else 0.5f,
        animationSpec = tween(orbDuration, easing = EaseInOut), label = "fill"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) (if (isInhale) 0.3f else if (isExhale) 0.075f else 0.2f) else 0.05f,
        animationSpec = tween(orbDuration, easing = EaseInOut), label = "glow"
    )
    val waveSmall by animateFloatAsState(
        targetValue = if (isActive) (if (isInhale) 1.6f else if (isExhale) 1f else 1.3f) else 1f,
        animationSpec = tween(orbDuration, easing = EaseInOut), label = "waveS"
    )
    val waveLarge by animateFloatAsState(
        targetValue = if (isActive) (if (isInhale) 2f else if (isExhale) 1f else 1.5f) else 1f,
        animationSpec = tween(orbDuration, easing = EaseInOut), label = "waveL"
    )

    val infinite = rememberInfiniteTransition(label = "inf")
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.15f, targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "ringA"
    )
    val orbGlow by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "orbG"
    )

    // Ring scale animations (ring 1,2,3 → offset 0.15, 0.30, 0.45)
    val r1 by animateFloatAsState(targetValue = if (isActive && isInhale) 1.15f else 1f, animationSpec = tween(orbDuration, easing = EaseInOut), label = "r1")
    val r2 by animateFloatAsState(targetValue = if (isActive && isInhale) 1.30f else 1f, animationSpec = tween(orbDuration, easing = EaseInOut), label = "r2")
    val r3 by animateFloatAsState(targetValue = if (isActive && isInhale) 1.45f else 1f, animationSpec = tween(orbDuration, easing = EaseInOut), label = "r3")
    val ringScales = listOf(r1, r2, r3)
    val ringSizesDp = listOf(170f, 220f, 270f)

    val isMoving = isActive && (isInhale || isExhale)
    val particleColor = if (isInhale) Emerald400 else Indigo400

    Box(modifier = Modifier.size(288.dp), contentAlignment = Alignment.Center) {

        // Concentric rings
        ringScales.forEachIndexed { i, rs ->
            Canvas(modifier = Modifier.size(ringSizesDp[i].dp).scale(rs)) {
                drawCircle(
                    color = phaseColor.copy(alpha = if (isActive) ringAlpha else 0.05f),
                    radius = size.minDimension / 2f,
                    style = Stroke(1.5.dp.toPx())
                )
            }
        }

        // Outer glow blob
        Canvas(modifier = Modifier.size(160.dp).scale(if (isActive) orbScale * 1.4f else 1f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(gradStart.copy(glowAlpha), gradEnd.copy(glowAlpha * 0.5f), Color.Transparent)
                )
            )
        }

        // Main orb container (scaled)
        Box(modifier = Modifier.size(128.dp).scale(orbScale), contentAlignment = Alignment.Center) {

            // Orb ring glow (inset blur ring)
            Canvas(modifier = Modifier.size(144.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            gradStart.copy(orbGlow * 0.3f),
                            gradEnd.copy(orbGlow * 0.15f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Core orb
            Box(
                modifier = Modifier.size(128.dp).clip(CircleShape)
                    .background(Slate50),
                contentAlignment = Alignment.Center
            ) {
                // Fill that rises/falls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillFraction)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(gradStart.copy(0f), gradEnd.copy(0.3f))
                            )
                        )
                )

                // Emoji + label with vertical nudge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = if (isActive) (if (isInhale) (-4).dp else if (isExhale) 4.dp else 0.dp) else 0.dp)
                ) {
                    Text(
                        text = when (breathPhase) {
                            BreathPhase.INHALE -> "🫁"; BreathPhase.EXHALE -> "💨"; else -> "✨"
                        },
                        fontSize = 26.sp
                    )
                    Text(
                        text = when (breathPhase) {
                            BreathPhase.INHALE -> "Rise"; BreathPhase.EXHALE -> "Lower"; else -> ""
                        },
                        fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, color = Slate500
                    )
                }
            }

            // 6 floating particles
            if (isMoving) {
                val xOffsets = listOf(-50f, -30f, -10f, 10f, 30f, 50f)
                xOffsets.forEachIndexed { i, xOff ->
                    BreathParticle(
                        color = particleColor,
                        startY = if (isInhale) 80f else -80f,
                        endY = if (isInhale) -120f else 120f,
                        xOffset = xOff,
                        delayMs = i * 300,
                        durationMs = if (isInhale) 3000 else 4000
                    )
                }
            }
        }

        // Belly wave bars
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            listOf(24.dp to waveSmall, 32.dp to waveLarge, 24.dp to waveSmall).forEach { (w, scale) ->
                Box(
                    modifier = Modifier
                        .width(w)
                        .height((12f * scale).dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(gradStart.copy(0.4f + 0.1f * (w.value / 32f)), gradEnd.copy(0.4f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun BreathParticle(
    color: Color, startY: Float, endY: Float,
    xOffset: Float, delayMs: Int, durationMs: Int
) {
    val inf = rememberInfiniteTransition(label = "p$xOffset")
    val y by inf.animateFloat(
        initialValue = startY, targetValue = endY,
        animationSpec = infiniteRepeatable(
            tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ), label = "py"
    )
    val alpha by inf.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = durationMs; delayMillis = delayMs
                0f at 0; 0.7f at (durationMs * 0.3).toInt(); 0f at durationMs
            }, RepeatMode.Restart
        ), label = "pa"
    )
    val sc by inf.animateFloat(
        initialValue = 0.5f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = durationMs; delayMillis = delayMs
                0.5f at 0; 1f at (durationMs / 2); 0.3f at durationMs
            }, RepeatMode.Restart
        ), label = "ps"
    )
    Canvas(modifier = Modifier.size(8.dp).offset(x = xOffset.dp, y = y.dp)) {
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 2f * sc)
    }
}

// ─── 2. Pursed Lip / Lion Breath: Golden Orb ─────────────────────────────────

@Composable
fun GoldenOrbViz(breathPhase: BreathPhase, isActive: Boolean) {
    val isInhale = breathPhase == BreathPhase.INHALE
    val isHoldFull = breathPhase == BreathPhase.HOLD
    val durationMs = if (isInhale || breathPhase == BreathPhase.EXHALE) 4000 else 500
    val targetScale = if (isInhale || isHoldFull) 1.4f else 0.6f

    val scale by animateFloatAsState(
        targetValue = if (isActive) targetScale else 1f,
        animationSpec = tween(durationMs, easing = EaseInOut), label = "gs"
    )
    val glowIntensity by animateFloatAsState(
        targetValue = if (isActive) scale else 0.2f,
        animationSpec = tween(durationMs, easing = EaseInOut), label = "gi"
    )

    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        // Outer halo
        Canvas(modifier = Modifier.size(200.dp).scale(scale * 1.3f)) {
            drawCircle(brush = Brush.radialGradient(colors = listOf(Yellow400.copy(0.15f), Color.Transparent)))
        }
        // Mid halo
        Canvas(modifier = Modifier.size(160.dp).scale(scale * 1.1f)) {
            drawCircle(brush = Brush.radialGradient(colors = listOf(Yellow400.copy(0.25f), Amber400.copy(0.1f), Color.Transparent)))
        }
        // Core orb
        Box(
            modifier = Modifier.size(90.dp).scale(scale).clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(Yellow300, Amber500, Amber600))),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Yellow400.copy(glowIntensity * 0.5f), Amber500.copy(glowIntensity * 0.3f), Color.Transparent)
                    ), radius = size.minDimension
                )
            }
            Text(
                text = if (isActive) breathPhase.label.replace("-", " ").uppercase() else "●",
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(0.9f), letterSpacing = 1.sp, textAlign = TextAlign.Center
            )
        }
    }
}

// ─── 3. Box Breathing ────────────────────────────────────────────────────────

@Composable
fun BoxTraceViz(breathPhase: BreathPhase, isActive: Boolean, cycle: BreathCycle) {
    val boxSize = 160f; val offset = 20f
    val corners = listOf(
        Offset(offset, offset), Offset(offset + boxSize, offset),
        Offset(offset + boxSize, offset + boxSize), Offset(offset, offset + boxSize)
    )
    val edgeIndex = when (breathPhase) {
        BreathPhase.INHALE -> 0; BreathPhase.HOLD -> 1
        BreathPhase.EXHALE -> 2; BreathPhase.HOLD_EMPTY -> 3
    }
    val phaseDurMs = phaseDurationMs(breathPhase, cycle)

    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(breathPhase, isActive) {
        progress = 0f
        if (!isActive) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            progress = (elapsed.toFloat() / phaseDurMs).coerceIn(0f, 1f)
            if (progress >= 1f) break
            delay(16)
        }
    }

    val from = corners[edgeIndex]; val to = corners[(edgeIndex + 1) % 4]
    val dotX = from.x + (to.x - from.x) * progress
    val dotY = from.y + (to.y - from.y) * progress

    val inf = rememberInfiniteTransition(label = "ping")
    val pingScale by inf.animateFloat(1f, 1.8f, infiniteRepeatable(tween(800), RepeatMode.Restart), label = "ps")
    val pingAlpha by inf.animateFloat(0.5f, 0f, infiniteRepeatable(tween(800), RepeatMode.Restart), label = "pa")

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sx = size.width / 200f; val sy = size.height / 200f
            drawRect(Blue100.copy(if (isActive) 0.3f else 0.1f), Offset(offset * sx, offset * sy), Size(boxSize * sx, boxSize * sy))
            drawRect(Blue500.copy(if (isActive) 0.8f else 0.4f), Offset(offset * sx, offset * sy), Size(boxSize * sx, boxSize * sy), style = Stroke(2.5.dp.toPx()))
            val cx = dotX * sx; val cy = dotY * sy; val r = 10.dp.toPx()
            drawCircle(Blue500.copy(pingAlpha), r * pingScale, Offset(cx, cy))
            drawCircle(Blue500.copy(0.3f), r * 1.4f, Offset(cx, cy))
            drawCircle(Blue500, r, Offset(cx, cy))
            drawCircle(Color.White.copy(0.5f), r * 0.35f, Offset(cx - r * 0.15f, cy - r * 0.15f))
        }
        Text("INHALE", Modifier.align(Alignment.TopCenter), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue500.copy(0.8f), letterSpacing = 1.sp)
        Text("EXHALE", Modifier.align(Alignment.BottomCenter), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Blue500.copy(0.8f), letterSpacing = 1.sp)
    }
}

// ─── 4 & 5. Arc Ring ─────────────────────────────────────────────────────────

@Composable
fun ArcRingViz(breathPhase: BreathPhase, isActive: Boolean, cycle: BreathCycle) {
    val phaseDurMs = phaseDurationMs(breathPhase, cycle)
    var arcProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(breathPhase, isActive) {
        arcProgress = 0f
        if (!isActive) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - start
            arcProgress = (elapsed.toFloat() / phaseDurMs).coerceIn(0f, 1f)
            if (arcProgress >= 1f) break
            delay(16)
        }
    }

    val phaseColor = when (breathPhase) {
        BreathPhase.INHALE -> Blue500; BreathPhase.EXHALE -> Violet500
        BreathPhase.HOLD -> Amber500; BreathPhase.HOLD_EMPTY -> Slate500
    }
    val r by animateFloatAsState(phaseColor.red, tween(300), label = "cr")
    val g by animateFloatAsState(phaseColor.green, tween(300), label = "cg")
    val b by animateFloatAsState(phaseColor.blue, tween(300), label = "cb")
    val color = Color(r, g, b)

    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * 0.8f
            val sw = 8.dp.toPx()
            val cx = Offset(size.width / 2f, size.height / 2f)
            val sweep = 360f * arcProgress
            drawCircle(Color.Gray.copy(0.1f), radius, cx, style = Stroke(sw))
            if (sweep > 0f) {
                val tl = Offset(cx.x - radius, cx.y - radius)
                val sz = Size(radius * 2f, radius * 2f)
                drawArc(color.copy(0.15f), -90f, sweep, false, tl, sz, style = Stroke(sw * 2f, cap = StrokeCap.Round))
                drawArc(color, -90f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (breathPhase) { BreathPhase.INHALE -> "↑"; BreathPhase.EXHALE -> "↓"; else -> "•" },
                fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color
            )
            Text(
                breathPhase.label.replace("-", " ").uppercase(),
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = Slate500, letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ─── Nadi Shodhana ───────────────────────────────────────────────────────────

private data class NostrilPhaseData(
    val label: String, val side: String, val action: String,
    val colorStart: Color, val colorEnd: Color, val icon: String
)

@Composable
fun NostrilViz() {
    val phases = listOf(
        NostrilPhaseData("Inhale Left",  "left",  "inhale", Blue400,   Blue500,   "🌊"),
        NostrilPhaseData("Hold",         "both",  "hold",   Purple400, Violet500, "✨"),
        NostrilPhaseData("Exhale Right", "right", "exhale", Teal400,   Teal500,   "🍃"),
        NostrilPhaseData("Inhale Right", "right", "inhale", Teal400,   Teal500,   "🍃"),
        NostrilPhaseData("Hold",         "both",  "hold",   Purple400, Violet500, "✨"),
        NostrilPhaseData("Exhale Left",  "left",  "exhale", Blue400,   Blue500,   "🌊"),
    )

    var idx by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    val durMs = (4000L / speed).toLong()
    val current = phases[idx]

    LaunchedEffect(isPlaying, speed, idx) {
        if (!isPlaying) return@LaunchedEffect
        delay(durMs); idx = (idx + 1) % phases.size
    }

    var prog by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(idx, isPlaying) {
        prog = 0f
        if (!isPlaying) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (true) {
            prog = ((System.currentTimeMillis() - start).toFloat() / durMs).coerceIn(0f, 1f)
            if (prog >= 1f) break; delay(16)
        }
    }

    val leftFill by animateFloatAsState(
        if (current.side == "left" && current.action == "inhale") 1f else 0f,
        tween(durMs.toInt(), easing = EaseInOut), label = "lf"
    )
    val rightFill by animateFloatAsState(
        if (current.side == "right" && current.action == "inhale") 1f else 0f,
        tween(durMs.toInt(), easing = EaseInOut), label = "rf"
    )

    val inf = rememberInfiniteTransition(label = "nInf")
    val holdScale by inf.animateFloat(1f, 1.15f, infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "hs")
    val holdGlow by inf.animateFloat(0.3f, 0.6f, infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "hg")
    val iconRot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "ir")

    val isHold = current.action == "hold"

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Slate50)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nadi Shodhana", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Blue600)
        Text("Alternate Nostril Breathing", fontSize = 12.sp, color = Slate500,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        Row(Modifier.fillMaxWidth().height(240.dp), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
            NostrilChannel("Left", leftFill, Blue500)

            Box(
                Modifier.size(128.dp * (if (isHold) holdScale else 1f)).clip(CircleShape).background(Color.White),
                Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(current.colorStart.copy(if (isHold) holdGlow else 0.2f), Color.Transparent)))
                }
                Box(
                    Modifier.size(76.dp).graphicsLayer { rotationZ = iconRot }
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(current.colorStart, current.colorEnd))),
                    Alignment.Center
                ) { Text(current.icon, fontSize = 28.sp) }
            }

            NostrilChannel("Right", rightFill, Teal500)
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(current, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "ac") { p ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(p.colorStart, p.colorEnd)))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text(p.action.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp) }
                Spacer(Modifier.height(6.dp))
                Text(p.label, fontSize = 20.sp, fontWeight = FontWeight.Light, color = Slate700)
            }
        }

        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator({ prog }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = current.colorEnd, trackColor = Slate200)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.SpaceBetween) {
            Text("Cycle ${idx / 6 + 1}", fontSize = 11.sp, color = Slate400)
            Text("Phase ${idx + 1}/6", fontSize = 11.sp, color = Slate400)
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(Blue500, Violet500)), RoundedCornerShape(12.dp)),
                    Alignment.Center
                ) { Text(if (isPlaying) "⏸ Pause" else "▶ Play", color = Color.White, fontWeight = FontWeight.Medium) }
            }
            OutlinedButton({ idx = 0; isPlaying = false }, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Text("↻ Reset")
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Slate100).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Speed", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate600)
                Text("${speed}x", fontSize = 13.sp, color = Slate500)
            }
            Slider(speed, { speed = it }, valueRange = 0.5f..2f, steps = 5, modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = Violet500, activeTrackColor = Violet500))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Slower", fontSize = 11.sp, color = Slate400)
                Text("Faster", fontSize = 11.sp, color = Slate400)
            }
        }

        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Blue50).padding(12.dp)) {
            Text(
                "Tip: Nadi Shodhana balances the left and right hemispheres of the brain, promoting calmness and mental clarity. Practice for 5–10 minutes daily.",
                fontSize = 12.sp, color = Slate600, lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun NostrilChannel(label: String, fillFraction: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 8.dp))
        Box(Modifier.width(16.dp).height(224.dp).clip(RoundedCornerShape(8.dp)).background(Slate200)) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(fillFraction).align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color.Transparent, color)))
            )
        }
    }
}
