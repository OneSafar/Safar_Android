package com.safarparmar.app.ui.drawer
import com.composables.ui.components.HorizontalSeparator as HorizontalDivider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────

data class DrawerItem(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
    val rainbowShimmer: Boolean = false,
    val requiresAdmin: Boolean = false,
    val requiresPremium: Boolean = false,
)

enum class DrawerSectionId {
    STUDY_PRODUCTIVITY,
    WELL_BEING_COMMUNITY,
}

data class DrawerSection(
    val id: DrawerSectionId,
    val labelRes: Int,
    val icon: ImageVector,
    val items: List<DrawerItem>,
    val defaultExpanded: Boolean = false,
)

val drawerPinnedTop = listOf(
    DrawerItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    DrawerItem(R.string.nav_dashboard, Icons.Default.Dashboard, Routes.DASHBOARD),
    DrawerItem(R.string.nishtha_tab_analytics, Icons.Default.Analytics, Routes.nishthaAnalytics()),
)

val drawerSections = listOf(
    DrawerSection(
        id = DrawerSectionId.STUDY_PRODUCTIVITY,
        labelRes = R.string.drawer_section_study_productivity,
        icon = Icons.Default.School,
        defaultExpanded = false,
        items = listOf(
            DrawerItem(
                R.string.nav_study_planner,
                Icons.AutoMirrored.Filled.EventNote,
                Routes.STUDY_PLANNER,
                requiresPremium = true,
            ),
            DrawerItem(R.string.module_ekagra, Icons.Default.Timer, Routes.EKAGRA),
            DrawerItem(R.string.nav_habit_tracker, Icons.Default.CheckCircle, Routes.HABIT_TRACKER),
            DrawerItem(
                R.string.nav_focus_shield,
                Icons.Default.Shield,
                Routes.FOCUS_SHIELD,
            ),
            DrawerItem(
                R.string.nav_leaderboard,
                Icons.Default.Leaderboard,
                Routes.LEADERBOARD,
            ),
        ),
    ),
    DrawerSection(
        id = DrawerSectionId.WELL_BEING_COMMUNITY,
        labelRes = R.string.drawer_section_well_being_community,
        icon = Icons.Default.VolunteerActivism,
        items = listOf(
            DrawerItem(R.string.module_nishtha, Icons.Default.SelfImprovement, Routes.NISHTHA),
            DrawerItem(R.string.module_dhyan, Icons.Default.Spa, Routes.DHYAN),
            DrawerItem(R.string.module_mehfil, Icons.Default.Groups, Routes.MEHFIL),
            DrawerItem(R.string.nav_study_circle, Icons.Default.GroupWork, Routes.STUDY_CIRCLES),
        ),
    ),
)

val drawerPinnedBottom = listOf(
    DrawerItem(R.string.nav_premium, Icons.Default.WorkspacePremium, Routes.PREMIUM),
    DrawerItem(R.string.nav_profile, Icons.Default.Person, Routes.PROFILE),
    DrawerItem(R.string.profile_section_settings, Icons.Default.Settings, Routes.SETTINGS),
    DrawerItem(
        R.string.nav_admin_notifications,
        Icons.Default.Campaign,
        Routes.ADMIN_NOTIFICATIONS,
        requiresAdmin = true,
    ),
)

val drawerItems: List<DrawerItem> =
    drawerPinnedTop + drawerSections.flatMap { it.items } + drawerPinnedBottom

// ─────────────────────────────────────────────────────────────────────────────
// COLOR TOKENS (CHARCOAL MATTE BLACK + UNIFIED DEEP PURPLE ACCENT)
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// COLOR TOKENS & M3 GOOGLE NOTES SIDEBAR THEME
// ─────────────────────────────────────────────────────────────────────────────

private object DarkFlat {
    val bg            = Color(0xFF1B1B1F) // M3 Dark Surface
    val cardBg        = Color(0xFF2B2B30)
    val textPrimary   = Color(0xFFE3E2E6)
    val textSecondary = Color(0xFFC7C5D0)
    val iconIndigo    = Color(0xFFC084FC)
    val selBg         = Color(0xFF381E72) // M3 Deep Purple Container
    val selText       = Color(0xFFE8DEF8)
    val selIcon       = Color(0xFFD0BCFF)
    val border        = Color(0xFF44474E)
    val chipBg        = Color(0xFF0F5223)
    val chipBorder    = Color(0xFF299947).copy(alpha = 0.5f)
    val chipText      = Color(0xFF6CFF95)
    val chipIcon      = Color(0xFF6CFF95)
}

private object LightFlat {
    val bg            = Color(0xFFFEF7FF) // M3 Light Surface
    val cardBg        = Color(0xFFF3EDF7)
    val textPrimary   = Color(0xFF1D1B20)
    val textSecondary = Color(0xFF49454F)
    val iconIndigo    = Color(0xFF581C87)
    val selBg         = Color(0xFFE8DEF8) // M3 Light Purple Container
    val selText       = Color(0xFF1D192B)
    val selIcon       = Color(0xFF581C87)
    val border        = Color(0xFFE7E0EC)
    val chipBg        = Color(0xFFE8F5E9)
    val chipBorder    = Color(0xFFA5D6A7)
    val chipText      = Color(0xFF1B5E20)
    val chipIcon      = Color(0xFF1B5E20)
}

// ─────────────────────────────────────────────────────────────────────────────
// SIDEBAR NAVIGATION DRAWER (GOOGLE NOTES M3 DESIGN SPECIFICATION)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SafarDrawer(
    currentRoute: String,
    isDarkTheme: Boolean,
    isAdmin: Boolean,
    isPremiumActive: Boolean,
    userName: String? = null,
    userEmail: String? = null,
    userAvatar: String? = null,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val isLight = !isDarkTheme
    val dk = DarkFlat
    val lt = LightFlat
    val currentBase = currentRoute.substringBefore("?")

    // Google Notes Drawer signature right-rounded 28.dp shape
    val drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp, topStart = 0.dp, bottomStart = 0.dp)

    val expandedSections = rememberSaveable(
        saver = listSaver(
            save = { map -> map.map { "${it.key.name}=${it.value}" } },
            restore = { saved ->
                val parsed = saved.mapNotNull { entry ->
                    val parts = entry.split('=', limit = 2)
                    if (parts.size != 2) return@mapNotNull null
                    val id = runCatching { DrawerSectionId.valueOf(parts[0]) }.getOrNull()
                        ?: return@mapNotNull null
                    id to (parts[1].toBooleanStrictOrNull() ?: false)
                }.toMap()
                mutableStateMapOf<DrawerSectionId, Boolean>().apply {
                    DrawerSectionId.entries.forEach { id ->
                        put(
                            id,
                            parsed[id]
                                ?: (drawerSections.firstOrNull { it.id == id }?.defaultExpanded == true),
                        )
                    }
                }
            },
        ),
    ) {
        mutableStateMapOf<DrawerSectionId, Boolean>().apply {
            drawerSections.forEach { put(it.id, it.defaultExpanded) }
        }
    }

    var entranceVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entranceVisible = true
    }

    val containerBgColor = if (isLight) lt.bg else dk.bg
    val containerBorderColor = if (isLight) lt.border else dk.border

    Surface(
        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.75f),
        color = containerBgColor,
        contentColor = if (isLight) lt.textPrimary else dk.textPrimary,
        tonalElevation = 1.dp,
        shape = drawerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {

            // 1. USER PROFILE HEADER
            StaggeredEntranceBox(index = 0, isVisible = entranceVisible) {
                DrawerUserProfileHeader(
                    userName        = userName,
                    userEmail       = userEmail,
                        userAvatar      = userAvatar,
                        isLight         = isLight,
                        isPremiumActive = isPremiumActive,
                        dk              = dk,
                        lt              = lt,
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color     = containerBorderColor,
                )

                // 2. NAV LIST IN EDGE-TO-EDGE STYLE WITH HAIRLINE DIVIDERS
                LazyColumn(
                    modifier       = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Pinned Top Items (Home, Dashboard)
                    item(key = "pinned-top") {
                        StaggeredEntranceBox(index = 1, isVisible = entranceVisible) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                drawerPinnedTop.forEach { item ->
                                    DrawerNavRow(
                                        item            = item,
                                        currentRoute    = currentRoute,
                                        isPremiumActive = isPremiumActive,
                                        isLight         = isLight,
                                        dk              = dk,
                                        lt              = lt,
                                        onNavigate      = onNavigate,
                                        onCloseDrawer   = onCloseDrawer,
                                    )
                                }
                            }
                        }
                    }

                    // Section Lists (Study & Productivity, Well-being & Community)
                    drawerSections.forEachIndexed { sIdx, section ->
                        val expanded = expandedSections[section.id] == true
                        val sectionHasSelection = section.items.any { item ->
                            isDrawerItemSelected(item = item, currentBase = currentBase)
                        }

                        item(key = "divider-section-${section.id}") {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color     = containerBorderColor.copy(alpha = 0.5f),
                                modifier  = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            )
                        }

                        item(key = "section-${section.id}") {
                            StaggeredEntranceBox(index = 2 + sIdx, isVisible = entranceVisible) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    DrawerSectionHeader(
                                        label = stringResource(section.labelRes),
                                        icon = section.icon,
                                        expanded = expanded,
                                        hasSelectedChild = sectionHasSelection && !expanded,
                                        isLight = isLight,
                                        dk = dk,
                                        lt = lt,
                                        onToggle = {
                                            expandedSections[section.id] = !expanded
                                        },
                                    )

                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeIn(animationSpec = tween(200)),
                                        exit = shrinkVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + fadeOut(animationSpec = tween(180)),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            section.items.forEach { item ->
                                                DrawerNavRow(
                                                    item            = item,
                                                    currentRoute    = currentRoute,
                                                    isPremiumActive = isPremiumActive,
                                                    isLight         = isLight,
                                                    dk              = dk,
                                                    lt              = lt,
                                                    onNavigate      = onNavigate,
                                                    onCloseDrawer   = onCloseDrawer,
                                                    indented        = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pinned Bottom Items (Profile, Settings, Admin)
                    item(key = "divider-bottom") {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color     = containerBorderColor.copy(alpha = 0.5f),
                            modifier  = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                        )
                    }

                    item(key = "pinned-bottom") {
                        StaggeredEntranceBox(index = 4, isVisible = entranceVisible) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                drawerPinnedBottom.filter { !it.requiresAdmin || isAdmin }.forEach { item ->
                                    DrawerNavRow(
                                        item            = item,
                                        currentRoute    = currentRoute,
                                        isPremiumActive = isPremiumActive,
                                        isLight         = isLight,
                                        dk              = dk,
                                        lt              = lt,
                                        onNavigate      = onNavigate,
                                        onCloseDrawer   = onCloseDrawer,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color     = containerBorderColor,
                )

                // 3. DARK MODE SWITCH AT FOOTER
                StaggeredEntranceBox(index = 5, isVisible = entranceVisible) {
                    DrawerDarkModeCard(
                        isLight           = isLight,
                        isDarkTheme       = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        dk                = dk,
                        lt                = lt,
                    )
                }
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
// STAGGERED ENTRANCE CONTAINER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredEntranceBox(
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit,
) {
    val slideOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else (18 + index * 12).dp,
        animationSpec = tween(
            durationMillis = 320,
            delayMillis = index * 40,
            easing = FastOutSlowInEasing,
        ),
        label = "staggeredOffset",
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            delayMillis = index * 40,
        ),
        label = "staggeredAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = slideOffset.toPx()
                alpha = alphaAnim
            }
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GROUPED SURFACE CARD CONTAINER (16.DP ROUNDED CORNERS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GroupedSurfaceCard(
    isLight: Boolean,
    dk: DarkFlat,
    lt: LightFlat,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardBgColor = if (isLight) lt.cardBg else dk.cardBg
    val cardBorderColor = if (isLight) lt.border else dk.border

    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isLight) 2.dp else 0.dp, cardShape)
            .clip(cardShape)
            .background(cardBgColor)
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = cardShape,
            )
            .padding(6.dp)
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// USER PROFILE HEADER BLOCK (GOOGLE NOTES M3 STYLE)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerUserProfileHeader(
    userName: String?,
    userEmail: String?,
    userAvatar: String?,
    isLight: Boolean,
    isPremiumActive: Boolean,
    dk: DarkFlat,
    lt: LightFlat,
) {
    val fallbackName = stringResource(R.string.drawer_default_user_name)
    val displayName = remember(userName, fallbackName) {
        userName?.trim()?.ifBlank { null } ?: fallbackName
    }
    val displayEmail = remember(userEmail) {
        userEmail?.trim()?.ifBlank { null } ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isLight) lt.selBg else dk.selBg)
                    .border(
                        width = 1.5.dp,
                        color = if (isLight) lt.border else dk.border,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!userAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = stringResource(R.string.drawer_user_profile),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = if (isLight) lt.selText else dk.selText,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = if (isLight) lt.textPrimary else dk.textPrimary,
                )
                if (isPremiumActive) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLight) lt.chipBg else dk.chipBg,
                        border = BorderStroke(1.dp, if (isLight) lt.chipBorder else dk.chipBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isLight) lt.chipIcon else dk.chipIcon,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = stringResource(R.string.drawer_premium_active),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = if (isLight) lt.chipText else dk.chipText,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACCORDION FEATURE CATEGORY HEADER (GOOGLE NOTES M3 STYLE)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerSectionHeader(
    label: String,
    icon: ImageVector,
    expanded: Boolean,
    hasSelectedChild: Boolean,
    isLight: Boolean,
    dk: DarkFlat,
    lt: LightFlat,
    onToggle: () -> Unit,
) {
    val textColor = if (isLight) lt.textSecondary else dk.textSecondary
    val iconColor = if (isLight) lt.textSecondary else dk.textSecondary

    val rotationDegrees by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "arrowSpringRotation",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "headerScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label.uppercase(Locale.US),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                ),
                color = textColor,
            )
            if (hasSelectedChild) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isLight) lt.selBg else dk.selBg),
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(if (expanded) R.string.drawer_collapse else R.string.drawer_expand),
            tint = textColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotationDegrees },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUBFEATURE NAVIGATION ROW (GOOGLE NOTES M3 STYLE 24.DP PILL)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerNavRow(
    item: DrawerItem,
    currentRoute: String,
    isPremiumActive: Boolean,
    isLight: Boolean,
    dk: DarkFlat,
    lt: LightFlat,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    indented: Boolean = false,
) {
    val label       = stringResource(item.labelRes)
    val currentBase = currentRoute.substringBefore("?")
    val selected    = isDrawerItemSelected(item = item, currentBase = currentBase)
    val showLock    = item.requiresPremium && !isPremiumActive

    val rowBg = when {
        selected && isLight -> lt.selBg
        selected -> dk.selBg
        else -> Color.Transparent
    }
    val textColor = when {
        selected && isLight -> lt.selText
        selected -> dk.selText
        isLight -> lt.textPrimary
        else -> dk.textPrimary
    }
    val iconColor = when {
        selected && isLight -> lt.selIcon
        selected -> dk.selIcon
        item.route == Routes.PREMIUM -> Color(0xFFE08A3C)
        isLight -> lt.textSecondary
        else -> dk.textSecondary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "itemScale",
    )

    // Google Notes M3 selection container pill shape
    val capsuleShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(capsuleShape)
            .background(rowBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                onNavigate(item.route)
                onCloseDrawer()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = textColor,
            )

            if (item.route == Routes.PREMIUM) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPremiumActive) {
                        if (isLight) Color(0xFFEBFBF3) else Color(0xFF064E3B)
                    } else {
                        if (isLight) Color(0xFFFFF7ED) else Color(0xFF431407)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isPremiumActive) {
                            if (isLight) Color(0xFFD1F4E0) else Color(0xFF059669).copy(alpha = 0.5f)
                        } else {
                            if (isLight) Color(0xFFFFEDD5) else Color(0xFF9A3412).copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Text(
                        text = if (isPremiumActive) "PRO" else stringResource(R.string.drawer_upgrade),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremiumActive) {
                            if (isLight) Color(0xFF059669) else Color(0xFF34D399)
                        } else {
                            Color(0xFFC85A32)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 0.5.sp,
                    )
                }
            } else if (item.requiresPremium) {
                ShimmerProBadge(isPremiumActive = isPremiumActive, isLight = isLight)
            } else if (item.route == Routes.ADMIN_NOTIFICATIONS) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isLight) lt.textSecondary else dk.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            } else if (showLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.drawer_premium_locked),
                    tint = if (isLight) lt.textSecondary else dk.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun isDrawerItemSelected(item: DrawerItem, currentBase: String): Boolean {
    val itemBase = item.route.substringBefore("?")
    return when {
        itemBase == Routes.COURSES ->
            currentBase == Routes.COURSES || currentBase.startsWith("live/")
        else -> currentBase == itemBase || currentBase.startsWith("$itemBase/")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRO BADGE COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShimmerProBadge(
    isPremiumActive: Boolean,
    isLight: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isPremiumActive) {
            if (isLight) Color(0xFFEBFBF3) else Color(0xFF064E3B)
        } else {
            if (isLight) Color(0xFFFEF3C7) else Color(0xFF78350F)
        },
    ) {
        Text(
            text = "PRO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPremiumActive) {
                if (isLight) Color(0xFF059669) else Color(0xFF34D399)
            } else {
                if (isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DARK MODE SWITCH FOOTER CARD (GOOGLE NOTES M3 STYLE)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerDarkModeCard(
    isLight: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    dk: DarkFlat,
    lt: LightFlat,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onToggleDarkTheme() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(if (isDarkTheme) R.string.drawer_dark_mode else R.string.drawer_light_mode),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
            color = if (isLight) lt.textPrimary else dk.textPrimary,
        )

        JetCoSwitchButton(
            checked = isDarkTheme,
            onCheckedChange = { onToggleDarkTheme() },
        )
    }
}

/**
 * JetCo-inspired animated switch button with sliding knob, 360° icon rotation,
 * and day/night crossfade matching developerchunk/JetCo switch_button animation.
 */
@Composable
fun JetCoSwitchButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonWidth: androidx.compose.ui.unit.Dp = 56.dp,
    buttonHeight: androidx.compose.ui.unit.Dp = 32.dp,
    switchPadding: androidx.compose.ui.unit.Dp = 3.dp,
    selectedTrackColor: Color = Color(0xFF1E90FF),
    unselectedTrackColor: Color = Color(0xFF636B7B),
    knobColor: Color = Color.White,
    iconColor: Color = Color(0xFF16212B),
    animationDuration: Int = 600,
) {
    val knobSize = buttonHeight - (switchPadding * 2)
    val maxOffset = buttonWidth - knobSize - (switchPadding * 2)

    val animatedOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "jetCoSwitchSlide",
    )

    val animatedBgColor by animateColorAsState(
        targetValue = if (checked) selectedTrackColor else unselectedTrackColor,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "jetCoSwitchBgColor",
    )

    val rotation by animateFloatAsState(
        targetValue = if (checked) 360f else 0f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "jetCoSwitchRotation",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = buttonWidth, height = buttonHeight)
            .clip(CircleShape)
            .background(animatedBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(switchPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .size(knobSize)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(knobColor),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = checked,
                animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
                label = "jetCoSwitchCrossfade",
            ) { isDark ->
                Icon(
                    painter = painterResource(
                        id = if (isDark) {
                            R.drawable.ic_switch_partly_cloudy_night
                        } else {
                            R.drawable.ic_switch_partly_cloudy_day
                        }
                    ),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        },
                )
            }
        }
    }
}
