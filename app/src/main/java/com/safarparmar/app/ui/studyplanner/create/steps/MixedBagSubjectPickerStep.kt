package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.PlannerDialog
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogAction
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogText
import com.safarparmar.app.ui.theme.isLightBackground
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
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * Mixed Bag's subject picker — redesigned using the flat hairline recipe from
 * Ekagra and Exam Planner Home.
 */
@Composable
fun MixedBagSubjectPickerStep(
    subjectNames: List<String>,
    onConfirm: (names: List<String>, orderMode: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(subjectNames) { mutableStateOf(listOf<String>()) }
    var orderMode by remember(subjectNames) { mutableStateOf("sequential") }
    val maxSelectable = 3
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Pick your 2-3 hardest subjects",
            fontFamily = LoraFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = scheme.onSurface,
        )
        Text(
            "These get scheduled first, one after another. Your other subjects start after that. Tap in the order you want to study them.",
            fontSize = 13.sp,
            color = scheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subjectNames) { name ->
                val pickIndex = selected.indexOf(name)
                val isSelected = pickIndex >= 0
                val disabled = !isSelected && selected.size >= maxSelectable

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !disabled) {
                            selected = if (isSelected) selected - name else selected + name
                        },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                disabled -> scheme.onSurface.copy(alpha = 0.35f)
                                isSelected -> accent
                                else -> scheme.onSurface
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${pickIndex + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    PlanHairline(alpha = 0.5f)
                }
            }
        }

        Text(
            "${selected.size} of $maxSelectable selected",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
        )

        if (selected.size >= 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MixedBagOrderOption(
                    label = "In my order",
                    info = "Finish the first subject you picked completely. Then start the next one. Keep going in the same order you tapped them.",
                    selected = orderMode == "sequential",
                    accent = accent,
                    scheme = scheme,
                    onClick = { orderMode = "sequential" },
                    modifier = Modifier.weight(1f),
                )
                MixedBagOrderOption(
                    label = "Mix them together",
                    info = "Each day you study a little bit of every subject you picked — mixed together, not one after another.",
                    selected = orderMode == "balanced",
                    accent = accent,
                    scheme = scheme,
                    onClick = { orderMode = "balanced" },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        val isLight = scheme.background.isLightBackground()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val canConfirm = selected.size in 2..maxSelectable
            MacOSPrimaryActionButton(
                text = "Confirm",
                onClick = { onConfirm(selected.toList(), orderMode) },
                enabled = canConfirm,
                isLight = isLight,
                customAccent = PlannerFlatColors.PrimaryAccent,
            )
            Text(
                text = "Skip — keep an even mix",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onSkip() }
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun MixedBagOrderOption(
    label: String,
    info: String,
    selected: Boolean,
    accent: Color,
    scheme: androidx.compose.material3.ColorScheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(accent.copy(alpha = 0.14f))
                else Modifier.border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else scheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/**
 * The two-phase schedule, drawn. [chosen] subjects run first and only then does
 * everything else begin — a rule that took three sentences to describe and one
 * strip to show. Updates live as the student taps, so the consequence of a pick
 * is visible at the moment of picking.
 */
@Composable
private fun PhaseStrip(chosen: List<String>, rest: List<String>) {
    if (chosen.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PhaseRow(label = "First", names = chosen, accent = scheme.primary, scheme = scheme)
        if (rest.isNotEmpty()) {
            PhaseRow(label = "Then", names = rest, accent = scheme.onSurfaceVariant, scheme = scheme)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhaseRow(label: String, names: List<String>, accent: Color, scheme: ColorScheme) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier.width(34.dp).padding(top = 3.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            names.forEach { name ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
