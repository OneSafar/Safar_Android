package com.safarparmar.app.ui.onboarding

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleSoft

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text
import com.safarparmar.app.R
import com.safarparmar.app.ui.theme.PoppinsFontFamily

internal val EditorialPlum = RoyalPurple

/** Header shared only by the redesigned signup, recovery, and questionnaire states. */
@Composable
internal fun EditorialHeader(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null,
    step: String? = null, showLogo: Boolean = false) {
    Row(modifier.fillMaxWidth().clip(GenericShape { size, _ ->
        moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height - size.height * .06f)
        cubicTo(size.width * .6f, size.height - size.height * .22f, size.width * .4f, size.height + size.height * .10f, 0f, size.height - size.height * .06f)
        close()
    }).background(EditorialPlum).drawWithCache {
        val leaves = Path().apply {
            moveTo(size.width - 20.dp.toPx(), size.height)
            quadraticTo(size.width - 44.dp.toPx(), size.height * .45f, size.width - 28.dp.toPx(), size.height * .32f)
            quadraticTo(size.width - 8.dp.toPx(), size.height * .55f, size.width - 20.dp.toPx(), size.height)
        }
        onDrawBehind {
            drawOval(Color(0xFFCDBBE9), Offset(size.width - 24.dp.toPx(), size.height * .45f), Size(60.dp.toPx(), size.height))
            drawPath(leaves, Color(0xFF8EAD7C))
        }
    }.padding(start = 12.dp, end = 42.dp, top = 16.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (onBack != null) IconButton(onClick = onBack, style = ButtonStyle.Ghost, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Color.White)
        } else if (showLogo && step == null) Image(painterResource(R.drawable.ic_safar_logo_brand_dark),
            contentDescription = stringResource(R.string.app_name), modifier = Modifier.size(62.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            step?.let { Text(it, color = Color(0xFFEDD8E5), fontSize = 12.sp) }
            Text(title, color = Color(0xFFFFFAFD), fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
        }
    }
}

/** Tutorial-style geometry, cached and static, with no bitmap or interaction surface. */
internal fun Modifier.editorialCornerArtwork(bottomCorrectionPx: Float = 0f, insetArtwork: Boolean = false): Modifier = drawWithCache {
    // The app-level IME padding can shrink this layer. Keep the artwork at the
    // physical window bottom, where the keyboard naturally occludes it.
    val bottom = if (insetArtwork) size.height - 16.dp.toPx() else size.height + bottomCorrectionPx.coerceAtLeast(0f)
    val leaf = Path().apply {
        moveTo(45.dp.toPx(), bottom)
        quadraticTo(6.dp.toPx(), bottom - 46.dp.toPx(), 17.dp.toPx(), bottom - 64.dp.toPx())
        quadraticTo(56.dp.toPx(), bottom - 48.dp.toPx(), 45.dp.toPx(), bottom)
        moveTo(45.dp.toPx(), bottom)
        quadraticTo(40.dp.toPx(), bottom - 43.dp.toPx(), 75.dp.toPx(), bottom - 37.dp.toPx())
        quadraticTo(72.dp.toPx(), bottom - 7.dp.toPx(), 45.dp.toPx(), bottom)
    }
    onDrawBehind {
        drawCircle(RoyalPurpleSoft, 64.dp.toPx(), Offset(-28.dp.toPx(), bottom + 8.dp.toPx()))
        drawCircle(EditorialPlum, 76.dp.toPx(), Offset(-28.dp.toPx(), bottom + 8.dp.toPx()), style = Stroke(1.dp.toPx()))
        drawPath(leaf, Color(0xFF8EAD7C))
    }
}

@Composable
internal fun EditorialAuthPage(title: String, onBack: (() -> Unit)? = null, showLogo: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    if (showLogo && onBack == null) {
        EditorialLoginPage(title, content)
        return
    }
    Column(Modifier.widthIn(max = 600.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Fixed chrome: scrolling and focused-field relocation apply only to the form below.
        EditorialHeader(title, onBack = onBack, showLogo = showLogo)
        Column(Modifier.weight(1f).fillMaxWidth().imePadding().clipToBounds()
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally, content = content)
            Spacer(Modifier.height(76.dp))
        }
    }
}

/** Approved sign-in composition: a centered brand hero followed by a real, scrollable form. */
@Composable
private fun EditorialLoginPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heroHeight = (maxHeight * .43f).coerceIn(240.dp, 360.dp)
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.fillMaxWidth().height(heroHeight)
                    .clip(GenericShape { size, _ ->
                        moveTo(0f, 0f); lineTo(size.width, 0f)
                        lineTo(size.width, size.height * .92f)
                        cubicTo(size.width * .7f, size.height * .86f,
                            size.width * .4f, size.height * 1.08f, 0f, size.height * .92f)
                        close()
                    })
                    .background(EditorialPlum)
                    .drawWithCache {
                        val leaf = Path().apply {
                            moveTo(size.width, size.height * .82f)
                            quadraticTo(size.width - 66.dp.toPx(), size.height * .61f,
                                size.width - 44.dp.toPx(), size.height * .37f)
                            quadraticTo(size.width - 12.dp.toPx(), size.height * .48f,
                                size.width, size.height * .82f)
                        }
                        onDrawBehind {
                            drawOval(RoyalPurpleSoft.copy(alpha = .8f),
                                Offset(size.width - 28.dp.toPx(), size.height * .46f),
                                Size(52.dp.toPx(), size.height * .34f))
                            drawPath(leaf, Color(0xFF8EAD7C))
                        }
                    }.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(title, color = Color.White, fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Image(painterResource(R.drawable.ic_safar_logo_brand_dark),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size((heroHeight * .5f).coerceAtMost(172.dp)))
            }
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()
                .padding(horizontal = 24.dp).padding(top = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally, content = content)
            Spacer(Modifier.height(96.dp))
        }
    }
}
