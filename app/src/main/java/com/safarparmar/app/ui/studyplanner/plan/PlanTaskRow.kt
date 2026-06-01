package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.logic.TopicRef

enum class PlanTaskRowAccent {
    Overdue,
    Planned,
    Done,
}

@Composable
fun PlanTaskRow(
    ref: TopicRef,
    accent: PlanTaskRowAccent,
    showDivider: Boolean,
    onClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dotColor = when (accent) {
        PlanTaskRowAccent.Overdue -> scheme.error
        PlanTaskRowAccent.Done -> MaterialTheme.colorScheme.tertiary
        PlanTaskRowAccent.Planned -> scheme.outline
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = PlanSpacing.horizontal, vertical = PlanSpacing.rowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
            ) {
                Text(
                    text = ref.topic.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface,
                )
                Text(
                    text = "${ref.subject.name} • ${ref.chapter.name}",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val noteText = ref.topic.notes?.trim().orEmpty()
                if (noteText.isNotEmpty()) {
                    Text(
                        text = noteText,
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Checkbox(
                checked = ref.topic.status == TopicStatus.DONE,
                onCheckedChange = { checked -> if (checked) onDone() },
                modifier = Modifier.size(40.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = scheme.tertiary,
                    checkmarkColor = scheme.onTertiary,
                ),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = PlanSpacing.horizontal),
                color = scheme.outline.copy(alpha = 0.08f),
            )
        }
    }
}

fun planTaskRowAccent(ref: TopicRef, isOverdue: Boolean): PlanTaskRowAccent = when {
    ref.topic.status == TopicStatus.DONE -> PlanTaskRowAccent.Done
    isOverdue -> PlanTaskRowAccent.Overdue
    else -> PlanTaskRowAccent.Planned
}
