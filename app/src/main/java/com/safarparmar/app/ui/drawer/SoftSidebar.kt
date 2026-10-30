package com.safarparmar.app.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import com.composables.ui.components.focusRing
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.ui.components.popups.SafarComposablesDropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuSide
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuLabel
import com.composables.ui.components.DropdownMenuSeparator
import com.composables.ui.components.Text as UiText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import com.safarparmar.app.ui.theme.SafarComposablesTheme
import com.composables.ui.components.IconButton as UiIconButton
import com.composables.ui.components.Icon as UiIcon
import com.composables.ui.components.ButtonStyle

internal data class SidebarArea(val route: String, val title: Int, val description: Int, val icon: ImageVector, val items: List<DrawerItem>)

internal fun sidebarAreas(): List<SidebarArea> {
    val study = drawerSections.first { it.id == DrawerSectionId.STUDY_PRODUCTIVITY }.items
    val other = drawerSections.first { it.id == DrawerSectionId.WELL_BEING_COMMUNITY }.items
    return listOf(
        SidebarArea(Routes.HOME, R.string.nav_home, R.string.sidebar_home_hint, Icons.Default.Home, emptyList()),
        SidebarArea(Routes.STUDY_HUB, R.string.rail_study, R.string.sidebar_study_hint, Icons.AutoMirrored.Filled.MenuBook,
            study.filterNot { it.route == Routes.LEADERBOARD } + drawerPinnedBottom.filter { it.route == Routes.COURSES }),
        SidebarArea(Routes.WELLBEING_HUB, R.string.sidebar_section_wellbeing, R.string.sidebar_wellbeing_hint, Icons.Default.Spa,
            other.filter { it.route in setOf(Routes.NISHTHA, Routes.DHYAN, Routes.LIVE_SESSIONS_ROOT) }),
        SidebarArea(Routes.COMMUNITY_HUB, R.string.rail_community, R.string.sidebar_community_hint, Icons.Default.Groups,
            other.filter { it.route in setOf(Routes.MEHFIL, Routes.STUDY_CIRCLES) } + study.filter { it.route == Routes.LEADERBOARD }),
        SidebarArea(Routes.PROGRESS_HUB, R.string.sidebar_progress, R.string.sidebar_progress_hint, Icons.Default.BarChart,
            drawerPinnedTop.filter { it.route != Routes.HOME }),
    )
}

internal fun selectedSidebarArea(route: String): String? {
    val base = route.substringBefore('?')
    // Analytics has its own root; legacy Nishtha analytics links must not select Wellbeing.
    if (base == Routes.ANALYTICS || (base == Routes.NISHTHA && route.substringAfter("tab=", "").substringBefore('&') == "4")) return Routes.PROGRESS_HUB
    return sidebarAreas().firstOrNull { area ->
        area.route == base || area.items.any { isDrawerItemSelected(it, base) }
    }?.route
}

private data class SidebarColors(val canvas: Color, val paper: Color, val ink: Color, val muted: Color, val peach: Color, val selection: Color, val line: Color)
private fun sidebarColors(dark: Boolean) = if (dark) SidebarColors(Color(0xFF211921), Color(0xFF342735), Color(0xFFFFF0F6), Color(0xFFCFBECB), Color(0xFF684047), Color(0xFF513551), Color(0xFF654D66))
else SidebarColors(Color(0xFFFFF5F0), Color(0xFFFFFDFC), Color(0xFF39132E), Color(0xFF75616F), Color(0xFFFFCBBE), Color(0xFFEDE0F4), Color(0xFFE8DEE5))

@Composable
fun SafarDrawer(currentRoute: String, isDarkTheme: Boolean, isAdmin: Boolean, isPremiumActive: Boolean,
    userName: String? = null, userEmail: String? = null, userAvatar: String? = null,
    onNavigate: (String) -> Unit, onToggleDarkTheme: () -> Unit, onCloseDrawer: () -> Unit) {
    Surface(modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().fillMaxHeight(),
        color = sidebarColors(isDarkTheme).canvas, shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        SoftSidebarContent(currentRoute, isDarkTheme, false, isAdmin, isPremiumActive, userName, userEmail, userAvatar,
            onNavigate = { onNavigate(it); onCloseDrawer() }, onMenu = onCloseDrawer, close = true)
    }
}

@Composable
fun SafarNavigationRail(currentRoute: String, isDarkTheme: Boolean, onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit, userName: String? = null, userEmail: String? = null,
    userAvatar: String? = null, isPremiumActive: Boolean = false, isAdmin: Boolean = false,
    isExpanded: Boolean, onExpandedChange: (Boolean) -> Unit,
    expansionProgress: () -> Float = { if (isExpanded) 1f else 0f }, modifier: Modifier = Modifier,
    transparentCanvas: Boolean = false) {
    SoftSidebarContent(currentRoute, isDarkTheme, !isExpanded, isAdmin, isPremiumActive, userName, userEmail, userAvatar,
        modifier.width(if (isExpanded) SafarExpandedRailWidth else SafarCompactRailWidth), onNavigate,
        onMenu = { onExpandedChange(!isExpanded) })
}

@Composable
private fun SoftSidebarContent(currentRoute: String, dark: Boolean, compact: Boolean, admin: Boolean, premium: Boolean,
    name: String?, email: String?, avatar: String?, modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit, onMenu: () -> Unit, close: Boolean = false) {
    val c = sidebarColors(dark)
    val active = selectedSidebarArea(currentRoute)
    SafarComposablesTheme {
        Column(modifier.fillMaxHeight().background(c.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(horizontal = if (compact) 8.dp else 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Same centerline and 48dp target as the rail's icon tiles; no brand artwork.
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = if (compact) Alignment.Center else Alignment.CenterEnd) {
                UiIconButton(onClick = onMenu, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
                    UiIcon(if (close) Icons.Default.Close else if (compact) Icons.Default.Menu else Icons.Default.ChevronLeft,
                        contentDescription = stringResource(if (close) R.string.sidebar_close else if (compact) R.string.sidebar_expand else R.string.sidebar_collapse),
                        tint = c.ink, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!compact) {
                    Surface(onClick = { onNavigate(Routes.PROFILE) }, color = c.paper, shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SidebarAvatar(name, avatar, c) { onNavigate(Routes.PROFILE) }
                                Column(Modifier.weight(1f)) {
                                    Text(name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.nav_profile), color = c.ink,
                                        fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    email?.takeIf { it.isNotBlank() }?.let { Text(it, color = c.muted, fontSize = 12.sp) }
                                }
                            }
                            Surface(onClick = { onNavigate(Routes.PREMIUM) }, color = if (dark) Color(0xFF28574D) else Color(0xFFC9EFE0), shape = CircleShape) {
                                Text(stringResource(if (premium) R.string.sidebar_premium_active else R.string.sidebar_free_plan),
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                sidebarAreas().forEach { area ->
                    SidebarAreaDestination(area, currentRoute, active == area.route, compact, premium, c, onNavigate)
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = c.line)
                SidebarDestination(stringResource(R.string.profile_section_settings), stringResource(R.string.sidebar_settings_hint), Icons.Default.Settings,
                    currentRoute.substringBefore('?') == Routes.SETTINGS, compact, c) { onNavigate(Routes.SETTINGS) }
                SidebarDestination(stringResource(R.string.sidebar_help_support), null, Icons.AutoMirrored.Filled.HelpOutline,
                    currentRoute.substringBefore('?') == Routes.SUPPORT, compact, c) { onNavigate(Routes.SUPPORT) }
                if (admin) SidebarDestination(stringResource(R.string.sidebar_admin), null, Icons.Default.AdminPanelSettings,
                    currentRoute.substringBefore('?') == Routes.ADMIN_NOTIFICATIONS, compact, c) { onNavigate(Routes.ADMIN_NOTIFICATIONS) }
                if (compact) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = c.line)
                    SidebarAvatar(name, avatar, c) { onNavigate(Routes.PROFILE) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Category options float over the drawer; only a feature selection navigates. */
@Composable
private fun SidebarAreaDestination(
    area: SidebarArea, currentRoute: String, active: Boolean, compact: Boolean,
    premium: Boolean, c: SidebarColors, onNavigate: (String) -> Unit,
) {
    val window = LocalWindowInfo.current.containerSize
    var expanded by remember(currentRoute, compact, window) { mutableStateOf(false) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    var anchorCenterY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val motion = LocalMotionPolicy.current.animationsEnabled
    val menuWidth = with(density) { (window.width.toDp() - 32.dp).coerceIn(1.dp, 280.dp) }
    val opensAbove = anchorCenterY > window.height / 2f
    val menuHeight = with(density) {
        val top = WindowInsets.safeDrawing.getTop(this)
        val bottom = window.height - WindowInsets.safeDrawing.getBottom(this)
        val available = when {
            compact -> (bottom - top).toFloat()
            opensAbove -> anchorCenterY - anchorSize.height / 6f - top
            else -> bottom - anchorCenterY - anchorSize.height / 6f
        }
        // Scroll in the chosen direction instead of forcing a tall menu to flip sides.
        (available.toDp() - 24.dp).coerceIn(1.dp, 560.dp)
    }
    val menuOrigin = TransformOrigin(0f, if (compact) .5f else if (opensAbove) 1f else 0f)
    val title = stringResource(area.title)
    if (area.items.isEmpty()) {
        SidebarDestination(title, stringResource(area.description), area.icon, active, compact, c) { onNavigate(area.route) }
        return
    }
    SafarComposablesDropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
        // Follow the tapped row's current window position, including scrolling and
        // rotation: upper-half anchors open down, lower-half anchors open up.
        // Phone panels overlap the drawer's right half; compact rails open beside it.
        side = when {
            compact -> DropdownMenuSide.End
            opensAbove -> DropdownMenuSide.Top
            else -> DropdownMenuSide.Bottom
        },
        alignment = DropdownMenuAlignment.Start,
        sideOffset = if (compact) 8.dp else with(density) { -(anchorSize.height / 3f).toDp() },
        alignmentOffset = if (compact) 0.dp else with(density) { (anchorSize.width * .42f).toDp() },
        panel = {
            DropdownMenuPanel(
                modifier = Modifier.heightIn(max = menuHeight).verticalScroll(rememberScrollState()),
                minWidth = minOf(248.dp, menuWidth), maxWidth = menuWidth,
                shape = RoundedCornerShape(22.dp), backgroundColor = c.paper, contentColor = c.ink,
                enter = if (motion) fadeIn(tween(120)) + scaleIn(
                    tween(160, easing = LinearOutSlowInEasing), initialScale = .98f, transformOrigin = menuOrigin,
                ) else EnterTransition.None,
                exit = if (motion) fadeOut(tween(90)) + scaleOut(
                    tween(90, easing = FastOutLinearInEasing), targetScale = .99f, transformOrigin = menuOrigin,
                ) else ExitTransition.None,
            ) {
                DropdownMenuLabel(contentColor = c.muted) { UiText(title, fontWeight = FontWeight.SemiBold) }
                DropdownMenuSeparator()
                area.items.forEach { item ->
                    val selected = isDrawerItemSelected(item, currentRoute.substringBefore('?'))
                    DropdownMenuItem(
                        onClick = { expanded = false; onNavigate(item.route) },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { this.selected = selected },
                        leading = { UiIcon(item.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                        trailing = {
                            if (selected) UiIcon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            else if (item.requiresPremium && !premium) UiIcon(Icons.Default.Lock,
                                contentDescription = stringResource(R.string.drawer_premium_locked), modifier = Modifier.size(16.dp))
                        },
                    ) { UiText(stringResource(item.labelRes)) }
                }
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().onGloballyPositioned {
            anchorSize = it.size
            anchorCenterY = it.boundsInWindow().center.y
        }) {
            SidebarDestination(title, stringResource(area.description), area.icon, active || expanded, compact, c,
                opensMenu = true, menuExpanded = expanded, menuOpensAbove = opensAbove) { expanded = !expanded }
        }
    }
}

@Composable
private fun SidebarAvatar(name: String?, avatar: String?, c: SidebarColors, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(c.peach).clickable(role = Role.Button, onClickLabel = stringResource(R.string.nav_profile), onClick = onClick), contentAlignment = Alignment.Center) {
        if (!avatar.isNullOrBlank()) AsyncImage(avatar, stringResource(R.string.nav_profile), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name?.firstOrNull()?.uppercase() ?: "•", color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SidebarDestination(title: String, description: String?, icon: ImageVector, active: Boolean,
    compact: Boolean, c: SidebarColors, opensMenu: Boolean = false, menuExpanded: Boolean = false,
    menuOpensAbove: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val motion = LocalMotionPolicy.current.animationsEnabled
    val pressScale = animateFloatAsState(
        if (pressed && motion) .97f else 1f,
        if (motion) spring(dampingRatio = 1f, stiffness = 900f) else tween(0), label = "sidebarPress",
    )
    val emphasis = animateFloatAsState(
        if (active) 1f else if (hovered || focused) .35f else 0f,
        tween(if (motion) 140 else 0, easing = FastOutSlowInEasing), label = "sidebarSelection",
    )
    val chevronRotation = animateFloatAsState(
        if (menuExpanded) { if (menuOpensAbove) -90f else 90f } else 0f,
        tween(if (motion) 140 else 0, easing = FastOutSlowInEasing), label = "sidebarChevron",
    )
    val target = Modifier.fillMaxWidth().graphicsLayer {
        scaleX = pressScale.value
        scaleY = pressScale.value
    }.clip(shape).drawBehind {
        drawRect(c.selection, alpha = emphasis.value)
    }.focusRing(interactionSource = interactions, shape = shape)
        .clickable(interactionSource = interactions, indication = null,
            role = if (opensMenu) Role.Button else Role.Tab, onClick = onClick)
        .semantics { selected = active }
    if (compact) {
        Column(target.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(if (active) c.selection else c.peach.copy(alpha = .5f)), contentAlignment = Alignment.Center) {
                UiIcon(icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = c.ink)
            }
            UiText(title, color = c.ink, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
        }
    } else {
        Row(target.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(c.peach.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                UiIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = c.ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                UiText(title, color = c.ink, fontSize = 15.sp, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                description?.let { UiText(it, color = c.muted, fontSize = 12.sp, lineHeight = 17.sp) }
            }
            if (opensMenu) UiIcon(Icons.Default.ChevronRight, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = chevronRotation.value })
        }
    }
}

/** Navigation-only category pages; existing feature screens and access checks stay intact. */
@Composable
internal fun NavigationHubScreen(route: String, dark: Boolean, onNavigate: (String) -> Unit, onToggleDarkTheme: () -> Unit) {
    val area = sidebarAreas().first { it.route == route }
    val c = sidebarColors(dark)
    SafarDrawerScaffold(title = stringResource(area.title), currentRoute = route, isDarkTheme = dark,
        onNavigate = onNavigate, onToggleDarkTheme = onToggleDarkTheme, containerColor = c.canvas) { padding ->
        LazyVerticalGrid(columns = GridCells.Adaptive(260.dp), modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(area.items, key = { it.route }) { item ->
                Surface(onClick = { onNavigate(item.route) }, shape = RoundedCornerShape(24.dp), color = c.paper) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(item.icon, null, Modifier.size(28.dp), tint = c.ink)
                        Text(stringResource(item.labelRes), Modifier.weight(1f), color = c.ink, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight, null, tint = c.ink)
                    }
                }
            }
        }
    }
}
