package com.safarparmar.app.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.composed
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon as ComposablesIcon
import com.safarparmar.app.ui.components.LanguageSelectionDialog
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.performance.LocalMotionPolicy

internal val SafarCompactRailWidth = 104.dp
internal val SafarExpandedRailWidth = 254.8.dp

private fun railOutlineIcon(icon: ImageVector): ImageVector = when (icon) {
    Icons.Default.Home -> Icons.Outlined.Home
    Icons.Default.Dashboard -> Icons.Outlined.Dashboard
    Icons.Default.Analytics -> Icons.Outlined.BarChart
    Icons.Default.School -> Icons.Outlined.School
    Icons.Default.SelfImprovement -> Icons.Outlined.SelfImprovement
    Icons.Default.Groups -> Icons.Outlined.Groups
    Icons.Default.Person -> Icons.Outlined.Person
    Icons.Default.Settings -> Icons.Outlined.Settings
    Icons.Default.Language -> Icons.Outlined.Language
    else -> icon
}

/** Optional screen-specific typography; other sidebars retain their existing sizing. */
internal val LocalSafarRailTextStyle = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.ui.text.TextStyle?> { null }

data class SafarRailItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
)

/** Persistent tablet navigation, with the same inset panel and spacing as the reference. */
@Composable
private fun LegacySafarNavigationRail(
    currentRoute: String,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    userName: String? = null,
    userEmail: String? = null,
    userAvatar: String? = null,
    isPremiumActive: Boolean = false,
    isAdmin: Boolean = false,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expansionProgress: () -> Float = { if (isExpanded) 1f else 0f },
    modifier: Modifier = Modifier,
    transparentCanvas: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val currentRouteBase = currentRoute.substringBefore("?")
    var studyExpanded by rememberSaveable { mutableStateOf(false) }
    var wellbeingExpanded by rememberSaveable { mutableStateOf(false) }
    var communityExpanded by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val motionEnabled = LocalMotionPolicy.current.animationsEnabled
    val enterDuration = if (motionEnabled) 180 else 0
    val exitDuration = if (motionEnabled) 120 else 0
    if (showLanguageDialog) {
        LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
    }
    val railBg = if (isDarkTheme) Color(0xFF131316) else Color(0xFFFEFBF6)
    val canvasBg = railBg
    val textColor = if (isDarkTheme) Color(0xFFD8D2EB) else Color(0xFF151B4D)

    if (!isExpanded) {
        SafarLabeledRail(
            currentRoute = currentRouteBase,
            isDarkTheme = isDarkTheme,
            isAdmin = isAdmin,
            modifier = modifier,
            onNavigate = onNavigate,
            onExpand = { onExpandedChange(true) },
            onStudy = { studyExpanded = true; onExpandedChange(true) },
            onWellbeing = { wellbeingExpanded = true; onExpandedChange(true) },
            onCommunity = { communityExpanded = true; onExpandedChange(true) },
            onToggleDarkTheme = onToggleDarkTheme,
            onLanguage = { showLanguageDialog = true },
        )
        return
    }

    Column(
        modifier = modifier
            .width(if (isExpanded) SafarExpandedRailWidth else SafarCompactRailWidth)
            .fillMaxHeight()
            .background(railBg)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
            .drawWithContent {
                drawContent()
                val edge = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) size.width else 0f
                drawLine(
                    if (isDarkTheme) Color(0xFF35313E) else Color(0xFFEDE8E1),
                    androidx.compose.ui.geometry.Offset(edge, 0f),
                    androidx.compose.ui.geometry.Offset(edge, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Only the incoming artwork is composed. AnimatedContent retains the outgoing
        // wordmark while the parent snaps to rail width, briefly clipping it to "SA".
        // Keep the complete brand at its natural width and reveal it near the end
        // of expansion; a reversal disposes it immediately, including its animation.
        val headerAlpha = remember(isExpanded, motionEnabled) {
            Animatable(if (motionEnabled) 0f else 1f)
        }
        LaunchedEffect(headerAlpha) {
            headerAlpha.animateTo(1f, tween(if (motionEnabled) 100 else 0))
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .sidebarClick(
                    label = if (isExpanded) "Collapse sidebar" else "Expand sidebar",
                ) { onExpandedChange(!isExpanded) }
                .semantics {
                    contentDescription = if (isExpanded) "Collapse sidebar" else "Expand sidebar"
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isExpanded) {
                Row(
                    modifier = Modifier.requiredWidth(160.dp).graphicsLayer {
                        val spaceAvailable = if (motionEnabled) {
                            ((expansionProgress() - 0.82f) / 0.18f).coerceIn(0f, 1f)
                        } else 1f
                        alpha = headerAlpha.value * spaceAvailable
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    Image(
                        painter = painterResource(
                            if (isDarkTheme) R.drawable.ic_safar_logo_brand_dark
                            else R.drawable.ic_safar_logo_brand_light,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    androidx.compose.material3.Text(
                        text = "SAFAR", fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily(Font(R.font.poppins_semibold, FontWeight.SemiBold)),
                        fontSize = 19.sp, letterSpacing = 1.8.sp, color = textColor,
                        maxLines = 1, softWrap = false,
                    )
                }
            } else {
                ComposablesIcon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp).graphicsLayer { alpha = headerAlpha.value },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Share the drawer's routes, but group them for the tablet sidebar.
            val groups = remember { tabletSidebarGroups() }

            @Composable
            fun NavigationItem(item: DrawerItem, isSubcategory: Boolean = false) {
                if (!item.requiresAdmin || isAdmin) {
                    SafarCollapsibleRailItem(
                        item = SafarRailItem(item.route, stringResource(item.labelRes), railOutlineIcon(item.icon)),
                        isSelected = isDrawerItemSelected(item, currentRouteBase),
                        isExpanded = isExpanded, isDarkTheme = isDarkTheme,
                        expansionProgress = expansionProgress,
                        isSubcategory = isSubcategory,
                        onClick = { onNavigate(item.route) },
                    )
                }
            }

            @Composable
            fun Section(label: String, icon: ImageVector, items: List<DrawerItem>, expanded: Boolean, onSectionChange: (Boolean) -> Unit) {
                val visibleItems = items.filter { !it.requiresAdmin || isAdmin }
                SafarRailSectionHeader(
                    title = label, icon = railOutlineIcon(icon), sidebarExpanded = isExpanded,
                    sectionExpanded = expanded,
                    hasSelectedChild = visibleItems.any { isDrawerItemSelected(it, currentRouteBase) },
                    isDarkTheme = isDarkTheme,
                    expansionProgress = expansionProgress,
                    onClick = {
                        val change = sectionTapResult(isExpanded, expanded)
                        if (change.openSidebar) onExpandedChange(true)
                        onSectionChange(change.sectionExpanded)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                )
                AnimatedVisibility(
                    visible = isExpanded && expanded,
                    enter = fadeIn(tween(enterDuration)) + scaleIn(tween(enterDuration), initialScale = 0.97f),
                    exit = fadeOut(tween(exitDuration)) + scaleOut(tween(exitDuration), targetScale = 0.98f),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        visibleItems.forEach { NavigationItem(it, isSubcategory = true) }
                    }
                }
            }

            drawerPinnedTop.forEach { NavigationItem(it) }
            Section(stringResource(R.string.drawer_section_study_productivity), Icons.Default.School, groups.study, studyExpanded) { studyExpanded = it }
            Section(stringResource(R.string.sidebar_section_wellbeing), Icons.Outlined.VolunteerActivism, groups.wellbeing, wellbeingExpanded) { wellbeingExpanded = it }
            Section(stringResource(R.string.sidebar_section_community_resources), Icons.Outlined.Diversity3, groups.community, communityExpanded) { communityExpanded = it }
            groups.account.forEach { NavigationItem(it) }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(if (isExpanded) 56.dp else 36.dp)
                .height(1.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isDarkTheme) Color(0xFF35313E) else Color(0xFFEDE8E1)),
        )
        Spacer(Modifier.height(8.dp))
        if (isExpanded) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkTheme) Color(0xFF272532) else Color(0xFFF6F5F8))
                    .toggleable(value = isDarkTheme, role = androidx.compose.ui.semantics.Role.Switch) {
                        onToggleDarkTheme()
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Crossfade(targetState = isDarkTheme, animationSpec = tween(enterDuration), label = "sidebarThemeIcon") { dark ->
                    ComposablesIcon(
                        imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null, tint = Color(0xFF6925DE), modifier = Modifier.size(22.dp),
                    )
                }
                com.composables.ui.components.Text(
                    text = stringResource(if (isDarkTheme) R.string.drawer_dark_mode else R.string.drawer_light_mode),
                    fontSize = LocalSafarRailTextStyle.current?.fontSize ?: 14.sp, color = textColor,
                    modifier = Modifier.weight(1f).sidebarLabelReveal(expansionProgress), maxLines = 1,
                )
                JetCoSwitchButton(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleDarkTheme() },
                    buttonWidth = 48.dp, buttonHeight = 28.dp, switchPadding = 2.5.dp,
                    iconSize = 16.dp, selectedTrackColor = Color(0xFF6925DE),
                    unselectedTrackColor = Color(0xFF9393A6), animationDuration = 240,
                )
            }
        } else {
            // A single icon toggle is clearer in the narrow rail than a miniature sliding switch.
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkTheme) Color(0xFF272532) else Color(0xFFF6F5F8))
                    .toggleable(value = isDarkTheme, role = androidx.compose.ui.semantics.Role.Switch) {
                        onToggleDarkTheme()
                    }
                    .semantics { contentDescription = "Dark Mode" },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = isDarkTheme, animationSpec = tween(enterDuration), label = "railThemeIcon") { dark ->
                    ComposablesIcon(
                        imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = if (dark) Color(0xFFC49AFF) else Color(0xFF6925DE),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        SafarCollapsibleRailItem(
            item = SafarRailItem("language", stringResource(R.string.profile_language_title), Icons.Default.Language),
            isSelected = false, isExpanded = isExpanded, isDarkTheme = isDarkTheme,
            expansionProgress = expansionProgress,
            onClick = { showLanguageDialog = true },
        )
    }
}

@Composable
private fun SafarRailSectionHeader(
    title: String,
    icon: ImageVector,
    sidebarExpanded: Boolean,
    sectionExpanded: Boolean,
    hasSelectedChild: Boolean,
    isDarkTheme: Boolean,
    expansionProgress: () -> Float,
    onClick: () -> Unit,
) {
    val tint = if (hasSelectedChild) { if (isDarkTheme) Color(0xFFC49AFF) else Color(0xFF6925DE) } else if (isDarkTheme) Color(0xFFD8D2EB) else Color(0xFF151B4D)
    val selected = hasSelectedChild && (!sidebarExpanded || !sectionExpanded)
    val indicator = if (selected) {
        if (isDarkTheme) Color(0xFF352447) else Color(0xFFF0E7FF)
    } else Color.Transparent
    val stateLabel = stringResource(if (sidebarExpanded && sectionExpanded) R.string.drawer_collapse else R.string.drawer_expand)
    if (sidebarExpanded) {
        val rotation by animateFloatAsState(
            targetValue = if (sectionExpanded) 180f else 0f,
            animationSpec = if (LocalMotionPolicy.current.animationsEnabled) {
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            } else snap(),
            label = "sectionChevron",
        )
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = if (LocalSafarRailTextStyle.current != null) 56.dp else 48.dp).padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(indicator)
                .sidebarClick(onClick = onClick)
                .semantics { stateDescription = stateLabel }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ComposablesIcon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            androidx.compose.material3.Text(
                title, modifier = Modifier.weight(1f).sidebarLabelReveal(expansionProgress), color = tint,
                fontSize = ((LocalSafarRailTextStyle.current?.fontSize?.value ?: 14f) + 1f).sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2,
            )
            ComposablesIcon(
                Icons.Default.KeyboardArrowDown, contentDescription = null, tint = tint,
                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation },
            )
        }
    } else {
        Box(
            modifier = Modifier.size(width = 54.dp, height = 48.dp).clip(RoundedCornerShape(14.dp))
                .background(indicator)
                .sidebarClick(onClick = onClick)
                .semantics { contentDescription = title; stateDescription = stateLabel },
            contentAlignment = Alignment.Center,
        ) {
            ComposablesIcon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SafarCollapsibleRailItem(
    item: SafarRailItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    isDarkTheme: Boolean,
    expansionProgress: () -> Float,
    isSubcategory: Boolean = false,
    onClick: () -> Unit,
) {
    val motionEnabled = LocalMotionPolicy.current.animationsEnabled
    val activeColor = if (isDarkTheme) Color(0xFFC49AFF) else Color(0xFF6925DE)
    val inactiveTint = if (isDarkTheme) Color.White.copy(alpha = 0.65f) else Color(0xFF151B4D)

    val indicatorBg by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDarkTheme) Color(0xFF352447) else Color(0xFFF0E7FF)
        } else {
            Color.Transparent
        },
        animationSpec = tween(if (motionEnabled) 160 else 0),
        label = "itemIndicatorBg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveTint,
        animationSpec = tween(if (motionEnabled) 160 else 0),
        label = "itemIconTint"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = tween(if (motionEnabled) 160 else 0),
        label = "itemScale"
    )

    if (isExpanded) {
        // Expanded: Full-width item with icon, label, and active pill background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (LocalSafarRailTextStyle.current != null) 56.dp else 48.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(indicatorBg)
                .sidebarClick(onClick = onClick)
                .semantics { selected = isSelected }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposablesIcon(
                imageVector = item.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(if (isSubcategory) 22.dp else 28.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
            Spacer(modifier = Modifier.width(12.dp))
            com.composables.ui.components.Text(
                text = item.title,
                fontSize = ((LocalSafarRailTextStyle.current?.fontSize?.value ?: 14f) - if (isSubcategory) 1f else 0f).sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else if (isSubcategory) FontWeight.Normal else FontWeight.Medium,
                color = if (isSelected) activeColor else (if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color(0xFF151B4D)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp).sidebarLabelReveal(expansionProgress),
            )
        }
    } else {
        // Collapsed: lightweight icon target with an accessible label
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(indicatorBg)
                .sidebarClick(onClick = onClick)
                .semantics { selected = isSelected },
            contentAlignment = Alignment.Center,
        ) {
            ComposablesIcon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

/** Draw-phase reveal, driven by the same position as the sidebar edge. */
private fun Modifier.sidebarLabelReveal(progress: () -> Float): Modifier = composed {
    val direction = if (androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Ltr) 1f else -1f
    graphicsLayer {
        val amount = ((progress() - 0.45f) / 0.55f).coerceIn(0f, 1f)
        alpha = amount
        translationX = -direction * (1f - amount) * 8.dp.toPx()
    }
}

/** Immediate, interruptible press feedback; focused/hovered targets remain easy to locate. */
private fun Modifier.sidebarClick(
    label: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val enabled = LocalMotionPolicy.current.animationsEnabled
    val scale = animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.97f
            hovered || focused -> 1.015f
            else -> 1f
        },
        animationSpec = if (enabled) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)
        } else snap(),
        label = "sidebarPress",
    )
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            role = Role.Button,
            onClickLabel = label,
            onClick = onClick,
        )
}

/** Wider icon-and-label rail shared by every screen hosted in SafarDrawerScaffold. */
@Composable
private fun SafarLabeledRail(
    currentRoute: String,
    isDarkTheme: Boolean,
    isAdmin: Boolean,
    modifier: Modifier,
    onNavigate: (String) -> Unit,
    onExpand: () -> Unit,
    onStudy: () -> Unit,
    onWellbeing: () -> Unit,
    onCommunity: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onLanguage: () -> Unit,
) {
    val groups = remember { tabletSidebarGroups() }
    val canvas = if (isDarkTheme) Color(0xFF131316) else Color(0xFFFEFBF6)
    val ink = if (isDarkTheme) Color(0xFFD8D2EB) else Color(0xFF151B4D)
    val purple = if (isDarkTheme) Color(0xFFC49AFF) else Color(0xFF6925DE)
    val activeSurface = if (isDarkTheme) Color(0xFF352447) else Color(0xFFF0E7FF)
    val divider = if (isDarkTheme) Color(0xFF35313E) else Color(0xFFEDE8E1)

    @Composable
    fun Item(label: String, icon: ImageVector, selectedItem: Boolean = false, action: () -> Unit) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(14.dp)).sidebarClick(label, action)
                .semantics { selected = selectedItem }
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                Modifier.size(44.dp).background(if (selectedItem) activeSurface else Color.Transparent, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ComposablesIcon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (selectedItem) purple else ink)
            }
            androidx.compose.material3.Text(
                label, color = if (selectedItem) purple else ink,
                fontSize = 12.sp, lineHeight = 15.sp,
                fontWeight = if (selectedItem) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Row(
        modifier.width(SafarCompactRailWidth).fillMaxHeight().background(canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)),
    ) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).sidebarClick("Expand sidebar", onExpand), contentAlignment = Alignment.Center) {
                ComposablesIcon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.nav_open_menu), modifier = Modifier.size(22.dp), tint = ink)
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                drawerPinnedTop.filter { !it.requiresAdmin || isAdmin }.forEach { item ->
                    val active = isDrawerItemSelected(item, currentRoute)
                    val icon = when (item.route.substringBefore("?")) {
                        Routes.HOME -> if (active) Icons.Filled.Home else Icons.Outlined.Home
                        Routes.DASHBOARD -> Icons.Outlined.Dashboard
                        else -> Icons.Outlined.BarChart
                    }
                    Item(stringResource(item.labelRes), icon, active) { onNavigate(item.route) }
                }
                Item(stringResource(R.string.rail_study), Icons.Outlined.School,
                    groups.study.any { isDrawerItemSelected(it, currentRoute) }, onStudy)
                Item(stringResource(R.string.sidebar_section_wellbeing), Icons.Outlined.VolunteerActivism,
                    groups.wellbeing.any { isDrawerItemSelected(it, currentRoute) }, onWellbeing)
                Item(stringResource(R.string.rail_community), Icons.Outlined.Diversity3,
                    groups.community.any { isDrawerItemSelected(it, currentRoute) }, onCommunity)
                groups.account.filter { !it.requiresAdmin || isAdmin }.forEach { item ->
                    Item(stringResource(item.labelRes), if (item.route == Routes.PROFILE) Icons.Outlined.Person else Icons.Outlined.Settings,
                        isDrawerItemSelected(item, currentRoute)) { onNavigate(item.route) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(1.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(divider),
            )
            Spacer(Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .sidebarClick(label = stringResource(if (isDarkTheme) R.string.drawer_dark_mode else R.string.drawer_light_mode), onClick = onToggleDarkTheme),
                    contentAlignment = Alignment.Center,
                ) {
                    ComposablesIcon(
                        if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = stringResource(if (isDarkTheme) R.string.drawer_dark_mode else R.string.drawer_light_mode),
                        modifier = Modifier.size(22.dp),
                        tint = purple,
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .sidebarClick(label = stringResource(R.string.profile_language_title), onClick = onLanguage),
                    contentAlignment = Alignment.Center,
                ) {
                    ComposablesIcon(
                        Icons.Outlined.Language,
                        contentDescription = stringResource(R.string.profile_language_title),
                        modifier = Modifier.size(22.dp),
                        tint = ink,
                    )
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(divider))
    }
}
