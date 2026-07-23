package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalDate

@Composable
internal fun AssistOptionRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 5.dp else 1.5.dp,
                    color = if (selected) GoalsFlatColors.Primary else GoalsFlatColors.Hairline,
                    shape = CircleShape,
                ),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (selected) GoalsFlatColors.Primary else GoalsFlatColors.Text,
            )
            Text(subtitle, color = GoalsFlatColors.Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun ScheduledDatePickerRow(selectedDate: LocalDate, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "When should this activate?",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoalsFlatColors.Text,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, GoalsFlatColors.Hairline, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GoalsFlatColors.Scheduled,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    IstDateUtils.labelFor(selectedDate.toString()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoalsFlatColors.Text,
                )
            }
        }
        Text(
            "This goal will activate on ${IstDateUtils.labelFor(selectedDate.toString())}",
            fontSize = 12.sp,
            color = GoalsFlatColors.Muted,
        )
    }
}

@Composable
internal fun GuideSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoalsFlatColors.Text)
        items.forEach { item ->
            Text("- $item", fontSize = 12.sp, color = GoalsFlatColors.Muted)
        }
    }
}
