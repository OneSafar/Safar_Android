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

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.safarparmar.app.ui.drawer.SafarDrawer

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
    containerColor: Color? = null,
    showTopBar: Boolean = true,
    showTopBarTitle: Boolean = true,
    useGlassTopBar: Boolean = false,
    useDetachedMenuGlass: Boolean = false,
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
    val actualContentColor = if (useGlassTopBar) {
        if (liveDark) Color(0xFFF2F2F5) else Color(0xFF16161A)
    } else {
        topBarContentColor ?: if (liveDark) Color.White else MaterialTheme.colorScheme.onSurface
    }

    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

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
            containerColor = containerColor ?: MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTopBar,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                            androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) +
                            androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    if (useGlassTopBar && useDetachedMenuGlass) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = GlassSurfaceModifier(RoundedCornerShape(14.dp), height = 52.dp)
                                    .width(52.dp)
                                    .clickable(onClick = openDrawer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.nav_open_menu),
                                    modifier = Modifier.size(22.dp),
                                    tint = actualContentColor,
                                )
                            }
                            Box(
                                modifier = GlassSurfaceModifier(RoundedCornerShape(50.dp))
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (showTopBarTitle) {
                                    Text(
                                        title,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 88.dp),
                                        fontSize = 17.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = actualContentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    topBarActions()
                                }
                            }
                        }
                    } else if (useGlassTopBar) {
                        // ── Floating liquid-glass top bar capsule ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        if (liveDark) {
                                            Color(0xFF1E1E22).copy(alpha = 0.78f)
                                        } else {
                                            Color.White.copy(alpha = 0.72f)
                                        }
                                    )
                                    .drawBehind {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    if (liveDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.40f),
                                                    Color.Transparent
                                                ),
                                                startY = 0f,
                                                endY = 16f
                                            )
                                        )
                                    }
                                    .border(
                                        width = 0.8.dp,
                                        brush = if (liveDark) {
                                            Brush.linearGradient(
                                                colors = listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f)),
                                                start = Offset(0f, 0f),
                                                end = Offset(400f, 50f)
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(Color.White.copy(alpha = 0.85f), Color.Black.copy(alpha = 0.08f)),
                                                start = Offset(0f, 0f),
                                                end = Offset(400f, 50f)
                                            )
                                        },
                                        shape = RoundedCornerShape(50.dp)
                                    )
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
                                                fontSize = 10.sp,
                                                lineHeight = 11.sp,
                                                color = actualContentColor.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            )
                                        }
                                        Text(
                                            title,
                                            fontSize = 17.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (title.uppercase() == "SAFAR") LoraFontFamily else null,
                                            color = actualContentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = openDrawer,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 4.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.nav_open_menu),
                                        modifier = Modifier.size(24.dp),
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
                    } else {
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
                                onClick = openDrawer,
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
                }
            },
            content = content,
        )
    }
}
