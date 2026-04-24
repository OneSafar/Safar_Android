package com.safar.app.ui.drawer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    DrawerItem(R.string.module_nishtha, Icons.Default.SelfImprovement, Routes.NISHTHA),
    DrawerItem(R.string.module_ekagra,  Icons.Default.Timer,           Routes.EKAGRA),
    DrawerItem(R.string.module_mehfil,  Icons.Default.Groups,          Routes.MEHFIL),
    DrawerItem(R.string.module_dhyan,   Icons.Default.Spa,             Routes.DHYAN),
    DrawerItem(R.string.nav_profile,    Icons.Default.Person,          Routes.PROFILE),
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
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text     = stringResource(R.string.app_name),
            style    = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 20.dp),
            color    = MaterialTheme.colorScheme.primary,
        )
        Text(
            text     = stringResource(R.string.drawer_tagline),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        drawerItems.forEach { item ->
            val label    = stringResource(item.labelRes)
            val selected = currentRoute.startsWith(item.route)
            NavigationDrawerItem(
                label    = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                icon     = { Icon(item.icon, contentDescription = label) },
                selected = selected,
                onClick  = { onNavigate(item.route); onCloseDrawer() },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Dark/Light theme toggle — properly wired
        NavigationDrawerItem(
            label    = {
                Text(if (isDarkTheme) "Light Mode" else "Dark Mode")
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )

        // Language toggle — switches to Hindi
//        NavigationDrawerItem(
//            label    = { Text(stringResource(R.string.nav_language)) },
//            icon     = { Icon(Icons.Default.Language, contentDescription = null) },
//            selected = false,
//            onClick  = { onLanguageClick(); onCloseDrawer() },
//            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
//        )

        Spacer(Modifier.height(24.dp))
    }
}
