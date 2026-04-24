package com.safar.app.ui.drawer

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.R
import com.safar.app.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafarDrawerScaffold(
    title: String,
    subtitle: String? = null,
    currentRoute: String,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Obtain the Activity so we can scope ThemeViewModel to it.
    // This guarantees we're always toggling the SAME instance that
    // drives SafarTheme in MainActivity — no matter how deeply nested.
    val context = LocalContext.current
    val activity = context as? Activity
    val themeVm: ThemeViewModel = if (activity != null) {
        hiltViewModel(activity as androidx.activity.ComponentActivity)
    } else {
        hiltViewModel()
    }
    val liveDark by themeVm.isDarkTheme.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SafarDrawer(
                currentRoute      = currentRoute,
                isDarkTheme       = liveDark,
                onNavigate        = onNavigate,
                onToggleDarkTheme = { themeVm.toggleDarkTheme() },
                onLanguageClick   = onLanguageClick,
                onCloseDrawer     = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor        = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.nav_open_menu))
                        }
                    },
                    title = {
                        Column {
                            if (subtitle != null) {
                                Text(
                                    subtitle.uppercase(),
                                    fontSize = 11.sp,
                                    lineHeight = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(title, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    actions = { topBarActions() },
                )
            },
            content = content,
        )
    }
}
