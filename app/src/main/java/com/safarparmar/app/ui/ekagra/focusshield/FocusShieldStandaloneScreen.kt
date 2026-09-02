package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.feature.kavachanalytics.ui.primaryText
import com.safarparmar.app.feature.kavachanalytics.ui.secondaryText
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Content
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2ViewModel
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import kotlinx.coroutines.launch

private object KavachTabColors {
    val RoyalPurple = Color(0xFF6B21A8)
}

@Composable
fun FocusShieldStandaloneScreen(
    currentRoute: String = Routes.FOCUS_SHIELD,
    isDarkTheme: Boolean = false,
    initialTab: Int = 0,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    viewModel: FocusShieldViewModel = hiltViewModel(),
    youtubeViewModel: YoutubeStudyV2ViewModel = hiltViewModel(),
) {
    val shieldState by viewModel.shieldState.collectAsStateWithLifecycle()
    val youtubeState by youtubeViewModel.state.collectAsStateWithLifecycle()
    val accent = KavachDesign.Primary
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val isLight = !isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, 1)) { 2 }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                youtubeViewModel.refreshPermission()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    fun openAccessibilitySettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    SafarDrawerScaffold(
        title = stringResource(R.string.nav_focus_shield),
        subtitle = stringResource(R.string.app_name),
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        emphasizeTopBar = true,
        topBarActions = {
            IconButton(
                onClick = { onNavigate(Routes.KAVACH_ABOUT) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.kavach_info_content_description),
                    tint = scheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            // ── Clean Flat Tab Header ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.background),
            ) {
                // Tab 0: App Shield
                val appShieldActive = pagerState.currentPage == 0
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                        .padding(top = 12.dp, bottom = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (appShieldActive) primaryText(isLight) else secondaryText(isLight),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "App Shield",
                            fontSize = 14.sp,
                            fontWeight = if (appShieldActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (appShieldActive) primaryText(isLight) else secondaryText(isLight),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(
                                if (appShieldActive) KavachTabColors.RoyalPurple
                                else Color.Transparent,
                            ),
                    )
                }

                // Tab 1: YouTube Mode
                val ytActive = pagerState.currentPage == 1
                val ytRunning = youtubeState.enabled && youtubeState.setupCompleted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                        .padding(top = 12.dp, bottom = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = if (ytActive) primaryText(isLight) else secondaryText(isLight),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "YouTube Mode",
                            fontSize = 14.sp,
                            fontWeight = if (ytActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (ytActive) primaryText(isLight) else secondaryText(isLight),
                        )
                        if (ytRunning) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(KavachTabColors.RoyalPurple),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(
                                if (ytActive) KavachTabColors.RoyalPurple
                                else Color.Transparent,
                            ),
                    )
                }
            }
            HorizontalDivider(
                color = secondaryText(isLight).copy(alpha = 0.10f),
                thickness = 1.dp,
            )

            // ── Horizontal Pager Content ───────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> {
                        FocusShieldSettingsContent(
                            state = shieldState,
                            accent = accent,
                            onToggleEnabled = viewModel::setEnabled,
                            onToggleProfile = viewModel::setKavachProfile,
                            onToggleStrictMode = viewModel::setStrictMode,
                            onToggleSchedule = viewModel::setScheduleEnabled,
                            onSetScheduleRange = viewModel::setScheduleRange,
                            onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
                            onOpenAppCategories = { onNavigate(Routes.KAVACH_APP_CATEGORIES) },
                            onOpenAnalytics = { onNavigate(Routes.nishthaAnalytics("kavach")) },
                            onGoToEkagra = { onNavigate(Routes.EKAGRA) },
                            onOpenOverlaySettings = viewModel::openOverlaySettings,
                            onRefreshPermissions = viewModel::refreshPermissions,
                            onMaybeLater = onBack,
                            onSave = onBack,
                        )
                    }
                    1 -> {
                        YoutubeStudyV2Content(
                            state = youtubeState,
                            isLight = isLight,
                            onAgree = {
                                youtubeViewModel.acceptDisclosure()
                                if (youtubeState.accessibilityEnabled) youtubeViewModel.goToStep2()
                                else openAccessibilitySettings()
                            },
                            onNotNow = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onSetEnabled = youtubeViewModel::setEnabled,
                            onOpenAccessibility = ::openAccessibilitySettings,
                            onReferenceChanged = youtubeViewModel::setReference,
                            onAddChannel = youtubeViewModel::resolveAndAllow,
                            onSetClassification = youtubeViewModel::setClassification,
                            onToggleAvailable = youtubeViewModel::toggleAvailable,
                            onSetAvailableClassification = youtubeViewModel::setAvailableClassification,
                            onDeleteChannel = youtubeViewModel::deleteChannel,
                            onBackToStep1 = youtubeViewModel::returnToStep1,
                            onStart = youtubeViewModel::finishSetup,
                        )
                    }
                }
            }
        }
    }
}
