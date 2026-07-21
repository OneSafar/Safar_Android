
package com.safarparmar.app.ui.dhyan

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.safarparmar.app.util.bounceClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.glass.GlassDivider
import com.safarparmar.app.ui.glass.LiquidGlassBackdrop
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.glassLiftShadow
import com.safarparmar.app.ui.glass.liquidGlass
import com.safarparmar.app.ui.glass.safarFrostedPanel
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.ui.tour.TourManager
import com.safarparmar.app.ui.tour.dhyanTourSteps
import kotlinx.coroutines.delay

// ─── Data ──────────────────────────────────────────────────────────────────────

private data class BreathingTechnique(
    val name: String,
    val iconRes: Int,
    val description: String,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val holdAfter: Int = 0,
    val pattern: String,
    val audioUrl: String? = null,
)

private data class BreathingSound(
    val id: String,
    val name: String,
    val description: String,
    val url: String = "",
    val localResId: Int? = null,
)

private val techniques = listOf(
    BreathingTechnique("Diaphragmatic", com.safarparmar.app.R.drawable.ic_wind, "Belly breathing for full oxygen exchange", 4, 0, 6, 0, "4-6"),
    BreathingTechnique("Pursed Lip", com.safarparmar.app.R.drawable.ic_wind, "Slows breathing and keeps airways open", 2, 0, 4, 0, "2-4"),
    BreathingTechnique("Box Breathing", com.safarparmar.app.R.drawable.ic_square, "Rhythmic 4-4-4-4 for stress reduction", 4, 4, 4, 4, "4-4-4-4", "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music/box_breathing.mp3"),
    BreathingTechnique("4-7-8 Breathing", com.safarparmar.app.R.drawable.ic_moon, "Deep relaxation for anxiety and sleep", 4, 7, 8, 0, "4-7-8", "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music/four_seven_eight.mp3"),
    BreathingTechnique("6-7-8 Breathing", com.safarparmar.app.R.drawable.ic_yin_yang, "Slower inhale variation for deeper calm", 6, 7, 8, 0, "6-7-8"),
)

private val breathingSounds = listOf(
    BreathingSound(
        id = "silent-breathing",
        name = "Silent Guidance",
        description = "No background music during breathing techniques",
    ),
)


private enum class DhyanBreathPhase(val label: String) {
    INHALE("INHALE"), HOLD("HOLD"), EXHALE("EXHALE"), HOLD_AFTER("REST")
}

private enum class DhyanAudioSource {
    MUSIC, BREATHING_SOUND
}

// musicOptions removed in favor of shared AudioLibrary

// ─── Dhyan liquid-glass tokens (pink palette preserved) ─────────────────────

private object DhyanColors {
    val LightLotus = Color(0xFFFFCDE0)
    val LightRose = Color(0xFFF49BB7)
    val LightCalm = Color(0xFFE37A9A)
    val LightSky = Color(0xFFBDE0FE)
    val DarkLotus = Color(0xFFE05282)
    val DarkRose = Color(0xFFB82D5C)
    val DarkCalm = Color(0xFF8A133B)
    val DarkSky = Color(0xFF5B9BD5)

    fun lotus(isDark: Boolean) = if (isDark) DarkLotus else LightLotus
    fun rose(isDark: Boolean) = if (isDark) DarkRose else LightRose
    fun calm(isDark: Boolean) = if (isDark) DarkCalm else LightCalm
    fun sky(isDark: Boolean) = if (isDark) DarkSky else LightSky
    fun accentBlue(isDark: Boolean) = if (isDark) Color(0xFF7CB9E8) else Color(0xFF5B9BD5)
    fun gradient(isDark: Boolean) = Brush.verticalGradient(listOf(lotus(isDark), rose(isDark), calm(isDark)))
    fun actionGradient(isDark: Boolean) = Brush.verticalGradient(
        listOf(
            // Light: full saturated pink (not pastel/faded)
            if (isDark) Color(0xFFE86B96) else Color(0xFFFF7AA8),
            if (isDark) DarkCalm else Color(0xFFF04880),
        ),
    )
    /** Solid accent pink for slider thumb/track when a flat color is needed. */
    fun actionPink(isDark: Boolean) = if (isDark) Color(0xFFE86B96) else Color(0xFFF04880)
    fun textPrimary(isDark: Boolean) =
        if (isDark) SafarGlassPalette.TextPrimary else SafarGlassPalette.LightTextPrimary
    fun textSecondary(isDark: Boolean) =
        if (isDark) SafarGlassPalette.TextSecondary else SafarGlassPalette.LightTextSecondary
}

private val DhyanPanelShape = RoundedCornerShape(14.dp)
/** Shared 14dp corner radius for all Dhyan chrome (matches menu chip). */
private val DhyanCornerRadius = 14.dp
private val DhyanCapsuleShape = RoundedCornerShape(DhyanCornerRadius)
private val DhyanPillShape = RoundedCornerShape(DhyanCornerRadius)
private val DhyanControlShape = RoundedCornerShape(DhyanCornerRadius)

// Mockup proportion tokens (relative to a ~390dp-wide phone)
private val DhyanOrbSize = 236.dp
private val DhyanPlaySize = 72.dp
private val DhyanSideControlSize = 52.dp
private val DhyanControlGap = 36.dp
private val DhyanSectionGap = 20.dp
private val DhyanContentHorizontal = 20.dp

/** Soft cool-grey shadow that lifts glass off the light canvas (Mac Control Center). */
private fun Modifier.dhyanGlassShadow(
    isDarkTheme: Boolean,
    shape: Shape,
    elevationLight: androidx.compose.ui.unit.Dp = 14.dp,
    elevationDark: androidx.compose.ui.unit.Dp = 6.dp,
    tint: Color? = null,
): Modifier {
    if (tint != null && !isDarkTheme) {
        return this.shadow(
            elevation = elevationLight,
            shape = shape,
            ambientColor = tint.copy(alpha = 0.28f),
            spotColor = tint.copy(alpha = 0.20f),
        )
    }
    return this.glassLiftShadow(
        shape = shape,
        isLight = !isDarkTheme,
        elevation = if (isDarkTheme) elevationDark else elevationLight,
    )
}

/** Soft cool-grey canvas — glass material does the work, not a pink wash. */
@Composable
private fun DhyanMockBackdrop(isDarkTheme: Boolean) {
    if (isDarkTheme) {
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize(), isLight = false)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(DhyanColors.rose(true).copy(alpha = 0.20f), Color.Transparent),
                        radius = 480f,
                    ),
                ),
        )
    } else {
        // Mac-like soft grey wall so frosted grey glass can read.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFEEF0F4),
                            Color(0xFFE6E9EF),
                            Color(0xFFDEE2E9),
                        ),
                    ),
                ),
        )
        // Very soft brand bloom behind the orb only (not a full pink canvas).
        Box(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopCenter)
                .size(360.dp)
                .offset(y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DhyanColors.lotus(false).copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

private fun Modifier.dhyanFrostedPanel(
    isDarkTheme: Boolean,
    shape: Shape = DhyanCapsuleShape,
    tintAlpha: Float? = null,
): Modifier = safarFrostedPanel(
    isLight = !isDarkTheme,
    shape = shape,
    tintAlpha = tintAlpha,
    elevation = if (!isDarkTheme) 16.dp else 6.dp,
)

@Composable
private fun DhyanGlassPill(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dhyanFrostedPanel(isDarkTheme = isDarkTheme, shape = DhyanCapsuleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Pink glass meditation sphere — single cohesive orb (no grey multi-ring shells). */
@Composable
private fun DhyanMeditationOrb(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLight = !isDarkTheme
    val rose = DhyanColors.rose(isDarkTheme)
    val lotus = DhyanColors.lotus(isDarkTheme)
    Box(
        modifier = modifier.size(DhyanOrbSize),
        contentAlignment = Alignment.Center,
    ) {
        // Soft pink glow behind the orb
        Box(
            Modifier
                .size(DhyanOrbSize + 12.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            rose.copy(alpha = if (isLight) 0.28f else 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        // Main glass sphere
        Box(
            Modifier
                .size(DhyanOrbSize)
                .dhyanGlassShadow(
                    isDarkTheme = isDarkTheme,
                    shape = CircleShape,
                    elevationLight = 20.dp,
                    elevationDark = 14.dp,
                    tint = rose,
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            lotus.copy(alpha = if (isLight) 0.55f else 0.45f),
                            rose.copy(alpha = if (isLight) 0.40f else 0.32f),
                            rose.copy(alpha = if (isLight) 0.18f else 0.16f),
                        ),
                    ),
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isLight) 0.85f else 0.55f),
                            Color.White.copy(alpha = if (isLight) 0.25f else 0.12f),
                            Color.White.copy(alpha = if (isLight) 0.45f else 0.28f),
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Inner pink core with meditation art
            Box(
                Modifier
                    .size(DhyanOrbSize * 0.78f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                lotus.copy(alpha = 0.75f),
                                rose.copy(alpha = 0.55f),
                                rose.copy(alpha = 0.25f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
                content = content,
            )
            // Top specular highlight
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isLight) 0.55f else 0.35f),
                                Color.Transparent,
                            ),
                            center = Offset(70f, 55f),
                            radius = 120f,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun DhyanTopBarGlassChip(
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLight = !isDarkTheme
    Box(
        modifier = Modifier
            .size(34.dp)
            .dhyanGlassShadow(
                isDarkTheme = isDarkTheme,
                shape = DhyanControlShape,
                elevationLight = 10.dp,
                elevationDark = 4.dp,
            )
            .liquidGlass(
                shape = DhyanControlShape,
                surfaceTint = if (isLight) SafarGlassPalette.LightGlassTint else Color.White,
                tintAlpha = if (isLight) 0.52f else 0.12f,
                isLight = isLight,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun DhyanSessionSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 1f..60f,
) {
    val density = LocalDensity.current
    val inactiveColor = DhyanColors.actionPink(isDarkTheme).copy(alpha = 0.22f)
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val thumbSizeDp = 22.dp
    val trackHeightDp = 3.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(thumbSizeDp)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .pointerInput(valueRange) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (change.pressed) {
                                change.consume()
                                val newFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                onValueChange(
                                    valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start),
                                )
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeightDp)
                .clip(RoundedCornerShape(2.dp))
                .background(inactiveColor),
        )
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeightDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DhyanColors.actionGradient(isDarkTheme)),
            )
        }
        val thumbOffsetPx = (trackWidthPx - thumbSizePx) * fraction
        val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }
        Box(
            Modifier
                .size(thumbSizeDp)
                .offset(x = thumbOffsetDp)
                .shadow(6.dp, CircleShape, ambientColor = Color(0x22000000), spotColor = Color(0x18000000))
                .clip(CircleShape)
                .background(DhyanColors.actionPink(isDarkTheme))
                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC2185B).copy(alpha = if (isDarkTheme) 0.55f else 0.45f)),
            )
        }
    }
}

@Composable
private fun DhyanLiquidActionButton(
    text: String,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
) {
    val isLight = !isDarkTheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .dhyanGlassShadow(
                isDarkTheme = isDarkTheme,
                shape = DhyanPillShape,
                elevationLight = 20.dp,
                elevationDark = 8.dp,
                tint = DhyanColors.actionPink(isDarkTheme),
            )
            .clip(DhyanPillShape)
            .border(1.dp, Color.White.copy(alpha = if (isLight) 0.55f else 0.22f), DhyanPillShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(DhyanColors.actionGradient(isDarkTheme)),
        )
        // Liquid glass gloss — keep sheen light so full pink stays dominant
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isLight) 0.32f else 0.45f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        endY = 90f,
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.06f),
                            ),
                        ),
                    )
                },
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                iconRes != null -> Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
                icon != null -> Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            if (icon != null || iconRes != null) Spacer(Modifier.width(10.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun DhyanControlButton(
    icon: ImageVector,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    style: DhyanControlStyle,
    contentDescription: String? = null,
) {
    val isLight = !isDarkTheme
    val modifier = when (style) {
        DhyanControlStyle.Reset -> Modifier
            .liquidGlass(
                shape = DhyanControlShape,
                surfaceTint = if (isLight) SafarGlassPalette.LightGlassTint else Color.White,
                tintAlpha = if (isLight) 0.50f else 0.10f,
                isLight = isLight,
            )
        DhyanControlStyle.Play -> Modifier.background(DhyanColors.actionGradient(isDarkTheme), DhyanControlShape)
        DhyanControlStyle.Volume -> Modifier
            .liquidGlass(
                shape = DhyanControlShape,
                surfaceTint = DhyanColors.sky(isDarkTheme),
                tintAlpha = if (isLight) 0.45f else 0.22f,
                isLight = isLight,
            )
    }
    Box(
        modifier = Modifier
            .size(size)
            .dhyanGlassShadow(
                isDarkTheme = isDarkTheme,
                shape = DhyanControlShape,
                elevationLight = if (style == DhyanControlStyle.Play) 22.dp else 14.dp,
                elevationDark = if (style == DhyanControlStyle.Play) 10.dp else 6.dp,
                tint = if (style == DhyanControlStyle.Play) DhyanColors.actionPink(isDarkTheme) else null,
            )
            .clip(DhyanControlShape)
            .then(modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (style == DhyanControlStyle.Play) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = if (isLight) 0.28f else 0.40f), Color.Transparent),
                            endY = 60f,
                        ),
                    ),
            )
        }
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when (style) {
                DhyanControlStyle.Reset -> DhyanColors.textSecondary(isDarkTheme)
                DhyanControlStyle.Play -> Color.White
                DhyanControlStyle.Volume -> if (isLight) Color(0xFF4A90D9) else Color.White
            },
            modifier = Modifier.size(if (style == DhyanControlStyle.Play) 36.dp else 22.dp),
        )
    }
}

private enum class DhyanControlStyle { Reset, Play, Volume }

@Composable
private fun DhyanStatusBar(
    isDarkTheme: Boolean,
    icon: ImageVector,
    title: String,
    statusLabel: String,
    statusActive: Boolean,
    onClick: (() -> Unit)? = null,
) {
    DhyanGlassPill(
        isDarkTheme = isDarkTheme,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DhyanColors.accentBlue(isDarkTheme),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DhyanColors.textPrimary(isDarkTheme),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            statusLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = if (statusActive) DhyanColors.accentBlue(isDarkTheme) else DhyanColors.textSecondary(isDarkTheme),
        )
    }
}

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhyanScreen(
    currentRoute: String = Routes.DHYAN,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
) {
    var showAudioLibraryPanel by remember { mutableStateOf(false) }
    var showBreathingSoundSheet by remember { mutableStateOf(false) }
    var showTechniquesSheet   by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedMusicTrack    by remember { mutableStateOf(com.safarparmar.app.ui.audio.AudioLibrary.getPersistedTrack(context)) }
    var selectedBreathingSound by remember { mutableStateOf(breathingSounds.first()) }
    // null = no technique chosen (show image), non-null = show animation
    var selectedTechnique   by remember { mutableStateOf<BreathingTechnique?>(null) }
    var tourState           by remember { mutableStateOf<com.safarparmar.app.ui.butterfly.ButterflyTourState?>(null) }
    var activeAudioSource   by remember { mutableStateOf(DhyanAudioSource.MUSIC) }

    LaunchedEffect(selectedTechnique) {
        if (selectedTechnique == null) {
            activeAudioSource = DhyanAudioSource.MUSIC
        } else {
            activeAudioSource = DhyanAudioSource.BREATHING_SOUND
        }
    }

    LaunchedEffect(tourState?.isVisible, tourState?.currentStepIndex) {
        val isVisible = tourState?.isVisible == true
        val step = tourState?.currentStepIndex
        if (isVisible) {
            showTechniquesSheet = (step == 2)
        }
    }

    val themeVm: ThemeViewModel = hiltViewModel()
    val dhyanVm: DhyanViewModel = hiltViewModel()

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        SafarDrawerScaffold(
            title    = "Dhyan",
            subtitle = null,
            currentRoute      = currentRoute,
            isDarkTheme       = isDarkTheme,
            onNavigate        = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            useGlassTopBar    = true,
            useDetachedMenuGlass = true,
            containerColor    = Color.Transparent,
            topBarActions = {
                DhyanTopBarGlassChip(isDarkTheme = isDarkTheme, onClick = { showAudioLibraryPanel = true }) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Meditation Audio Library",
                        tint = DhyanColors.textPrimary(isDarkTheme),
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (selectedTechnique != null) {
                    DhyanTopBarGlassChip(isDarkTheme = isDarkTheme, onClick = { showBreathingSoundSheet = true }) {
                        Icon(
                            Icons.Default.Air,
                            contentDescription = "Breathing Sounds",
                            tint = DhyanColors.textPrimary(isDarkTheme),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DhyanTopBarGlassChip(isDarkTheme = isDarkTheme, onClick = { tourState?.start() }) {
                    Image(
                        painter = painterResource(R.drawable.ic_butterfly_tour),
                        contentDescription = "Guide",
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                DhyanMockBackdrop(isDarkTheme = isDarkTheme)

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .padding(bottom = padding.calculateBottomPadding()),
                ) {
                    BreathingTab(
                        isDarkTheme       = isDarkTheme,
                        selectedTechnique = selectedTechnique,
                        selectedMusicTrack = selectedMusicTrack,
                        selectedBreathingSound = selectedBreathingSound,
                        activeAudioSource = activeAudioSource,
                        onActiveAudioSourceChange = { activeAudioSource = it },
                        onBreatheWithMe   = { showTechniquesSheet = true },
                        onClearTechnique  = { selectedTechnique = null },
                        onSessionComplete = { minutes -> dhyanVm.trackCompletedSession(minutes) },
                    )
                }
            }
        }

        TourManager(
            dataStore        = themeVm.dataStore,
            steps            = dhyanTourSteps,
            section          = "dhyan",
            askOnFirstVisit  = true,
            onTourStateReady = { tourState = it },
        )

        if (showTechniquesSheet) {
            BreathingOptionsSheet(
                selectedTechnique = selectedTechnique,
                onSelectTechnique = {
                    selectedTechnique = it
                    activeAudioSource = DhyanAudioSource.BREATHING_SOUND
                    showTechniquesSheet = false
                },
                onDismiss = { showTechniquesSheet = false },
            )
        }

        if (showAudioLibraryPanel) {
            com.safarparmar.app.ui.audio.AudioLibraryPanel(
                selectedTrackId = selectedMusicTrack.id,
                onTrackSelect = {
                    selectedMusicTrack = it
                    activeAudioSource = DhyanAudioSource.MUSIC
                    com.safarparmar.app.ui.audio.AudioLibrary.persistTrackId(context, it.id)
                },
                onDismiss = { showAudioLibraryPanel = false }
            )
        }

        if (showBreathingSoundSheet) {
            BreathingSoundSheet(
                selectedSound = selectedBreathingSound,
                onSelectSound = {
                    selectedBreathingSound = it
                    activeAudioSource = DhyanAudioSource.BREATHING_SOUND
                    showBreathingSoundSheet = false
                },
                onDismiss = { showBreathingSoundSheet = false },
            )
        }
    } // end outer Box
}

// ─── Breathing Tab ─────────────────────────────────────────────────────────────

@Composable
private fun BreathingTab(
    isDarkTheme: Boolean,
    selectedTechnique: BreathingTechnique?,
    selectedMusicTrack: com.safarparmar.app.ui.audio.AudioTrack,
    selectedBreathingSound: BreathingSound,
    activeAudioSource: DhyanAudioSource,
    onActiveAudioSourceChange: (DhyanAudioSource) -> Unit,
    onBreatheWithMe: () -> Unit,
    onClearTechnique: () -> Unit,
    onSessionComplete: (Int) -> Unit,
) {
    var sessionLengthMin    by remember { mutableIntStateOf(5) }
    var isRunning           by remember { mutableStateOf(false) }
    var phase               by remember { mutableStateOf(DhyanBreathPhase.INHALE) }
    var phaseSecondsLeft    by remember { mutableIntStateOf(selectedTechnique?.inhale ?: 4) }
    var sessionSecondsLeft  by remember { mutableIntStateOf(sessionLengthMin * 60) }
    var isSessionAudioMuted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    fun releasePlayer() {
        val playerToRelease = mediaPlayer.value
        mediaPlayer.value = null
        if (playerToRelease != null) {
            kotlin.concurrent.thread {
                runCatching { playerToRelease.stop() }
                runCatching { playerToRelease.release() }
            }
        }
    }

    LaunchedEffect(isRunning, selectedMusicTrack, selectedBreathingSound, selectedTechnique, isSessionAudioMuted, activeAudioSource) {
        val technique = selectedTechnique
        val isMusicSelected = activeAudioSource == DhyanAudioSource.MUSIC
        val isBreathingSoundSelected = activeAudioSource == DhyanAudioSource.BREATHING_SOUND

        val shouldPlayMeditationMusic = isRunning &&
            !isSessionAudioMuted &&
            (technique == null || isMusicSelected) &&
            selectedMusicTrack.url.isNotBlank() &&
            selectedMusicTrack.name != "None" &&
            selectedMusicTrack.id != "none-track"

        val shouldPlayBreathingSound = isRunning &&
            !isSessionAudioMuted &&
            technique != null &&
            isBreathingSoundSelected &&
            (!technique.audioUrl.isNullOrBlank() || selectedBreathingSound.url.isNotBlank() || selectedBreathingSound.localResId != null)

        if (shouldPlayMeditationMusic || shouldPlayBreathingSound) {
            releasePlayer()
            try {
                val audioUri = if (shouldPlayBreathingSound) {
                    if (!technique.audioUrl.isNullOrBlank()) {
                        Uri.parse(technique.audioUrl)
                    } else {
                        selectedBreathingSound.localResId?.let {
                            Uri.parse("android.resource://${context.packageName}/$it")
                        } ?: Uri.parse(selectedBreathingSound.url)
                    }
                } else {
                    if (selectedMusicTrack.isLocal && selectedMusicTrack.localResId != null) {
                        Uri.parse("android.resource://${context.packageName}/${selectedMusicTrack.localResId}")
                    } else {
                        Uri.parse(selectedMusicTrack.url)
                    }
                }
                val mp = MediaPlayer().apply {
                    setDataSource(context, audioUri)
                    isLooping = true
                    setVolume(0.7f, 0.7f)
                    prepareAsync()
                    setOnPreparedListener { player ->
                        // prepareAsync() can finish after releasePlayer() already ran
                        // (screen left, technique/sound switched). Starting a released or
                        // superseded player throws IllegalStateException — only start if
                        // this is still the active player, and guard against the race.
                        if (mediaPlayer.value === player) {
                            runCatching { player.start() }
                        }
                    }
                }
                mediaPlayer.value = mp
            } catch (e: Exception) { /* ignore */ }
        } else {
            releasePlayer()
        }
    }

    DisposableEffect(Unit) { onDispose { releasePlayer() } }

    fun resetTimer(t: BreathingTechnique? = selectedTechnique, lengthMin: Int = sessionLengthMin) {
        isRunning = false
        phase = DhyanBreathPhase.INHALE
        phaseSecondsLeft   = t?.inhale ?: 4
        sessionSecondsLeft = lengthMin * 60
    }

    LaunchedEffect(selectedTechnique, sessionLengthMin) {
        // Always reset when technique changes (mode switch) — including back to null (default screen)
        resetTimer()
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning && sessionSecondsLeft > 0) {
            delay(1000L)
            sessionSecondsLeft--
            // Only drive breathing phase transitions when a technique is selected
            val t = selectedTechnique
            if (t != null) {
                phaseSecondsLeft--
                if (phaseSecondsLeft <= 0) {
                    phase = when (phase) {
                        DhyanBreathPhase.INHALE     -> if (t.hold > 0) { phaseSecondsLeft = t.hold; DhyanBreathPhase.HOLD } else { phaseSecondsLeft = t.exhale; DhyanBreathPhase.EXHALE }
                        DhyanBreathPhase.HOLD       -> { phaseSecondsLeft = t.exhale; DhyanBreathPhase.EXHALE }
                        DhyanBreathPhase.EXHALE     -> if (t.holdAfter > 0) { phaseSecondsLeft = t.holdAfter; DhyanBreathPhase.HOLD_AFTER } else { phaseSecondsLeft = t.inhale; DhyanBreathPhase.INHALE }
                        DhyanBreathPhase.HOLD_AFTER -> { phaseSecondsLeft = t.inhale; DhyanBreathPhase.INHALE }
                    }
                }
            }
        }
        if (sessionSecondsLeft <= 0) {
            onSessionComplete(sessionLengthMin)
            isRunning = false
        }
    }

    val vizPhase = when (phase) {
        DhyanBreathPhase.INHALE     -> BreathPhase.INHALE
        DhyanBreathPhase.HOLD       -> BreathPhase.HOLD
        DhyanBreathPhase.EXHALE     -> BreathPhase.EXHALE
        DhyanBreathPhase.HOLD_AFTER -> BreathPhase.HOLD_EMPTY
    }
    val vizCycle = BreathCycle(
        inhale  = selectedTechnique?.inhale ?: 4,
        holdIn  = selectedTechnique?.hold ?: 0,
        exhale  = selectedTechnique?.exhale ?: 4,
        holdOut = selectedTechnique?.holdAfter ?: 0,
    )
    val vizSessionId = when (techniques.indexOf(selectedTechnique)) {
        0 -> "1"; 1 -> "2"; 2 -> "3"; 3 -> "4"; else -> "1"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DhyanContentHorizontal)
            .padding(top = 2.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "\"Silence is the language of God.\"",
            fontSize = 12.sp,
            color = DhyanColors.textSecondary(isDarkTheme).copy(alpha = 0.85f),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        // Hero cluster (orb + timer) — shares remaining height so lower controls stay compact.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = selectedTechnique,
                transitionSpec = {
                    (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(fadeOut(tween(280)) + scaleOut(targetScale = 0.97f))
                },
                label = "dhyanHero",
            ) { technique ->
                if (technique == null) {
                    DhyanMeditationOrb(isDarkTheme = isDarkTheme) {
                        Image(
                            painter = painterResource(R.drawable.meditation_transparent_background),
                            contentDescription = "Meditate",
                            modifier = Modifier.fillMaxSize(0.88f),
                            contentScale = ContentScale.Fit,
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DhyanMeditationOrb(isDarkTheme = isDarkTheme) {
                            BreathingVisualizer(
                                sessionId = vizSessionId,
                                breathPhase = vizPhase,
                                isActive = isRunning,
                                cycle = vizCycle,
                                modifier = Modifier.fillMaxSize(0.92f),
                            )
                        }
                        AnimatedVisibility(visible = isRunning) {
                            Text(
                                phase.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = DhyanColors.textSecondary(isDarkTheme),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(DhyanPillShape)
                                .background(DhyanColors.rose(isDarkTheme).copy(alpha = 0.14f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = technique.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = DhyanColors.rose(isDarkTheme),
                                )
                                Text(
                                    "${technique.name} · ${technique.pattern}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DhyanColors.textPrimary(isDarkTheme),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = density.fontScale.coerceAtMost(1.3f),
                ),
            ) {
                Text(
                    "%02d:%02d".format(sessionSecondsLeft / 60, sessionSecondsLeft % 60),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    color = if (isDarkTheme) DhyanColors.textPrimary(true) else Color(0xFF1A1A2E),
                )
            }
        }

        Spacer(Modifier.height(DhyanSectionGap))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dhyanFrostedPanel(isDarkTheme = isDarkTheme, shape = DhyanCapsuleShape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "SESSION LENGTH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = DhyanColors.textSecondary(isDarkTheme),
                )
                Text(
                    "${sessionLengthMin} min",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DhyanColors.accentBlue(isDarkTheme),
                )
            }
            Spacer(Modifier.height(10.dp))
            DhyanSessionSlider(
                value = sessionLengthMin.toFloat(),
                onValueChange = {
                    sessionLengthMin = it.toInt()
                    if (!isRunning) resetTimer(lengthMin = it.toInt())
                },
                isDarkTheme = isDarkTheme,
                valueRange = 1f..60f,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(DhyanSectionGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DhyanControlGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DhyanControlButton(
                icon = Icons.Default.Refresh,
                isDarkTheme = isDarkTheme,
                onClick = { resetTimer() },
                size = DhyanSideControlSize,
                style = DhyanControlStyle.Reset,
                contentDescription = "Reset",
            )
            DhyanControlButton(
                icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                isDarkTheme = isDarkTheme,
                onClick = { isRunning = !isRunning },
                size = DhyanPlaySize,
                style = DhyanControlStyle.Play,
                contentDescription = if (isRunning) "Pause" else "Play",
            )
            DhyanControlButton(
                icon = if (isSessionAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                isDarkTheme = isDarkTheme,
                onClick = { isSessionAudioMuted = !isSessionAudioMuted },
                size = DhyanSideControlSize,
                style = DhyanControlStyle.Volume,
                contentDescription = if (isSessionAudioMuted) "Unmute" else "Mute",
            )
        }

        Spacer(Modifier.height(18.dp))

        if (selectedTechnique == null) {
            DhyanLiquidActionButton(
                text = "Breathe with me",
                iconRes = R.drawable.ic_wind,
                isDarkTheme = isDarkTheme,
                onClick = onBreatheWithMe,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DhyanGlassPill(
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    onClick = onClearTechnique,
                ) {
                    Icon(Icons.Default.Close, null, tint = DhyanColors.textSecondary(isDarkTheme), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Close", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanColors.textPrimary(isDarkTheme))
                }
                Box(Modifier.weight(1f)) {
                    DhyanLiquidActionButton(
                        text = "Change",
                        icon = Icons.Default.Edit,
                        isDarkTheme = isDarkTheme,
                        onClick = onBreatheWithMe,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (selectedTechnique == null) {
            DhyanStatusBar(
                isDarkTheme = isDarkTheme,
                icon = Icons.Default.MusicNote,
                title = "Dhyan",
                statusLabel = when {
                    isSessionAudioMuted -> "MUTED"
                    isRunning -> "PLAYING"
                    else -> "READY"
                },
                statusActive = isRunning && !isSessionAudioMuted,
            )
        } else {
            val isBreathingActive = activeAudioSource == DhyanAudioSource.BREATHING_SOUND
            DhyanStatusBar(
                isDarkTheme = isDarkTheme,
                icon = Icons.Default.Air,
                title = selectedBreathingSound.name,
                statusLabel = when {
                    !isBreathingActive -> "PAUSED"
                    isSessionAudioMuted -> "MUTED"
                    isRunning -> "PLAYING"
                    else -> "READY"
                },
                statusActive = isBreathingActive && isRunning && !isSessionAudioMuted,
                onClick = { onActiveAudioSourceChange(DhyanAudioSource.BREATHING_SOUND) },
            )
            if (selectedMusicTrack.name != "None" && selectedMusicTrack.id != "none-track") {
                val isMusicActive = activeAudioSource == DhyanAudioSource.MUSIC
                Spacer(Modifier.height(8.dp))
                DhyanStatusBar(
                    isDarkTheme = isDarkTheme,
                    icon = Icons.Default.MusicNote,
                    title = selectedMusicTrack.name,
                    statusLabel = when {
                        !isMusicActive -> "PAUSED"
                        isSessionAudioMuted -> "MUTED"
                        isRunning -> "PLAYING"
                        else -> "READY"
                    },
                    statusActive = isMusicActive && isRunning && !isSessionAudioMuted,
                    onClick = { onActiveAudioSourceChange(DhyanAudioSource.MUSIC) },
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ─── Breathe with me sheet — list only ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingOptionsSheet(
    selectedTechnique: BreathingTechnique?,
    onSelectTechnique: (BreathingTechnique) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDarkTheme = !MaterialTheme.colorScheme.background.isLightBackground()
    // Opaque sheet — translucent liquidGlass let the session timer/controls bleed through.
    val sheetBg = if (isDarkTheme) Color(0xFF16161E) else Color(0xFFF7F5FB)
    val cardBg = if (isDarkTheme) Color(0xFF22222C) else Color.White
    val cardSelectedBg = if (isDarkTheme) Color(0xFF2A3140) else Color(0xFFEEF5FC)
    val cardBorder = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E0F0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(DhyanColors.textSecondary(isDarkTheme).copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DhyanColors.rose(isDarkTheme).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wind),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = DhyanColors.rose(isDarkTheme),
                    )
                }
                Column {
                    Text("Breathe with me", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DhyanColors.textPrimary(isDarkTheme))
                    Text("Choose a technique to start", fontSize = 13.sp, color = DhyanColors.textSecondary(isDarkTheme))
                }
            }

            GlassDivider()

            techniques.forEach { t ->
                val isSelected = t.name == selectedTechnique?.name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DhyanPanelShape)
                        .background(if (isSelected) cardSelectedBg else cardBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) DhyanColors.accentBlue(isDarkTheme) else cardBorder,
                            shape = DhyanPanelShape,
                        )
                        .bounceClick(onClick = { onSelectTechnique(t) })
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DhyanColors.accentBlue(isDarkTheme).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = t.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = DhyanColors.accentBlue(isDarkTheme),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanColors.textPrimary(isDarkTheme))
                            Text(t.description, fontSize = 12.sp, color = DhyanColors.textSecondary(isDarkTheme), lineHeight = 17.sp)
                        }
                        Box(
                            Modifier
                                .clip(DhyanPillShape)
                                .background(DhyanColors.accentBlue(isDarkTheme).copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(t.pattern, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DhyanColors.accentBlue(isDarkTheme))
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = DhyanColors.accentBlue(isDarkTheme), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingSoundSheet(
    selectedSound: BreathingSound,
    onSelectSound: (BreathingSound) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDarkTheme = !MaterialTheme.colorScheme.background.isLightBackground()
    val sheetBg = if (isDarkTheme) Color(0xFF16161E) else Color(0xFFF7F5FB)
    val cardBg = if (isDarkTheme) Color(0xFF22222C) else Color.White
    val cardSelectedBg = if (isDarkTheme) Color(0xFF2A3140) else Color(0xFFEEF5FC)
    val cardBorder = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color(0xFFE5E0F0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(DhyanColors.textSecondary(isDarkTheme).copy(alpha = 0.35f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DhyanColors.rose(isDarkTheme).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(22.dp), tint = DhyanColors.rose(isDarkTheme))
                }
                Column {
                    Text("Breathing sounds", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DhyanColors.textPrimary(isDarkTheme))
                    Text("Guidance audio for your technique", fontSize = 13.sp, color = DhyanColors.textSecondary(isDarkTheme))
                }
            }

            GlassDivider()

            breathingSounds.forEach { sound ->
                val isSelected = sound.id == selectedSound.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DhyanPanelShape)
                        .background(if (isSelected) cardSelectedBg else cardBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) DhyanColors.accentBlue(isDarkTheme) else cardBorder,
                            shape = DhyanPanelShape,
                        )
                        .bounceClick(onClick = { onSelectSound(sound) })
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DhyanColors.accentBlue(isDarkTheme).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = DhyanColors.accentBlue(isDarkTheme),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(sound.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanColors.textPrimary(isDarkTheme))
                            Text(sound.description, fontSize = 12.sp, color = DhyanColors.textSecondary(isDarkTheme), lineHeight = 17.sp)
                        }
                        if (isSelected) Icon(Icons.Default.Check, null, tint = DhyanColors.accentBlue(isDarkTheme), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// MusicSheet removed in favor of shared AudioLibraryPanel
