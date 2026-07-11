package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.safarparmar.app.domain.model.studyplanner.DailyTodo
import java.util.UUID

/** Full-screen step between calendar review and plan confirmation. */
@Composable
fun DailyTopicsStep(
    isSaving: Boolean,
    error: String?,
    onContinue: (List<DailyTodo>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var taskName by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf(emptyList<DailyTodo>()) }

    fun addTopic() {
        val name = taskName.trim()
        if (name.isEmpty() || topics.any { it.name.equals(name, ignoreCase = true) }) return
        topics = topics + DailyTodo(id = UUID.randomUUID().toString(), name = name)
        taskName = ""
    }

    Column(
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Add daily topics?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Add topics you want to do every day. You'll see them on Home.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Revise vocabulary") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            IconButton(
                onClick = ::addTopic,
                enabled = taskName.isNotBlank() && !isSaving,
                modifier = Modifier.size(56.dp).background(
                    if (taskName.isNotBlank()) scheme.primary else scheme.surfaceVariant,
                    RoundedCornerShape(14.dp),
                ),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add daily topic",
                    tint = if (taskName.isNotBlank()) scheme.onPrimary else scheme.onSurfaceVariant,
                )
            }
        }

        if (topics.isEmpty()) {
            Text(
                text = "No topics yet.",
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                textAlign = TextAlign.Center,
                color = scheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(topics, key = { it.id }) { topic ->
                    Surface(shape = RoundedCornerShape(14.dp), color = scheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(topic.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { topics = topics - topic }, enabled = !isSaving) {
                                Icon(Icons.Default.Remove, contentDescription = "Remove ${topic.name}", tint = scheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (topics.isEmpty()) Spacer(Modifier.weight(1f))
        error?.let { Text(it, color = scheme.error, style = MaterialTheme.typography.bodyMedium) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onContinue(emptyList()) },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) { Text("Not now") }
            Button(
                onClick = { onContinue(topics) },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(if (topics.isEmpty()) "Continue" else "Save topics")
            }
        }
    }
}
