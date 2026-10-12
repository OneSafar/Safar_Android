package com.safarparmar.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import com.safarparmar.app.ui.navigation.Routes

@Composable
internal fun HomeFeatureBanners(onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { 2 })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(pager, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(5_000L)
                pager.animateScrollToPage((pager.currentPage + 1) % pager.pageCount)
            }
        }
    }
    val descriptions = listOf(
        stringResource(R.string.home_banner_youtube_focus_description),
        stringResource(R.string.home_banner_habit_tracker_description),
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = false,
            pageSpacing = 12.dp,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        ) { page ->
            Image(
                painter = painterResource(if (page == 0) R.drawable.home_banner_youtube_focus else R.drawable.home_banner_habit_tracker),
                contentDescription = descriptions[page],
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(3f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button) {
                        onNavigate(if (page == 0) Routes.YOUTUBE_STUDY_MODE_V2 else Routes.HABIT_TRACKER)
                    },
            )
        }
    }
}
