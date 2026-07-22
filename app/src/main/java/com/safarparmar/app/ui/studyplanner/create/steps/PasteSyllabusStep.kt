package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safarparmar.app.data.remote.api.StructuredSyllabusPreview

@Composable
fun PasteSyllabusStep(
    pasteText: String,
    onPasteTextChange: (String) -> Unit,
    isStructuring: Boolean,
    structureError: String?,
    structuredPreview: StructuredSyllabusPreview?,
    onStructure: () -> Unit,
    onTryAgain: () -> Unit,
    onBuildManually: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (structuredPreview == null) {
            Text(
                "Copy text from a PDF, website, notes, or any source.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pasteText,
                onValueChange = onPasteTextChange,
                minLines = 8,
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Paste syllabus") },
            )
            if (isStructuring) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
            } else if (structureError != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "We could not read enough syllabus text.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Try pasting clearer text, or build manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onTryAgain) { Text("Try Again") }
                            OutlinedButton(onClick = onBuildManually) { Text("Build Manually") }
                        }
                    }
                }
            } else {
                Button(
                    onClick = onStructure,
                    enabled = pasteText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Organize syllabus", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text("We organized your syllabus.", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Please check once before building the plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                structuredPreview.subjects.forEach { subject ->
                    // No keys: a pasted syllabus repeats subject/chapter names freely, and
                    // duplicate keys crash the list while it measures. This is a static
                    // preview (no reorder/animateItem), so the default index keys are fine.
                    item {
                        Text(subject.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(subject.chapters) { chapter ->
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(chapter.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            chapter.topics.forEach { topic ->
                                Text(topic, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Looks correct", fontWeight = FontWeight.Bold)
            }
        }
    }
}
