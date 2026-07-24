package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.DailyTodo
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.glassSurface
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import java.util.UUID

/** Full-screen step between calendar review and plan confirmation. */
@Composable
fun DailyTopicsStep(
    isSaving: Boolean,
    error: String?,
    onContinue: (List<DailyTodo>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isPlannerDark
    val accent = PlannerFlatColors.PrimaryAccent
    val ink = PlannerFlatColors.TextDark
    val muted = PlannerFlatColors.TextMuted
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
        PlanEyebrow("Daily topics")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add daily topics?",
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .glassSurface(shape = RoundedCornerShape(16.dp), isDarkTheme = isDark)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                if (taskName.isEmpty()) {
                    Text(
                        text = "e.g. Revise vocabulary",
                        fontSize = 14.sp,
                        color = muted,
                    )
                }
                BasicTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        color = ink,
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
            }
            GlassButton(
                onClick = ::addTopic,
                accentColor = if (taskName.isNotBlank()) accent else muted,
                enabled = taskName.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(16.dp),
                isDarkTheme = isDark,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.size(52.dp),
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
                            .glassSurface(shape = RoundedCornerShape(14.dp), isDarkTheme = isDark)
                            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = topic.name,
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = ink,
                        )
                        GlassButton(
                            onClick = { topics = topics - topic },
                            accentColor = muted,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            isDarkTheme = isDark,
                            contentPadding = PaddingValues(8.dp),
                            tintTopAlpha = if (isDark) 0.28f else 0.22f,
                            tintBottomAlpha = if (isDark) 0.14f else 0.10f,
                            greyShadeAlpha = 0.08f,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove ${topic.name}",
                                tint = ink,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFDC2626), fontSize = 13.sp)
        }

        Spacer(Modifier.height(12.dp))
        PlanHairline()
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassButton(
                onClick = { onContinue(emptyList()) },
                accentColor = muted,
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                isDarkTheme = isDark,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                tintTopAlpha = if (isDark) 0.22f else 0.16f,
                tintBottomAlpha = if (isDark) 0.10f else 0.08f,
                greyShadeAlpha = if (isDark) 0.18f else 0.12f,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Not now",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = ink,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            GlassButton(
                onClick = { onContinue(topics) },
                accentColor = accent,
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                isDarkTheme = isDark,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = if (topics.isEmpty()) "Continue" else "Save topics",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
