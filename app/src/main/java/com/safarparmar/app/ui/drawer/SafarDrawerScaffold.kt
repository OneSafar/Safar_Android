package com.safarparmar.app.ui.drawer

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.safarparmar.app.ui.drawer.SafarDrawer
import com.composables.ui.components.ButtonSize
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon as ComposablesIcon
import com.composables.ui.components.IconButton as ComposablesIconButton

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
    topBarGradient: Brush? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    secondaryNavigationIcon: ImageVector? = null,
    secondaryNavigationContentDescription: String? = null,
    onSecondaryNavigationClick: (() -> Unit)? = null,
    emphasizeTopBar: Boolean = false,
    containerColor: Color? = null,
    showTopBar: Boolean = true,
    showTopBarTitle: Boolean = true,
    useGlassTopBar: Boolean = false,
    useDetachedMenuGlass: Boolean = false,
    /**
     * Hands the caller a function that opens this scaffold's drawer. Screens
     * that draw their own top bar (Ekagra) still need the hamburger to work,
     * and the drawer state lives in here — so the opener is hoisted out rather
     * than duplicating the state outside.
     */
    onDrawerControllerReady: (() -> Unit) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val appName = stringResource(R.string.app_name)

    // Obtain the Activity so we can scope ThemeViewModel to it.
    // This guarantees we're always toggling the SAME instance that
    // drives SafarTheme in MainActivity — no matter how deeply nested.
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val themeVm: ThemeViewModel = if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
    val liveDark by themeVm.isDarkTheme.collectAsStateWithLifecycle()
    val isAdmin by themeVm.dataStore.isAdmin.collectAsStateWithLifecycle(initialValue = false)
    val userName by themeVm.dataStore.userName.collectAsStateWithLifecycle(initialValue = null)
    val userEmail by themeVm.dataStore.userEmail.collectAsStateWithLifecycle(initialValue = null)
    val userAvatar by themeVm.dataStore.userAvatar.collectAsStateWithLifecycle(initialValue = null)
    val premiumVm: PremiumViewModel = if (activity != null) {
        hiltViewModel(activity)
    } else {
        hiltViewModel()
    }
    val premiumStatus by premiumVm.premiumStatus.collectAsStateWithLifecycle()
    val actualContentColor = if (useGlassTopBar) {
        if (liveDark) Color(0xFFF2F2F5) else Color(0xFF16161A)
    } else {
        topBarContentColor ?: if (liveDark) Color.White else MaterialTheme.colorScheme.onSurface
    }

    // Remembered so the identity is stable — otherwise a fresh lambda every
    // recomposition would re-fire the callback below on every frame.
    val openDrawer: () -> Unit = remember(scope, drawerState) {
        { scope.launch { drawerState.open() } }
    }

    LaunchedEffect(openDrawer) { onDrawerControllerReady(openDrawer) }

    @Composable
    fun GlassSurfaceModifier(shape: RoundedCornerShape, height: androidx.compose.ui.unit.Dp = 52.dp): Modifier {
        return Modifier
            .height(height)
            .shadow(
                elevation = if (liveDark) 6.dp else 14.dp,
                shape = shape,
                ambientColor = if (liveDark) Color(0x12000000) else Color(0xFF7A8498).copy(alpha = 0.32f),
                spotColor = if (liveDark) Color(0x0E000000) else Color(0xFF7A8498).copy(alpha = 0.24f),
            )
            .clip(shape)
            .background(if (liveDark) Color(0xFF1E1E22).copy(alpha = 0.78f) else Color(0xFFD6DAE2).copy(alpha = 0.50f))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (liveDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = 18f,
                    ),
                )
            }
            .border(
                width = 0.9.dp,
                brush = if (liveDark) {
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f)),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 50f),
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.90f),
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.55f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 50f),
                    )
                },
                shape = shape,
            )
    }

    val haptic = LocalHapticFeedback.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SafarDrawer(
                currentRoute      = currentRoute,
                isDarkTheme       = liveDark,
                isAdmin           = isAdmin,
                isPremiumActive   = premiumStatus.hasAnyPaidAccess,
                userName          = userName,
                userEmail         = userEmail,
                userAvatar        = userAvatar,
                onNavigate        = onNavigate,
                onToggleDarkTheme = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    themeVm.toggleDarkTheme()
                },
                onCloseDrawer     = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            containerColor = containerColor ?: MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTopBar,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .then(if (topBarGradient != null) Modifier.background(topBarGradient) else Modifier)
                            .background(if (topBarGradient == null) containerColor ?: MaterialTheme.colorScheme.background else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        com.composables.ui.components.Toolbar(
                            title = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            backgroundColor = Color.Transparent,
                            contentColor = actualContentColor,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            leading = {
                                ComposablesIconButton(
                                    onClick = onNavigationClick ?: openDrawer,
                                    style = ButtonStyle.Ghost,
                                    buttonSize = ButtonSize.Small,
                                    contentColor = actualContentColor,
                                ) {
                                    ComposablesIcon(
                                        navigationIcon ?: Icons.Default.Menu,
                                        contentDescription = navigationContentDescription ?: stringResource(R.string.nav_open_menu),
                                        modifier = Modifier.size(22.dp),
                                        tint = actualContentColor,
                                    )
                                }
                                if (secondaryNavigationIcon != null && onSecondaryNavigationClick != null) {
                                    ComposablesIconButton(
                                        onClick = onSecondaryNavigationClick,
                                        style = ButtonStyle.Ghost,
                                        buttonSize = ButtonSize.Small,
                                        contentColor = actualContentColor,
                                    ) {
                                        ComposablesIcon(
                                            secondaryNavigationIcon,
                                            contentDescription = secondaryNavigationContentDescription,
                                            modifier = Modifier.size(22.dp),
                                            tint = actualContentColor,
                                        )
                                    }
                                }
                            },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp),
                                ) {
                                    topBarActions()
                                }
                            },
                        )
                        if (showTopBarTitle) {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(horizontal = if (secondaryNavigationIcon != null) 112.dp else 56.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val shouldShowSubtitle = subtitle != null &&
                                    !subtitle.contains("SAFAR", ignoreCase = true) &&
                                    !subtitle.contains(appName, ignoreCase = true) &&
                                    subtitle.isNotBlank()
                                if (shouldShowSubtitle) {
                                    com.composables.ui.components.Text(
                                        subtitle!!.uppercase(),
                                        color = actualContentColor.copy(alpha = 0.72f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                com.composables.ui.components.Text(
                                    title,
                                    color = actualContentColor,
                                    fontSize = if (emphasizeTopBar) 21.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (title.uppercase() == "SAFAR" || title.equals("Mehfil", ignoreCase = true)) LoraFontFamily else null,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            },
            content = content,
        )
    }
}
