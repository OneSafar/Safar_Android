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
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.theme.isLightBackground
import java.text.SimpleDateFormat
import java.util.Locale

private val LightCanvas = Color(0xFFF8F6F2)
private val LightPanel = Color(0xFFF8FAFC)
private val DarkPanel = Color(0xFF151A20)
private val ProductiveLight = Color(0xFF047857)
private val ProductiveDark = Color(0xFF4ADE80)
private val NeutralLight = Color(0xFF64748B)
private val NeutralDark = Color(0xFF94A3B8)

private fun primaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary

private fun secondaryText(isLight: Boolean) =
    if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary

private fun productive(isLight: Boolean) = if (isLight) ProductiveLight else ProductiveDark
private fun neutral(isLight: Boolean) = if (isLight) NeutralLight else NeutralDark

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
        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.background else LightCanvas,
        topBarActions = {
            IconButton(onClick = viewModel::refresh) {
                if ((uiState as? LeaderboardUiState.Success)?.isRefreshing == true) {
                    CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh rankings")
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
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
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            PodiumPanel(podium, podiumPeriod)
            Spacer(Modifier.height(22.dp))
            YourPosition(rank, period, userEntry, entries)
            Spacer(Modifier.height(24.dp))
            SectionHeading()
            Spacer(Modifier.height(8.dp))
        }
        if (entries.isEmpty()) {
            item { EmptyRankings() }
        } else {
            items(entries, key = { it.userId }) { entry ->
                LeaderboardRow(entry, entry.userId == currentUserId, maxMinutes)
            }
        }
        if (userEntry != null && entries.none { it.userId == userEntry.userId }) {
            item {
                Spacer(Modifier.height(6.dp))
                LeaderboardRow(
                    entry = userEntry,
                    isCurrentUser = true,
                    maxMinutes = maxOf(maxMinutes, userEntry.totalFocusMinutes.coerceAtLeast(1)),
                )
            }
        }
        item { PaginationBar(page, totalPages, onPageSelect) }
    }
}

@Composable
private fun PodiumPanel(
    podium: List<WeeklyLeaderboardEntryDto>,
    podiumPeriod: WeeklyLeaderboardPeriodDto?,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    AnalyticsPanel(Modifier.padding(horizontal = 16.dp)) {
        Text("Last week's top 3", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        Spacer(Modifier.height(2.dp))
        Text(
            if (podiumPeriod != null) "Final rankings · ${formatWeekRange(podiumPeriod)}"
            else "Final rankings from the previous week",
            fontSize = 12.sp,
            color = secondaryText(isLight),
        )
        Spacer(Modifier.height(12.dp))
        if (podium.isEmpty()) {
            Text(
                "Last week's winners will appear here after the weekly ranking closes.",
                fontSize = 13.sp,
                color = secondaryText(isLight),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            )
        } else {
            val display = listOf(podium.getOrNull(1), podium.getOrNull(0), podium.getOrNull(2))
            Row(
                Modifier.fillMaxWidth().height(276.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                display.forEachIndexed { index, entry ->
                    val place = when (index) { 0 -> 2; 1 -> 1; else -> 3 }
                    val blockHeight = when (place) { 1 -> 126.dp; 2 -> 92.dp; else -> 78.dp }
                    PodiumPerson(
                        entry = entry,
                        place = place,
                        blockHeight = blockHeight,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumPerson(
    entry: WeeklyLeaderboardEntryDto?,
    place: Int,
    blockHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    if (entry == null) {
        Spacer(modifier)
        return
    }
    val badge = when (place) {
        1 -> if (isLight) Color(0xFFD99A00) else Color(0xFFFBBF24)
        2 -> neutral(isLight)
        else -> if (isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Avatar(entry, if (place == 1) 60.dp else 54.dp, badge)
        Spacer(Modifier.height(7.dp))
        Text(
            entry.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText(isLight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatMinutes(entry.totalFocusMinutes),
            fontSize = 12.sp,
            color = secondaryText(isLight),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(blockHeight),
            color = badge.copy(alpha = if (isLight) 0.08f else 0.12f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            border = BorderStroke(1.dp, badge.copy(alpha = 0.38f)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 15.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    place.toString(),
                    color = badge,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun YourPosition(
    rank: Int?,
    period: WeeklyLeaderboardPeriodDto?,
    entry: WeeklyLeaderboardEntryDto?,
    entries: List<WeeklyLeaderboardEntryDto>,
) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val accent = productive(isLight)
    val ranked = rank != null && rank > 0 && entry != null
    val leaderMinutes = entries.maxOfOrNull { it.totalFocusMinutes }?.coerceAtLeast(1) ?: 1
    val progress = if (ranked) {
        (entry!!.totalFocusMinutes.toFloat() / leaderMinutes).coerceIn(0f, 1f)
    } else 0f

    val rankText = if (ranked) "#$rank" else "—"
    val rankFontSize = when {
        rankText.length <= 3 -> 40.sp   // e.g. #1, #99
        rankText.length == 4 -> 32.sp   // e.g. #100, #999
        rankText.length == 5 -> 25.sp   // e.g. #1000, #9999 (e.g. #1212)
        rankText.length == 6 -> 21.sp   // e.g. #10000..#99999
        else -> 18.sp                    // e.g. #100000+
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rankText,
            fontSize = rankFontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            color = primaryText(isLight),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (ranked) "Your position" else "Not ranked yet",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (ranked) "${formatMinutes(entry!!.totalFocusMinutes)} focused · ${formatWeekRange(period)}"
                else "Complete a focus session to rank · ${formatWeekRange(period)}",
                fontSize = 12.sp,
                color = secondaryText(isLight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    .background(secondaryText(isLight).copy(alpha = 0.12f)),
            ) {
                if (progress > 0f) {
                    Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape).background(accent))
                }
            }
        }
    }
}

@Composable
private fun SectionHeading() {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Live rankings", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight), maxLines = 1)
        Text("Focus time completed in Ekagra this week", fontSize = 12.sp, color = secondaryText(isLight), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LeaderboardRow(entry: WeeklyLeaderboardEntryDto, isCurrentUser: Boolean, maxMinutes: Int) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val accent = productive(isLight)
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        color = if (isCurrentUser) accent.copy(alpha = if (isLight) 0.07f else 0.13f) else Color.Transparent,
        shape = shape,
        border = if (isCurrentUser) BorderStroke(1.dp, accent) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val rankText = entry.rank.takeIf { it > 0 }?.toString() ?: "—"
            Text(
                text = rankText,
                fontSize = if (rankText.length >= 4) 11.5.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start,
                modifier = Modifier.widthIn(min = 28.dp, max = 46.dp),
            )
            Avatar(entry, 40.dp, accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isCurrentUser) "You" else entry.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText(isLight),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "YOU",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(accent.copy(alpha = 0.10f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                val fraction = (entry.totalFocusMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                        .background(secondaryText(isLight).copy(alpha = 0.12f)),
                ) {
                    Box(Modifier.fillMaxWidth(fraction).height(5.dp).clip(CircleShape).background(accent))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                formatMinutes(entry.totalFocusMinutes),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun Avatar(entry: WeeklyLeaderboardEntryDto, size: Dp, accent: Color) {
    if (!entry.avatar.isNullOrBlank()) {
        AsyncImage(
            model = entry.avatar,
            contentDescription = entry.name,
            modifier = Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
        )
    } else {
        Box(
            Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(entry.name.take(2).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun PaginationBar(currentPage: Int, totalPages: Int, onPageSelect: (Int) -> Unit) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val accent = productive(isLight)
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onPageSelect(currentPage - 1) }, enabled = currentPage > 1) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page")
        }
        for (page in 1..totalPages) {
            val selected = currentPage == page
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                    .then(if (selected) Modifier.background(accent.copy(alpha = 0.08f)) else Modifier)
                    .clickable { onPageSelect(page) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    page.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) accent else primaryText(isLight),
                )
            }
        }
        IconButton(onClick = { onPageSelect(currentPage + 1) }, enabled = currentPage < totalPages) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page")
        }
    }
}

@Composable
private fun EmptyRankings() {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    Text(
        "This week's leaderboard is being prepared. It updates as Ekagra sessions are completed.",
        fontSize = 13.sp,
        color = secondaryText(isLight),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
    )
}

@Composable
private fun AnalyticsPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isLight) LightPanel else DarkPanel,
        border = BorderStroke(1.dp, secondaryText(isLight).copy(alpha = if (isLight) 0.14f else 0.20f)),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun LeaderboardLoadingSkeleton() {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val transition = rememberInfiniteTransition(label = "leaderboardSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "leaderboardSkeletonAlpha",
    )
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(2) { index ->
            Box(
                Modifier.fillMaxWidth().height(if (index == 0) 180.dp else 210.dp)
                    .clip(RoundedCornerShape(20.dp)).graphicsLayer { this.alpha = alpha }
                    .background(if (isLight) LightPanel else DarkPanel),
            )
        }
        repeat(4) {
            Box(
                Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(14.dp))
                    .graphicsLayer { this.alpha = alpha }
                    .background(secondaryText(isLight).copy(alpha = 0.12f)),
            )
        }
    }
}

@Composable
private fun LeaderboardErrorState(message: String, onRetry: () -> Unit) {
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Unable to load leaderboard", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = primaryText(isLight))
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 13.sp, color = secondaryText(isLight), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = productive(isLight).copy(alpha = 0.10f),
            border = BorderStroke(1.dp, productive(isLight)),
            modifier = Modifier.clickable(onClick = onRetry),
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, null, tint = productive(isLight), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Try Again", fontWeight = FontWeight.Bold, color = productive(isLight))
            }
        }
    }
}
