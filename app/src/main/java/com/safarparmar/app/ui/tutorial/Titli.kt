package com.safarparmar.app.ui.tutorial

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.performance.LocalMotionPolicy
import kotlinx.coroutines.launch

/** All geometry is Kotlin vector drawing; the toolbar icon never animates. */
@Composable
internal fun TitliIcon(modifier: Modifier = Modifier) {
    Box(modifier.drawWithCache {
        val wing = Path().apply {
            moveTo(49f, 45f)
            cubicTo(31f, 1f, 2f, 4f, 8f, 32f)
            cubicTo(10f, 47f, 28f, 53f, 44f, 51f)
            cubicTo(18f, 49f, 10f, 66f, 20f, 83f)
            cubicTo(30f, 96f, 43f, 76f, 49f, 55f)
            close()
        }
        val antenna = Path().apply { moveTo(49f, 29f); cubicTo(46f, 16f, 39f, 18f, 37f, 12f) }
        val pink = Color(0xFFFF1685)
        val spots = listOf(Offset(18f, 25f) to 3.3f, Offset(30f, 33f) to 4.2f,
            Offset(25f, 69f) to 3.6f, Offset(39f, 59f) to 2.2f)
        onDrawBehind {
            scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
                fun androidx.compose.ui.graphics.drawscope.DrawScope.wingAndSpots() {
                    drawPath(wing, pink.copy(alpha = 0.12f), style = Stroke(8f))
                    drawPath(wing, pink.copy(alpha = 0.24f), style = Stroke(4f))
                    drawPath(wing, Color(0xFF09070B))
                    drawPath(wing, pink, style = Stroke(1.5f))
                    spots.forEach { (position, radius) ->
                        drawCircle(pink.copy(alpha = 0.10f), radius + 3f, position)
                        drawCircle(pink.copy(alpha = 0.25f), radius + 1.5f, position)
                        drawCircle(pink, radius, position)
                    }
                    drawPath(antenna, Color.Black, style = Stroke(1.7f))
                    drawCircle(pink, 1.4f, Offset(37f, 12f))
                }
                wingAndSpots()
                scale(-1f, 1f, pivot = Offset(50f, 50f)) { wingAndSpots() }
                drawOval(Color.Black, Offset(47f, 31f), Size(6f, 38f))
                drawOval(pink.copy(alpha = 0.6f), Offset(47f, 31f), Size(6f, 38f), style = Stroke(0.6f))
                drawCircle(Color.Black, 4.1f, Offset(50f, 29f))
            }
        }
    })
}

/** Decorative guide follows the measured spotlight, without intercepting practice controls. */
@Composable
internal fun TitliSpotlightCompanion(target: () -> Rect, stageSize: Size, positioned: Boolean = true) {
    val enabled = LocalMotionPolicy.current.animationsEnabled
    val phase = if (enabled) {
        rememberInfiniteTransition(label = "titliCompanion").animateFloat(
            0f, (Math.PI * 2).toFloat(),
            infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "titliLevitation",
        )
    } else remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val companionSize = with(density) { Size(64.dp.toPx(), 50.dp.toPx()) }
    val gap = with(density) { 10.dp.toPx() }
    val amplitude = with(density) { 3.dp.toPx() }
    Box(Modifier.size(64.dp, 50.dp)
        .graphicsLayer {
            val anchor = if (positioned) titliCompanionPosition(target(), stageSize, companionSize, gap) else Offset.Zero
            translationX = anchor.x
            translationY = anchor.y + if (enabled) sin(phase.value) * amplitude else 0f
        }.testTag("titli.companion").semantics { hideFromAccessibility() }) {
        TitliIcon(Modifier.align(Alignment.BottomStart).size(46.dp))

    }
}

/** The speech and its guide float above the feature; they never participate in its layout. */
@Composable
internal fun TitliSpeechBubble(
    target: () -> Rect,
    stageSize: Size,
    modifier: Modifier = Modifier,
    positionReady: Boolean = true,
    bubble: @Composable (Modifier) -> Unit,
) {
    var measured by remember { mutableStateOf(Size.Zero) }
    val gap = with(LocalDensity.current) { 12.dp.toPx() }
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val enabled = LocalMotionPolicy.current.animationsEnabled
    val position = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var positioned by remember { mutableStateOf(false) }
    val destination = if (positionReady && measured.width > 0f && measured.height > 0f && stageSize.width > 0f && stageSize.height > 0f) {
        titliCompanionPosition(target(), stageSize, measured, gap)
    } else null
    LaunchedEffect(destination, enabled) {
        if (destination == null) return@LaunchedEffect
        if (!positioned || !enabled) {
            position.snapTo(destination)
            positioned = true
        } else {
            // Animatable retains its current position when a newer step interrupts the flight.
            position.animateTo(destination, tween(350, easing = FastOutSlowInEasing))
        }
    }
    Column(modifier.graphicsLayer {
        translationX = position.value.x
        translationY = position.value.y
        alpha = if (positioned) 1f else 0f
    }.onSizeChanged { measured = Size(it.width.toFloat(), it.height.toFloat()) }
        .testTag("titli.speech")) {
        bubble(Modifier.fillMaxWidth())
        androidx.compose.foundation.Canvas(Modifier.padding(start = 28.dp).size(20.dp, 12.dp)) {
            val tail = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(0f, size.height); close() }
            drawPath(tail, surface)
        }
        TitliSpotlightCompanion(target, stageSize, positioned = false)
    }
}

/** Prefer open space below the target; try above and either side before clamping. */
internal fun titliCompanionPosition(target: Rect, stage: Size, companion: Size, gap: Float): Offset {
    val margin = gap / 2f
    val maxX = (stage.width - companion.width - margin).coerceAtLeast(margin)
    val maxY = (stage.height - companion.height - margin).coerceAtLeast(margin)
    val candidates = listOf(
        Offset(target.left, target.bottom + gap),
        Offset(target.left, target.top - gap - companion.height),
        Offset(target.right + gap, target.center.y - companion.height / 2f),
        Offset(target.left - gap - companion.width, target.center.y - companion.height / 2f),
    )
    val safeTarget = target.inflate(gap / 2f)
    return candidates.map { Offset(it.x.coerceIn(margin, maxX), it.y.coerceIn(margin, maxY)) }
        .firstOrNull { !Rect(it, companion).overlaps(safeTarget) }
        ?: Offset(maxX, maxY)
}

@Stable
internal class TitliController {
    internal val guides = mutableStateMapOf<Any, Pair<() -> Unit, () -> Unit>>()
    var requested by mutableStateOf(false)
        private set
    fun tap() { requested = true }
    fun dismiss() { requested = false }
}
internal val LocalTitliController = staticCompositionLocalOf<TitliController?> { null }

/** Register only while a feature is actually composed; callbacks retain its eligibility checks. */
@Composable
internal fun RegisterTitliGuide(start: () -> Unit, lessons: () -> Unit) {
    val controller = LocalTitliController.current ?: return
    val currentStart by rememberUpdatedState(start)
    val currentLessons by rememberUpdatedState(lessons)
    val key = remember { Any() }
    DisposableEffect(controller, key) {
        controller.guides[key] = Pair({ currentStart() }, { currentLessons() })
        onDispose { controller.guides.remove(key) }
    }
}

@Composable
internal fun TitliGuideButton(onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val controller = LocalTitliController.current
    val action = onClick ?: controller?.let { it::tap } ?: return
    val description = stringResource(R.string.titli_description)
    IconButton(onClick = action, modifier = modifier.size(48.dp).testTag("titli.button").semantics { contentDescription = description }) {
        TitliIcon(Modifier.size(36.dp))
    }
}

@Composable
internal fun TitliIntroduction(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            val motion = LocalMotionPolicy.current
            val lift = if (motion.animationsEnabled) {
                val transition = rememberInfiniteTransition(label = "titliIntroduction")
                transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "titliLevitation")
            } else remember { mutableFloatStateOf(0f) }
            val amplitude = with(LocalDensity.current) { 5.dp.toPx() }
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                TitliIcon(Modifier.size(120.dp).testTag("titli.portrait").graphicsLayer { translationY = lift.value * amplitude })
            }
        },
        title = { Text(stringResource(R.string.titli_hello)) },
        text = { Text(stringResource(R.string.titli_introduction), modifier = Modifier.verticalScroll(rememberScrollState()).testTag("titli.introduction")) },
        confirmButton = { TextButton(onClick = onContinue, modifier = Modifier.testTag("titli.begin")) { Text(stringResource(R.string.titli_begin)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ekagra_tutorial_later)) } },
    )
}

/** Supplies actions/dialogs only. It adds no toolbar, space, insets, or screen overlay. */
@Composable
internal fun TitliHost(dataStore: SafarDataStore, onOpenEkagra: () -> Unit, onOpenGoals: () -> Unit, introductionKey: String = "titli_introduction_v1", content: @Composable () -> Unit) {
    val controller = remember { TitliController() }
    val seen by remember(dataStore, introductionKey) { dataStore.titliIntroductionSeen(introductionKey) }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var showAvailable by remember { mutableStateOf(false) }
    fun showFeature(start: Boolean) {
        val guide = controller.guides.values.lastOrNull()
        if (guide != null) { if (start) guide.first() else guide.second() }
        else showAvailable = true
    }
    LaunchedEffect(controller.requested, seen) {
        if (controller.requested && seen == true) { controller.dismiss(); showFeature(false) }
    }
    CompositionLocalProvider(LocalTitliController provides controller) { content() }
    if (controller.requested && seen == false) TitliIntroduction(
        onContinue = { controller.dismiss(); scope.launch { dataStore.setTitliIntroductionSeen(introductionKey) }; showFeature(true) },
        onDismiss = { controller.dismiss(); scope.launch { dataStore.setTitliIntroductionSeen(introductionKey) } },
    )
    if (showAvailable) AlertDialog(
        onDismissRequest = { showAvailable = false },
        icon = { TitliIcon(Modifier.size(64.dp)) },
        title = { Text(stringResource(R.string.titli_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.titli_available_guides))
                TextButton(onClick = { showAvailable = false; onOpenEkagra() }) { Text(stringResource(R.string.ekagra_tutorial_learn)) }
                TextButton(onClick = { showAvailable = false; onOpenGoals() }) { Text(stringResource(R.string.goals_tutorial_learn)) }
            }
        },
        confirmButton = { TextButton(onClick = { showAvailable = false }) { Text(stringResource(R.string.common_close)) } },
    )
}
