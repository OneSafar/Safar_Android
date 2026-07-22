package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.ChapterDifficulty
import com.safarparmar.app.ui.studyplanner.create.DeepFocusOutlineSubject

/**
 * Optional, skippable "Rate your chapters" step shown after Plan Settings for
 * manual/paste plans (templates arrive pre-weighted and never see this).
 * Ratings weight whole chapters for scheduling: tough chapters count as more,
 * easy ones as less. Everything left unrated stays normal.
 */
@Composable
internal fun ChapterRatingStep(
    outline: List<DeepFocusOutlineSubject>,
    ratings: Map<Pair<String, String>, String>,
    onRate: (subjectName: String, chapterName: String, difficulty: String?) -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val chapterCount = outline.sumOf { it.chapters.size }
                val ratedCount = ratings.size
                Column(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Tough chapters get more room in your schedule; easy ones get less.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                    // With 50-80 chapters in a big template, the screen must make
                    // it obvious that rating a handful is a complete action —
                    // otherwise it reads as 80 required decisions.
                    Text(
                        text = "Rate only the ones you find hard. The other " +
                            "${(chapterCount - ratedCount).coerceAtLeast(0)} stay Normal.",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                    )
                }
            }
            outline.forEach { subject ->
                item(key = "subject-${subject.name}") {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                subject.chapters.forEach { chapter ->
                    item(key = "chapter-${subject.name}-${chapter.name}") {
                        val selected = ratings[subject.name to chapter.name]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = chapter.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            ChapterDifficulty.entries.forEach { option ->
                                val isSelected = option.wireValue == selected
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onRate(
                                            subject.name,
                                            chapter.name,
                                            if (isSelected) null else option.wireValue,
                                        )
                                    },
                                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = ButtonDefaults.shape,
            ) {
                Text("Build my plan", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("Skip — keep everything normal")
            }
        }
    }
}
