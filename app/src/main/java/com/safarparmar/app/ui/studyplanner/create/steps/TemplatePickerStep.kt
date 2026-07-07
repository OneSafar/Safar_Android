package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.ExamTemplate
import com.safarparmar.app.domain.model.studyplanner.ExamTemplateSummary
import com.safarparmar.app.ui.studyplanner.screens.PremiumPlannerGateCard

@Composable
fun TemplatePickerStep(
    templates: List<ExamTemplateSummary>,
    loadingTemplates: Boolean,
    selectedTemplateId: String?,
    templateDetail: ExamTemplate?,
    loadingTemplateDetail: Boolean,
    excludedTopicKeys: Set<Triple<Int, Int, Int>>,
    canUsePremiumPlannerFeatures: Boolean,
    onUpgrade: () -> Unit,
    onSelectTemplate: (String) -> Unit,
    onToggleTopic: (Int, Int, Int) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!canUsePremiumPlannerFeatures) {
            PremiumPlannerGateCard(
                title = "Templates are premium",
                body = "You can still create a custom plan manually for free.",
                action = "View Premium",
                onUpgrade = onUpgrade,
            )
        }

        if (selectedTemplateId == null || templateDetail == null) {
            Text("Choose an exam template", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            if (loadingTemplates) {
                CircularProgressIndicator()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(templates, key = { it.id }) { template ->
                        TemplateSummaryCard(
                            template = template,
                            loading = loadingTemplateDetail && selectedTemplateId == template.id,
                            onClick = { onSelectTemplate(template.id) },
                        )
                    }
                }
            }
        } else {
            Text(templateDetail.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Uncheck any topics you already know.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                templateDetail.subjects.forEachIndexed { subjectIndex, subject ->
                    item(key = "subject-$subjectIndex") {
                        Text(
                            subject.name,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    subject.chapters.forEachIndexed { chapterIndex, chapter ->
                        item(key = "chapter-$subjectIndex-$chapterIndex") {
                            Text(
                                chapter.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            )
                        }
                        items(
                            items = chapter.topics.withIndex().toList(),
                            key = { (topicIndex, _) -> "topic-$subjectIndex-$chapterIndex-$topicIndex" },
                        ) { (topicIndex, topicName) ->
                            val key = Triple(subjectIndex, chapterIndex, topicIndex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                                    .clickable { onToggleTopic(subjectIndex, chapterIndex, topicIndex) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = key !in excludedTopicKeys,
                                    onCheckedChange = { onToggleTopic(subjectIndex, chapterIndex, topicIndex) },
                                )
                                Text(topicName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onContinue,
                enabled = canUsePremiumPlannerFeatures,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TemplateSummaryCard(
    template: ExamTemplateSummary,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(template.name, fontWeight = FontWeight.Bold)
                val subtitle = listOfNotNull(
                    template.subjectCount?.let { "$it subjects" },
                    template.topicCount?.let { "$it topics" },
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
        }
    }
}
