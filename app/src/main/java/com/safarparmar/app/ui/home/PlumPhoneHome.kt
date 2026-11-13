package com.safarparmar.app.ui.home

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleInk

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

// Scope the approved mockup typography here; the app-wide alias uses another family.
private val HomeMockupFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)
private val Lilac = Color(0xFFCDBBE9)
private data class HomeTool(val label: Int, val route: String, val icon: Int)

/** Native controls follow the reference; existing raster artwork stays untouched.
 * Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · supplied Home reference */
@Composable
internal fun PlumPhoneHome(
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    autoRotateCarousels: Boolean = true,
    actions: @Composable RowScope.() -> Unit,
) {
    var modulePage by rememberSaveable { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, autoRotateCarousels) {
        if (autoRotateCarousels) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(4_000)
                modulePage = (modulePage + 1) % homeModuleSlides.size
            }
        }
    }
    val paper = if (isDarkTheme) BgDark else Color.White
    val ink = if (isDarkTheme) Color(0xFFF4EFFF) else RoyalPurpleInk
    val accent = if (isDarkTheme) RoyalPurpleDark else RoyalPurple
    val tools = listOf(
        HomeImageTool(R.string.module_ekagra, Routes.EKAGRA, R.drawable.home_royal_ekagra),
        HomeImageTool(R.string.module_nishtha, Routes.nishthaRoot(), R.drawable.home_royal_nishtha),
        HomeImageTool(R.string.module_mehfil, Routes.MEHFIL, R.drawable.home_royal_mehfil),
        HomeImageTool(R.string.module_study_planner, Routes.STUDY_PLANNER, R.drawable.home_royal_planner),
        HomeImageTool(R.string.module_dhyan, Routes.DHYAN, R.drawable.home_royal_dhyan),
        HomeImageTool(R.string.nav_study_circle, Routes.STUDY_CIRCLES, R.drawable.home_royal_circle),
        HomeImageTool(R.string.nav_focus_shield, Routes.FOCUS_SHIELD, R.drawable.home_royal_kavach),
        HomeImageTool(R.string.nav_leaderboard, Routes.LEADERBOARD, R.drawable.home_royal_leaderboard),
    )
    BoxWithConstraints(modifier.background(paper).navigationBarsPadding()) {
        val largeText = LocalDensity.current.fontScale > 1.2f
        val columns = if (maxWidth < 340.dp || largeText) 2 else 4
        val baseToolFontSize = when {
            columns == 4 && maxWidth < 400.dp -> 11.sp
            columns == 4 && maxWidth < 440.dp -> 12.sp
            else -> 13.sp
        }
        val toolLabels = tools.map { stringResource(it.label) }
        HomeModuleBackground(isDarkTheme, modulePage, Modifier.matchParentSize())
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 68.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Menu, stringResource(R.string.nav_open_menu), tint = ink,
                        modifier = Modifier.size(28.dp))
                }
                Text(stringResource(R.string.nav_home), Modifier.weight(1f), color = ink,
                    fontFamily = HomeMockupFont, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Row(content = actions, verticalAlignment = Alignment.CenterVertically)
            }
            Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp).padding(top = 14.dp, bottom = 24.dp)) {
                HomeModuleCarousel(isDarkTheme, onNavigate, page = modulePage, onPage = { modulePage = it })
                Spacer(Modifier.height(144.dp))
                V5HomeFeatureBanners(onNavigate, autoRotate = autoRotateCarousels)
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.plum_home_tools), Modifier.fillMaxWidth(), color = ink,
                    fontFamily = HomeMockupFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                tools.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tool ->
                            com.composables.ui.components.Button(onClick = { onNavigate(tool.route) },
                                style = com.composables.ui.components.ButtonStyle.Ghost,
                                shape = RoundedCornerShape(20.dp), contentColor = accent,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)),
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // The illustration AND lavender circular padding are generated pixels.
                                    // Coil decodes to the displayed size; no tint or code-drawn icon is applied.
                                    AsyncImage(model = tool.imageRes, contentDescription = null,
                                        modifier = Modifier.size(68.dp).clip(CircleShape)
                                            .testTag("home-tool-image-${tool.label}"),
                                        contentScale = ContentScale.Crop)
                                    FittedHomeToolLabel(
                                        label = stringResource(tool.label), labels = toolLabels,
                                        baseFontSize = baseToolFontSize, color = ink,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                com.composables.ui.components.Button(onClick = { onNavigate(Routes.DASHBOARD) },
                    style = com.composables.ui.components.ButtonStyle.Ghost,
                    shape = RoundedCornerShape(18.dp), contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
                        .background(RoyalPurple, RoundedCornerShape(18.dp)),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                    com.composables.ui.components.Text(stringResource(R.string.royal_home_dashboard),
                        modifier = Modifier.fillMaxWidth(), color = Color.White, fontFamily = HomeMockupFont,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
            }
        }
    }
}

/** Equal-width tiles measure all labels so every label uses the same largest fitting size. */
@Composable
internal fun FittedHomeToolLabel(
    label: String,
    labels: List<String>,
    baseFontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val inheritedStyle = LocalTextStyle.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // These are the label's actual pixel constraints, after all button/grid padding.
        val availableWidth = constraints.maxWidth
        val style = inheritedStyle.copy(
            fontFamily = HomeMockupFont, fontWeight = FontWeight.SemiBold,
            fontSize = baseFontSize, lineHeight = 19.sp, textAlign = TextAlign.Center,
        )
        fun fits(size: Float): Boolean = labels.all { text ->
            val layout = measurer.measure(
                text = text, style = style.copy(fontSize = size.sp),
                softWrap = false, maxLines = 1,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = availableWidth),
            )
            !layout.didOverflowWidth && layout.getLineRight(0) - layout.getLineLeft(0) <= availableWidth
        }
        // Re-measure candidates instead of scaling widths: sp scaling and letter spacing
        // need not be proportional to the font size.
        val size = if (fits(baseFontSize.value)) baseFontSize.value else {
            var low = 0.01f
            var high = baseFontSize.value
            repeat(16) {
                val candidate = (low + high) / 2f
                if (fits(candidate)) low = candidate else high = candidate
            }
            low
        }
        Text(
            text = label, modifier = Modifier.fillMaxWidth(), color = color,
            style = style.copy(fontSize = size.sp), maxLines = 1, softWrap = false,
        )
    }
}

private data class HomeImageTool(val label: Int, val route: String, val imageRes: Int)

/** One touch target, with feedback clipped to the square artwork rather than its label. */
@Composable
private fun HomeToolTile(tool: HomeTool, ink: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "Tool tile press",
    )
    Column(modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
        .padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            // Align artwork to the label edge so spare row height never separates them.
            val tileSize = minOf(maxWidth, maxHeight)
            Image(painterResource(tool.icon), contentDescription = null,
                modifier = Modifier.size(tileSize).clip(RoundedCornerShape(18.dp))
                    .indication(interactions, ripple(color = ink)),
                contentScale = ContentScale.Fit)
        }
        Text(stringResource(tool.label), modifier = Modifier.fillMaxWidth().heightIn(min = 16.dp),
            color = ink, fontFamily = HomeMockupFont, fontSize = 12.sp,
            lineHeight = 15.sp, maxLines = 2, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

/** Artwork includes all visible copy and the CTA; semantics keep the whole banner accessible. */
@Composable
internal fun PlumHomePromo(habitTracker: Boolean, onNavigate: (String) -> Unit, modifier: Modifier = Modifier, isDarkTheme: Boolean = false) {
    val description = stringResource(if (habitTracker) R.string.home_banner_habit_tracker_description
        else R.string.home_banner_youtube_focus_description)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val bannerWidth = minOf(maxWidth, maxHeight * (805f / 355f))
        Image(painterResource(if (habitTracker) R.drawable.home_plum_habit_banner else R.drawable.home_plum_youtube_banner),
            contentDescription = description,
            modifier = Modifier.width(bannerWidth).aspectRatio(805f / 355f)
                .clip(RoundedCornerShape(16.dp))
                .then(if (isDarkTheme) Modifier.border(1.dp, Lilac, RoundedCornerShape(16.dp)) else Modifier)
                .clickable(role = Role.Button) {
                    onNavigate(if (habitTracker) Routes.HABIT_TRACKER else Routes.YOUTUBE_STUDY_MODE_V2)
                },
            contentScale = ContentScale.Fit)
    }
}
