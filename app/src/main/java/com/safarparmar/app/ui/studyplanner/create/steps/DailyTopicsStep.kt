package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.DailyTodo
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
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
    val isLight = scheme.background.isLightBackground()
    val accent = PlannerFlatColors.PrimaryAccent
    val ink = scheme.onSurface
    val muted = scheme.onSurfaceVariant
    var taskName by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf(emptyList<DailyTodo>()) }

    fun addTopic() {
        val name = taskName.trim()
        if (name.isEmpty() || topics.any { it.name.equals(name, ignoreCase = true) }) return
        topics = topics + DailyTodo(id = UUID.randomUUID().toString(), name = name)
        taskName = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PlanEyebrow("Daily routine")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add a daily routine",
            fontFamily = LoraFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Normal,
            color = ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add topics you want to do every day. You'll see them on Home.",
            fontSize = 13.5.sp,
            color = muted,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(18.dp))
        PlanHairline()
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = taskName,
                onValueChange = { taskName = it },
                placeholder = { Text("e.g. Revise vocabulary", color = muted) },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = PlannerFlatColors.BorderSoft,
                    focusedTextColor = ink,
                    unfocusedTextColor = ink,
                    cursorColor = accent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
            val canAdd = taskName.isNotBlank() && !isSaving
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (canAdd) accent else scheme.outlineVariant.copy(alpha = 0.45f))
                    .clickable(
                        enabled = canAdd,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::addTopic,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add daily topic",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (topics.isEmpty()) {
            Text(
                text = "No topics yet.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = muted,
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(topics, key = { it.id }) { topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(14.dp))
                            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = topic.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = ink,
                        )
                        IconButton(
                            onClick = { topics = topics - topic },
                            enabled = !isSaving,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove ${topic.name}",
                                tint = muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = scheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(12.dp))
        PlanHairline()
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .clickable(enabled = !isSaving) { onContinue(emptyList()) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Not now",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            MacOSPrimaryActionButton(
                text = if (topics.isEmpty()) "Continue" else "Save topics",
                onClick = { onContinue(topics) },
                enabled = !isSaving,
                isLoading = isSaving,
                isLight = isLight,
                customAccent = accent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
