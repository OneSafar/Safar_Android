package com.safar.app.ui.studyplanner.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.domain.model.studyplanner.PlanProgress
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.ui.studyplanner.components.ExamDaysCountdownBadge
import com.safar.app.ui.studyplanner.logic.daysUntil
import com.safar.app.ui.studyplanner.logic.readableDate

@Composable
fun PlanHeaderBanner(
    plan: StudyPlan,
    progress: PlanProgress,
    todayCount: Int,
    overdueCount: Int,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PlanShapes.banner)
            .background(scheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    ExamDaysCountdownBadge(days = daysUntil(plan.examDate))
                }
                Text(
                    text = readableDate(plan.examDate).takeIf { it.isNotBlank() } ?: "Set exam date",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${progress.completionPercent}% complete",
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$overdueCount overdue",
                        fontSize = 11.sp,
                        color = if (overdueCount > 0) scheme.error else scheme.onSurfaceVariant,
                        fontWeight = if (overdueCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$todayCount today",
                    fontSize = 10.sp,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
            IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = "Export",
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress.completionPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(99.dp)),
            color = scheme.primary,
            trackColor = scheme.surfaceVariant,
        )
    }
}
