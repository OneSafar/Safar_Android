package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.viewmodel.HabitTab
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * Circular progress ring gauge with animated percentage and status label.
 * Matches the Concept 3 desk ledger aesthetic.
 */
@Composable
fun CircularProgressGauge(
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).toInt()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "gaugeProgress"
    )

    val isAllDone = totalCount > 0 && completedCount == totalCount
    val trackColor = HabitColors.Outline
    val progressColor = if (isAllDone) HabitColors.CheckDone else HabitColors.RoyalPurple

    Box(
        modifier = modifier.size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val strokeWidth = 3.5.dp.toPx()
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$percentage%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAllDone) HabitColors.CheckDone else HabitColors.TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = if (isAllDone) "Done ✓" else "Complete",
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = if (isAllDone) HabitColors.CheckDone else HabitColors.TextSecondary
            )
        }
    }
}

/**
 * Editorial Masthead: Left-aligned desk diary header with an authentic progress stamp or circular gauge.
 * Replaces the AI-cliché DoodleClover banner.
 */
@Composable
fun LedgerHeader(
    title: String,
    dateSubtitle: String? = null,
    completedCount: Int? = null,
    totalCount: Int? = null,
    showCircularGauge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HabitColors.TextPrimary,
                letterSpacing = (-0.4).sp
            )
            if (dateSubtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateSubtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = HabitColors.TextSecondary
                )
            }
        }

        if (completedCount != null && totalCount != null && totalCount > 0) {
            if (showCircularGauge) {
                CircularProgressGauge(
                    completedCount = completedCount,
                    totalCount = totalCount
                )
            } else {
                val allDone = completedCount == totalCount
                val stampBg = if (allDone) HabitColors.CheckDoneBg else HabitColors.Parchment
                val stampBorder = if (allDone) HabitColors.CheckDone else HabitColors.Outline
                val stampText = if (allDone) HabitColors.CheckDone else HabitColors.TextPrimary

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stampBg)
                        .border(1.dp, stampBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = HabitColors.CheckDone,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (allDone) "All Done" else "$completedCount of $totalCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stampText
                    )
                }
            }
        }
    }
}

/**
 * Tactile Segmented Controller: Inset pill tabs reflecting an authentic desk ledger.
 * Replaces the stock Material TabRow underline.
 */
@Composable
fun LedgerSegmentedTabs(
    selectedTab: HabitTab,
    onTabSelected: (HabitTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(HabitColors.Parchment)
            .border(1.dp, HabitColors.Outline, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HabitTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val label = when (tab) {
                HabitTab.TODAY -> "Today"
                HabitTab.WEEKLY -> "Weekly"
                HabitTab.MONTHLY -> "Monthly"
                HabitTab.INSIGHTS -> "Insights"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) HabitColors.Surface else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.border(1.dp, HabitColors.OutlineStrong, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) HabitColors.RoyalPurple else HabitColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Dignified Habit Row: Clear typography and honest ink tones without random rainbow badges.
 *
 * UX-05 FIX: Optionally shows a streak badge (🔥 N days) to motivate consistency.
 * UX-08 FIX: Edit icon onClickLabel explains action clearly for screen readers and
 *            power users long-pressing for context.
 */
@Composable
internal fun HabitIdentity(
    habit: HabitEntity,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    currentStreak: Int = 0
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val editDescription = androidx.compose.ui.res.stringResource(
        com.safarparmar.app.R.string.habits_edit_name_schedule,
        habit.name,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (focused) HabitColors.FocusRing else HabitColors.Outline,
                RoundedCornerShape(10.dp)
            )
            .background(HabitColors.Surface)
            .clickable(
                interactionSource = interactions,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = editDescription,
                onClick = onEdit
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minimal ledger index mark (first letter in muted ink)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(HabitColors.Parchment)
                .border(1.dp, HabitColors.Outline, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = habit.name.take(1).uppercase(),
                color = HabitColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = habit.name,
                color = HabitColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
            Spacer(Modifier.height(1.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scheduleSummary(habit.targetDays),
                    color = HabitColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                // UX-05 FIX: Streak badge — shown only when streak > 0
                if (currentStreak > 0) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HabitColors.CheckDoneBg)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = "🔥", fontSize = 10.sp)
                        Text(
                            text = "$currentStreak",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabitColors.CheckDone,
                            letterSpacing = (-0.1).sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // UX-08 FIX: explicit onClickLabel for edit icon
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = editDescription,
                tint = HabitColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


/**
 * Tactile Day Cell: Proportional, square planner tile with tabular date figures
 * and crisp stamp completion fills.
 */
@Composable
internal fun HabitDayCell(
    habit: HabitEntity,
    date: LocalDate,
    today: LocalDate,
    completed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    minHeight: Dp = 46.dp
) {
    val status = habitDayState(habit, date, today, completed)
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val isPressed by interactions.collectIsPressedAsState()
    val isToday = date == today

    val cellBackground by animateColorAsState(
        targetValue = when {
            completed -> HabitColors.CheckDone
            !status.scheduled -> HabitColors.Parchment.copy(alpha = 0.6f)
            else -> HabitColors.Surface
        },
        animationSpec = tween(120),
        label = "cellBg"
    )

    val borderStroke: Dp = when {
        focused -> 2.dp
        isToday -> 1.5.dp
        else -> 1.dp
    }

    val borderColor: Color = when {
        focused -> HabitColors.FocusRing
        isToday -> HabitColors.RoyalPurple
        completed -> HabitColors.CheckDone
        else -> HabitColors.Outline
    }

    val dateInk = when {
        completed -> HabitColors.CheckDoneOn
        isToday -> HabitColors.RoyalPurple
        !status.scheduled || status.future -> HabitColors.TextTertiary
        else -> HabitColors.TextPrimary
    }

    val iconTint = when {
        completed -> HabitColors.CheckDoneOn
        !status.scheduled -> HabitColors.TextTertiary.copy(alpha = 0.5f)
        status.future -> HabitColors.TextTertiary
        else -> HabitColors.OutlineStrong
    }

    val description = "${habit.name}, ${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))}" +
            if (isToday) ", Today" else ""

    Column(
        modifier = modifier
            .heightIn(min = minHeight)
            .graphicsLayer {
                scaleX = if (isPressed) 0.95f else 1f
                scaleY = if (isPressed) 0.95f else 1f
            }
            .clip(RoundedCornerShape(8.dp))
            .background(cellBackground)
            .border(borderStroke, borderColor, RoundedCornerShape(8.dp))
            .toggleable(
                value = completed,
                interactionSource = interactions,
                indication = ripple(),
                enabled = status.editable,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .semantics {
                contentDescription = description
                stateDescription = status.description
            }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showDate) {
            Text(
                text = date.dayOfMonth.toString(),
                color = dateInk,
                fontSize = if (minHeight >= 54.dp) 13.sp else 12.sp,
                lineHeight = 15.sp,
                fontWeight = if (isToday || completed) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clearAndSetSemantics {}
            )
            Spacer(Modifier.height(3.dp))
        }

        when {
            completed -> Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (minHeight >= 54.dp) 16.dp else 15.dp)
            )
            !status.scheduled -> Icon(
                Icons.Default.Remove,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            status.future -> Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            else -> Box(
                modifier = Modifier
                    .size(if (minHeight >= 54.dp) 12.dp else 10.dp)
                    .border(1.dp, HabitColors.OutlineStrong, CircleShape)
            )
        }
    }
}

/**
 * Hallmark Editorial Aggregate Day Cell (Approach 2: Ink Density Shade Washes).
 * Displays consistency across ALL habits scheduled for this calendar date.
 */
@Composable
internal fun AggregateDayCell(
    date: LocalDate,
    today: LocalDate,
    completedCount: Int,
    scheduledCount: Int,
    modifier: Modifier = Modifier,
    minHeight: Dp = 58.dp,
    onClick: () -> Unit = {}
) {
    val isToday = date == today
    val isFuture = date.isAfter(today)
    val ratio = if (scheduledCount > 0) (completedCount.toFloat() / scheduledCount).coerceIn(0f, 1f) else 0f
    val isScheduled = scheduledCount > 0
    val is100Percent = isScheduled && completedCount == scheduledCount

    val cellBackground by animateColorAsState(
        targetValue = when {
            !isScheduled -> HabitColors.Parchment.copy(alpha = 0.5f)
            completedCount > 0 -> HabitColors.getInkDensityColor(ratio)
            else -> HabitColors.Surface
        },
        animationSpec = tween(140),
        label = "aggCellBg"
    )

    val borderStroke: Dp = when {
        isToday -> 1.5.dp
        else -> 1.dp
    }

    val borderColor: Color = when {
        isToday -> HabitColors.RoyalPurple
        is100Percent -> HabitColors.CheckDone
        completedCount > 0 -> HabitColors.getInkDensityColor(ratio)
        else -> HabitColors.Outline
    }

    val dateInk: Color = when {
        !isScheduled -> HabitColors.TextTertiary
        isFuture && completedCount == 0 -> HabitColors.TextTertiary
        completedCount > 0 -> HabitColors.getInkDensityText(ratio)
        isToday -> HabitColors.RoyalPurple
        else -> HabitColors.TextPrimary
    }

    val description = "${date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))}: " +
            if (isScheduled) "$completedCount of $scheduledCount completed" else "Rest day"

    Column(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(cellBackground)
            .border(borderStroke, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
            }
            .padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = dateInk,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = if (isToday || is100Percent) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.clearAndSetSemantics {}
        )

        Spacer(Modifier.height(3.dp))

        when {
            !isScheduled -> {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    tint = HabitColors.TextTertiary.copy(alpha = 0.45f),
                    modifier = Modifier.size(12.dp)
                )
            }
            is100Percent -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            completedCount > 0 -> {
                // Micro completion ratio stamp (e.g. 3/4)
                Text(
                    text = "$completedCount/$scheduledCount",
                    color = dateInk,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                )
            }
            isFuture -> {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = HabitColors.TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
            }
            else -> {
                // 0 completed on a scheduled day
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .border(1.dp, HabitColors.OutlineStrong, CircleShape)
                )
            }
        }
    }
}

/**
 * Refined Editorial Period Switcher: Inline date header with compact navigation controls.
 */
@Composable
internal fun PeriodNavigation(
    label: String,
    unit: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previousDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_previous_unit, unit)
    val nextDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_next_unit, unit)
    val currentLabel = if (unit == "week") {
        androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_this_week)
    } else {
        androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_this_month)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = HabitColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // "Today" jump button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(HabitColors.Parchment)
                    .border(1.dp, HabitColors.Outline, RoundedCornerShape(6.dp))
                    .clickable(onClick = onCurrent)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HabitColors.RoyalPurple
                )
            }

            // Previous
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HabitColors.Parchment)
                    .border(1.dp, HabitColors.Outline, RoundedCornerShape(6.dp))
                    .clickable(onClick = onPrevious)
                    .semantics { contentDescription = previousDescription },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = HabitColors.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Next
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(HabitColors.Parchment)
                    .border(1.dp, HabitColors.Outline, RoundedCornerShape(6.dp))
                    .clickable(onClick = onNext)
                    .semantics { contentDescription = nextDescription },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = HabitColors.TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Weekday header row for weekly and monthly calendar tables.
 */
@Composable
internal fun WeekdayLabels(modifier: Modifier = Modifier) {
    val style = if (LocalDensity.current.fontScale > 1.3f) TextStyle.NARROW else TextStyle.SHORT
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(style, Locale.getDefault()).uppercase(),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HabitColors.TextTertiary,
                letterSpacing = 0.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {}
            )
        }
    }
}

@Composable
internal fun HabitEmptyState(
    title: String,
    detail: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HabitColors.Parchment.copy(alpha = 0.7f))
            .border(1.dp, HabitColors.Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HabitColors.TextPrimary,
            letterSpacing = (-0.2).sp
        )
        Text(
            text = detail,
            fontSize = 13.sp,
            color = HabitColors.TextSecondary,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(
                containerColor = HabitColors.RoyalPurple,
                contentColor = HabitColors.OnRoyalPurple
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_add), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
