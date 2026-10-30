/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · approved Plum Membership */
package com.safarparmar.app.ui.premium

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.composables.ui.components.Disclosure
import com.composables.ui.components.DisclosureButton
import com.composables.ui.components.DisclosurePanel
import com.composables.ui.components.Icon as UiIcon
import com.composables.ui.components.Text as UiText
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.PoppinsFontFamily

internal data class PremiumMembershipColors(
    val canvas: Color, val paper: Color, val ink: Color, val muted: Color,
    val action: Color, val onAction: Color, val border: Color, val selected: Color,
    val hero: Color, val heroWave: Color, val heroWaveLight: Color,
    val onHero: Color, val heroMuted: Color, val peach: Color, val coral: Color,
    val lilac: Color, val sage: Color, val mint: Color, val onMint: Color,
    val badge: Color, val onBadge: Color,
)
internal fun premiumMembershipColors(dark: Boolean) = PremiumMembershipColors(
    canvas = if (dark) Color(0xFF211921) else Color(0xFFFFF5F0),
    paper = if (dark) Color(0xFF342735) else Color(0xFFFFFDFC),
    ink = if (dark) Color(0xFFFFF0F6) else Color(0xFF39132E),
    muted = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F),
    action = if (dark) Color(0xFFF0BEDC) else Color(0xFF542347),
    onAction = if (dark) Color(0xFF39132E) else Color.White,
    border = if (dark) Color(0xFF654D66) else Color(0xFFE6DCEC),
    selected = if (dark) Color(0xFF483149) else Color(0xFFF4ECF6),
    hero = Color(0xFF421735), heroWave = Color(0xFF572342), heroWaveLight = Color(0xFF793D67),
    onHero = Color(0xFFFFFAFD), heroMuted = Color(0xFFEDD8E5),
    peach = Color(0xFFFFCBBE), coral = Color(0xFFED8A7D), lilac = Color(0xFFCDBBE9),
    sage = Color(0xFF8EAD7C), mint = Color(0xFFC9EFE0), onMint = Color(0xFF174E40),
    badge = Color(0xFFFFA014), onBadge = Color(0xFF39132E),
)
internal val LocalPremiumMembershipColors = staticCompositionLocalOf { premiumMembershipColors(false) }

/** Filled native paths, not a flattened mockup or a replacement for Titli. */
@Composable
internal fun PremiumMembershipHero(status: String, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalPremiumMembershipColors.current
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
        .background(c.hero)) {
        val artWidth = if (maxWidth < 360.dp) 112.dp else 148.dp
        Canvas(Modifier.matchParentSize()) {
            val w = size.width; val h = size.height
            drawPath(Path().apply {
                moveTo(0f, h * .91f)
                cubicTo(w * .30f, h * .98f, w * .42f, h * .55f, w, h * .84f)
                lineTo(w, h); lineTo(0f, h); close()
            }, c.heroWave)
            drawPath(Path().apply {
                moveTo(w * .43f, h)
                cubicTo(w * .68f, h * .78f, w * .80f, h * .78f, w, h * .70f)
                lineTo(w, h); close()
            }, c.heroWaveLight)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.clip(CircleShape).background(c.mint).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = c.onMint, modifier = Modifier.size(18.dp))
                Text(status, color = c.onMint, fontFamily = PoppinsFontFamily, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
            }
            // The artwork is a measured sibling, not an absolute overlay. Status/details
            // always start below BOTH the illustration and the wrapped/scaled headline.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.premium_membership_headline), modifier = Modifier.weight(1f),
                    color = c.onHero, fontFamily = PoppinsFontFamily, fontSize = 28.sp, lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-.7).sp)
                PremiumMembershipArtwork(Modifier.width(artWidth).height(166.dp))
            }
            content()
        }
    }
}

/** Own layout bounds prevent pale artwork from ever sitting behind white membership text. */
@Composable
private fun PremiumMembershipArtwork(modifier: Modifier = Modifier) {
    val c = LocalPremiumMembershipColors.current
    Canvas(modifier.clipToBounds()) {
            val w = size.width
            val h = size.height
            fun arch(x: Float, y: Float, width: Float, bottom: Float) = Path().apply {
                val r = width / 2f
                moveTo(x, bottom); lineTo(x, y + r)
                quadraticTo(x, y, x + r, y)
                quadraticTo(x + width, y, x + width, y + r)
                lineTo(x + width, bottom); close()
            }
            drawPath(arch(w * .25f, 0f, w * .67f, h), c.peach)
            drawPath(arch(w * .42f, h * .24f, w * .40f, h), c.coral)
            drawCircle(c.lilac, radius = w * .17f, center = Offset(w * .22f, h * .32f))
            val leaf = Path().apply {
                moveTo(w * .88f, h)
                cubicTo(w * .53f, h * .88f, w * .55f, h * .63f, w * .54f, h * .59f)
                cubicTo(w * .89f, h * .66f, w * .93f, h * .82f, w * .88f, h)
                moveTo(w * .87f, h)
                cubicTo(w * .69f, h * .75f, w * .85f, h * .58f, w, h * .49f)
                cubicTo(w * 1.04f, h * .77f, w * .98f, h * .91f, w * .87f, h)
            }
            drawPath(leaf, c.sage)
            val star = Path().apply {
                moveTo(w * .12f, h * .05f); quadraticTo(w * .14f, h * .11f, w * .20f, h * .12f)
                quadraticTo(w * .14f, h * .13f, w * .12f, h * .19f)
                quadraticTo(w * .10f, h * .13f, w * .04f, h * .12f)
                quadraticTo(w * .10f, h * .11f, w * .12f, h * .05f)
            }
            drawPath(star, c.peach)
    }
}

@Composable
internal fun PremiumHeroDetail(icon: ImageVector, title: String, description: String? = null,
    action: (@Composable () -> Unit)? = null) {
    val c = LocalPremiumMembershipColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(c.peach), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.hero, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = c.onHero, fontFamily = PoppinsFontFamily, fontSize = 14.sp, lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold)
            description?.let { Text(it, color = c.heroMuted, fontFamily = PoppinsFontFamily, fontSize = 12.sp, lineHeight = 18.sp) }
        }
        action?.invoke()
    }
}

/** Composables UI disclosure pattern; full existing benefit copy remains in each panel. */
@Composable
internal fun PremiumBenefitGroups() {
    val groups = listOf(
        Triple(Icons.Default.Description, R.string.premium_group_preparation,
            listOf(R.string.premium_benefit_planner, R.string.premium_benefit_revision)),
        Triple(Icons.Default.BarChart, R.string.premium_group_progress, listOf(R.string.premium_benefit_analytics)),
        Triple(Icons.Default.SelfImprovement, R.string.premium_group_focus,
            listOf(R.string.premium_benefit_connect, R.string.premium_benefit_dhyan_audio, R.string.premium_benefit_live_sessions)),
    )
    val summaries = listOf(R.string.premium_group_preparation_summary, R.string.premium_group_progress_summary,
        R.string.premium_group_focus_summary)
    val c = LocalPremiumMembershipColors.current
    groups.forEachIndexed { index, (icon, titleId, benefits) ->
        var expanded by rememberSaveable(titleId) { mutableStateOf(false) }
        Disclosure(expanded = expanded, onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.paper)) {
            DisclosureButton(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                indicator = {
                    UiIcon(if (it) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null, tint = c.ink, modifier = Modifier.size(22.dp))
                }) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.peach), contentAlignment = Alignment.Center) {
                    UiIcon(icon, contentDescription = null, tint = c.hero, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    UiText(stringResource(titleId), color = c.ink, fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp)
                    UiText(stringResource(summaries[index]), color = c.muted, fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            DisclosurePanel(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                enter = EnterTransition.None, exit = ExitTransition.None) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    benefits.forEach { benefit ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            UiIcon(Icons.Default.Check, contentDescription = null, tint = c.action, modifier = Modifier.padding(top = 2.dp).size(17.dp))
                            UiText(stringResource(benefit), color = c.ink, fontFamily = PoppinsFontFamily,
                                fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
