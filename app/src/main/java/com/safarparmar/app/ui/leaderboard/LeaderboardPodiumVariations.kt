package com.safarparmar.app.ui.leaderboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardEntryDto
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardPeriodDto
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark

/**
 * Leaderboard Architectural Podium Section
 */
@Composable
fun MultiVariationPodiumSection(
    podium: List<WeeklyLeaderboardEntryDto>,
    podiumPeriod: WeeklyLeaderboardPeriodDto?,
    modifier: Modifier = Modifier,
) {
    PodiumVariationArchitecturalStudio(podium, podiumPeriod, modifier)
}

@Composable
fun PodiumSection(
    podium: List<WeeklyLeaderboardEntryDto>,
    podiumPeriod: WeeklyLeaderboardPeriodDto?,
    modifier: Modifier = Modifier,
) {
    PodiumVariationArchitecturalStudio(podium, podiumPeriod, modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
// Architectural Studio Podium
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PodiumVariationArchitecturalStudio(
    podium: List<WeeklyLeaderboardEntryDto>,
    podiumPeriod: WeeklyLeaderboardPeriodDto?,
    modifier: Modifier = Modifier,
) {
    val isDark = isPlannerDark

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF18181B) else Color(0xFFFAF7F2),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE7E5E4)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Architectural Spotlight Cone descending from top center onto #1
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                val conePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.42f, 0f)
                    lineTo(w * 0.58f, 0f)
                    lineTo(w * 0.72f, h)
                    lineTo(w * 0.28f, h)
                    close()
                }

                drawPath(
                    path = conePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFBBF24).copy(alpha = if (isDark) 0.18f else 0.12f),
                            Color(0xFFFBBF24).copy(alpha = 0.02f),
                        ),
                    ),
                )
            }

            Column(
                modifier = Modifier.padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Last Week's Champions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlannerFlatColors.TextDark,
                    )
                    Text(
                        text = formatWeekRange(podiumPeriod),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PlannerFlatColors.TextMuted,
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (podium.isEmpty()) {
                    Text(
                        text = "Podium winners will appear once the weekly cycle completes.",
                        fontSize = 13.sp,
                        color = PlannerFlatColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    )
                } else {
                    val ordered = listOf(
                        Triple(podium.getOrNull(1), 2, 76.dp),
                        Triple(podium.getOrNull(0), 1, 104.dp),
                        Triple(podium.getOrNull(2), 3, 62.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().height(290.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        ordered.forEach { (entry, place, pedestalHeight) ->
                            ArchitecturalPodiumColumn(
                                entry = entry,
                                place = place,
                                pedestalHeight = pedestalHeight,
                                riseDelayMs = when (place) { 2 -> 50; 1 -> 150; else -> 250 },
                                badgeDelayMs = when (place) { 2 -> 350; 1 -> 450; else -> 550 },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchitecturalPodiumColumn(
    entry: WeeklyLeaderboardEntryDto?,
    place: Int,
    pedestalHeight: Dp,
    riseDelayMs: Int,
    badgeDelayMs: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = isPlannerDark
    if (entry == null) {
        Spacer(modifier)
        return
    }

    val medalColor = when (place) {
        1 -> if (isDark) Color(0xFFFDE047) else Color(0xFFB45309) // Golden
        2 -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF475569) // Silver
        else -> if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412) // Bronze
    }

    val pedestalColor = when (place) {
        1 -> if (isDark) Color(0xFF27272A) else Color(0xFFF5EBE1)
        2 -> if (isDark) Color(0xFF1F2430) else Color(0xFFEAEBED)
        else -> if (isDark) Color(0xFF2B2118) else Color(0xFFF5E4D7)
    }

    val avatarSize = if (place == 1) 64.dp else 52.dp

    var pedestalVisible by remember { mutableStateOf(false) }
    LaunchedEffect(entry.userId) {
        kotlinx.coroutines.delay(riseDelayMs.toLong())
        pedestalVisible = true
    }
    val pedestalScale by animateFloatAsState(
        targetValue = if (pedestalVisible) 1f else 0f,
        animationSpec = tween(550, easing = EaseOutBack),
        label = "archScale",
    )

    var badgeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(entry.userId) {
        kotlinx.coroutines.delay(badgeDelayMs.toLong())
        badgeVisible = true
    }
    val badgeScale by animateFloatAsState(
        targetValue = if (badgeVisible) 1f else 0f,
        animationSpec = tween(420, easing = EaseOutBack),
        label = "badgeScale",
    )

    val floatTransition = rememberInfiniteTransition(label = "archFloat$place")
    val floatOffset by floatTransition.animateFloat(
        initialValue = 0f, targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(2250, easing = FastOutSlowInEasing, delayMillis = place * 150), RepeatMode.Reverse),
        label = "floatOffset",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Avatar + Crown Headpiece positioned directly on top of the user profile
        Box(
            modifier = Modifier
                .offset { IntOffset(0, floatOffset.dp.roundToPx()) },
            contentAlignment = Alignment.TopCenter,
        ) {
            // User Avatar Box (with top padding for crown clearance)
            Box(
                modifier = Modifier
                    .padding(top = if (place == 1) 18.dp else 16.dp)
                    .size(avatarSize + 4.dp)
                    .background(pedestalColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(pedestalColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!entry.avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = entry.avatar,
                            contentDescription = entry.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Text(
                            text = entry.name.take(2).uppercase(),
                            fontSize = (avatarSize.value * 0.32f).sp,
                            fontWeight = FontWeight.Bold,
                            color = medalColor,
                        )
                    }
                }
            }

            // High-Craft Vector Headpiece Badge crowning directly on top of the avatar head
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .scale(badgeScale),
                contentAlignment = Alignment.Center,
            ) {
                when (place) {
                    1 -> ImperialGoldCrownBadge(modifier = Modifier.size(38.dp))
                    2 -> HeraldicSilverLaurelShieldBadge(modifier = Modifier.size(34.dp))
                    3 -> RadiantBronzeFlameMedallionBadge(modifier = Modifier.size(34.dp))
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = entry.name,
            fontSize = if (place == 1) 13.sp else 12.sp,
            fontWeight = if (place == 1) FontWeight.Bold else FontWeight.SemiBold,
            color = PlannerFlatColors.TextDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Text(
            text = formatMinutes(entry.totalFocusMinutes),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (place == 1) medalColor else PlannerFlatColors.TextMuted,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // Monolithic Architectural Stepped Plinth with Serif Font Style
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .graphicsLayer {
                    scaleY = pedestalScale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    alpha = pedestalScale
                },
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            color = pedestalColor,
            border = BorderStroke(1.dp, medalColor.copy(alpha = 0.35f)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = medalColor.copy(alpha = if (isDark) 0.20f else 0.14f),
                    border = BorderStroke(1.dp, medalColor.copy(alpha = 0.45f)),
                ) {
                    Text(
                        text = place.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.5.dp),
                        color = medalColor,
                        fontSize = if (place == 1) 17.sp else 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

// ── Shared Helpers ───────────────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    if (safe < 60) return "${safe}m"
    val hours = safe / 60
    val rest = safe % 60
    return if (rest == 0) "${hours}h" else "${hours}h ${rest}m"
}

private fun formatWeekRange(period: WeeklyLeaderboardPeriodDto?): String {
    if (period?.start.isNullOrBlank() || period?.end.isNullOrBlank()) return "This week"
    return runCatching {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val display = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        val startDate = parser.parse(period.start)
        val endDate = parser.parse(period.end)
        if (startDate != null && endDate != null) {
            "${display.format(startDate)}–${display.format(endDate)}"
        } else "This week"
    }.getOrDefault("This week")
}
