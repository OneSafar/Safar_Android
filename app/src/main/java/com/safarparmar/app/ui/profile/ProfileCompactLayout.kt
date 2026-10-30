/* Hallmark · pre-emit critique: P5 H5 E4 S5 R5 V4 · approved Compact Profile */
package com.safarparmar.app.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.SafarCircularProgressIndicator
import com.safarparmar.app.ui.theme.PoppinsFontFamily

internal data class CompactProfileColors(val canvas: Color, val paper: Color, val ink: Color,
    val muted: Color, val action: Color, val onAction: Color, val border: Color,
    val peach: Color, val mint: Color, val onMint: Color, val plum: Color, val onPlum: Color,
    val lilac: Color, val sage: Color, val danger: Color, val dangerSurface: Color)
internal fun compactProfileColors(dark: Boolean) = CompactProfileColors(
    canvas = if (dark) Color(0xFF131316) else Color(0xFFFFF5F0),
    paper = if (dark) Color(0xFF1E1E24) else Color(0xFFFFFDFC),
    ink = if (dark) Color(0xFFFFF0F6) else Color(0xFF39132E),
    muted = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F),
    action = if (dark) Color(0xFFF0BEDC) else Color(0xFF542347),
    onAction = if (dark) Color(0xFF39132E) else Color.White,
    border = if (dark) Color(0xFF654D66) else Color(0xFFE8DEE5),
    peach = Color(0xFFFFCBBE), mint = Color(0xFFC9EFE0), onMint = Color(0xFF174E40),
    plum = Color(0xFF542347), onPlum = Color(0xFFFFF5FA), lilac = Color(0xFFCDBBE9),
    sage = Color(0xFF8EAD7C), danger = if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E),
    dangerSurface = if (dark) Color(0xFF43282E) else Color(0xFFFFF0EF))
internal val LocalCompactProfileColors = staticCompositionLocalOf { compactProfileColors(false) }

/** Actual available width (after the existing navigation rail), not a physical-device flag. */
@Composable
internal fun CompactProfileLayout(modifier: Modifier = Modifier,
    identity: @Composable (Boolean) -> Unit, form: @Composable () -> Unit,
    membership: @Composable () -> Unit, save: @Composable () -> Unit,
    accountActions: @Composable () -> Unit, footer: @Composable () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val columns = maxWidth >= 700.dp && fontScale < 1.5f
        val landscape = columns && maxWidth >= 1000.dp
        val padding = if (columns) 24.dp else 20.dp
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.widthIn(max = 1200.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                if (landscape) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            identity(true)
                            membership()
                            accountActions()
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            CompactProfileHeading()
                            form()
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Box(Modifier.widthIn(max = 320.dp).fillMaxWidth()) { save() }
                            }
                        }
                    }
                } else {
                    identity(false)
                    CompactProfileHeading()
                    if (columns) Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.weight(1.4f)) { form() }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            membership(); save(); accountActions()
                        }
                    } else {
                        form(); membership(); save(); accountActions()
                    }
                }
                footer()
            }
        }
    }
}

@Composable
private fun CompactProfileHeading() {
    val c = LocalCompactProfileColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.profile_compact_your_details), color = c.ink, fontFamily = PoppinsFontFamily,
            fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.profile_update_details), color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
    }
}

@Composable
internal fun CompactProfileSurface(content: @Composable ColumnScope.() -> Unit) {
    val c = LocalCompactProfileColors.current
    Surface(color = c.paper, contentColor = c.ink, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}

@Composable
internal fun CompactProfileMembership(premium: Boolean, onPremium: () -> Unit) {
    val c = LocalCompactProfileColors.current
    Surface(color = c.plum, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.peach), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = c.plum, modifier = Modifier.size(26.dp))
            }
            Text(stringResource(if (premium) R.string.profile_safar_premium else R.string.profile_safar_plus),
                color = c.onPlum, fontFamily = PoppinsFontFamily, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(if (premium) R.string.profile_premium_active else R.string.profile_free_plan),
                color = c.onPlum, fontSize = 13.sp, lineHeight = 21.sp)
            // Artwork is measured below the text, so no translated/large text can overlap it.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Button(onClick = onPremium, style = ButtonStyle.Ghost,
                        modifier = Modifier.fillMaxWidth().background(c.onPlum.copy(alpha = .13f), RoundedCornerShape(16.dp))) {
                        Text(stringResource(if (premium) R.string.profile_manage_plan else R.string.profile_explore_premium),
                            color = c.onPlum, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Canvas(Modifier.size(70.dp, 80.dp).clip(RoundedCornerShape(bottomEnd = 16.dp))) {
                    val w = size.width; val h = size.height
                    drawCircle(c.lilac, w * .48f, Offset(w * .91f, h * .45f))
                    drawPath(Path().apply {
                        moveTo(0f, h); lineTo(0f, h * .48f)
                        cubicTo(0f, h * .04f, w * .72f, h * .04f, w * .72f, h * .48f)
                        lineTo(w * .72f, h); close()
                    }, c.peach)
                    drawPath(Path().apply {
                        moveTo(w * .7f, h); cubicTo(w * .25f, h * .8f, w * .35f, h * .58f, w * .34f, h * .53f)
                        cubicTo(w * .77f, h * .62f, w * .77f, h * .85f, w * .7f, h)
                        cubicTo(w * .55f, h * .70f, w * .85f, h * .56f, w, h * .52f)
                        cubicTo(w, h * .84f, w * .86f, h * .97f, w * .7f, h)
                    }, c.sage)
                }
            }
        }
    }
}

@Composable
internal fun CompactProfileSave(saving: Boolean, enabled: Boolean, onSave: () -> Unit) {
    val c = LocalCompactProfileColors.current
    Button(onClick = onSave, enabled = enabled && !saving, style = ButtonStyle.Primary,
        shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        if (saving) {
            SafarCircularProgressIndicator(Modifier.size(20.dp), color = c.onAction, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(stringResource(R.string.profile_save_profile), color = c.onAction,
            fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun CompactProfileAccountActions(enabled: Boolean, onLogout: () -> Unit, onDelete: () -> Unit) {
    val c = LocalCompactProfileColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(false, true).forEach { destructive ->
            val ink = if (destructive) c.danger else c.ink
            Button(onClick = if (destructive) onDelete else onLogout, enabled = enabled,
                style = ButtonStyle.Ghost, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().background(if (destructive) c.dangerSurface else c.paper, RoundedCornerShape(20.dp))) {
                Icon(if (destructive) Icons.Default.DeleteForever else Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(if (destructive) R.string.profile_delete_account else R.string.profile_logout),
                    color = ink, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}
