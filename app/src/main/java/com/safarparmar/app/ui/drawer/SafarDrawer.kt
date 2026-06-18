package com.safarparmar.app.ui.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.RainbowShimmerText

data class DrawerItem(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
    val rainbowShimmer: Boolean = false,
    val requiresAdmin: Boolean = false,
)

val drawerItems = listOf(
    DrawerItem(R.string.nav_home, Icons.Default.Home, Routes.HOME),
    DrawerItem(R.string.nav_dashboard, Icons.Default.Dashboard, Routes.DASHBOARD),
    DrawerItem(
        R.string.nav_study_planner,
        Icons.AutoMirrored.Filled.EventNote,
        Routes.STUDY_PLANNER,
        rainbowShimmer = true,
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
    DrawerItem(R.string.nav_live_classes, Icons.Default.LiveTv, Routes.LIVE_SESSIONS_ROOT),
    DrawerItem(R.string.nav_admin_notifications, Icons.Default.Campaign, Routes.ADMIN_NOTIFICATIONS, requiresAdmin = true),
    DrawerItem(R.string.nav_profile, Icons.Default.Person, Routes.PROFILE),
    DrawerItem(R.string.profile_section_settings, Icons.Default.Settings, Routes.SETTINGS),
)

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
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 360.dp)
            .statusBarsPadding(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "SAFAR",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.drawer_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (isPremiumActive) {
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(
                                color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(999.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Premium Active",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDarkTheme) Color(0xFFD1FAE5) else Color(0xFF065F46),
                        )
                    }
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(drawerItems.filter { !it.requiresAdmin || isAdmin }) { item ->
                    DrawerNavRow(
                        item = item,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        onCloseDrawer = onCloseDrawer,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onToggleDarkTheme() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.Nightlight else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkTheme) "Dark theme enabled" else "Light theme enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerNavRow(
    item: DrawerItem,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val label = stringResource(item.labelRes)
    val selected = currentRoute.startsWith(item.route)
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    NavigationDrawerItem(
        label = {
            if (item.rainbowShimmer) {
                RainbowShimmerText(
                    text = label,
                    fontWeight = fontWeight,
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                Text(
                    text = label,
                    fontWeight = fontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        icon = { Icon(item.icon, contentDescription = label) },
        selected = selected,
        onClick = { onNavigate(item.route); onCloseDrawer() },
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .heightIn(min = 48.dp),
    )
}
