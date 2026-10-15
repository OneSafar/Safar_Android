package com.safarparmar.app.ui.tutorial

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.safarparmar.app.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Stable IDs identify targets even when a tutorial changes tabs or windows. */
data class FeatureTutorialStep(
    val id: String,
    val targetId: String,
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val instructionRes: Int = titleRes,
)

@Stable
class FeatureTutorialState(val steps: List<FeatureTutorialStep>) {
    init {
        require(steps.isNotEmpty())
        require(steps.map { it.id }.distinct().size == steps.size)
    }
    var index by mutableIntStateOf(0)
        private set
    val currentStep get() = steps[index]
    val isLast get() = index == steps.lastIndex
    fun next() { if (!isLast) index++ }
    fun previous() { if (index > 0) index-- }
    fun restart() { index = 0 }
}

private class TutorialTarget(val requester: BringIntoViewRequester) {
    var bounds by mutableStateOf(Rect.Zero)
}
private class TutorialTargets {
    val entries = mutableStateMapOf<String, TutorialTarget>()
}
private val LocalTutorialTargets = staticCompositionLocalOf<TutorialTargets?> { null }

/** A no-op outside a tutorial host. Never holds detached LayoutCoordinates. */
fun Modifier.tutorialTarget(id: String): Modifier = composed {
    val registry = LocalTutorialTargets.current
    if (registry == null) this else {
        val target = remember(registry, id) { TutorialTarget(BringIntoViewRequester()) }
        DisposableEffect(registry, id, target) {
            registry.entries[id] = target
            onDispose {
                if (registry.entries[id] === target) registry.entries.remove(id)
            }
        }
        this.testTag("tutorial.target.$id")
            .bringIntoViewRequester(target.requester)
            .onGloballyPositioned { target.bounds = it.boundsInWindow() }
    }
}

/**
 * Compose this inside the window containing the targets. Guidance floats beside
 * Titli over the full-size feature, without changing its constraints. Content is
 * display-only: Next drives demonstrations; the scrim consumes all stage touches.
 */
@Composable
fun FeatureTutorialHost(
    state: FeatureTutorialState,
    onComplete: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit = {},
    onHelp: (() -> Unit)? = null,
    @StringRes practiceLabelRes: Int = R.string.ekagra_tutorial_practice,
    practiceLabelTag: String = "tutorial.practiceLabel",
    closeTag: String = "tutorial.close",
    content: @Composable (FeatureTutorialStep) -> Unit,
) {
    val targets = remember { TutorialTargets() }
    var viewport by remember { mutableStateOf(Rect.Zero) }
    var readyStep by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var resolvedBounds by remember { mutableStateOf<Rect?>(null) }
    val step = state.currentStep
    var showInfo by remember(step.id) { mutableStateOf(false) }
    LaunchedEffect(step.id, retry, viewport) {
        readyStep = null
        failed = false
        val found = withTimeoutOrNull(4_000) {
            snapshotFlow { targets.entries[step.targetId] }
                .first { it != null }!!
                .also { withFrameNanos { }; it.requester.bringIntoView() }
            withFrameNanos { }
            withFrameNanos { }
            snapshotFlow {
                targets.entries[step.targetId]?.bounds?.let { bounds ->
                    viewport.width > 0 && bounds.width > 0 && bounds.height > 0 &&
                        bounds.top >= viewport.top - 2 && bounds.bottom <= viewport.bottom + 2
                } == true
            }.first { it }
            true
        } == true
        failed = !found
        if (found) {
            // Keep the previous destination while targets unregister/measure on Back/Next.
            resolvedBounds = targets.entries[step.targetId]!!.bounds.translate(-viewport.topLeft)
            readyStep = step.id
        }
    }
    val ready = readyStep == step.id && !failed
    // Start at the first measured bounds; later targets animate from the current value.
    val spotlight: State<Rect> = resolvedBounds?.let {
        animateRectAsState(it, tween(350, easing = FastOutSlowInEasing), label = "tutorialSpotlight")
    } ?: remember { mutableStateOf(Rect.Zero) }
    val accent = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier) {
        background()
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val guidanceContent: @Composable (Modifier) -> Unit = { guidanceModifier ->
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = guidanceModifier.testTag("tutorial.guidance"),
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val title = stringResource(step.titleRes)
                    val practiceDescription = stringResource(practiceLabelRes)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.feature_tutorial_compact_progress, state.index + 1, state.steps.size),
                                fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.feature_tutorial_practice_short),
                                fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(practiceLabelTag).semantics { contentDescription = practiceDescription })
                        }
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.testTag("tutorial.info")) {
                            Icon(Icons.Outlined.Info, stringResource(R.string.feature_tutorial_more_info), modifier = Modifier.size(20.dp))
                        }
                        if (onHelp != null) IconButton(onClick = onHelp, modifier = Modifier.testTag("tutorial.help")) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.feature_help_title), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onExit, modifier = Modifier.testTag(closeTag)) {
                            Icon(Icons.Default.Close, stringResource(R.string.common_close), modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 21 / 13 ≈ 1.618: the heading leads; supporting copy stays quiet.
                        Text(title, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("tutorial.title").semantics { heading() })
                        Text(stringResource(if (failed) R.string.feature_tutorial_retry_short else step.instructionRes),
                            fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("tutorial.instruction"))
                    }
                    LinearProgressIndicator(progress = { (state.index + 1f) / state.steps.size }, modifier = Modifier.fillMaxWidth().height(2.dp))
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onExit, modifier = Modifier.testTag("tutorial.skip")) { Text(stringResource(R.string.feature_tutorial_skip)) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = state::previous, enabled = state.index > 0, modifier = Modifier.testTag("tutorial.back")) { Text(stringResource(R.string.feature_tutorial_back)) }
                            Button(
                                onClick = { if (failed) retry++ else if (state.isLast) onComplete() else state.next() },
                                enabled = ready || failed,
                                modifier = Modifier.testTag("tutorial.next"),
                            ) { Text(stringResource(if (failed) R.string.feature_tutorial_retry else if (state.isLast) R.string.feature_tutorial_finish else R.string.feature_tutorial_next)) }
                        }
                    }
                }
            }
        }
        val stageContent: @Composable (Modifier) -> Unit = { stageModifier ->
            Box(stageModifier.clipToBounds().testTag("tutorial.stage").semantics {
                stateDescription = if (ready) step.id else if (failed) "unavailable" else "loading"
            }.onGloballyPositioned {
                viewport = Rect(it.positionInWindow(), Size(it.size.width.toFloat(), it.size.height.toFloat()))
            }) {
                CompositionLocalProvider(LocalTutorialTargets provides targets) {
                    // Recreate presentation state on Back/Next for deterministic demonstrations.
                    key(step.id) {
                        Box(Modifier.fillMaxSize().semantics { hideFromAccessibility() }) { content(step) }
                    }
                }
                Canvas(Modifier.fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .pointerInput(Unit) { detectTapGestures { /* Display-only practice stage. */ } }
                ) {
                    drawRect(Color.Black.copy(alpha = 0.38f))
                    if (resolvedBounds != null && !failed) {
                        val padding = 5.dp.toPx()
                        val bounds = spotlight.value
                        val origin = bounds.topLeft - Offset(padding, padding)
                        val size = Size(bounds.width + padding * 2, bounds.height + padding * 2)
                        drawRoundRect(Color.Transparent, origin, size, CornerRadius(16.dp.toPx()), blendMode = BlendMode.Clear)
                        drawRoundRect(accent, origin, size, CornerRadius(16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
            }
        }
        stageContent(Modifier.fillMaxSize())
        TitliSpeechBubble(
            // Choose the placement once against the destination, never against an in-flight spotlight.
            target = { resolvedBounds ?: Rect(0f, 0f, viewport.width, 0f) },
            positionReady = ready || failed,
            stageSize = viewport.size,
            modifier = Modifier.width(minOf(320.dp, (availableWidth - 24.dp).coerceAtLeast(0.dp)))
                .heightIn(max = (availableHeight - 16.dp).coerceAtLeast(1.dp)),
        ) { bubbleModifier ->
            guidanceContent(bubbleModifier.heightIn(max = (availableHeight - 100.dp).coerceAtLeast(96.dp)))
        }
    }
    if (showInfo) {
        val featureDensity = LocalDensity.current
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { CompositionLocalProvider(LocalDensity provides featureDensity) {
                Text(stringResource(step.titleRes), fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
            } },
            text = {
                CompositionLocalProvider(LocalDensity provides featureDensity) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.feature_tutorial_progress, state.index + 1, state.steps.size), fontSize = 11.sp)
                    Text(stringResource(if (failed) R.string.feature_tutorial_unavailable else step.messageRes),
                        fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.testTag("tutorial.details"))
                }
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalDensity provides featureDensity) {
                TextButton(onClick = { showInfo = false }, modifier = Modifier.testTag("tutorial.info.close")) {
                    Text(stringResource(R.string.feature_tutorial_info_close))
                }
                }
            },
        )
    }
}
