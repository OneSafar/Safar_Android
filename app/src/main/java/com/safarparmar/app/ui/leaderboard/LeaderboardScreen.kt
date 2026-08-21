package com.safarparmar.app.ui.leaderboard

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardEntryDto
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardPeriodDto
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import com.safarparmar.app.ui.studycircle.LeaderboardRankBadge
import com.safarparmar.app.ui.studycircle.Rank1GoldCrownBadge
import com.safarparmar.app.ui.studycircle.Rank2SilverLaurelBadge
import com.safarparmar.app.ui.studycircle.Rank3BronzeFlameBadge
import java.text.SimpleDateFormat
import java.util.Locale

// ── Formatting helpers ───────────────────────────────────────────────────────

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
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val start = requireNotNull(parser.parse(period?.start.orEmpty()))
        val end = requireNotNull(parser.parse(period?.end.orEmpty()))
        val formatter = SimpleDateFormat("d MMM", Locale.ENGLISH)
        "${formatter.format(start)}–${formatter.format(end)}"
    }.getOrDefault("${period?.start}–${period?.end}")
}

// ── Screen root ──────────────────────────────────────────────────────────────

@Composable
fun LeaderboardScreen(
    currentRoute: String = Routes.LEADERBOARD,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()

    SafarDrawerScaffold(
        title = "Leaderboard",
        subtitle = null,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        useGlassTopBar = false,
        containerColor = PlannerFlatColors.BgCream,
        topBarActions = {
            IconButton(onClick = viewModel::refresh) {
                if ((uiState as? LeaderboardUiState.Success)?.isRefreshing == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(19.dp),
                        strokeWidth = 2.dp,
                        color = PlannerAccent.Coral,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh rankings",
                        tint = PlannerFlatColors.TextDark,
                    )
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PlannerFlatColors.BgCream)
        ) {
            when (val state = uiState) {
                LeaderboardUiState.Loading -> LeaderboardLoadingSkeleton()
                is LeaderboardUiState.Error -> LeaderboardErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadPage(1, showLoading = true) },
                )
                is LeaderboardUiState.Success -> {
                    val data = state.data
                    val userEntry = data.currentUserEntry
                        ?: data.entries.firstOrNull { it.userId == currentUserId }
                    LeaderboardContent(
                        period = data.period,
                        podiumPeriod = data.podiumPeriod,
                        rank = data.currentUserRank,
                        userEntry = userEntry,
                        podium = data.podium,
                        entries = data.entries,
                        currentUserId = currentUserId,
                        page = state.page,
                        totalPages = data.totalPages.coerceIn(1, 5),
                        onPageSelect = { viewModel.loadPage(it, showLoading = false) },
                    )
                }
            }
        }
    }
}

// ── Content Layout ───────────────────────────────────────────────────────────

@Composable
private fun LeaderboardContent(
    period: WeeklyLeaderboardPeriodDto?,
    podiumPeriod: WeeklyLeaderboardPeriodDto?,
    rank: Int?,
    userEntry: WeeklyLeaderboardEntryDto?,
    podium: List<WeeklyLeaderboardEntryDto>,
    entries: List<WeeklyLeaderboardEntryDto>,
    currentUserId: String?,
    page: Int,
    totalPages: Int,
    onPageSelect: (Int) -> Unit,
) {
    val maxMinutes = remember(entries) {
        entries.maxOfOrNull { it.totalFocusMinutes }?.coerceAtLeast(1) ?: 1
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            MultiVariationPodiumSection(podium = podium, podiumPeriod = podiumPeriod)
            Spacer(Modifier.height(20.dp))
            UserSummaryCard(rank = rank, period = period, entry = userEntry, leaderMinutes = maxMinutes)
            Spacer(Modifier.height(24.dp))
            LiveRankingsHeader()
            Spacer(Modifier.height(10.dp))
        }

        if (entries.isEmpty()) {
            item { EmptyRankingsNotice() }
        } else {
            items(entries, key = { it.userId }) { entry ->
                LeaderboardRow(
                    entry = entry,
                    isCurrentUser = entry.userId == currentUserId,
                )
            }
        }

        if (userEntry != null && entries.none { it.userId == userEntry.userId }) {
            item {
                Spacer(Modifier.height(8.dp))
                LeaderboardRow(
                    entry = userEntry,
                    isCurrentUser = true,
                )
            }
        }

        item {
            PaginationBar(
                currentPage = page,
                totalPages = totalPages,
                onPageSelect = onPageSelect,
            )
        }
    }
}

// ── Stepped Anti-Slop Podium ──────────────────────────────────────────────────

// ── Stepped Podium, animated ─────────────────────────────────────────────────

@Composable
private fun PodiumSection(
    podium: List<WeeklyLeaderboardEntryDto>,
    podiumPeriod: WeeklyLeaderboardPeriodDto?,
) {
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted
    val borderColor = PlannerFlatColors.BorderSoft
    val cardBg = PlannerFlatColors.CardWhite

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
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
                    color = textColor,
                )
                Text(
                    text = formatWeekRange(podiumPeriod),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted,
                )
            }

            Spacer(Modifier.height(16.dp))

            if (podium.isEmpty()) {
                Text(
                    text = "Podium winners will appear once the weekly cycle completes.",
                    fontSize = 13.sp,
                    color = textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                )
            } else {
                // Stepped Podium Row: Rank 2 (Left), Rank 1 (Center, Elevated), Rank 3 (Right)
                val ordered = listOf(
                    Triple(podium.getOrNull(1), 2, 76.dp),
                    Triple(podium.getOrNull(0), 1, 102.dp),
                    Triple(podium.getOrNull(2), 3, 62.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ordered.forEach { (entry, place, pedestalHeight) ->
                        PodiumColumn(
                            entry = entry,
                            place = place,
                            pedestalHeight = pedestalHeight,
                            // Silver → Gold → Bronze stagger, matching the web mock
                            riseDelayMs = when (place) {
                                2 -> 50
                                1 -> 150
                                else -> 250
                            },
                            badgeDelayMs = when (place) {
                                2 -> 350
                                1 -> 450
                                else -> 550
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: WeeklyLeaderboardEntryDto?,
    place: Int,
    pedestalHeight: Dp,
    riseDelayMs: Int,
    badgeDelayMs: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = isPlannerDark
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted

    if (entry == null) {
        Spacer(modifier)
        return
    }

    val medalColor = when (place) {
        1 -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706) // Gold
        2 -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B) // Silver
        else -> if (isDark) Color(0xFFF97316) else Color(0xFFB45309) // Bronze
    }

    val pedestalBg = medalColor.copy(alpha = if (isDark) 0.12f else 0.08f)
    val pedestalBorder = medalColor.copy(alpha = if (isDark) 0.40f else 0.28f)
    val avatarSize = if (place == 1) 64.dp else 52.dp

    // ── Pedestal rise-in (once, on first composition) ──────────────────────
    var pedestalVisible by remember { mutableStateOf(false) }
    LaunchedEffect(entry.userId) {
        kotlinx.coroutines.delay(riseDelayMs.toLong())
        pedestalVisible = true
    }
    val pedestalScale by animateFloatAsState(
        targetValue = if (pedestalVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = EaseOutBack),
        label = "pedestalScale",
    )

    // ── Badge pop-in (once, after the pedestal rises) ──────────────────────
    var badgeVisible by remember { mutableStateOf(false) }
    LaunchedEffect(entry.userId) {
        kotlinx.coroutines.delay(badgeDelayMs.toLong())
        badgeVisible = true
    }
    val badgeScale by animateFloatAsState(
        targetValue = if (badgeVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = EaseOutBack),
        label = "badgeScale",
    )

    // ── Gentle float for the avatar (infinite, staggered per column) ───────
    val floatTransition = rememberInfiniteTransition(label = "podiumFloat$place")
    val floatOffset by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2250, easing = FastOutSlowInEasing, delayMillis = place * 150),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatOffset",
    )

    // ── Gold-only glow pulse on the avatar ring ─────────────────────────────
    val glowAlpha by if (place == 1) {
        floatTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "goldGlow",
        )
    } else {
        androidx.compose.runtime.remember { mutableStateOf(0f) }
    }

    // ── One-shot diagonal shine sweep across the badge ──────────────────────
    val shineTransition = rememberInfiniteTransition(label = "shine$place")
    val shineX by shineTransition.animateFloat(
        initialValue = -60f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing, delayMillis = 1200 + place * 300),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shineX",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Animated badge floating above the avatar, with pop-in + shine sweep
        Box(
            modifier = Modifier
                .padding(bottom = 2.dp)
                .scale(badgeScale)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            contentAlignment = Alignment.Center,
        ) {
            when (place) {
                1 -> Rank1GoldCrownBadge(modifier = Modifier.size(30.dp))
                2 -> Rank2SilverLaurelBadge(modifier = Modifier.size(26.dp))
                3 -> Rank3BronzeFlameBadge(modifier = Modifier.size(26.dp))
            }
            // Shine highlight — a thin bright diagonal band sweeping over the badge
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = shineX
                        rotationZ = 20f
                    }
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(40f, 0f),
                        ),
                    ),
            )
        }

        // Avatar + Rank Ring, floating gently, gold pulses
        Box(
            modifier = Modifier
                .offset { IntOffset(0, floatOffset.dp.roundToPx()) }
                .size(avatarSize + 6.dp)
                .then(
                    if (place == 1) {
                        Modifier.background(
                            medalColor.copy(alpha = glowAlpha * 0.35f),
                            CircleShape,
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(pedestalBg)
                    .then(
                        Modifier.background(Color.Transparent, CircleShape)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!entry.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.avatar,
                        contentDescription = entry.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
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

        Spacer(Modifier.height(6.dp))

        // Name
        Text(
            text = entry.name,
            fontSize = if (place == 1) 13.sp else 12.sp,
            fontWeight = if (place == 1) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        // Focus Time
        Text(
            text = formatMinutes(entry.totalFocusMinutes),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (place == 1) medalColor else textMuted,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // Pedestal step — rises in from zero height, small rank chip instead of
        // a dominant numeral so the badge/avatar/name read first
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
            color = pedestalBg,
            border = BorderStroke(1.dp, pedestalBorder),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = medalColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, medalColor.copy(alpha = 0.4f)),
                ) {
                    Text(
                        text = place.toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        color = medalColor,
                        fontSize = if (place == 1) 16.sp else 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                    )
                }
            }
        }
    }
}

// ── User's Position Banner ───────────────────────────────────────────────────

@Composable
private fun UserSummaryCard(
    rank: Int?,
    period: WeeklyLeaderboardPeriodDto?,
    entry: WeeklyLeaderboardEntryDto?,
    leaderMinutes: Int,
) {
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted
    val borderColor = PlannerFlatColors.BorderSoft
    val cardBg = PlannerFlatColors.CardWhite
    val accentCoral = PlannerAccent.Coral

    val isRanked = rank != null && rank > 0 && entry != null
    val progress = if (isRanked && leaderMinutes > 0) {
        (entry!!.totalFocusMinutes.toFloat() / leaderMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Big Rank Typography
            Text(
                text = if (isRanked) "#$rank" else "—",
                fontSize = if ((rank ?: 0) >= 1000) 26.sp else 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = if (isRanked) accentCoral else textMuted,
                modifier = Modifier.widthIn(min = 60.dp),
            )

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isRanked) "Your Position" else "Not Ranked Yet",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                )
                Text(
                    text = if (isRanked) {
                        "${formatMinutes(entry!!.totalFocusMinutes)} focused this week"
                    } else {
                        "Complete an Ekagra session to enter rankings"
                    },
                    fontSize = 12.sp,
                    color = textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isRanked) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(borderColor),
                    ) {
                        if (progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(accentCoral),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Live Rankings Header ─────────────────────────────────────────────────────

@Composable
private fun LiveRankingsHeader() {
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Live Rankings",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Text(
            text = "Ekagra Focus Time",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textMuted,
        )
    }
}

// ── Leaderboard Row (Clean list row without progress bar noise) ──────────────

@Composable
private fun LeaderboardRow(
    entry: WeeklyLeaderboardEntryDto,
    isCurrentUser: Boolean,
) {
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted
    val accentCoral = PlannerAccent.Coral
    val bg = PlannerFlatColors.BorderSoft

    val rowBg = if (isCurrentUser) {
        accentCoral.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = rowBg,
        border = if (isCurrentUser) BorderStroke(1.dp, accentCoral.copy(alpha = 0.35f)) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored Rank Numeral Box (Golden, Silver, Bronze)
            val isDark = isPlannerDark
            val rankBg = when (entry.rank) {
                1 -> if (isDark) Color(0xFF854D0E).copy(alpha = 0.35f) else Color(0xFFFEF9C3) // Golden
                2 -> if (isDark) Color(0xFF334155).copy(alpha = 0.50f) else Color(0xFFF1F5F9) // Silver
                3 -> if (isDark) Color(0xFF7C2D12).copy(alpha = 0.35f) else Color(0xFFFFEDD5) // Bronze
                else -> PlannerFlatColors.BorderSoft
            }
            val rankBorder = when (entry.rank) {
                1 -> BorderStroke(1.dp, if (isDark) Color(0xFFFACC15).copy(alpha = 0.8f) else Color(0xFFEAB308)) // Gold Border
                2 -> BorderStroke(1.dp, if (isDark) Color(0xFFCBD5E1).copy(alpha = 0.7f) else Color(0xFF94A3B8)) // Silver Border
                3 -> BorderStroke(1.dp, if (isDark) Color(0xFFFB923C).copy(alpha = 0.8f) else Color(0xFFD97706)) // Bronze Border
                else -> null
            }
            val rankTextColor = when (entry.rank) {
                1 -> if (isDark) Color(0xFFFEF08A) else Color(0xFF854D0E) // Luminous Gold Text
                2 -> if (isDark) Color(0xFFF8FAFC) else Color(0xFF334155) // Lustrous Silver Text
                3 -> if (isDark) Color(0xFFFDBA74) else Color(0xFF7C2D12) // Radiant Bronze Text
                else -> textMuted
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = rankBg,
                border = rankBorder,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.rank.takeIf { it > 0 }?.toString() ?: "—",
                        fontSize = 12.sp,
                        fontWeight = if (entry.rank <= 3) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = rankTextColor,
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                if (!entry.avatar.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.avatar,
                        contentDescription = entry.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    Text(
                        text = entry.name.take(2).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // User Name
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isCurrentUser) "You" else entry.name,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isCurrentUser) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentCoral.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "YOU",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentCoral,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Focus Time
            Text(
                text = formatMinutes(entry.totalFocusMinutes),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrentUser) accentCoral else textColor,
            )
        }
    }
}

// ── Pagination Bar ───────────────────────────────────────────────────────────

@Composable
private fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageSelect: (Int) -> Unit,
) {
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted
    val accentCoral = PlannerAccent.Coral

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onPageSelect(currentPage - 1) },
            enabled = currentPage > 1,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous page",
                tint = if (currentPage > 1) textColor else textMuted.copy(alpha = 0.4f),
            )
        }

        for (page in 1..totalPages) {
            val isSelected = currentPage == page
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) Modifier.background(accentCoral.copy(alpha = 0.12f))
                        else Modifier
                    )
                    .clickable { onPageSelect(page) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = page.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentCoral else textColor,
                )
            }
        }

        IconButton(
            onClick = { onPageSelect(currentPage + 1) },
            enabled = currentPage < totalPages,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next page",
                tint = if (currentPage < totalPages) textColor else textMuted.copy(alpha = 0.4f),
            )
        }
    }
}

// ── Empty / Loading / Error States ───────────────────────────────────────────

@Composable
private fun EmptyRankingsNotice() {
    val textMuted = PlannerFlatColors.TextMuted

    Text(
        text = "This week's leaderboard is being prepared. It updates automatically as Ekagra sessions are completed.",
        fontSize = 13.sp,
        color = textMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
    )
}

@Composable
private fun LeaderboardLoadingSkeleton() {
    val cardBg = PlannerFlatColors.CardWhite
    val shimmerColor = PlannerFlatColors.BorderSoft

    val transition = rememberInfiniteTransition(label = "leaderboardSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Podium Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
                .graphicsLayer { this.alpha = alpha }
                .background(cardBg),
        )

        // Position Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(18.dp))
                .graphicsLayer { this.alpha = alpha }
                .background(cardBg),
        )

        Spacer(Modifier.height(6.dp))

        // Row Skeletons
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer { this.alpha = alpha }
                    .background(shimmerColor.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun LeaderboardErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    val textColor = PlannerFlatColors.TextDark
    val textMuted = PlannerFlatColors.TextMuted
    val accentCoral = PlannerAccent.Coral

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Unable to load leaderboard",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accentCoral.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accentCoral.copy(alpha = 0.4f)),
            modifier = Modifier.clickable(onClick = onRetry),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = accentCoral,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = accentCoral,
                )
            }
        }
    }
}
