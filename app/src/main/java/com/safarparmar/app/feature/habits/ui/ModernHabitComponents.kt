package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import com.safarparmar.app.feature.habits.viewmodel.HabitTab
import com.safarparmar.app.performance.LocalMotionPolicy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal val ModernCardShape = RoundedCornerShape(22.dp)
internal val ModernMediumShape = RoundedCornerShape(16.dp)
internal val ModernSmallShape = RoundedCornerShape(12.dp)
internal val MondayFirst = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

@Composable
internal fun ModernTabs(
    selected: HabitTab,
    onSelected: (HabitTab) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
) {
    val tabs = remember {
        listOf(
            HabitTab.TODAY to "Daily",
            HabitTab.WEEKLY to "Weekly",
            HabitTab.MONTHLY to "Month",
            HabitTab.INSIGHTS to "Insights"
        )
    }
    val purpleBorder = HabitColors.RoyalPurple
    val pillShape = CircleShape

    Row(
        modifier = modifier
            .height(42.dp)
            .clip(pillShape)
            .background(HabitColors.Surface)
            .border(1.5.dp, purpleBorder, pillShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, (tab, label) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(purpleBorder)
                )
            }
            val active = selected == tab
            val tabBackground by animateColorAsState(
                targetValue = if (active) purpleBorder else Color.Transparent,
                animationSpec = tween(150),
                label = "habitTabBackground",
            )
            val tabContent by animateColorAsState(
                targetValue = if (active) Color.White else HabitColors.TextSecondary,
                animationSpec = tween(150),
                label = "habitTabContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(tabBackground)
                    .clickable(
                        role = Role.Tab,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = tabContent,
                    fontSize = 13.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun ModernPageHeader(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow.uppercase(Locale.getDefault()),
                    color = HabitColors.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = title,
                color = HabitColors.TextPrimary,
                fontSize = 31.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.7).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = HabitColors.TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
internal fun ModernSoftCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(ModernCardShape)
            .background(HabitColors.Surface)
            .border(1.dp, HabitColors.Outline, ModernCardShape),
        content = content
    )
}

@Composable
internal fun ModernPeriodToolbar(
    label: String,
    nowLabel: String,
    onNow: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(end = 10.dp),
            color = HabitColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onNow,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = HabitColors.RoyalPurple)
            ) {
                Text(nowLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            ModernSquareIconButton("Previous", onPrevious) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = HabitColors.TextPrimary)
            }
            ModernSquareIconButton("Next", onNext) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = HabitColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun ModernSquareIconButton(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(HabitColors.Surface)
            .border(1.dp, HabitColors.Outline, RoundedCornerShape(13.dp))
            .semantics { contentDescription = label }
    ) { content() }
}

@Composable
internal fun ModernSectionRow(title: String, meta: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = HabitColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (!meta.isNullOrBlank()) {
            Text(
                meta,
                color = HabitColors.TextTertiary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ModernCountBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(HabitColors.Parchment)
            .border(1.dp, HabitColors.Outline, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = HabitColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun ModernHabitRow(
    habit: HabitEntity,
    completed: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(HabitColors.RoyalPurple.copy(alpha = .72f), CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                habit.name,
                color = if (completed) HabitColors.TextSecondary else HabitColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (habit.isEveryDay) "Every day" else scheduleSummary(habit.targetDays),
                color = HabitColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Default.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_edit_named, habit.name), tint = HabitColors.TextTertiary, modifier = Modifier.size(18.dp))
        }
        ModernCheckButton(
            completed = completed,
            enabled = true,
            onClick = onToggle,
            contentDescription = if (completed) "Mark ${habit.name} as incomplete" else "Mark ${habit.name} as complete"
        )
    }
}

@Composable
internal fun ModernCheckButton(
    completed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val bg by animateColorAsState(if (completed) HabitColors.CheckDone else Color.Transparent, tween(160), label = "checkBg")
    val border by animateColorAsState(if (completed) HabitColors.CheckDone else HabitColors.OutlineStrong, tween(160), label = "checkBorder")
    val motion = LocalMotionPolicy.current
    val buttonScale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.96f,
        animationSpec = if (motion.animationsEnabled) spring(dampingRatio = 0.62f, stiffness = 520f) else tween(0),
        label = "checkScale",
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .scale(buttonScale)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, border, CircleShape)
            .then(
                if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            )
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = completed,
            enter = fadeIn(tween(if (motion.animationsEnabled) 140 else 0)) + scaleIn(initialScale = 0.55f),
            exit = fadeOut(tween(if (motion.animationsEnabled) 100 else 0)) + scaleOut(targetScale = 0.55f),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = HabitColors.OnRoyalPurple, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun ModernHabitIdentity(habit: HabitEntity, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HabitColors.Parchment)
                .border(1.dp, HabitColors.Outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(habit.name.firstOrNull()?.uppercase() ?: "H", fontWeight = FontWeight.ExtraBold, color = HabitColors.TextPrimary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(habit.name, color = HabitColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (habit.isEveryDay) "Every day" else scheduleSummary(habit.targetDays), color = HabitColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_edit_named, habit.name), tint = HabitColors.TextTertiary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun ModernWeekDayCell(
    date: LocalDate,
    today: LocalDate,
    scheduled: Boolean,
    completed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showWeekday: Boolean = false
) {
    val future = date.isAfter(today)
    val isToday = date == today
    val targetBg = when {
        completed -> HabitColors.CheckDone
        else -> HabitColors.Surface
    }
    val targetBorder = when {
        completed -> HabitColors.CheckDone
        isToday -> HabitColors.RoyalPurple
        else -> HabitColors.Outline
    }
    val bg by animateColorAsState(targetBg, tween(180), label = "weekDayBg")
    val borderColor by animateColorAsState(targetBorder, tween(180), label = "weekDayBorder")
    val textColor = when {
        completed -> Color.White
        isToday -> HabitColors.RoyalPurple
        !scheduled || future -> HabitColors.TextTertiary
        else -> HabitColors.TextPrimary
    }
    Column(
        modifier = modifier
            .heightIn(min = if (showWeekday) 68.dp else 60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (isToday && !completed) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = scheduled && (!future || completed), onClick = onToggle)
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showWeekday) {
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault()),
                color = textColor.copy(alpha = .74f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
        }
        Text(date.dayOfMonth.toString(), color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(
            when {
                completed -> "✓"
                scheduled && !future -> "○"
                scheduled && future -> "◷"
                else -> "–"
            },
            color = textColor,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

internal data class ModernDaySummary(val done: Int, val total: Int) {
    val complete: Boolean get() = total > 0 && done == total
}

internal fun modernDaySummary(habits: List<HabitWithCompletions>, date: LocalDate): ModernDaySummary {
    val scheduled = habits.filter { it.completionByDate.containsKey(date) }
    return ModernDaySummary(
        done = scheduled.count { it.completionByDate[date] == true },
        total = scheduled.size
    )
}

@Composable
internal fun ModernWeeklyProgressCard(
    habits: List<HabitWithCompletions>,
    weekStart: LocalDate,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit
) {
    ModernSoftCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_daily_progress), color = HabitColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_completed_scheduled), color = HabitColors.TextTertiary, fontSize = 10.sp, maxLines = 1)
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 9.dp, end = 9.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(7) { index ->
                val date = weekStart.plusDays(index.toLong())
                val summary = modernDaySummary(habits, date)
                val complete = summary.complete
                val current = date == today
                val future = date.isAfter(today)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (complete) HabitColors.CheckDone else HabitColors.Surface)
                        .border(
                            if (current && !complete) 2.dp else 1.dp,
                            when {
                                complete -> HabitColors.CheckDone
                                current -> HabitColors.RoyalPurple
                                else -> HabitColors.Outline
                            },
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { onOpenDay(date) }
                        .padding(vertical = 6.dp, horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val color = when {
                        complete -> Color.White
                        current -> HabitColors.RoyalPurple
                        future -> HabitColors.TextTertiary
                        else -> HabitColors.TextPrimary
                    }
                    Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault()), color = color.copy(alpha = .76f), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(date.dayOfMonth.toString(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(if (summary.total > 0) "${summary.done}/${summary.total}" else "–", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun ModernHabitFilterPicker(
    habits: List<HabitWithCompletions>,
    selectedId: Long?,
    onClick: () -> Unit,
    onEditSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedItem = habits.firstOrNull { it.habit.id == selectedId }
    ModernSoftCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selectedItem != null) HabitColors.RoyalPurpleBg else HabitColors.Parchment)
                    .border(
                        1.dp,
                        if (selectedItem != null) HabitColors.RoyalPurple.copy(alpha = 0.35f) else HabitColors.Outline,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedItem != null) {
                    Text(
                        selectedItem.habit.name.firstOrNull()?.uppercase() ?: "H",
                        color = HabitColors.RoyalPurple,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = HabitColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = selectedItem?.habit?.name ?: androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all),
                    color = HabitColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (selectedItem != null) {
                        if (selectedItem.habit.isEveryDay) "Repeats every day"
                        else "Repeats on ${scheduleSummary(selectedItem.habit.targetDays)}"
                    } else {
                        "${habits.size} habits • Combined overview"
                    },
                    color = HabitColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onEditSelected != null) {
                IconButton(
                    onClick = onEditSelected,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_edit),
                        tint = HabitColors.TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(HabitColors.Parchment)
                    .border(1.dp, HabitColors.Outline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Change",
                        color = HabitColors.RoyalPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = HabitColors.RoyalPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModernHabitFilterSheet(
    habits: List<HabitWithCompletions>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HabitColors.Background,
        contentColor = HabitColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 5.dp)
                    .size(width = 50.dp, height = 5.dp)
                    .background(HabitColors.TextSecondary, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Select Habit",
                        color = HabitColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "View a single habit's calendar or all habits combined.",
                        color = HabitColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_close), tint = HabitColors.TextPrimary)
                }
            }

            Spacer(Modifier.height(8.dp))

            val isAllSelected = selectedId == null
            ModernFilterOptionRow(
                title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all),
                subtitle = "${habits.size} habits combined overview",
                selected = isAllSelected,
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAllSelected) HabitColors.RoyalPurpleBg else HabitColors.Parchment)
                            .border(
                                1.dp,
                                if (isAllSelected) HabitColors.RoyalPurple.copy(alpha = 0.35f) else HabitColors.Outline,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = if (isAllSelected) HabitColors.RoyalPurple else HabitColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = {
                    onSelect(null)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = HabitColors.Outline)
            Spacer(Modifier.height(8.dp))

            Text(
                "INDIVIDUAL HABITS",
                color = HabitColors.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .7.sp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            habits.forEach { item ->
                val isSelected = selectedId == item.habit.id
                val doneCount = item.completionByDate.values.count { it }
                val scheduledCount = item.completionByDate.size
                ModernFilterOptionRow(
                    title = item.habit.name,
                    subtitle = if (item.habit.isEveryDay) "Every day • $doneCount/$scheduledCount done"
                    else "${scheduleSummary(item.habit.targetDays)} • $doneCount/$scheduledCount done",
                    selected = isSelected,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) HabitColors.RoyalPurpleBg else HabitColors.Parchment)
                                .border(
                                    1.dp,
                                    if (isSelected) HabitColors.RoyalPurple.copy(alpha = 0.35f) else HabitColors.Outline,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.habit.name.firstOrNull()?.uppercase() ?: "H",
                                color = if (isSelected) HabitColors.RoyalPurple else HabitColors.TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    },
                    onClick = {
                        onSelect(item.habit.id)
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ModernFilterOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) HabitColors.RoyalPurpleBg else HabitColors.Surface)
            .border(
                1.dp,
                if (selected) HabitColors.RoyalPurple.copy(alpha = 0.35f) else HabitColors.Outline,
                RoundedCornerShape(14.dp)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leadingIcon()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) HabitColors.RoyalPurple else HabitColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = HabitColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = HabitColors.RoyalPurple,
                unselectedColor = HabitColors.TextTertiary
            )
        )
    }
}

@Composable
internal fun ModernFilterRow(
    habits: List<HabitWithCompletions>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModernFilterChip(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all), selectedId == null) { onSelected(null) }
        habits.forEach { item ->
            ModernFilterChip(item.habit.name, selectedId == item.habit.id) { onSelected(item.habit.id) }
        }
    }
}

@Composable
private fun ModernFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .widthIn(max = 180.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) HabitColors.RoyalPurple else HabitColors.Surface)
            .border(1.dp, if (selected) HabitColors.RoyalPurple else HabitColors.OutlineStrong, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) HabitColors.OnRoyalPurple else HabitColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ModernMetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    subtitle: String
) {
    ModernSoftCard(modifier) {
        Column(Modifier.padding(15.dp)) {
            Text(label.uppercase(Locale.getDefault()), color = HabitColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Text(value, color = HabitColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = HabitColors.TextSecondary, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun ModernEmptyState(title: String, detail: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = HabitColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(detail, color = HabitColors.TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun ModernPrimaryFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(listOf(HabitColors.RoyalPurple, HabitColors.RoyalPurple2)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("＋", color = Color.White, fontSize = 20.sp)
        Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_new), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModernDayDetailSheet(
    date: LocalDate,
    today: LocalDate,
    habits: List<HabitWithCompletions>,
    onToggle: (Long, LocalDate, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scheduled = habits.filter { it.completionByDate.containsKey(date) }
    val done = scheduled.count { it.completionByDate[date] == true }
    val future = date.isAfter(today)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HabitColors.Background,
        contentColor = HabitColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 5.dp).size(width = 50.dp, height = 5.dp).background(HabitColors.TextSecondary, CircleShape))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
                color = HabitColors.TextPrimary,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (scheduled.isEmpty()) "Rest Day" else "$done of ${scheduled.size} completed",
                color = HabitColors.TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = HabitColors.Outline)
            if (scheduled.isEmpty()) {
                ModernEmptyState("Rest Day", "No habits were scheduled for this date.")
            } else {
                scheduled.forEachIndexed { index, item ->
                    val completed = item.completionByDate[date] == true
                    val isToday = date == today
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !future || completed) { onToggle(item.habit.id, date, completed) }
                            .padding(vertical = 14.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(item.habit.name, color = HabitColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                when {
                                    completed -> "Completed"
                                    future -> "Upcoming"
                                    isToday -> "To do"
                                    else -> "Missed"
                                },
                                color = when {
                                    completed -> HabitColors.CheckDone
                                    isToday -> HabitColors.RoyalPurple
                                    else -> HabitColors.TextSecondary
                                },
                                fontSize = 12.sp
                            )
                        }
                        ModernCheckButton(
                            completed = completed,
                            enabled = !future || completed,
                            onClick = { onToggle(item.habit.id, date, completed) },
                            contentDescription = if (completed) "Mark ${item.habit.name} as incomplete" else "Mark ${item.habit.name} as complete"
                        )
                    }
                    if (index != scheduled.lastIndex) HorizontalDivider(color = HabitColors.Outline)
                }
            }
        }
    }
}
