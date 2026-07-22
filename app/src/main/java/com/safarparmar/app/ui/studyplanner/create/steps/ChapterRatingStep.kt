package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.theme.isLightBackground
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.domain.model.studyplanner.ChapterDifficulty
import com.safarparmar.app.ui.studyplanner.components.TopicEffortBars
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import com.safarparmar.app.ui.studyplanner.create.DeepFocusOutlineSubject
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

/**
 * Optional, skippable "Rate your chapters" step shown after Plan Settings for
 * manual/paste plans. Redesigned using the Ekagra / Exam Planner flat hairline recipe.
 */
@Composable
internal fun ChapterRatingStep(
    outline: List<DeepFocusOutlineSubject>,
    ratings: Map<Pair<String, String>, String>,
    onRate: (subjectName: String, chapterName: String, difficulty: String?) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    studyStyle: String = "balanced",
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = "Rate your chapters",
            fontFamily = LoraFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Normal,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val chapterCount = outline.sumOf { it.chapters.size }
                val ratedCount = ratings.size
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Only rate the hard ones. " +
                            "${(chapterCount - ratedCount).coerceAtLeast(0)} stay Normal.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }

            outline.forEach { subject ->
                item(key = "subject-${subject.name}") {
                    Column {
                        PlanHairline(alpha = 0.6f)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = subject.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                subject.chapters.forEach { chapter ->
                    item(key = "chapter-${subject.name}-${chapter.name}") {
                        val selected = ratings[subject.name to chapter.name]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = chapter.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = scheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ChapterDifficulty.entries.forEach { option ->
                                    val isSelected = option.wireValue == selected
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(accent)
                                                } else {
                                                    Modifier.border(
                                                        1.dp,
                                                        scheme.outlineVariant.copy(alpha = 0.5f),
                                                        CircleShape,
                                                    )
                                                },
                                            )
                                            .clickable {
                                                onRate(
                                                    subject.name,
                                                    chapter.name,
                                                    if (isSelected) null else option.wireValue,
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                      Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                      ) {
                                        Text(
                                            text = option.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else scheme.onSurfaceVariant,
                                        )
                                        // Weight shown on the control itself, so
                                        // no sentence above has to explain that
                                        // Tough means "takes more room".
                                        TopicEffortBars(
                                            size = when (option) {
                                                ChapterDifficulty.EASY -> TopicSize.SMALL
                                                ChapterDifficulty.NORMAL -> TopicSize.MEDIUM
                                                ChapterDifficulty.TOUGH -> TopicSize.BIG
                                            },
                                            activeColor = if (isSelected) Color.White else scheme.onSurfaceVariant,
                                            inactiveColor = if (isSelected) {
                                                Color.White.copy(alpha = 0.3f)
                                            } else {
                                                scheme.outlineVariant.copy(alpha = 0.5f)
                                            },
                                        )
                                      }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        PlanHairline()
        Spacer(Modifier.height(12.dp))

        val isLight = scheme.background.isLightBackground()
        val styleAccent = when (studyStyle) {
            "deep_focus" -> PlannerAccent.Coral
            "mixed_bag" -> PlannerAccent.Teal
            "balanced" -> PlannerAccent.Amber
            else -> null
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MacOSPrimaryActionButton(
                text = "Build my plan",
                onClick = onContinue,
                isLight = isLight,
                customAccent = styleAccent,
            )
            Text(
                text = "Skip — keep everything normal",
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

