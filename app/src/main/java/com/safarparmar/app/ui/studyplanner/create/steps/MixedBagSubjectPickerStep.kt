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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Mixed Bag's "split focus" prompt — shown immediately when the user picks Mixed
 * Bag, the same way Deep Focus immediately opens its own reorder screen. Choosing
 * 2-3 subjects here means those subjects get topics every study day, while every
 * other subject rotates in one at a time on alternate days; the split is entirely
 * optional (Skip keeps the plain interleaved mix with no subject singled out).
 */
@Composable
fun MixedBagSubjectPickerStep(
    subjectNames: List<String>,
    onConfirm: (List<String>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(subjectNames) { mutableStateOf(setOf<String>()) }
    val maxSelectable = 3

    Column(
        modifier = modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Choose your 2 or 3 most difficult subjects.",
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "These get a topic every single day. Everything else still gets covered — " +
                "just one subject at a time, rotating in on alternate days.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(subjectNames, key = { it }) { name ->
                val isSelected = name in selected
                val disabled = !isSelected && selected.size >= maxSelectable
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !disabled) {
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
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
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

        Button(
            onClick = { onConfirm(selected.toList()) },
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
