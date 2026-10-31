package com.safarparmar.app.ui.nishtha

import androidx.compose.ui.platform.LocalLocale

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.ui.graphics.vector.ImageVector
import com.safarparmar.app.ui.nishtha.journal.JournalMockupFont as StreakMockupFont
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.PoppinsFontFamily
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isGoalCompleted
import androidx.compose.material3.MaterialTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Streaks accent palette — amber fire (matches Nishtha Streaks tab). */
private object StreaksPalette {
    val Primary @Composable get() = if (!MaterialTheme.colorScheme.background.isLightBackground()) {
        Color(0xFFFBBF24)
    } else {
        Color(0xFFD97706)
    }
    val CheckIn @Composable get() = if (!MaterialTheme.colorScheme.background.isLightBackground()) {
        Color(0xFF34D399)
    } else {
        Color(0xFF059669)
    }
    val Login @Composable get() = if (!MaterialTheme.colorScheme.background.isLightBackground()) {
        Color(0xFFF87171)
    } else {
        Color(0xFFDC2626)
    }
    val Health @Composable get() = if (!MaterialTheme.colorScheme.background.isLightBackground()) {
        Color(0xFF38BDF8)
    } else {
        Color(0xFF0284C7)
    }
    val Trend @Composable get() = if (!MaterialTheme.colorScheme.background.isLightBackground()) {
        Color(0xFFC084FC)
    } else {
        Color(0xFF581C87)
    }
}

@Composable
fun StreaksScreen(viewModel: NishthaViewModel = hiltViewModel()) {
    androidx.compose.runtime.LaunchedEffect(viewModel) { viewModel.loadTab(com.safarparmar.app.ui.nishtha.NishthaTab.STREAKS) }
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    com.safarparmar.app.ui.tutorial.ScreenPracticeEntry(com.safarparmar.app.ui.tutorial.TitliFeature.STREAKS,
        !isLight, eligible = !state.isLoadingStreaks && !state.isRestoringStreak)
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        StreaksScreenContent(uiState = state, isLight = isLight, onRestore = viewModel::restoreCheckInStreak)
    }
}

@Composable
internal fun StreaksScreenContent(
    uiState: NishthaUiState, isLight: Boolean, onRestore: () -> Unit = {},
) {
    NishthaWarmTheme(isLight) { WarmStreaksBody(uiState, isLight, onRestore) }
}

/**
 * macOS Control Center glass tile for a streak metric —
 * same chrome as Plan-tab exam cards / check-in mood tiles.
 */
@Composable
private fun StreakGlassCard(
    label: String,
    value: Int,
    accent: Color,
    isLight: Boolean,
    iconRes: Int,
    bgIconRes: Int,
    bgIconRotation: Float,
    bgIconOffsetX: Dp,
    bgIconOffsetY: Dp,
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val bodyColor = if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E)
    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
    }
    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)
    val titleColor = if (isLight) Color.Black else Color.White
    val subtitleColor = if (isLight) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(shadowElevation, shape, spotColor = shadowColor, ambientColor = shadowColor)
            .clip(shape)
            .background(bodyColor, shape)
            .border(0.5.dp, borderBrush, shape),
    ) {
        Icon(
            painter = painterResource(bgIconRes),
            contentDescription = null,
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.TopEnd)
                .offset(x = bgIconOffsetX, y = bgIconOffsetY)
                .graphicsLayer(rotationZ = bgIconRotation),
            tint = accent.copy(alpha = 0.12f),
        )

        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = accent,
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$value",
                    fontFamily = LoraFontFamily,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    color = titleColor,
                    lineHeight = 48.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.streaks_days_unit),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            bottomContent()
        }
    }
}

@Composable
private fun CalendarSection(
    loginDates: Set<LocalDate> = emptySet(),
    accent: Color,
) {
    val today = LocalDate.now()
    val firstOfMonth = today.withDayOfMonth(1)
    val daysInMonth = today.lengthOfMonth()
    val startDow = firstOfMonth.dayOfWeek.value % 7

    Text(
        today.format(DateTimeFormatter.ofPattern("MMMM yyyy", LocalLocale.current.platformLocale)).uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = PlannerFlatColors.TextMuted,
    )
    Spacer(Modifier.height(12.dp))

    val dow = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(Modifier.fillMaxWidth()) {
        dow.forEach { d ->
            Text(
                d,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlannerFlatColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(6.dp))

    val totalCells = startDow + daysInMonth
    val rows = (totalCells + 6) / 7
    repeat(rows) { row ->
        Row(Modifier.fillMaxWidth().then(if (row == 0) Modifier.tutorialTarget("streaks.calendar") else Modifier)) {
            repeat(7) { col ->
                val cellIndex = row * 7 + col
                val day = cellIndex - startDow + 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .heightIn(min = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (day in 1..daysInMonth) {
                        val date = today.withDayOfMonth(day)
                        val isToday = day == today.dayOfMonth
                        val isLoggedIn = loginDates.contains(date)
                        val bgColor = when {
                            isToday -> accent
                            isLoggedIn -> accent.copy(alpha = 0.28f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.78f)
                                .clip(CircleShape)
                                .then(
                                    if (bgColor == Color.Transparent) {
                                        Modifier.border(1.dp, PlannerFlatColors.BorderSoft, CircleShape)
                                    } else {
                                        Modifier.background(bgColor)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day",
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isToday -> Color.White
                                    else -> PlannerFlatColors.TextDark
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
            Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.nishtha_today), fontSize = 10.sp, color = PlannerFlatColors.TextMuted)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(accent.copy(0.28f)))
            Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.nishtha_logged_in), fontSize = 10.sp, color = PlannerFlatColors.TextMuted)
        }
    }
}

@Composable
private fun StreakLineChart(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val stepX = w / (values.size - 1).coerceAtLeast(1)
        val pts = values.mapIndexed { i, v ->
            Offset(i * stepX, h - (v / maxVal) * (h * 0.7f) - h * 0.1f)
        }
        drawLine(
            color = lineColor.copy(alpha = 0.2f),
            start = Offset(0f, h * 0.9f),
            end = Offset(w, h * 0.9f),
            strokeWidth = 1f,
        )
        if (pts.size >= 2) {
            for (i in 0 until pts.size - 1) {
                drawLine(
                    color = lineColor,
                    start = pts[i],
                    end = pts[i + 1],
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
        }
        pts.forEach { drawCircle(color = lineColor, radius = 5f, center = it) }
    }
}

@Composable
private fun WarmStreaksBody(state: NishthaUiState, light: Boolean, onRestore: () -> Unit) {
    val c = nishthaWarmColors(light)
    val zone = com.safarparmar.app.util.IstDateUtils.zone
    val today = LocalDate.now(zone)
    val dates = remember(state.loginHistory) { state.loginHistory.mapNotNull {
        runCatching { java.time.ZonedDateTime.parse(it.timestamp).withZoneSameInstant(zone).toLocalDate() }.getOrNull()
    }.toSet() }
    val checkedIn = state.moods.any {
        runCatching { java.time.ZonedDateTime.parse(it.timestamp).withZoneSameInstant(zone).toLocalDate() == today }.getOrDefault(false)
    }
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { date -> state.goals.count { it.source != "ekagra" && it.isGoalCompleted() && it.assignedDateKey() == date.toString() }.toFloat() }
    Box(Modifier.fillMaxSize().background(c.canvas), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 1080.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.nishtha_tab_streaks), color = c.ink, fontFamily = StreakMockupFont, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (state.isLoadingStreaks) { StatCardSkeleton(); StatCardSkeleton() }
            else BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 720.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) { WarmStreakSummary(state, checkedIn, c, onRestore) }
                    Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) { WarmStreakHistory(dates, today, days, values, c) }
                } else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WarmStreakSummary(state, checkedIn, c, onRestore)
                    WarmStreakHistory(dates, today, days, values, c)
                }
            }
        }
    }
}

@Composable
private fun WarmStreakSummary(state: NishthaUiState, checkedIn: Boolean, c: NishthaWarmColors, onRestore: () -> Unit) {
    WarmNishthaCard(c, Modifier.tutorialTarget("streaks.counts")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StreakIconTile(Icons.Outlined.LocalFireDepartment, c.mint, c.ink)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.warm_checkin_streak), color = c.ink, fontFamily = StreakMockupFont, fontWeight = FontWeight.SemiBold)
                Text("${state.streaks.checkInStreak} ${stringResource(R.string.streaks_days_unit)}", color = c.ink,
                    fontFamily = StreakMockupFont, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }
        }
        if (checkedIn) Text(stringResource(R.string.warm_checked_today), color = c.ink, fontSize = 12.sp,
            modifier = Modifier.background(c.mint, RoundedCornerShape(12.dp)).padding(10.dp))
        if (state.streaks.checkInRestore.available) {
            Row(Modifier.fillMaxWidth().background(c.peach, RoundedCornerShape(16.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.composables.ui.components.Icon(Icons.Default.Error, contentDescription = null,
                    tint = if (MaterialTheme.colorScheme.background.isLightBackground()) Color(0xFFA34C43) else c.ink,
                    modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.warm_missed_checkin), color = c.ink, fontFamily = StreakMockupFont,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.warm_restore_hint, state.streaks.checkInRestore.missedDate.orEmpty(), state.streaks.checkInRestore.projectedStreak ?: 0),
                        color = c.muted, fontFamily = StreakMockupFont, fontSize = 11.sp, lineHeight = 16.sp)
                }
                com.composables.ui.components.Button(onClick = onRestore, enabled = !state.isRestoringStreak,
                    style = com.composables.ui.components.ButtonStyle.Outlined, shape = RoundedCornerShape(50),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                    com.composables.ui.components.Text(stringResource(if (state.isRestoringStreak) R.string.warm_restoring else R.string.warm_restore_streak),
                        fontFamily = StreakMockupFont, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
                }
            }
        }
        state.streakMessage?.let { Text(it, color = c.muted, fontSize = 12.sp) }
    }
    WarmNishthaCard(c) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakIconTile(Icons.Default.BarChart, c.lilac, c.action)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.streaks_monthly_health), color = c.ink, fontFamily = StreakMockupFont, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.warm_average_focus_score), color = c.muted, fontFamily = StreakMockupFont, fontSize = 12.sp)
            }
            Text("${state.streaks.goalCompletionStreak * 10}%", color = c.ink, fontFamily = StreakMockupFont, fontSize = 23.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.background(c.mint, RoundedCornerShape(14.dp)).padding(10.dp))
        }
    }
}

@Composable
private fun WarmStreakHistory(dates: Set<LocalDate>, today: LocalDate, days: List<LocalDate>, values: List<Float>, c: NishthaWarmColors) {
    var month by remember { mutableStateOf(java.time.YearMonth.from(today)) }
    val locale = LocalLocale.current.platformLocale
    WarmNishthaCard(c, spacing = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakIconTile(Icons.Default.DateRange, c.peach, c.ink)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.warm_activity_calendar), color = c.ink, fontFamily = StreakMockupFont, fontWeight = FontWeight.SemiBold)
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), color = c.muted, fontFamily = StreakMockupFont, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                com.composables.ui.components.IconButton(onClick = { month = month.minusMonths(1) },
                    modifier = Modifier.border(1.dp, c.line, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), style = com.composables.ui.components.ButtonStyle.Ghost) {
                    com.composables.ui.components.Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.warm_previous_month), tint = c.ink)
                }
                com.composables.ui.components.IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < java.time.YearMonth.from(today),
                    modifier = Modifier.border(1.dp, c.line, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), style = com.composables.ui.components.ButtonStyle.Ghost) {
                    com.composables.ui.components.Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.warm_next_month), tint = c.ink)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            java.time.DayOfWeek.entries.forEach { d -> Text(d.getDisplayName(java.time.format.TextStyle.NARROW, locale), Modifier.weight(1f), color = c.muted, textAlign = TextAlign.Center, fontSize = 12.sp) }
        }
        val offset = month.atDay(1).dayOfWeek.value - 1
        repeat((offset + month.lengthOfMonth() + 6) / 7) { week ->
            Row(Modifier.fillMaxWidth().then(if (week == 0) Modifier.tutorialTarget("streaks.calendar") else Modifier), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { col ->
                    val day = week * 7 + col - offset + 1
                    val date = if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
                    Box(Modifier.weight(1f).heightIn(min = 34.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (date in dates) c.mint else c.calendarCell)
                        .then(if (date == today) Modifier.border(1.dp, c.action, RoundedCornerShape(10.dp)) else Modifier)
                        .semantics { if (date != null) contentDescription = "$date, ${if (date in dates) "active" else "no activity"}" }
                        .padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                        Text(date?.dayOfMonth?.toString().orEmpty(), color = c.ink, fontSize = 12.sp)
                    }
                }
            }
        }
        Text(stringResource(R.string.nishtha_logged_in), color = c.muted, fontSize = 11.sp)
    }
    WarmNishthaCard(c, Modifier.tutorialTarget("streaks.trend")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakIconTile(Icons.Default.BarChart, c.lilac, c.action)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.warm_consistency_trend), color = c.ink, fontFamily = StreakMockupFont, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.streaks_trend_subtitle), color = c.muted, fontFamily = StreakMockupFont, fontSize = 12.sp)
            }
        }
        Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            days.forEachIndexed { i, date ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(values[i].toInt().toString(), color = c.muted, fontSize = 11.sp)
                            Box(Modifier.fillMaxWidth().height((values[i] / max * 68).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(8.dp)).background(c.lilac))
                        }
                    }
                    Text(date.format(DateTimeFormatter.ofPattern("EEE", locale)), color = c.muted, fontSize = 10.sp)
                }
            }
        }
    }
}

/** Explicit vector icons avoid transparent legacy drawable paths in the status tiles. */
@Composable
private fun StreakIconTile(icon: ImageVector, background: Color, tint: Color) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(background), contentAlignment = Alignment.Center) {
        com.composables.ui.components.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    }
}
