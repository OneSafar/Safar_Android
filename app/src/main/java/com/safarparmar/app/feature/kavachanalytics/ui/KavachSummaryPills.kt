package com.safarparmar.app.feature.kavachanalytics.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.kavachanalytics.domain.DataCoverage
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * A compact, same-source snapshot of today's Kavach analytics.
 *
 * All three values come from the daily usage report. Keeping Ekagra timer history
 * out of this row avoids implying that focus time is a subset of phone usage.
 */
@Composable
fun KavachSummaryPills(
    modifier: Modifier = Modifier,
    ink: com.safarparmar.app.ui.ekagra.EkagraInk? = null,
    onClick: () -> Unit = {},
    viewModel: KavachAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var refreshed by remember { mutableStateOf(false) }
    val isLight = MaterialTheme.colorScheme.background.isLightBackground()

    LaunchedEffect(Unit) {
        if (!refreshed) {
            refreshed = true
            viewModel.refresh()
        }
    }

    val report = state.report
    val coverage = report?.coverage ?: DataCoverage.UNAVAILABLE
    val measured = state.hasUsageAccess && coverage != DataCoverage.UNAVAILABLE

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryPill(
            label = "Screen time",
            value = if (measured) {
                KavachAnalyticsFormat.duration(report?.allDay?.totalSeconds ?: 0)
            } else {
                "—"
            },
            icon = Icons.Outlined.Schedule,
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
        SummaryPill(
            label = "Distracting",
            value = if (measured) {
                KavachAnalyticsFormat.duration(report?.allDay?.distractingSeconds ?: 0)
            } else {
                "—"
            },
            icon = Icons.Outlined.WarningAmber,
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
        SummaryPill(
            label = "Blocked today",
            value = if (measured) "${report?.blockedAttempts ?: 0}" else "—",
            icon = Icons.Outlined.Shield,
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
    }
}

@Composable
private fun SummaryPill(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cardBg = Color.Black.copy(alpha = 0.22f)
    val borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    val titleTextColor = Color.White.copy(alpha = 0.85f)
    val valueTextColor = Color.White

    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = borderStroke,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleTextColor,
                    maxLines = 1,
                )
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = valueTextColor,
                maxLines = 1,
            )
        }
    }
}
