package com.safarparmar.app.ui.leaderboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
            PodiumSection(podium = podium, podiumPeriod = podiumPeriod)
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
                        .height(264.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ordered.forEach { (entry, place, pedestalHeight) ->
                        PodiumColumn(
                            entry = entry,
                            place = place,
                            pedestalHeight = pedestalHeight,
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Avatar + Rank Ring
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(pedestalBg),
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
            color = textMuted,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // Architectural Pedestal Step
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            color = pedestalBg,
            border = BorderStroke(1.dp, pedestalBorder),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = place.toString(),
                    color = medalColor,
                    fontSize = if (place == 1) 26.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                )
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
            // Rank Number
            val rankText = entry.rank.takeIf { it > 0 }?.toString() ?: "—"
            Text(
                text = rankText,
                fontSize = 13.sp,
                fontWeight = if (entry.rank <= 3) FontWeight.Bold else FontWeight.SemiBold,
                color = when (entry.rank) {
                    1 -> Color(0xFFF59E0B)
                    2 -> Color(0xFF64748B)
                    3 -> Color(0xFFD97706)
                    else -> textMuted
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.widthIn(min = 28.dp, max = 40.dp),
            )

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
