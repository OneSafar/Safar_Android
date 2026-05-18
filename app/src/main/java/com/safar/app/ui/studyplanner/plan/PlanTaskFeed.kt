package com.safar.app.ui.studyplanner.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.ui.studyplanner.logic.TopicRef

@Composable
fun PlanTaskFeed(
    todayTopics: List<TopicRef>,
    overdueTopics: List<TopicRef>,
    upcomingTopics: List<TopicRef>,
    todayExpanded: Boolean,
    onToggleToday: () -> Unit,
    upcomingExpanded: Boolean,
    onToggleUpcoming: () -> Unit,
    onTopicClick: (TopicRef) -> Unit,
    onTopicDone: (TopicRef) -> Unit,
    onAddTopics: () -> Unit,
    onSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlanSpacing.horizontal, vertical = PlanSpacing.section),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onAddTopics,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Add Topics", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(
                onClick = onSchedule,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Schedule", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            item {
                PlanSectionHeader(
                    title = "Today's focus",
                    subtitle = "${todayTopics.size} planned",
                    expanded = todayExpanded,
                    onToggle = onToggleToday,
                )
            }
            if (todayExpanded) {
                if (todayTopics.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlanSpacing.horizontal, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "No topics planned today",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = onSchedule) {
                                Text("Schedule topics")
                            }
                        }
                    }
                } else {
                    itemsIndexed(todayTopics, key = { _, ref -> "today_${ref.topic.id}" }) { index, ref ->
                        PlanTaskRow(
                            ref = ref,
                            accent = planTaskRowAccent(ref, isOverdue = false),
                            showDivider = index < todayTopics.lastIndex,
                            onClick = { onTopicClick(ref) },
                            onDone = { onTopicDone(ref) },
                        )
                    }
                }
            }

            if (overdueTopics.isNotEmpty()) {
                item {
                    PlanSectionHeader(
                        title = "Overdue",
                        subtitle = "${overdueTopics.size} pending",
                        expanded = true,
                        onToggle = null,
                    )
                }
                itemsIndexed(overdueTopics, key = { _, ref -> "overdue_${ref.topic.id}" }) { index, ref ->
                    PlanTaskRow(
                        ref = ref,
                        accent = PlanTaskRowAccent.Overdue,
                        showDivider = index < overdueTopics.lastIndex,
                        onClick = { onTopicClick(ref) },
                        onDone = { onTopicDone(ref) },
                    )
                }
            }

            if (upcomingTopics.isNotEmpty()) {
                item {
                    PlanSectionHeader(
                        title = "Upcoming",
                        subtitle = "Next topics",
                        expanded = upcomingExpanded,
                        onToggle = onToggleUpcoming,
                    )
                }
                if (upcomingExpanded) {
                    itemsIndexed(upcomingTopics, key = { _, ref -> "upcoming_${ref.topic.id}" }) { index, ref ->
                        PlanTaskRow(
                            ref = ref,
                            accent = PlanTaskRowAccent.Planned,
                            showDivider = index < upcomingTopics.lastIndex,
                            onClick = { onTopicClick(ref) },
                            onDone = { onTopicDone(ref) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSectionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggle != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggle)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = PlanSpacing.horizontal, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onToggle != null) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = scheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onToggle != null) 4.dp else 0.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}
