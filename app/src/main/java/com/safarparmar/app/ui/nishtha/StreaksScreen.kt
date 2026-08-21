package com.safarparmar.app.ui.nishtha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
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
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        StreaksScreenContent(viewModel = viewModel, isLight = isLight)
    }
}

@Composable
private fun StreaksScreenContent(
    viewModel: NishthaViewModel,
    isLight: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streaks = uiState.streaks
    val loginDates = remember(uiState.loginHistory) {
        uiState.loginHistory.mapNotNull { entry ->
            runCatching { java.time.ZonedDateTime.parse(entry.timestamp).toLocalDate() }.getOrNull()
        }.toSet()
    }
    val today = LocalDate.now(com.safarparmar.app.util.IstDateUtils.zone)
    val last7Days = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val weeklyCompletions = remember(uiState.goals) {
        last7Days.map { date ->
            uiState.goals.count { goal ->
                goal.source != "ekagra" && goal.isGoalCompleted() && goal.assignedDateKey() == date.toString()
            }.toFloat()
        }
    }
    val weekDayLabels = remember {
        last7Days.map { it.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlannerFlatColors.BgCream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Flat hairline header ────────────────────────────────────────────
        PlanEyebrow("Nishtha")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_flame),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = StreaksPalette.Primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.streaks_header_title),
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.streaks_header_subtitle),
            fontSize = 13.sp,
            color = PlannerFlatColors.TextMuted,
        )
        Spacer(Modifier.height(16.dp))
        PlanHairline()
        Spacer(Modifier.height(20.dp))

        if (uiState.isLoadingStreaks) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCardSkeleton()
                StatCardSkeleton()
                StatCardSkeleton()
            }
        } else {
            Text(
                "ACTIVE STREAKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = PlannerFlatColors.TextMuted,
            )
            Spacer(Modifier.height(12.dp))

            // ── macOS glass streak tiles ────────────────────────────────────
            StreakGlassCard(
                label = stringResource(R.string.streaks_checkin_label),
                value = streaks.checkInStreak,
                accent = StreaksPalette.CheckIn,
                isLight = isLight,
                iconRes = R.drawable.ic_zap,
                bgIconRes = R.drawable.ic_heart_straight,
                bgIconRotation = 12f,
                bgIconOffsetX = 20.dp,
                bgIconOffsetY = (-20).dp,
                bottomContent = {
                    if (streaks.checkInRestore.available) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "You missed ${streaks.checkInRestore.missedDate}. Restore once to continue at ${streaks.checkInRestore.projectedStreak} days.",
                                color = PlannerFlatColors.TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    enabled = !uiState.isRestoringStreak,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = viewModel::restoreCheckInStreak,
                                ),
                            ) {
                                if (uiState.isRestoringStreak) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = StreaksPalette.CheckIn,
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = StreaksPalette.CheckIn,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (uiState.isRestoringStreak) "Restoring…" else "Restore streak",
                                    color = StreaksPalette.CheckIn,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.streaks_start_today),
                                color = StreaksPalette.CheckIn,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = StreaksPalette.CheckIn,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                },
            )

            uiState.streakMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    color = StreaksPalette.CheckIn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }



            Spacer(Modifier.height(22.dp))
            PlanHairline()
            Spacer(Modifier.height(18.dp))

            // ── Flat monthly health ─────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = StreaksPalette.Health,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.streaks_monthly_health).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = PlannerFlatColors.TextMuted,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${streaks.goalCompletionStreak * 10}%",
                fontFamily = LoraFontFamily,
                fontSize = 36.sp,
                color = StreaksPalette.Health,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.streaks_avg_focus_score).uppercase(),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = PlannerFlatColors.TextMuted,
            )
        }

        Spacer(Modifier.height(22.dp))
        PlanHairline()
        Spacer(Modifier.height(18.dp))

        // ── Flat calendar heatmap ───────────────────────────────────────────
        CalendarSection(loginDates = loginDates, accent = StreaksPalette.CheckIn)

        Spacer(Modifier.height(22.dp))
        PlanHairline()
        Spacer(Modifier.height(18.dp))

        // ── Flat consistency trend ──────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = StreaksPalette.Trend,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.streaks_consistency_trend),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.TextDark,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.streaks_trend_subtitle),
            fontSize = 12.sp,
            color = PlannerFlatColors.TextMuted,
        )
        Spacer(Modifier.height(14.dp))
        StreakLineChart(
            values = weeklyCompletions,
            lineColor = StreaksPalette.Trend,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekDayLabels.forEach { d ->
                Text(d, fontSize = 10.sp, color = PlannerFlatColors.TextMuted)
            }
        }
    }
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
        today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())).uppercase(),
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
        Row(Modifier.fillMaxWidth()) {
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
            Text("Today", fontSize = 10.sp, color = PlannerFlatColors.TextMuted)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(accent.copy(0.28f)))
            Text("Logged in", fontSize = 10.sp, color = PlannerFlatColors.TextMuted)
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
