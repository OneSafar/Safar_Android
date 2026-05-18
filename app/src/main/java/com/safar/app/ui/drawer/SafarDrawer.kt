package com.safar.app.ui.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.R
import com.safar.app.ui.navigation.Routes

data class DrawerItem(
    val labelRes: Int,
    val icon: ImageVector,
    val route: String,
)

val drawerItems = listOf(
    DrawerItem(R.string.nav_home,       Icons.Default.Home,            Routes.HOME),
    DrawerItem(R.string.nav_dashboard,  Icons.Default.Dashboard,       Routes.DASHBOARD),
    DrawerItem(R.string.nav_study_planner, Icons.AutoMirrored.Filled.EventNote, Routes.STUDY_PLANNER),
    DrawerItem(R.string.module_nishtha, Icons.Default.SelfImprovement, Routes.NISHTHA),
    DrawerItem(R.string.module_ekagra,  Icons.Default.Timer,           Routes.EKAGRA),
    DrawerItem(R.string.nav_focus_shield, Icons.Default.Shield,     Routes.FOCUS_SHIELD),
    DrawerItem(R.string.module_mehfil,  Icons.Default.Groups,          Routes.MEHFIL),
    DrawerItem(R.string.module_dhyan,   Icons.Default.Spa,             Routes.DHYAN),
    DrawerItem(R.string.nav_profile,    Icons.Default.Person,          Routes.PROFILE),
    DrawerItem(R.string.profile_section_settings, Icons.Default.Settings, Routes.SETTINGS),
)

@Composable
fun SafarDrawer(
    currentRoute: String,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onLanguageClick: () -> Unit,
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
            // Header Section
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text     = stringResource(R.string.app_name),
                    style    = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color    = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text     = stringResource(R.string.drawer_tagline),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            HorizontalDivider()

            // Scrollable Navigation List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(drawerItems) { item ->
                    DrawerNavRow(
                        item = item,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        onCloseDrawer = onCloseDrawer,
                    )
                }
            }

            HorizontalDivider()

            // Footer Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                NavigationDrawerItem(
                    label    = {
                        Text(
                            if (isDarkTheme) "Light Mode" else "Dark Mode",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon     = {
                        Icon(
                            if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            contentDescription = null
                        )
                    },
                    selected = false,
                    badge    = {
                        Switch(
                            checked         = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() },
                        )
                    },
                    onClick  = { onToggleDarkTheme() },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
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

    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        icon = { Icon(item.icon, contentDescription = label) },
        selected = selected,
        onClick = { onNavigate(item.route); onCloseDrawer() },
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .heightIn(min = 48.dp),
    )
}
