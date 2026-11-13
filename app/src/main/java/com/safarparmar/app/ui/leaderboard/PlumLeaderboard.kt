package com.safarparmar.app.ui.leaderboard

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.HorizontalSeparator
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.safarparmar.app.R
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardEntryDto
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardResponseDto
import com.safarparmar.app.ui.theme.PoppinsFontFamily

internal const val LeaderboardPageSize = 20
internal const val LeaderboardPageCount = 5
internal fun leaderboardRankRange(page: Int): IntRange {
    val start = (page.coerceIn(1, LeaderboardPageCount) - 1) * LeaderboardPageSize + 1
    return start until start + LeaderboardPageSize
}

internal data class LeaderboardColors(val canvas: Color, val paper: Color, val ink: Color,
    val muted: Color, val line: Color, val peach: Color, val lavender: Color, val accent: Color)
@Composable
internal fun leaderboardColors(dark: Boolean) = LeaderboardColors(
    if (dark) BgDark else Color(0xFFFAF8FF),
    if (dark) Color(0xFF342735) else Color(0xFFFFFDFC),
    if (dark) Color(0xFFFFF0F6) else Color(0xFF241544),
    if (dark) Color(0xFFCFBECB) else Color(0xFF75616F),
    if (dark) Color(0xFF654D66) else Color(0xFFE8DEE5),
    if (dark) RoyalPurpleSoftDark else RoyalPurpleSoft,
    if (dark) RoyalPurpleSoftDark else RoyalPurpleSoft,
    if (dark) RoyalPurpleDark else RoyalPurple,
)

@Composable
internal fun PlumLeaderboard(data: WeeklyLeaderboardResponseDto, page: Int, currentUserId: String?,
    dark: Boolean, refreshing: Boolean, onPage: (Int) -> Unit) {
    val c = leaderboardColors(dark)
    val list = rememberLazyListState()
    LaunchedEffect(page) { list.scrollToItem(0) }
    val entries = data.entries.filter { it.rank in leaderboardRankRange(page) }.take(LeaderboardPageSize)
    val user = data.currentUserEntry ?: data.entries.firstOrNull { it.userId == currentUserId }
    LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "podium") { PlumPodium(data, c) }
        item(key = "position") {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.paper)
                .border(1.dp, c.peach, RoundedCornerShape(20.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(data.currentUserRank?.takeIf { it > 0 }?.let { "#$it" } ?: "—",
                    Modifier.clip(RoundedCornerShape(12.dp)).background(c.peach.copy(alpha = .45f)).padding(12.dp),
                    color = c.accent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(if (data.currentUserRank != null && data.currentUserRank > 0) R.string.leaderboard_your_position else R.string.leaderboard_not_ranked),
                        color = c.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (user != null) stringResource(R.string.leaderboard_focused_this_week, plumFormatMinutes(user.totalFocusMinutes))
                        else stringResource(R.string.leaderboard_enter_rankings_help), color = c.muted, fontSize = 12.sp)
                }
            }
        }
        item(key = "rankings") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.paper)
                .border(1.dp, c.line, RoundedCornerShape(22.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.leaderboard_live_rankings), color = c.ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(stringResource(R.string.leaderboard_ekagra_focus_time), color = c.muted, fontSize = 12.sp)
                if (entries.isEmpty()) Text(stringResource(R.string.leaderboard_preparing), color = c.muted)
                entries.forEach { entry ->
                    PlumRankingRow(entry, entry.userId == currentUserId, c)
                    HorizontalSeparator(color = c.line)
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                    (1..LeaderboardPageCount).forEach { number ->
                        Button(onClick = { if (number != page) onPage(number) }, enabled = !refreshing,
                            style = ButtonStyle.Ghost, shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp), contentColor = c.ink,
                            modifier = Modifier.size(48.dp).semantics { selected = number == page }
                                .background(if (number == page) c.lavender else Color.Transparent, RoundedCornerShape(14.dp))) {
                            Text(number.toString(), color = c.ink, fontWeight = if (number == page) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlumPodium(data: WeeklyLeaderboardResponseDto, c: LeaderboardColors) {
    val stage = RoyalPurple
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(stage).drawBehind {
        val arch = Path().apply {
            moveTo(size.width * .35f, size.height)
            lineTo(size.width * .35f, size.height * .47f)
            cubicTo(size.width * .35f, size.height * .23f, size.width * .65f, size.height * .23f,
                size.width * .65f, size.height * .47f)
            lineTo(size.width * .65f, size.height)
        }
        drawPath(arch, Color(0xFFEDD8E5).copy(alpha = .18f), style = Stroke(1.dp.toPx()))
    }.padding(top = 18.dp)) {
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.leaderboard_last_week_champions), color = Color(0xFFFFFAFD),
                fontFamily = PoppinsFontFamily, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(plumFormatWeekRange(data.podiumPeriod), color = Color(0xFFEDD8E5), fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom) {
            listOf(2, 1, 3).forEach { rank ->
                val entry = data.podium.firstOrNull { it.rank == rank }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (rank == 1) {
                        Icon(ImageVector.vectorResource(R.drawable.ic_sidebar_premium_crown), contentDescription = null,
                            tint = Color(0xFFF4C95D), modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(4.dp))
                    }
                    LeaderboardAvatar(entry, if (rank == 1) 72 else 56, Color(0xFFFAF8FF), c)
                    Spacer(Modifier.height(8.dp))
                    Text(entry?.name?.takeIf { it.isNotBlank() } ?: "—", color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.heightIn(min = 40.dp).wrapContentHeight())
                    Text(entry?.let { plumFormatMinutes(it.totalFocusMinutes) } ?: "—", color = Color(0xFFEDD8E5), fontSize = 12.sp,
                        maxLines = 1, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(if (rank == 1) 88.dp else if (rank == 2) 68.dp else 58.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(if (rank == 2) Color(0xFFE1D5F0) else RoyalPurpleSoft), contentAlignment = Alignment.TopCenter) {
                        Text(rank.toString(), Modifier.padding(top = 12.dp).border(1.dp, Color(0xFF9B6886), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp), color = RoyalPurple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlumRankingRow(entry: WeeklyLeaderboardEntryDto, you: Boolean, c: LeaderboardColors) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(if (you) c.lavender else Color.Transparent).padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(when(entry.rank) {
            1 -> Color(0xFFFFE8A4); 3 -> c.peach; else -> c.lavender
        }), contentAlignment = Alignment.Center) {
            Text(entry.rank.toString(), fontSize = 13.sp, color = if (entry.rank == 1) Color(0xFF794E16) else c.ink, fontWeight = FontWeight.SemiBold)
        }
        LeaderboardAvatar(entry, 34, c.lavender, c)
        Text(if (you) stringResource(R.string.study_circle_you) else entry.name,
            Modifier.weight(1f), fontSize = 14.sp, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontWeight = if (you) FontWeight.Bold else FontWeight.Medium)
        Text(plumFormatMinutes(entry.totalFocusMinutes), fontSize = 12.sp, color = c.ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LeaderboardAvatar(entry: WeeklyLeaderboardEntryDto?, diameter: Int, ring: Color, c: LeaderboardColors) {
    Box(Modifier.size(diameter.dp).border(2.dp, ring, CircleShape).padding(3.dp).clip(CircleShape).background(c.lavender),
        contentAlignment = Alignment.Center) {
        Text(entry?.name?.take(1)?.uppercase() ?: "—", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (!entry?.avatar.isNullOrBlank()) AsyncImage(entry?.avatar, contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
