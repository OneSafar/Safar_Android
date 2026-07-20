package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.ui.studyplanner.PlannerActions
import com.safarparmar.app.ui.studyplanner.logic.TopicRef
import com.safarparmar.app.ui.studyplanner.logic.flattenTopics

/**
 * Dedicated full-section screen for revision. Previously this lived as a hidden
 * sub-tab inside the crowded Home tab; it is now its own [PlannerSection.REVISION]
 * destination with a back affordance (Back returns to wherever the user came from,
 * e.g. Calendar or Progress, via the ViewModel's backDestination).
 */
@Composable
internal fun RevisionScreen(
    plan: StudyPlan,
    actions: PlannerActions,
    modifier: Modifier = Modifier,
) {
    val revisionRefs = remember(plan) {
        plan.flattenTopics()
            .filter { it.topic.status == TopicStatus.REVISION_NEEDED }
            .sortedWith(
                compareBy<TopicRef> {
                    it.topic.plannedDate?.take(10).orEmpty().ifBlank { "9999-99-99" }
                }.thenBy { it.topic.name.lowercase() },
            )
    }
    var editingRef by remember { mutableStateOf<TopicRef?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { actions.navigateBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Revision Tasks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Consolidate what you've learned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (revisionRefs.isNotEmpty()) {
                Text(
                    text = "${revisionRefs.size} total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }

        if (revisionRefs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🧠", fontSize = 36.sp)
                        Text(
                            text = "No Revision Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Topics marked for revision will show up here to help you consolidate your learning.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = revisionRefs,
                    key = { ref -> "revision_${ref.topic.id}" },
                    contentType = { "revisionTopic" },
                ) { ref ->
                    RevisionTopicCard(
                        ref = ref,
                        onCompleteSession = { date -> actions.completeRevisionSession(ref.topic.id, date) },
                        onUncompleteSession = { date -> actions.uncompleteRevisionSession(ref.topic.id, date) },
                        onEdit = { editingRef = ref },
                    )
                }
            }
        }
    }

    editingRef?.let { ref ->
        RevisionScheduleSheet(
            topicName = ref.topic.name,
            examDate = plan.examDate,
            onRevisionScheduled = { dates, scheduleType ->
                actions.markForRevision(ref.topic.id, dates, scheduleType)
            },
            onDismiss = { editingRef = null },
            isAlreadyRevisionNeeded = ref.topic.status == TopicStatus.REVISION_NEEDED,
            onCancelRevision = { actions.cancelRevision(ref.topic.id) },
        )
    }
}
