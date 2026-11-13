package com.safarparmar.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.performance.LocalMotionPolicy
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.*

internal data class HomeModuleSlide(
    val title: Int, val headline: Int, val description: Int,
    val lightImage: Int, val darkImage: Int, val route: String,
)

/** Same module images, copy and destinations as v5. */
internal val homeModuleSlides = listOf(
    HomeModuleSlide(R.string.module_ekagra, R.string.home_slide_ekagra_headline,
        R.string.home_slide_ekagra_body, R.drawable.ekagra_light, R.drawable.ekagra_dark, Routes.EKAGRA),
    HomeModuleSlide(R.string.module_nishtha, R.string.home_slide_nishtha_headline,
        R.string.home_slide_nishtha_body, R.drawable.bg_home_light, R.drawable.bg_home_dark, Routes.nishthaRoot()),
    HomeModuleSlide(R.string.module_mehfil, R.string.home_slide_mehfil_headline,
        R.string.home_slide_mehfil_body, R.drawable.light_mehfil, R.drawable.dark_mehfil, Routes.MEHFIL),
    HomeModuleSlide(R.string.module_dhyan, R.string.home_slide_dhyan_headline,
        R.string.home_slide_dhyan_body, R.drawable.dhyan_liight, R.drawable.dark_dhyan, Routes.DHYAN),
    HomeModuleSlide(R.string.module_study_planner, R.string.home_slide_planner_headline,
        R.string.home_slide_planner_body, R.drawable.study_planner_light, R.drawable.study_planner_dark, Routes.STUDY_PLANNER),
)

/** Full-viewport background, independent of the compact text/indicator layout. */
@Composable
internal fun HomeModuleBackground(dark: Boolean, page: Int, modifier: Modifier = Modifier) {
    val motion = LocalMotionPolicy.current
    val paper = if (dark) BgDark else Color.White
    Box(modifier.testTag("home-module-background")) {
        Crossfade(page, animationSpec = tween(if (motion.animationsEnabled) 800 else 0),
            label = "Home background") { index ->
            val slide = homeModuleSlides[index]
            Image(painterResource(if (dark) slide.darkImage else slide.lightImage), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        // Keep the v5 background visible across Home while preserving readable controls.
        Box(Modifier.matchParentSize().drawWithCache {
            val fade = Brush.verticalGradient(
                0f to paper.copy(alpha = .12f * .85f),
                .22f to paper.copy(alpha = .35f * .85f),
                .55f to paper.copy(alpha = .85f * .85f),
                1f to paper.copy(alpha = .92f * .85f),
            )
            onDrawBehind { drawRect(fade) }
        })
    }
}

@Composable
internal fun HomeModuleCarousel(
    dark: Boolean, onNavigate: (String) -> Unit, page: Int, onPage: (Int) -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val accent = if (dark) RoyalPurpleDark else RoyalPurple
    val ink = if (dark) Color.White else RoyalPurpleInk
    Column(Modifier.fillMaxWidth().testTag("home-module-carousel")) {
        Crossfade(page, animationSpec = tween(if (motion.animationsEnabled) 600 else 0), label = "Home module") { index ->
            val slide = homeModuleSlides[index]
            Box(Modifier.fillMaxWidth().heightIn(min = 166.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .clickable(role = Role.Button) { onNavigate(slide.route) }
                .testTag("home-module-$index"), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(slide.headline), color = ink, fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(stringResource(slide.description), color = ink.copy(alpha = .78f),
                        fontFamily = PoppinsFontFamily, fontSize = 12.sp, lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            homeModuleSlides.forEachIndexed { index, slide ->
                val title = stringResource(slide.title)
                Box(Modifier.size(if (page == index) 24.dp else 16.dp, 32.dp).clip(CircleShape).selectable(selected = page == index, role = Role.Tab,
                    onClick = { onPage(index) }).semantics { contentDescription = title }
                    .testTag("home-module-tab-$index"), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(if (page == index) 20.dp else 6.dp, 6.dp)
                        .background(accent.copy(alpha = if (page == index) 1f else .3f), CircleShape))
                }
            }
        }
    }
}
