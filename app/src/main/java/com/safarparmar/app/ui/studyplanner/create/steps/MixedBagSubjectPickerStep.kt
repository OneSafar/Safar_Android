package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Mixed Bag's subject picker — shown immediately when the user picks Mixed
 * Bag, the same way Deep Focus immediately opens its own reorder screen.
 *
 * The 2-3 subjects chosen here are scheduled EXCLUSIVELY first; every other
 * subject only starts once these run out. Selection is ordered (tapping appends,
 * and each pick shows its 1/2/3 rank) because that order drives the schedule
 * when "In my order" is chosen. Skip keeps the plain even mix with no subject
 * singled out.
 */
@Composable
fun MixedBagSubjectPickerStep(
    subjectNames: List<String>,
    onConfirm: (names: List<String>, orderMode: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A List, not a Set — pick order is meaningful and must survive to the server.
    var selected by remember(subjectNames) { mutableStateOf(listOf<String>()) }
    var orderMode by remember(subjectNames) { mutableStateOf("sequential") }
    val maxSelectable = 3

    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Pick your 2-3 hardest subjects.",
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "These get scheduled first, one after another, until they're covered. Your other subjects start after that. Tap in the order you want to study them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // No key: a syllabus can repeat a subject name, and duplicate keys crash the
            // list while it measures. There is no reorder/animateItem here, so the default
            // index key costs nothing.
            items(subjectNames) { name ->
                val pickIndex = selected.indexOf(name)
                val isSelected = pickIndex >= 0
                val disabled = !isSelected && selected.size >= maxSelectable
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !disabled) {
                            // Removing a pick renumbers the rest, so ranks stay 1..n.
                            selected = if (isSelected) selected - name else selected + name
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            fontWeight = FontWeight.Bold,
                            color = if (disabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            // Rank, not a tick — the number is the whole point of
                            // an ordered pick.
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${pickIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            "${selected.size} of $maxSelectable selected",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // How the chosen subjects are ordered *among themselves*. Either way
        // they still come before every other subject.
        if (selected.size >= 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MixedBagOrderChip(
                    label = "In my order",
                    detail = selected.joinToString(" → "),
                    selected = orderMode == "sequential",
                    onClick = { orderMode = "sequential" },
                    modifier = Modifier.weight(1f),
                )
                MixedBagOrderChip(
                    label = "Mix them together",
                    detail = "A bit of each, every day",
                    selected = orderMode == "balanced",
                    onClick = { orderMode = "balanced" },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Button(
            onClick = { onConfirm(selected.toList(), orderMode) },
            enabled = selected.size in 2..maxSelectable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Confirm", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Skip — keep an even mix")
        }
    }
}

@Composable
private fun MixedBagOrderChip(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) scheme.primaryContainer.copy(alpha = 0.5f) else scheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) scheme.primary else scheme.onSurface,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
