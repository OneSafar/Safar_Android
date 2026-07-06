package com.safarparmar.app.ui.drawer

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.ThemeViewModel
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
    topBarActions: @Composable RowScope.() -> Unit = {},
    topBarContentColor: Color? = null,
    emphasizeTopBar: Boolean = false,
    showTopBar: Boolean = true,
    showTopBarTitle: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val appName = stringResource(R.string.app_name)

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
    val liveDark by themeVm.isDarkTheme.collectAsStateWithLifecycle()
    val isAdmin by themeVm.dataStore.isAdmin.collectAsStateWithLifecycle(initialValue = false)
    val premiumVm: PremiumViewModel = if (activity != null) {
        hiltViewModel(activity as androidx.activity.ComponentActivity)
    } else {
        hiltViewModel()
    }
    val premiumStatus by premiumVm.premiumStatus.collectAsStateWithLifecycle()

    val actualContentColor = topBarContentColor ?: if (liveDark) Color.White else MaterialTheme.colorScheme.onSurface

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SafarDrawer(
                currentRoute      = currentRoute,
                isDarkTheme       = liveDark,
                isAdmin           = isAdmin,
                isPremiumActive   = premiumStatus.hasAnyPaidAccess,
                onNavigate        = onNavigate,
                onToggleDarkTheme = { themeVm.toggleDarkTheme() },
                onCloseDrawer     = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTopBar,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                            androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) +
                           androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(44.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showTopBarTitle) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val shouldShowSubtitle = subtitle != null &&
                                    !subtitle.contains("SAFAR", ignoreCase = true) &&
                                    !subtitle.contains("Safar", ignoreCase = true) &&
                                    !subtitle.contains(appName, ignoreCase = true) &&
                                    subtitle.isNotBlank()
                                if (shouldShowSubtitle) {
                                    Text(
                                        subtitle!!.uppercase(),
                                        fontSize = if (emphasizeTopBar) 12.sp else 11.sp,
                                        lineHeight = 12.sp,
                                        color = actualContentColor.copy(alpha = if (emphasizeTopBar) 0.82f else 0.7f),
                                        fontFamily = if (subtitle.uppercase() == "SAFAR") LoraFontFamily else null,
                                        fontWeight = if (subtitle.uppercase() == "SAFAR") FontWeight.Bold else null,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                                Text(
                                    title,
                                    fontSize = if (emphasizeTopBar) 20.sp else 18.sp,
                                    lineHeight = if (emphasizeTopBar) 22.sp else 20.sp,
                                    fontWeight = if (emphasizeTopBar) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontFamily = if (title.uppercase() == "SAFAR") LoraFontFamily else null,
                                    color = actualContentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }

                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 4.dp),
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.nav_open_menu),
                                modifier = Modifier.size(if (emphasizeTopBar) 26.dp else 24.dp),
                                tint = actualContentColor
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            topBarActions()
                        }
                    }
                }
            },
            content = content,
        )
    }
}
