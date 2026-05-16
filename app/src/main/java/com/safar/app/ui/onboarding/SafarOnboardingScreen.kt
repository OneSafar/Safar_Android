package com.safar.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.R
import com.safar.app.ui.theme.LoraFontFamily
import com.safar.app.ui.theme.SafarTheme
import com.safar.app.ui.theme.shimmer

/**
 * Marketing-style onboarding hero with SAFAR logo and the same primary action pattern as splash
 * ([string]splash_start_safar[/string] + arrow).
 *
 * Usage:
 * ```
 * SafarOnboardingScreen(
 *     currentPage = 0,
 *     pageCount = 3,
 *     onStartSafar = { /* navigate forward */ },
 * )
 * ```
 */
@Composable
fun SafarOnboardingScreen(
    modifier: Modifier = Modifier,
    currentPage: Int = 0,
    pageCount: Int = 3,
    onStartSafar: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                HeroVisualSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.60f),
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.40f),
                )
            }

            BottomContentPanel(
                currentPage = currentPage,
                pageCount = pageCount,
                onStartSafar = onStartSafar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun HeroVisualSection(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val accent = scheme.tertiary
    val bg = scheme.background

    Box(
        modifier = modifier
            .background(primary)
            .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.88f),
                            accent.copy(alpha = 0.72f),
                            bg.copy(alpha = 0.96f),
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 1100f),
                    ),
                ),
        )

        GlowOrb(
            color = accent,
            modifier = Modifier
                .size(380.dp)
                .offset(x = 120.dp, y = (-110).dp),
            alpha = 0.70f,
            blurRadius = 80.dp,
        )

        GlowOrb(
            color = primary,
            modifier = Modifier
                .size(480.dp)
                .offset(x = (-110).dp, y = 130.dp),
            alpha = 0.80f,
            blurRadius = 100.dp,
        )

        // Light plate so navy brand logo stays crisp on the gradient
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_safar_logo_brand_light),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .padding(28.dp)
                    .size(220.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun GlowOrb(
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float,
    blurRadius: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = modifier
            .blur(blurRadius)
            .background(color.copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun BottomContentPanel(
    currentPage: Int,
    pageCount: Int,
    onStartSafar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.background
    val muted = scheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(353.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                ambientColor = bg.copy(alpha = 0.5f),
                spotColor = bg.copy(alpha = 0.5f),
            )
            .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
            .background(bg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_headline),
                color = scheme.onSurface,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_body),
                color = muted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.82f),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageIndicators(
                currentPage = currentPage,
                pageCount = pageCount,
            )

            Spacer(modifier = Modifier.height(24.dp))

            StartSafarPrimaryButton(onClick = onStartSafar)
        }
    }
}

/** Matches splash: primary pill, Lora label, forward arrow (same string resource). */
@Composable
private fun StartSafarPrimaryButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 18.dp,
                shape = CircleShape,
                ambientColor = scheme.primary.copy(alpha = 0.25f),
                spotColor = scheme.primary.copy(alpha = 0.25f),
            )
            .clip(CircleShape)
            .shimmer(),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp,
        ),
    ) {
        Text(
            text = stringResource(R.string.splash_start_safar),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LoraFontFamily,
            letterSpacing = 0.5.sp,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(22.dp),
        )
    }
}

@Composable
private fun PageIndicators(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .size(
                        width = if (isSelected) 32.dp else 8.dp,
                        height = 8.dp,
                    )
                    .clip(CircleShape)
                    .background(if (isSelected) scheme.primary else scheme.surfaceVariant),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SafarOnboardingScreenPreviewLight() {
    SafarTheme(darkTheme = false) {
        SafarOnboardingScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun SafarOnboardingScreenPreviewDark() {
    SafarTheme(darkTheme = true) {
        SafarOnboardingScreen()
    }
}
