package com.safar.app.ui.nishtha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.R
import com.safar.app.ui.components.StatCardSkeleton
import com.safar.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StreaksScreen(viewModel: NishthaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val streaks = uiState.streaks
    val loginDates = remember(uiState.loginHistory) {
        uiState.loginHistory.mapNotNull { entry ->
            runCatching { java.time.ZonedDateTime.parse(entry.timestamp).toLocalDate() }.getOrNull()
        }.toSet()
    }
    val today = LocalDate.now()
    val last7Days = remember { (6 downTo 0).map { today.minusDays(it.toLong()) } }
    val weeklyCompletions = remember(uiState.goals) {
        last7Days.map { date ->
            uiState.goals.count { goal ->
                goal.completed && (goal.completedAt?.take(10) ?: goal.scheduledDate?.take(10)) == date.toString()
            }.toFloat()
        }
    }
    val weekDayLabels = remember { last7Days.map { it.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_flame),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.streaks_header_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
        Text(stringResource(R.string.streaks_header_subtitle), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (uiState.isLoadingStreaks) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCardSkeleton()
                StatCardSkeleton()
                StatCardSkeleton()
            }
        } else {
            StreakCard(
                label     = stringResource(R.string.streaks_checkin_label),
                value     = streaks.checkInStreak,
                accent    = Emerald500,
                iconRes   = R.drawable.ic_zap,
                bgIconRes = R.drawable.ic_heart_straight,
                bgIconRotation = 12f,
                bgIconOffsetX = 24.dp,
                bgIconOffsetY = (-24).dp,
                bottomContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Handle click if needed */ }
                    ) {
                        Text(
                            stringResource(R.string.streaks_start_today), 
                            color = Emerald500, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward, 
                            contentDescription = null, 
                            tint = Emerald500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )

            StreakCard(
                label     = stringResource(R.string.streaks_login_label),
                value     = streaks.loginStreak,
                accent    = Orange500,
                iconRes   = R.drawable.ic_zap,
                bgIconRes = R.drawable.ic_flame,
                bgIconRotation = -12f,
                bgIconOffsetX = 24.dp,
                bgIconOffsetY = 24.dp,
                bottomContent = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Orange500.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Orange500.copy(alpha = 0.1f))
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                androidx.compose.ui.res.painterResource(R.drawable.ic_sparkle), 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp), 
                                tint = Orange500
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.streaks_amazing), 
                                color = Orange500.copy(alpha = 0.8f), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            )

            // Monthly Health card
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.streaks_monthly_health).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${streaks.goalCompletionStreak * 10}%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(stringResource(R.string.streaks_avg_focus_score).uppercase(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Calendar heatmap (current month)
        CalendarSection(loginDates = loginDates)

        // Goal Consistency Trend — line chart
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.streaks_consistency_trend), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Text(stringResource(R.string.streaks_trend_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                StreakLineChart(values = weeklyCompletions, modifier = Modifier.fillMaxWidth().height(100.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    weekDayLabels.forEach { d ->
                        Text(d, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(
    label: String, 
    value: Int, 
    accent: Color,
    iconRes: Int,
    bgIconRes: Int,
    bgIconRotation: Float,
    bgIconOffsetX: androidx.compose.ui.unit.Dp,
    bgIconOffsetY: androidx.compose.ui.unit.Dp,
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)),
        border = BorderStroke(2.dp, accent.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background Icon
            Icon(
                painter = androidx.compose.ui.res.painterResource(bgIconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = bgIconOffsetX, y = bgIconOffsetY)
                    .graphicsLayer(rotationZ = bgIconRotation),
                tint = accent.copy(alpha = 0.1f)
            )
            
            // Content
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Top Label
                Surface(
                    shape = RoundedCornerShape(50), 
                    color = accent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(iconRes), contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label.uppercase(), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black, 
                            color = accent,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                // Value
                Row(verticalAlignment = Alignment.Bottom) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = density.density,
                            fontScale = density.fontScale.coerceAtMost(1.3f)
                        )
                    ) {
                        Text(
                            "$value", 
                            fontSize = 60.sp, 
                            fontWeight = FontWeight.Black, 
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 60.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.streaks_days_unit), 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Bottom Content
                bottomContent()
            }
        }
    }
}

@Composable
private fun CalendarSection(loginDates: Set<java.time.LocalDate> = emptySet()) {
    val today = LocalDate.now()
    val firstOfMonth = today.withDayOfMonth(1)
    val daysInMonth = today.lengthOfMonth()
    val startDow = firstOfMonth.dayOfWeek.value % 7

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            val dow = listOf("S","M","T","W","T","F","S")
            Row(Modifier.fillMaxWidth()) {
                dow.forEach { d -> Text(d, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            val totalCells = startDow + daysInMonth
            val rows = (totalCells + 6) / 7
            val emerald    = Emerald500
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
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
                            contentAlignment = Alignment.Center
                        ) {
                            if (day in 1..daysInMonth) {
                                val date = today.withDayOfMonth(day)
                                val isToday = day == today.dayOfMonth
                                val isLoggedIn = loginDates.contains(date)
                                val bgColor = when {
                                    isToday    -> emerald
                                    isLoggedIn -> emerald.copy(alpha = 0.3f)
                                    else       -> surfaceVariant.copy(alpha = 0.4f)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.85f)
                                        .clip(CircleShape)
                                        .background(bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day", fontSize = 11.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(emerald))
                    Text("Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(emerald.copy(0.3f)))
                    Text("Logged in", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StreakLineChart(values: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = ChartLine
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val stepX = w / (values.size - 1).coerceAtLeast(1)
        val pts = values.mapIndexed { i, v -> Offset(i * stepX, h - (v / maxVal) * (h * 0.7f) - h * 0.1f) }
        // Draw baseline
        drawLine(color = lineColor.copy(alpha = 0.2f), start = Offset(0f, h * 0.9f), end = Offset(w, h * 0.9f), strokeWidth = 1f)
        // Draw line
        if (pts.size >= 2) {
            for (i in 0 until pts.size - 1) {
                drawLine(color = lineColor, start = pts[i], end = pts[i + 1], strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
        // Draw dots
        pts.forEach { drawCircle(color = lineColor, radius = 6f, center = it) }
    }
}
