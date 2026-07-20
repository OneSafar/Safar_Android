package com.safarparmar.app.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.RainbowShimmerText

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

val drawerItems = listOf(
    DrawerItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    DrawerItem(R.string.nav_dashboard, Icons.Default.Dashboard, Routes.DASHBOARD),
    DrawerItem(
        R.string.nav_study_planner,
        Icons.AutoMirrored.Filled.EventNote,
        Routes.STUDY_PLANNER,
        rainbowShimmer = true,
        requiresPremium = true,
    ),
    DrawerItem(
        R.string.nav_focus_shield,
        Icons.Default.Shield,
        Routes.FOCUS_SHIELD,
        rainbowShimmer = true,
    ),
    DrawerItem(R.string.module_nishtha, Icons.Default.SelfImprovement, Routes.NISHTHA),
    DrawerItem(R.string.module_ekagra, Icons.Default.Timer, Routes.EKAGRA),
    DrawerItem(R.string.module_mehfil, Icons.Default.Groups, Routes.MEHFIL),
    DrawerItem(R.string.module_dhyan, Icons.Default.Spa, Routes.DHYAN),
    DrawerItem(R.string.module_courses, Icons.AutoMirrored.Filled.MenuBook, Routes.COURSES),
    DrawerItem(R.string.nav_admin_notifications, Icons.Default.Campaign, Routes.ADMIN_NOTIFICATIONS, requiresAdmin = true),
    DrawerItem(R.string.nav_profile, Icons.Default.Person, Routes.PROFILE),
    DrawerItem(R.string.profile_section_settings, Icons.Default.Settings, Routes.SETTINGS),
)

// ─────────────────────────────────────────────────────────────────────────────
// LIQUID-GLASS DRAWER THEME TOKENS
// Each mode is a self-contained object — no cross-mode bleed.
// ─────────────────────────────────────────────────────────────────────────────

private object DarkGlass {
    // Panel
    val base       = Color(0xFF0D0D0F)        // ~100% opaque charcoal base
    val overlayTop = Color(0xFF1E1E22)        // ~78% translucent top
    val overlayBot = Color(0xFF131316)        // ~70% translucent bottom
    val topAlpha   = 0.78f
    val botAlpha   = 0.70f
    // Rim border — soft white on left/top edges, fades out toward bottom-right
    val rimBrushColors = listOf(
        Color.White.copy(alpha = 0.16f),
        Color.White.copy(alpha = 0.06f),
        Color.White.copy(alpha = 0.10f),
    )
    // Top gloss catch-light
    val sheenColor = Color.White.copy(alpha = 0.04f)
    // Right-edge shadow depth
    val edgeShadow = Color.Black.copy(alpha = 0.42f)
    // Text / icon
    val textPrimary   = Color(0xFFF2F2F5)
    val textSecondary = Color(0xFF8A8A98)
    val iconTint      = Color(0xFF72728A)
    // Selected capsule — brighter charcoal glass
    val selBg     = Color(0xFF2A2A30)
    val selBorder = Color.White.copy(alpha = 0.14f)
    val selText   = Color(0xFFF0F0F4)
    val selIcon   = Color(0xFFCCCCD8)
    // Divider
    val divider   = Color.White.copy(alpha = 0.08f)
    // Premium chip
    val chipBg     = Color(0xFF12362A)
    val chipBorder = Color(0xFF255E42)
    val chipText   = Color(0xFFA0EBBF)
    val chipIcon   = Color(0xFF68D89A)
    // Dark mode card
    val cardBg     = Color(0xFF1B1B20)
    val cardBorder = Color.White.copy(alpha = 0.09f)
    // Switch track colors
    val trackOn  = Color(0xFF3E3E46)
    val trackOff = Color(0xFF28282E)
}

private object LightGlass {
    // Panel
    val base       = Color(0xFFF7F7F9)        // fully opaque clean white base
    val overlayTop = Color.White              // ~75% milky highlight
    val overlayBot = Color(0xFFF0F0F3)        // ~65% lower tone
    val topAlpha   = 0.75f
    val botAlpha   = 0.65f
    // Rim border — white inner + subtle gray outer
    val rimBrushColors = listOf(
        Color.White.copy(alpha = 0.80f),      // bright top-left inner rim
        Color.Black.copy(alpha = 0.08f),
        Color.Black.copy(alpha = 0.05f),
    )
    // Top gloss
    val sheenColor = Color.White.copy(alpha = 0.60f)
    // Right-edge depth
    val edgeShadow = Color.Black.copy(alpha = 0.10f)
    // Text / icon
    val textPrimary   = Color(0xFF16161A)
    val textSecondary = Color(0xFF62627A)
    val iconTint      = Color(0xFF4E6080)     // blue-gray
    // Selected capsule — raised white glass
    val selBg     = Color.White
    val selBorder = Color.Black.copy(alpha = 0.09f)
    val selText   = Color(0xFF0E0E14)
    val selIcon   = Color(0xFF364E78)
    // Divider
    val divider   = Color.Black.copy(alpha = 0.07f)
    // Premium chip
    val chipBg     = Color(0xFFD1FAE5)
    val chipBorder = Color(0xFF34D399).copy(alpha = 0.55f)
    val chipText   = Color(0xFF065F46)
    val chipIcon   = Color(0xFF047857)
    // Dark mode card
    val cardBg     = Color.Black.copy(alpha = 0.032f)
    val cardBorder = Color.Black.copy(alpha = 0.08f)
    // Switch track colors
    val trackOn  = Color(0xFF444454)
    val trackOff = Color(0xFFDDDDE8)
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SafarDrawer(
    currentRoute: String,
    isDarkTheme: Boolean,
    isAdmin: Boolean,
    isPremiumActive: Boolean,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val isLight = !isDarkTheme
    val dk = DarkGlass
    val lt = LightGlass

    val drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 310.dp)
            .statusBarsPadding(),
        drawerContainerColor = Color.Transparent,
        drawerContentColor   = Color.Transparent,
        drawerTonalElevation = 0.dp,
        drawerShape          = drawerShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(drawerShape)
                // ── Layer 1: fully opaque base — blocks all content behind ──
                .background(if (isLight) lt.base else dk.base)
                // ── Layer 2: frosted glass gradient overlay ──
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            (if (isLight) lt.overlayTop else dk.overlayTop)
                                .copy(alpha = if (isLight) lt.topAlpha else dk.topAlpha),
                            (if (isLight) lt.overlayBot else dk.overlayBot)
                                .copy(alpha = if (isLight) lt.botAlpha else dk.botAlpha),
                        )
                    )
                )
                // ── Layer 3: top gloss catch-light + right-edge depth ──
                .drawBehind {
                    // Horizontal gloss sheen near the top — the "glass highlight"
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isLight) lt.sheenColor else dk.sheenColor,
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY   = 72f,
                        )
                    )
                    // Right-side soft depth — simulated shadow without elevation artifacts
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isLight) lt.edgeShadow else dk.edgeShadow,
                            ),
                            startX = size.width - 14f,
                            endX   = size.width,
                        )
                    )
                }
                // ── Layer 4: angled rim border (white inner / gray outer) ──
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = if (isLight) lt.rimBrushColors else dk.rimBrushColors,
                        start  = Offset(0f, 0f),
                        end    = Offset(310f, 900f),
                    ),
                    shape = drawerShape,
                ),
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {

                // ── HEADER: SAFAR title + tagline + Premium chip ──
                DrawerHeader(
                    isLight        = isLight,
                    isPremiumActive = isPremiumActive,
                    dk = dk,
                    lt = lt,
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color     = if (isLight) lt.divider else dk.divider,
                )

                // ── NAV ITEMS LIST ──
                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(drawerItems.filter { !it.requiresAdmin || isAdmin }) { item ->
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

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color     = if (isLight) lt.divider else dk.divider,
                )

                // ── DARK MODE SWITCH CARD ──
                DrawerDarkModeCard(
                    isLight           = isLight,
                    isDarkTheme       = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    dk = dk,
                    lt = lt,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerHeader(
    isLight: Boolean,
    isPremiumActive: Boolean,
    dk: DarkGlass,
    lt: LightGlass,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 22.dp)
            .padding(top = 28.dp, bottom = 20.dp)
    ) {
        Text(
            text  = "SAFAR",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            // No purple tint — pure charcoal (light) / pure white (dark)
            color = if (isLight) lt.textPrimary else dk.textPrimary,
        )
        Text(
            text     = stringResource(R.string.drawer_tagline),
            style    = MaterialTheme.typography.bodySmall,
            color    = if (isLight) lt.textSecondary else dk.textSecondary,
            modifier = Modifier.padding(top = 3.dp),
        )
        if (isPremiumActive) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isLight) lt.chipBg else dk.chipBg)
                    .border(
                        width = 1.dp,
                        color = if (isLight) lt.chipBorder else dk.chipBorder,
                        shape = RoundedCornerShape(999.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector       = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint              = if (isLight) lt.chipIcon else dk.chipIcon,
                    modifier          = Modifier.size(15.dp),
                )
                Text(
                    text  = "Premium Active",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isLight) lt.chipText else dk.chipText,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NAV ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerNavRow(
    item: DrawerItem,
    currentRoute: String,
    isPremiumActive: Boolean,
    isLight: Boolean,
    dk: DarkGlass,
    lt: LightGlass,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val label       = stringResource(item.labelRes)
    val currentBase = currentRoute.substringBefore("?")
    val itemBase    = item.route.substringBefore("?")
    val selected    = currentBase == itemBase || currentBase.startsWith("$itemBase/")
    val fontWeight  = if (selected) FontWeight.Bold else FontWeight.Normal
    val showLock    = item.requiresPremium && !isPremiumActive

    // Selected state resolves to a slightly brighter glass capsule
    val rowBg     = when {
        selected && isLight  -> lt.selBg
        selected             -> dk.selBg
        else                 -> Color.Transparent
    }
    val textColor = when {
        selected && isLight  -> lt.selText
        selected             -> dk.selText
        isLight              -> lt.textSecondary
        else                 -> dk.textSecondary
    }
    val iconColor = when {
        selected && isLight  -> lt.selIcon
        selected             -> dk.selIcon
        isLight              -> lt.iconTint
        else                 -> dk.iconTint
    }

    val capsuleShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            // For selected, add a soft raised-glass shadow before clipping
            .then(
                if (selected) Modifier.shadow(
                    elevation     = if (isLight) 3.dp else 2.dp,
                    shape         = capsuleShape,
                    ambientColor  = if (isLight) Color.Black.copy(alpha = 0.05f)
                                    else         Color.Black.copy(alpha = 0.35f),
                    spotColor     = if (isLight) Color.Black.copy(alpha = 0.07f)
                                    else         Color.Black.copy(alpha = 0.45f),
                ) else Modifier
            )
            .clip(capsuleShape)
            .background(rowBg)
            .then(
                if (selected) Modifier.border(
                    width = 0.8.dp,
                    color = if (isLight) lt.selBorder else dk.selBorder,
                    shape = capsuleShape,
                ) else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onNavigate(item.route)
                onCloseDrawer()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier              = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = label,
                tint               = iconColor,
                modifier           = Modifier.size(20.dp),
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.weight(1f),
            ) {
                if (item.rainbowShimmer) {
                    RainbowShimmerText(
                        text       = label,
                        modifier   = Modifier.weight(1f, fill = false),
                        fontWeight = fontWeight,
                        style      = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    )
                } else {
                    Text(
                        text       = label,
                        modifier   = Modifier.weight(1f, fill = false),
                        fontWeight = fontWeight,
                        color      = textColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        style      = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (showLock) {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = "Premium locked",
                        tint               = if (isLight) lt.iconTint else dk.iconTint,
                        modifier           = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DARK MODE SWITCH CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerDarkModeCard(
    isLight: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    dk: DarkGlass,
    lt: LightGlass,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 18.dp, top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLight) lt.cardBg else dk.cardBg)
            .border(
                width = 0.8.dp,
                color = if (isLight) lt.cardBorder else dk.cardBorder,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onToggleDarkTheme() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector        = if (isDarkTheme) Icons.Default.Nightlight else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = if (isDarkTheme)
                        Color(0xFF8BAEFF)    // cool periwinkle moon
                    else
                        Color(0xFFF5A623),   // warm amber sun
                    modifier = Modifier.size(22.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text  = "Dark Mode",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = if (isLight) lt.textPrimary else dk.textPrimary,
                    )
                    Text(
                        text  = if (isDarkTheme) "Dark theme enabled" else "Light theme enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLight) lt.textSecondary else dk.textSecondary,
                    )
                }
            }

            Switch(
                checked         = isDarkTheme,
                onCheckedChange = { onToggleDarkTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor    = Color.White,
                    checkedTrackColor    = if (isLight) lt.trackOn else dk.trackOn,
                    uncheckedThumbColor  = Color(0xFFB0B0BC),
                    uncheckedTrackColor  = if (isLight) lt.trackOff else dk.trackOff,
                    uncheckedBorderColor = Color.Transparent,
                    checkedBorderColor   = Color.Transparent,
                ),
            )
        }
    }
}
