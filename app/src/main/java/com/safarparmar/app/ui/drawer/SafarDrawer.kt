package com.safarparmar.app.ui.drawer

import androidx.compose.animation.AnimatedVisibility
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
)

val drawerSections = listOf(
    DrawerSection(
        id = DrawerSectionId.STUDY_PRODUCTIVITY,
        labelRes = R.string.drawer_section_study_productivity,
        icon = Icons.Default.School,
        defaultExpanded = true,
        items = listOf(
            DrawerItem(
                R.string.nav_study_planner,
                Icons.AutoMirrored.Filled.EventNote,
                Routes.STUDY_PLANNER,
                requiresPremium = true,
            ),
            DrawerItem(R.string.module_ekagra, Icons.Default.Timer, Routes.EKAGRA),
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
            DrawerItem(R.string.module_courses, Icons.AutoMirrored.Filled.MenuBook, Routes.COURSES),
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

private object DarkFlat {
    val bg            = Color(0xFF141416)
    val cardBg        = Color(0xFF1E1F24)
    val textPrimary   = Color(0xFFF3F4F6)
    val textSecondary = Color(0xFF9CA3AF)
    val iconIndigo    = Color(0xFF818CF8)
    val selBg         = Color(0xFF2E1065)
    val selText       = Color(0xFFC084FC)
    val selIcon       = Color(0xFFC084FC)
    val border        = Color(0xFF27272A)
    val chipBg        = Color(0xFF064E3B)
    val chipBorder    = Color(0xFF059669).copy(alpha = 0.5f)
    val chipText      = Color(0xFF34D399)
    val chipIcon      = Color(0xFF34D399)
}

private object LightFlat {
    val bg            = Color(0xFFFFFFFF)
    val cardBg        = Color(0xFFFFFFFF)
    val textPrimary   = Color(0xFF111827)
    val textSecondary = Color(0xFF6B7280)
    val iconIndigo    = Color(0xFF3730A3)
    val selBg         = Color(0xFFF3E8FF)
    val selText       = Color(0xFF6D28D9)
    val selIcon       = Color(0xFF6D28D9)
    val border        = Color(0xFFF3F4F6)
    val chipBg        = Color(0xFFEBFBF3)
    val chipBorder    = Color(0xFFD1F4E0)
    val chipText      = Color(0xFF059669)
    val chipIcon      = Color(0xFF059669)
}

// ─────────────────────────────────────────────────────────────────────────────
// SIDEBAR NAVIGATION DRAWER WITH 24.DP RIGHT CORNER SHAPE & GROUPED CARDS
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

    // Rounded 24.dp corners on top-right and bottom-right as per design
    val drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, topStart = 0.dp, bottomStart = 0.dp)

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

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 420.dp)
            .fillMaxWidth(0.88f),
        drawerContainerColor = containerBgColor,
        drawerContentColor   = if (isLight) lt.textPrimary else dk.textPrimary,
        drawerTonalElevation = 0.dp,
        drawerShape          = drawerShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
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
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Pinned Top Items (Home, Dashboard)
                    item(key = "pinned-top") {
                        StaggeredEntranceBox(index = 1, isVisible = entranceVisible) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                Column {
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
                                            modifier = Modifier.padding(top = 2.dp),
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
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
// USER PROFILE HEADER BLOCK
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
    val displayName = remember(userName) {
        userName?.trim()?.ifBlank { null } ?: "Aspirant"
    }
    val displayEmail = remember(userEmail) {
        userEmail?.trim()?.ifBlank { null } ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isLight) Brush.verticalGradient(
                    colors = listOf(Color(0xFFF5F3FF), Color.White)
                ) else Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1F24), dk.bg)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isLight) Color(0xFF6366F1) else Color(0xFF4F46E5))
                    .border(
                        width = 3.dp,
                        color = if (isLight) Color.White else Color(0xFF1E1F24),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!userAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = "User Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                        color = Color.White,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = if (isLight) lt.textPrimary else dk.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (displayEmail.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = displayEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isLight) lt.textSecondary else dk.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isPremiumActive) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isLight) lt.chipBg else dk.chipBg,
                        border = BorderStroke(1.dp, if (isLight) lt.chipBorder else dk.chipBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isLight) lt.chipIcon else dk.chipIcon,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Premium Active",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
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
// ACCORDION FEATURE CATEGORY HEADER
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
    val textColor = if (isLight) Color(0xFF4B5563) else Color(0xFF9CA3AF)
    val iconColor = if (isLight) lt.iconIndigo else dk.iconIndigo

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
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "headerScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasSelectedChild) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isLight) lt.selText else dk.selText),
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = textColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotationDegrees },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUBFEATURE NAVIGATION ROW
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
        isLight -> lt.iconIndigo
        else -> dk.iconIndigo
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "itemScale",
    )

    val capsuleShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (selected) {
                val barHeight by animateDpAsState(
                    targetValue = if (selected) 18.dp else 0.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "barHeight",
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(if (isLight) lt.selText else dk.selText)
                )
            }

            Icon(
                imageVector = item.icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp),
            )

            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.requiresPremium) {
                ShimmerProBadge(isPremiumActive = isPremiumActive, isLight = isLight)
            } else if (item.route == Routes.ADMIN_NOTIFICATIONS) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(18.dp),
                )
            } else if (showLock) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium locked",
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
// DARK MODE SWITCH FOOTER CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerDarkModeCard(
    isLight: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    dk: DarkFlat,
    lt: LightFlat,
) {
    val iconRotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconRotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onToggleDarkTheme() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.Nightlight else Icons.Default.Nightlight,
                contentDescription = null,
                tint = if (isDarkTheme) Color(0xFF818CF8) else Color(0xFF3730A3),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = iconRotation },
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                    color = if (isLight) lt.textPrimary else dk.textPrimary,
                )
                Text(
                    text = if (isDarkTheme) "Dark theme enabled" else "Light theme enabled",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = if (isLight) lt.textSecondary else dk.textSecondary,
                )
            }
        }

        Switch(
            checked = isDarkTheme,
            onCheckedChange = { onToggleDarkTheme() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (isLight) Color(0xFF6D28D9) else Color(0xFF38BDF8),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF475569),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent,
            ),
        )
    }
}
