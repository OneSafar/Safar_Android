package com.safarparmar.app.ui.drawer

import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.foundation.border
import com.safarparmar.app.ui.components.LanguageSelectionDialog
import androidx.appcompat.app.AppCompatDelegate
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
import com.composables.ui.components.Text as UiText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
        SidebarArea(Routes.DASHBOARD, R.string.nav_dashboard, R.string.sidebar_dashboard_hint, Icons.Default.Dashboard, emptyList()),
        SidebarArea(Routes.STUDY_HUB, R.string.rail_study, R.string.sidebar_study_hint, Icons.AutoMirrored.Filled.MenuBook,
            study.filterNot { it.route == Routes.LEADERBOARD }),
        SidebarArea(Routes.WELLBEING_HUB, R.string.sidebar_section_wellbeing, R.string.sidebar_wellbeing_hint, Icons.Default.Spa,
            other.filter { it.route in setOf(Routes.NISHTHA, Routes.DHYAN, Routes.CALM_CENTRE, Routes.LIVE_SESSIONS_ROOT) }),
        SidebarArea(Routes.COMMUNITY_HUB, R.string.rail_community, R.string.sidebar_community_hint, Icons.Default.Groups,
            other.filter { it.route in setOf(Routes.MEHFIL, Routes.STUDY_CIRCLES) } + study.filter { it.route == Routes.LEADERBOARD }),
        SidebarArea(Routes.PROGRESS_HUB, R.string.sidebar_progress, R.string.sidebar_progress_hint, Icons.Default.BarChart,
            emptyList()),
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
private fun sidebarColors(dark: Boolean) = if (dark) SidebarColors(BgDark, Color(0xFF342735), Color(0xFFFFF0F6), Color(0xFFCFBECB), RoyalPurpleSoftDark, RoyalPurpleSoftDark, Color(0xFF654D66))
else SidebarColors(Color(0xFFFAF8FF), Color(0xFFFFFDFC), Color(0xFF241544), Color(0xFF75616F), RoyalPurpleSoft, RoyalPurpleSoft, Color(0xFFE8DEE5))

@Composable
fun SafarDrawer(currentRoute: String, isDarkTheme: Boolean, isAdmin: Boolean, isPremiumActive: Boolean,
    userName: String? = null, userEmail: String? = null, userAvatar: String? = null,
    onNavigate: (String) -> Unit, onToggleDarkTheme: () -> Unit, onCloseDrawer: () -> Unit) {
    Surface(modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().fillMaxHeight(),
        color = sidebarColors(isDarkTheme).canvas, shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) {
        SoftSidebarContent(currentRoute, isDarkTheme, false, isAdmin, isPremiumActive, userName, userEmail, userAvatar,
            onNavigate = { onNavigate(it); onCloseDrawer() }, onToggleDarkTheme = onToggleDarkTheme, onMenu = onCloseDrawer, close = true)
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
        onToggleDarkTheme = onToggleDarkTheme, onMenu = { onExpandedChange(!isExpanded) })
}

@Composable
private fun SoftSidebarContent(currentRoute: String, dark: Boolean, compact: Boolean, admin: Boolean, premium: Boolean,
    name: String?, email: String?, avatar: String?, modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit, onToggleDarkTheme: () -> Unit, onMenu: () -> Unit, close: Boolean = false) {
    val c = sidebarColors(dark)
    val active = selectedSidebarArea(currentRoute)
    SafarComposablesTheme {
        Column(modifier.fillMaxHeight().background(c.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(horizontal = if (compact) 8.dp else 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            var showLanguage by remember { mutableStateOf(false) }
            if (showLanguage) LanguageSelectionDialog(onDismiss = { showLanguage = false })
            Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start) {
                if (!compact) {
                    Row(Modifier.weight(1f).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(72.dp)) {
                            SidebarAvatar(name, avatar, c, enlarged = true, premium = premium) { onNavigate(Routes.PROFILE) }
                            if (premium) {
                                UiIconButton(onClick = { onNavigate(Routes.PREMIUM) }, style = ButtonStyle.Ghost,
                                    modifier = Modifier.size(48.dp).align(Alignment.BottomEnd)) {
                                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFC99B3E))
                                        .border(2.dp, c.canvas, CircleShape), contentAlignment = Alignment.Center) {
                                        UiIcon(ImageVector.vectorResource(R.drawable.ic_sidebar_premium_crown),
                                            contentDescription = stringResource(R.string.sidebar_premium_active),
                                            tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        UiText(name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.nav_profile),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).wrapContentHeight()
                                .clickable(role = Role.Button) { onNavigate(Routes.PROFILE) },
                            color = c.ink, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                UiIconButton(onClick = onMenu, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
                    UiIcon(if (close) Icons.Default.Close else if (compact) Icons.Default.Menu else Icons.Default.ChevronLeft,
                        contentDescription = stringResource(if (close) R.string.sidebar_close else if (compact) R.string.sidebar_expand else R.string.sidebar_collapse),
                        tint = c.ink, modifier = Modifier.size(24.dp))
                }
            }
            if (!compact) HorizontalDivider(Modifier.padding(bottom = 10.dp), color = c.line)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            // Use spare height above the destinations, keeping the final row near the footer.
            // Short windows still scroll the entire list rather than clipping destinations.
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight).padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)) {
                sidebarAreas().forEach { area ->
                    SidebarAreaDestination(area, currentRoute, active == area.route, compact, premium, c, onNavigate)
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = c.line)
                SidebarDestination(stringResource(R.string.profile_section_settings), stringResource(R.string.sidebar_settings_hint), Icons.Default.Settings,
                    currentRoute.substringBefore('?') == Routes.SETTINGS, compact, c) { onNavigate(Routes.SETTINGS) }
                SidebarDestination(stringResource(R.string.nav_profile), null, Icons.Default.Person,
                    currentRoute.substringBefore('?') == Routes.PROFILE, compact, c) { onNavigate(Routes.PROFILE) }
                if (admin) SidebarDestination(stringResource(R.string.sidebar_admin), null, Icons.Default.AdminPanelSettings,
                    currentRoute.substringBefore('?') == Routes.ADMIN_NOTIFICATIONS, compact, c) { onNavigate(Routes.ADMIN_NOTIFICATIONS) }
                Spacer(Modifier.height(8.dp))
            }
            }
            HorizontalDivider(color = c.line)
            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val language = AppCompatDelegate.getApplicationLocales().get(0)?.displayLanguage
                    ?: java.util.Locale.getDefault().displayLanguage
                SidebarDestination(stringResource(R.string.sidebar_language), if (compact) null else language,
                    Icons.Default.Language, false, compact, c) { showLanguage = true }
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (compact) Arrangement.Center else Arrangement.SpaceBetween) {
                    if (!compact) UiText(stringResource(if (dark) R.string.drawer_dark_mode else R.string.drawer_light_mode), color = c.ink, fontSize = 14.sp)
                    JetCoSwitchButton(checked = dark, onCheckedChange = { onToggleDarkTheme() },
                        selectedTrackColor = c.selection, unselectedTrackColor = c.peach, knobColor = c.paper, iconColor = c.ink)
                }
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
    val title = stringResource(area.title)
    if (area.items.isEmpty()) {
        SidebarDestination(title, stringResource(area.description), area.icon, active, compact, c) { onNavigate(if (area.route == Routes.PROGRESS_HUB) Routes.ANALYTICS else area.route) }
        return
    }
    Box(Modifier.fillMaxWidth().onGloballyPositioned {
        anchorSize = it.size
        anchorCenterY = it.boundsInWindow().center.y
    }) {
        SidebarDestination(title, stringResource(area.description), area.icon, active || expanded, compact, c,
            opensMenu = true, menuExpanded = expanded, menuOpensAbove = opensAbove) { expanded = !expanded }
        if (expanded) {
            val margin = with(density) { 16.dp.roundToPx() }
            val gap = with(density) { 8.dp.roundToPx() }
            val topInset = WindowInsets.safeDrawing.getTop(density)
            val bottomInset = WindowInsets.safeDrawing.getBottom(density)
            val position = remember(compact, margin, gap, topInset, bottomInset) {
                SidebarMenuPositionProvider(compact, margin, gap, topInset, bottomInset)
            }
            // A native popup has its own window. It is not constrained by the
            // drawer sheet or by the anchored library panel's layout container.
            androidx.compose.ui.window.Popup(
                popupPositionProvider = position,
                onDismissRequest = { expanded = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
            ) {
                androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides density) {
                    Surface(
                        modifier = Modifier.width(menuWidth).heightIn(max = menuHeight),
                        shape = RoundedCornerShape(22.dp), color = c.selection, contentColor = c.ink,
                        shadowElevation = 12.dp,
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
                            Text(title, Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = c.ink, fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(Modifier.padding(bottom = 6.dp), color = c.line)
                            area.items.forEach { item ->
                                val selected = isDrawerItemSelected(item, currentRoute.substringBefore('?'))
                                DropdownMenuItem(
                                    text = { Text(stringResource(item.labelRes), color = c.ink) },
                                    onClick = { expanded = false; onNavigate(item.route) },
                                    modifier = Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) c.peach.copy(alpha = .5f) else Color.Transparent)
                                        .semantics { this.selected = selected },
                                    leadingIcon = { Icon(item.icon, null, Modifier.size(22.dp), tint = c.ink) },
                                    trailingIcon = {
                                        if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = c.ink)
                                        else if (item.requiresPremium && !premium) Icon(Icons.Default.Lock,
                                            stringResource(R.string.drawer_premium_locked), Modifier.size(16.dp), tint = c.muted)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarAvatar(name: String?, avatar: String?, c: SidebarColors, enlarged: Boolean = false, premium: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(if (enlarged) 64.dp else 48.dp).then(if (premium) Modifier.border(2.dp, Color(0xFFD6AE62), CircleShape).padding(3.dp) else Modifier).clip(CircleShape).background(c.peach).clickable(role = Role.Button, onClickLabel = stringResource(R.string.nav_profile), onClick = onClick), contentAlignment = Alignment.Center) {
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
