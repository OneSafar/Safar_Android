package com.safar.app.ui.studyplanner.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

private val dayIndices = 0..6
private val dayLabelsFull = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val dayLabelsShort = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun PlanRestDaysRow(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "My Rest Days",
            fontSize = 12.sp,
            color = scheme.onSurfaceVariant,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useShort = maxWidth < 320.dp
            val labels = if (useShort) dayLabelsShort else dayLabelsFull
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dayIndices.forEach { index ->
                    val isSelected = index in selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) scheme.secondaryContainer
                                else scheme.surface,
                            )
                            .clickable { onToggle(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labels[index],
                            fontSize = if (useShort) 11.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
