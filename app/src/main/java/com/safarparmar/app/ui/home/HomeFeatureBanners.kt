package com.safarparmar.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.IconButton
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes

@Composable
internal fun HomeFeatureBanners(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    bannerHeight: Dp? = null,
    leadingBanner: (@Composable () -> Unit)? = null,
    phonePlumStyle: Boolean = false,
) {
    val hasLeadingBanner = leadingBanner != null
    val pager = rememberPagerState(pageCount = { if (hasLeadingBanner) 3 else 2 })
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(pager, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(5_000L)
                // Snap automatic changes so reading text is never clipped mid-translation.
                if (!pager.isScrollInProgress) {
                    pager.scrollToPage((pager.currentPage + 1) % pager.pageCount)
                }
            }
        }
    }
    val descriptions = listOfNotNull(
        "Study streak".takeIf { hasLeadingBanner },
        stringResource(R.string.home_banner_youtube_focus_description),
        stringResource(R.string.home_banner_habit_tracker_description),
    )
    Column(
        modifier = modifier.widthIn(max = maxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pager,
            pageSpacing = 12.dp,
            modifier = if (phonePlumStyle) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
        ) { page ->
            if (hasLeadingBanner && page == 0) {
                Box(Modifier.fillMaxWidth()) { leadingBanner() }
            } else {
                val featurePage = page - if (hasLeadingBanner) 1 else 0
                if (phonePlumStyle) PlumHomePromo(habitTracker = featurePage != 0, onNavigate = onNavigate, modifier = Modifier.fillMaxSize(), isDarkTheme = isDarkTheme)
                else ScaledTabletHomeBanner(
                    habitTracker = featurePage != 0,
                    isDarkTheme = isDarkTheme,
                    onNavigate = onNavigate,
                )
            }
        }
        Row(
            modifier = if (phonePlumStyle) Modifier.height(36.dp) else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick = {
                    scope.launch { pager.scrollToPage((pager.currentPage - 1 + pager.pageCount) % pager.pageCount) }
                },
                style = ButtonStyle.Ghost,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous banner", tint = MaterialTheme.colorScheme.onSurface)
            }
            descriptions.forEachIndexed { page, description ->
                Box(
                    modifier = Modifier.size(32.dp)
                        .selectable(
                            selected = pager.currentPage == page,
                            role = Role.Tab,
                            onClick = { scope.launch { pager.scrollToPage(page) } },
                        )
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(width = if (pager.currentPage == page) 20.dp else 7.dp, height = 7.dp)
                            .background(
                                if (pager.currentPage == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                CircleShape,
                            ),
                    )
                }
            }
            IconButton(
                onClick = {
                    scope.launch { pager.scrollToPage((pager.currentPage + 1) % pager.pageCount) }
                },
                style = ButtonStyle.Ghost,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next banner", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
internal fun V5HomeFeatureBanners(onNavigate: (String) -> Unit, modifier: Modifier = Modifier, autoRotate: Boolean = true) {
    val pager = rememberPagerState(pageCount = { 3 })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(pager, lifecycle, autoRotate) {
        if (autoRotate) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(5_000L)
                pager.animateScrollToPage((pager.currentPage + 1) % pager.pageCount)
            }
        }
    }
    val descriptions = listOf(
        stringResource(R.string.home_banner_youtube_focus_description),
        stringResource(R.string.home_banner_habit_tracker_description),
        stringResource(R.string.home_banner_timetable_description),
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = true,
            pageSpacing = 12.dp,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("home-promo-carousel"),
        ) { page ->
            Image(
                painter = painterResource(when (page) { 0 -> R.drawable.home_banner_youtube_focus; 1 -> R.drawable.home_banner_habit_tracker; else -> R.drawable.home_banner_timetable }),
                contentDescription = descriptions[page],
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(3f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) {
                        onNavigate(when (page) { 0 -> Routes.YOUTUBE_STUDY_MODE_V2; 1 -> Routes.HABIT_TRACKER; else -> Routes.TIMETABLE })
                    },
            )
        }
    }
}
