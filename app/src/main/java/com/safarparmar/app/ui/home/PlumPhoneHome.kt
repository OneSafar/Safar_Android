package com.safarparmar.app.ui.home

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val Plum = Color(0xFF491438)
private val Peach = Color(0xFFFFDFD5)
private val Lilac = Color(0xFFCDBBE9)
private val Mint = Color(0xFFC9EFE0)
private data class HomeTool(val label: Int, val route: String, val icon: Int)

@Composable
internal fun PlumPhoneHome(isDarkTheme: Boolean, slideTitle: String, slideHeadline: String,
    slideCount: Int, currentPage: Int, onPage: (Int) -> Unit, onHero: () -> Unit,
    onNavigate: (String) -> Unit, onMenu: () -> Unit, modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit) {
    val canvas = if (isDarkTheme) BgDark else Color(0xFFFFF5F0)
    val paper = if (isDarkTheme) Color(0xFF151215) else Color(0xFFFFFDFC)
    val ink = if (isDarkTheme) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val muted = if (isDarkTheme) Color(0xFFCFBECB) else Color(0xFF75616F)
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = canvas, surface = paper,
        onSurface = ink, onSurfaceVariant = muted, primary = if (isDarkTheme) Lilac else Plum)) {
        Column(modifier.background(canvas).navigationBarsPadding()) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp)) {
                // The optical center matches the mockup while retaining all 48dp action targets.
                val actionsWidth = 144.dp
                IconButton(onClick = onMenu, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) {
                    Icon(Icons.Default.Menu, stringResource(R.string.nav_open_menu), Modifier.size(28.dp), tint = ink)
                }
                Text(stringResource(R.string.nav_home),
                    Modifier.align(Alignment.Center).offset(x = if (maxWidth >= 360.dp) 0.dp else (-16).dp),
                    fontFamily = HomeMockupFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = ink)
                Row(Modifier.width(actionsWidth).align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                val tileSide = ((maxWidth - 24.dp - 32.dp - 36.dp) / 4).coerceAtLeast(1.dp)
                val toolsBudget = 68.dp + (tileSide + 38.dp) * 2
                val topBudget = (maxHeight - toolsBudget - 16.dp).coerceAtLeast(0.dp)
                val heroHeight = minOf(maxWidth * (386f / 856f), topBudget * .46f)
                val promoHeight = minOf((maxWidth - 24.dp) * (355f / 805f) + 48.dp, topBudget - heroHeight)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(heroHeight)
                    .then(if (isDarkTheme) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else Modifier)
                    .clip(if (isDarkTheme) RoundedCornerShape(20.dp) else androidx.compose.ui.graphics.RectangleShape)) {
                    Image(painterResource(if (isDarkTheme) R.drawable.home_dark_hero_background else R.drawable.home_mockup_hero_background), null,
                        Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                    Column(Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onHero)
                        .padding(horizontal = 16.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                        Text(slideTitle.uppercase(), Modifier.clip(CircleShape)
                            .background(if (isDarkTheme) Color.Black.copy(alpha = .55f) else Lilac.copy(alpha = .45f))
                            .then(if (isDarkTheme) Modifier.border(1.dp, Lilac, CircleShape) else Modifier).padding(horizontal = 16.dp, vertical = 5.dp),
                            color = ink, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                        Text(slideHeadline, fontFamily = HomeMockupFont, color = ink, fontSize = 24.sp, lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.Center) {
                            repeat(slideCount) { index ->
                                Box(Modifier.width(if (currentPage == index) 26.dp else 18.dp).height(32.dp)
                                    .selectable(currentPage == index, role = Role.Tab, onClick = { onPage(index) })
                                    .semantics { contentDescription = "${index + 1} / $slideCount" }, contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(if (currentPage == index) 18.dp else 7.dp, 7.dp).background(if (currentPage == index) { if (isDarkTheme) Lilac else Plum } else { if (isDarkTheme) Color(0xFF76677F) else Lilac }, CircleShape))
                                }
                            }
                        }
                    }
                }
                HomeFeatureBanners(onNavigate, Modifier.fillMaxWidth().height(promoHeight).padding(horizontal = 12.dp, vertical = 6.dp), isDarkTheme = isDarkTheme, phonePlumStyle = true)
                Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = RoundedCornerShape(24.dp), color = paper,
                    border = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF454045)) else null) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.plum_home_tools), Modifier.weight(1f), color = ink, fontFamily = HomeMockupFont, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        val tools = listOf(
                            HomeTool(R.string.module_ekagra, Routes.EKAGRA, if (isDarkTheme) R.drawable.home_dark_ekagra else R.drawable.home_mockup_ekagra),
                            HomeTool(R.string.module_nishtha, Routes.nishthaRoot(), if (isDarkTheme) R.drawable.home_dark_nishtha else R.drawable.home_nishtha_book),
                            HomeTool(R.string.module_mehfil, Routes.MEHFIL, if (isDarkTheme) R.drawable.home_dark_mehfil else R.drawable.home_mockup_mehfil),
                            HomeTool(R.string.module_study_planner, Routes.STUDY_PLANNER, if (isDarkTheme) R.drawable.home_dark_planner else R.drawable.home_mockup_planner),
                            HomeTool(R.string.module_dhyan, Routes.DHYAN, if (isDarkTheme) R.drawable.home_dark_dhyan else R.drawable.home_mockup_dhyan),
                            HomeTool(R.string.nav_study_circle, Routes.STUDY_CIRCLES, if (isDarkTheme) R.drawable.home_dark_circle else R.drawable.home_mockup_circle),
                            HomeTool(R.string.nav_focus_shield, Routes.FOCUS_SHIELD, if (isDarkTheme) R.drawable.home_dark_kavach else R.drawable.home_kavach_mint),
                            HomeTool(R.string.nav_leaderboard, Routes.LEADERBOARD, if (isDarkTheme) R.drawable.home_dark_leaderboard else R.drawable.home_mockup_leaderboard))
                        // Keep the approved 4 × 2 arrangement inside the height reserved for tools.
                        val columns = 4
                        tools.chunked(columns).forEachIndexed { rowIndex, row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEachIndexed { index, tool ->
                                    HomeToolTile(tool = tool, ink = ink, onClick = { onNavigate(tool.route) },
                                        modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                }
            }
            Button(onClick = { onNavigate(Routes.DASHBOARD) }, colors = ButtonDefaults.buttonColors(containerColor = Plum, contentColor = Color.White),
                    border = if (isDarkTheme) BorderStroke(1.dp, Lilac) else null,
                    shape = CircleShape, modifier = Modifier.fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp).height(56.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(20.dp))
                    Text(stringResource(R.string.plum_home_dashboard), Modifier.padding(horizontal = 10.dp), fontFamily = HomeMockupFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
                }
        }
    }
}

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
