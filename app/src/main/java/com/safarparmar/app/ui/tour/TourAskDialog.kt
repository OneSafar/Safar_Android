package com.safarparmar.app.ui.tour

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safarparmar.app.R
import com.safarparmar.app.ui.studyplanner.components.rememberPlannerBackdropBlur
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.glassSurface
import com.safarparmar.app.ui.theme.isLightBackground

@Composable
fun TourAskDialog(onYes: () -> Unit, onNo: () -> Unit) {
    val mediumRosePink = Color(0xFFF49BB7)
    val deepCalmingPink = Color(0xFFE37A9A)

    val sparklePulse = rememberInfiniteTransition(label = "nishthaSparklePulse")
    val sparkleAlpha by sparklePulse.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleAlpha",
    )
    val sparkleScale by sparklePulse.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sparkleScale",
    )

    val isDark = !MaterialTheme.colorScheme.background.isLightBackground()

    Dialog(
        onDismissRequest = onNo,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Real backdrop blur via the window's FLAG_BLUR_BEHIND (API 31+, and only
        // when the system still has cross-window blur enabled). With it live,
        // Titli's translucent card and her pink GlassButton show genuinely
        // blurred content through them. Where it isn't available this falls back
        // to the simulated glass documented in PlannerGlass.kt — translucency +
        // top-edge light border + depth shadow — so the dialog never regresses.
        val blurred = rememberPlannerBackdropBlur()
        val scrimColor = when {
            blurred && isDark -> Color.Black.copy(alpha = 0.28f)
            blurred -> Color(0xFF1C1C1E).copy(alpha = 0.12f)
            isDark -> Color.Black.copy(alpha = 0.55f)
            else -> Color(0xFF1C1C1E).copy(alpha = 0.28f)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                        .glassSurface(shape = RoundedCornerShape(28.dp), isDarkTheme = isDark),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("Hi, I'm ")
                                withStyle(SpanStyle(color = deepCalmingPink)) {
                                    append("Titli")
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        )

                        Text(
                            text = stringResource(R.string.tour_ask_body_primary),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.tour_ask_body_secondary_prefix).trimEnd())
                                append(" ")
                                withStyle(SpanStyle(color = deepCalmingPink, fontWeight = FontWeight.Bold)) {
                                    append(stringResource(R.string.tour_ask_body_secondary_highlight))
                                }
                                append(" ")
                                append(stringResource(R.string.tour_ask_body_secondary_suffix).trimStart())
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp),
                        )

                        // Titli keeps her pink — GlassButton takes the accent as
                        // the fill and only adds the macOS chrome on top, so the
                        // colour is unchanged, just rendered as translucent glass.
                        GlassButton(
                            onClick = onYes,
                            accentColor = deepCalmingPink,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            isDarkTheme = isDark,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.tour_ask_accept),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = onNo,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.tour_ask_decline),
                                color = mediumRosePink,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.ic_butterfly_sparkle),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-9).dp, y = (-2).dp)
                        .size(277.dp)
                        .graphicsLayer {
                            alpha = 0.08f + ((sparkleAlpha - 0.78f) * 0.35f)
                            scaleX = sparkleScale * 1.08f
                            scaleY = sparkleScale * 1.08f
                        },
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_butterfly_sparkle),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-9).dp, y = (-2).dp)
                        .size(277.dp)
                        .graphicsLayer {
                            alpha = sparkleAlpha
                            scaleX = sparkleScale
                            scaleY = sparkleScale
                        },
                )
            }
        }
    }
}
