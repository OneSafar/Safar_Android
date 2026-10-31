package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.*
import com.safarparmar.app.R
import com.safarparmar.app.util.IstDateUtils
import java.time.LocalDate

@Composable
internal fun AssistOptionRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Checkbox(checked = selected, accessibilityLabel = title, onCheckedChange = { onClick() },
        modifier = Modifier.fillMaxWidth().background(if (selected) GoalsFlatColors.PrimarySoft else GoalsFlatColors.Paper,
            RoundedCornerShape(16.dp)).padding(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GoalsFlatColors.Text)
            Text(subtitle, color = GoalsFlatColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
internal fun ScheduledDatePickerRow(selectedDate: LocalDate, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.goals_when_activate), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GoalsFlatColors.Text)
        Button(onClick = onClick, style = ButtonStyle.Outlined, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp), tint = GoalsFlatColors.Primary)
            Spacer(Modifier.width(10.dp))
            Text(IstDateUtils.labelFor(selectedDate.toString()), fontSize = 14.sp, color = GoalsFlatColors.Text)
        }
        Text(stringResource(R.string.goals_activates_on, IstDateUtils.labelFor(selectedDate.toString())), fontSize = 12.sp, color = GoalsFlatColors.Muted)
    }
}

@Composable
internal fun GuideSection(title: String, items: List<String>) {
    Column(Modifier.fillMaxWidth().background(GoalsFlatColors.PrimarySoft, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GoalsFlatColors.Text)
        items.forEach { Text("• $it", fontSize = 12.sp, lineHeight = 18.sp, color = GoalsFlatColors.Muted) }
    }
}
