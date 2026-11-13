package com.safarparmar.app.ui.calmcentre

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon as UiIcon
import com.composables.ui.components.IconButton as UiIconButton
import com.composables.ui.components.Text as UiText
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.SafarComposablesTheme

/* Hallmark · SAFAR native / guided check-in · ivory, plum and practice-specific pastels.
 * Existing Hanken Grotesk typography and drawer retained. One clock coordinates practice motion.
 * Pre-emit critique: Philosophy 4 / Hierarchy 4 / Execution 4 / Specificity 5 / Restraint 4 / Variety 4.
 */
@Composable
fun CalmCentreScreen(
    currentRoute: String = Routes.CALM_CENTRE,
    isDarkTheme: Boolean = false,
    initialPractice: String? = null,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
) {
    com.safarparmar.app.ui.dhyan.DhyanScreen(
        initialTab = com.safarparmar.app.ui.dhyan.DhyanTab.CALM_CENTRE,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        initialCalmPractice = initialPractice,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
    )
}

/** State lives above page transitions, so answers and elapsed time survive rotation and back. */
@Composable
internal fun CalmCentreFlow(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    initialPractice: CalmPractice? = null,
) {
    val colors = CalmColors.forTheme(isDarkTheme)
    var page by rememberSaveable { mutableIntStateOf(if (initialPractice != null) 3 else 0) }
    var mood by rememberSaveable { mutableStateOf<CalmMood?>(null) }
    var intention by rememberSaveable { mutableStateOf<CalmIntention?>(null) }
    var minutes by rememberSaveable { mutableIntStateOf(2) }
    var technique by rememberSaveable { mutableStateOf(CalmBreathTechnique.GENTLE) }
    var practice by rememberSaveable { mutableStateOf(initialPractice ?: CalmPractice.BREATHE) }
    val elapsed = rememberSaveable { mutableLongStateOf(0L) }
    var playing by rememberSaveable { mutableStateOf(false) }
    val motion = LocalMotionPolicy.current
    val haptic = LocalHapticFeedback.current
    val recommendation = recommendCalmPractice(mood ?: CalmMood.OKAY, intention ?: CalmIntention.RESET)
    val goBack: () -> Unit = {
        playing = false
        page = when (page) { 5 -> 3; 4 -> 3; else -> (page - 1).coerceAtLeast(0) }
    }
    BackHandler { if (page > 0) goBack() else onExit() }
    CompositionLocalProvider(LocalSafarComposablesAccent provides colors.plum) {
        SafarComposablesTheme {
            Box(modifier.background(colors.background), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = if (page == 4) 0.dp else 20.dp)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (page > 0) {
                            UiIconButton(onClick = goBack, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
                                UiIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = colors.ink, modifier = Modifier.size(21.dp))
                            }
                        }
                        Text(if (page < 3) "YOUR MOMENT OF CALM" else if (page == 4) practice.category.uppercase() else "MADE FOR THIS MOMENT",
                            color = colors.muted, style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.8.sp, modifier = Modifier.weight(1f))
                        if (page < 3) Text("${page + 1} of 3", color = colors.muted, style = MaterialTheme.typography.labelMedium)
                    }
                    if (page < 3) {
                        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) { i -> Box(Modifier.weight(1f).height(3.dp).clip(CircleShape)
                                .background(if (i <= page) colors.plum else colors.line)) }
                        }
                    }
                    AnimatedContent(
                        targetState = page,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        transitionSpec = { fadeIn(tween(motion.navigationMillis)) togetherWith fadeOut(tween(motion.navigationMillis)) },
                        label = "Calm journey",
                    ) { currentPage ->
                        when (currentPage) {
                            0, 1, 2 -> CalmQuestions(
                                currentPage, mood, intention, minutes, colors,
                                onBrowse = { page = 3 },
                                onMood = { mood = it }, onIntention = { intention = it }, onMinutes = { minutes = it },
                                onContinue = {
                                    if (currentPage == 2) practice = recommendation.practice
                                    page = currentPage + 1
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            )
                            3 -> CalmCatalogue(recommendation, minutes, colors, hasCheckIn = mood != null && intention != null,
                                onMinutes = { minutes = it }, onStart = { item ->
                                    practice = item.practice
                                    technique = item.technique ?: CalmBreathTechnique.GENTLE
                                    elapsed.longValue = 0L
                                    playing = true
                                    page = 4
                                }, onCheckIn = { page = 0 })
                            4 -> CalmSession(practice, technique, minutes, colors, elapsed, playing,
                                onPlayingChange = { playing = it }, onComplete = { playing = false; page = 5 },
                                onChooseAnother = { playing = false; page = 3 })
                            5 -> CalmComplete(practice, minutes, colors, onDone = { page = 3 }, onCheckIn = { page = 0 })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalmQuestions(
    step: Int, mood: CalmMood?, intention: CalmIntention?, minutes: Int, colors: CalmColors,
    onMood: (CalmMood) -> Unit, onIntention: (CalmIntention) -> Unit, onMinutes: (Int) -> Unit,
    onContinue: () -> Unit,
    onBrowse: () -> Unit,
) {
    val title = when (step) {
        0 -> "How are you\narriving today?"
        1 -> "What would feel\ngood right now?"
        else -> "A little time,\njust for you."
    }
    val subtitle = when (step) {
        0 -> "There is no right answer. Start where you are."
        1 -> "We will find a practice to meet you there."
        else -> "A small pause counts. Choose what fits today."
    }
    val visual = when (step) {
        0 -> when (mood) {
            CalmMood.LOW -> CalmPractice.KINDNESS
            CalmMood.RESTLESS -> CalmPractice.GROUND
            CalmMood.TIRED -> CalmPractice.REST
            else -> CalmPractice.BREATHE
        }
        1 -> when (intention) {
            CalmIntention.SUPPORT -> CalmPractice.KINDNESS
            CalmIntention.FOCUS -> CalmPractice.GROUND
            CalmIntention.REST -> CalmPractice.REST
            else -> CalmPractice.BREATHE
        }
        else -> CalmPractice.BREATHE
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CalmHeading(title, subtitle, colors)
            Box(Modifier.fillMaxWidth().height(if (step == 2) 190.dp else 116.dp), contentAlignment = Alignment.Center) {
                CalmArtwork(visual, Modifier.width(250.dp).fillMaxHeight())
            }
            when (step) {
                0 -> CalmMood.entries.forEach { item ->
                    CalmChoice(item.title, item.detail, mood == item, colors, onClick = { onMood(item) })
                }
                1 -> CalmIntention.entries.forEach { item ->
                    CalmChoice(item.title, item.detail, intention == item, colors, onClick = { onIntention(item) })
                }
                else -> listOf(1, 2, 5).forEach { value ->
                    CalmChoice("$value ${if (value == 1) "minute" else "minutes"}",
                        when (value) { 1 -> "A small pocket of peace"; 2 -> "Room to find your rhythm"; else -> "A little longer to settle in" },
                        minutes == value, colors, onClick = { onMinutes(value) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(top = 12.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CalmAction(if (step == 2) "Find my practice" else "Continue", colors,
                enabled = when (step) { 0 -> mood != null; 1 -> intention != null; else -> true }, onClick = onContinue)
            if (step == 0) UiButton(onClick = onBrowse, style = ButtonStyle.Ghost, modifier = Modifier.heightIn(min = 48.dp)) {
                UiText("Browse all practices", color = colors.plum)
            }
            if (step == 0) Text("A check-in, without the pressure.", color = colors.muted,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun CalmHeading(title: String, subtitle: String, colors: CalmColors, centered: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(title, color = colors.ink, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 35.sp),
            fontWeight = FontWeight.SemiBold, textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.semantics { heading() })
        Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start)
    }
}

@Composable
private fun CalmChoice(title: String, detail: String, checked: Boolean, colors: CalmColors, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    UiButton(
        onClick = onClick, style = ButtonStyle.Ghost, shape = shape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .background(if (checked) colors.rose else colors.surface, shape)
            .border(if (checked) 1.5.dp else 1.dp, if (checked) colors.plum else colors.line, shape)
            .semantics { selected = checked; role = Role.RadioButton },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            UiText(title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            UiText(detail, color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(21.dp).border(1.dp, if (checked) colors.plum else colors.muted, CircleShape)
            .background(if (checked) colors.plum else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            if (checked) UiIcon(Icons.Default.Check, contentDescription = null, tint = colors.onPlum, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CalmAction(label: String, colors: CalmColors, enabled: Boolean = true, icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward, onClick: () -> Unit) {
    UiButton(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), enabled = enabled,
        style = ButtonStyle.Primary, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(16.dp)) {
        UiText(label, color = colors.onPlum, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (icon != null) UiIcon(icon, contentDescription = null, tint = colors.onPlum, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun CalmSession(
    practice: CalmPractice, technique: CalmBreathTechnique, minutes: Int, colors: CalmColors, elapsed: MutableLongState, playing: Boolean,
    onPlayingChange: (Boolean) -> Unit, onComplete: () -> Unit, onChooseAnother: () -> Unit,
) {
    val duration = minutes * 60_000L
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle by lifecycleOwner.lifecycle.currentStateAsState()
    val foreground = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val latestComplete by rememberUpdatedState(onComplete)
    val latestPlayingChange by rememberUpdatedState(onPlayingChange)
    val motion = LocalMotionPolicy.current
    // Pause synchronously on loss of focus, before the background recomposer stops drawing.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) latestPlayingChange(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(playing, foreground, duration) {
        if (playing && foreground) {
            var previousFrame = withFrameNanos { it }
            var remainderNanos = 0L
            while (elapsed.longValue < duration) {
                withFrameNanos { frame ->
                    val delta = (frame - previousFrame).coerceAtLeast(0L) + remainderNanos
                    elapsed.longValue = (elapsed.longValue + delta / 1_000_000L).coerceAtMost(duration)
                    remainderNanos = delta % 1_000_000L
                    previousFrame = frame
                }
            }
            latestComplete()
        }
    }
    // Only the artwork and progress track observe every frame. Text updates once per second.
    val second by remember(elapsed) { derivedStateOf { elapsed.longValue / 1000L } }
    val cue = if (practice == CalmPractice.BREATHE) {
        calmBreathFrame(second * 1000L, technique).let { CalmCue(it.title, it.detail) }
    } else calmCue(practice, second * 1000L, duration)
    val remaining = calmTimeLabel(duration - second * 1000L)
    CalmImmersiveSession(practice, technique, minutes, colors, elapsed, playing, cue, remaining, duration,
        motion.animationsEnabled, onPlayingChange, onChooseAnother)

}

@Composable
private fun CalmComplete(practice: CalmPractice, minutes: Int, colors: CalmColors, onDone: () -> Unit, onCheckIn: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.size(64.dp).background(colors.sage, CircleShape), contentAlignment = Alignment.Center) {
            UiIcon(Icons.Default.Check, contentDescription = null, tint = colors.ink, modifier = Modifier.size(30.dp))
        }
        CalmHeading("A little more room.", "You made $minutes ${if (minutes == 1) "minute" else "minutes"} for yourself. That counts.", colors, centered = true)
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp)).background(colors.stage(practice))) {
            CalmArtwork(practice, Modifier.fillMaxSize())
        }
        Text("No perfect way to feel.\nTake this softer pace with you.", color = colors.muted,
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        CalmAction("Back to my practices", colors, onClick = onDone)
        UiButton(onCheckIn, style = ButtonStyle.Ghost, modifier = Modifier.heightIn(min = 48.dp)) {
            UiText("Check in again", color = colors.plum, fontSize = 14.sp)
        }
    }
}
