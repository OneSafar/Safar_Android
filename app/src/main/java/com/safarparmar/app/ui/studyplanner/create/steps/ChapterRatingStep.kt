package com.safarparmar.app.ui.studyplanner.create.steps

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import com.safarparmar.app.ui.glass.MacOSPrimaryActionButton
import com.safarparmar.app.ui.studyplanner.components.PlannerAccent
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Optional, skippable "Rate your chapters" step shown after Plan Settings for
 * manual/paste plans. Redesigned using the Ekagra / Exam Planner flat hairline recipe.
 */
@Composable
internal fun ChapterRatingStep(
    outline: List<DeepFocusOutlineSubject>,
    ratings: Map<Pair<String, String>, String>,
    onRate: (subjectName: String, chapterName: String, difficulty: String?) -> Unit,
    onContinue: () -> Unit,
    /** "Same number every day" — every chapter Normal, so each day holds exactly
     *  the student's number. */
    onBuildEven: () -> Unit = {},
    dailyGoal: Int = 0,
    modifier: Modifier = Modifier,
    studyStyle: String = "balanced",
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary

    // Null until the student picks how their days should be built. The effort-point
    // maths behind all this is deliberately never named on screen — they choose
    // between "every day the same" and "go by how hard it is", nothing more.
    var goByDifficulty by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = if (goByDifficulty == true) "Rate your chapters" else "How should we plan your days?",
            fontFamily = LoraFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Normal,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))

        if (goByDifficulty == null) {
            val n = if (dailyGoal > 0) dailyGoal else 3
            // The two options ARE the screen: one fills the top half, one the
            // bottom, so each is a full tap target and neither reads as secondary.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlanChoiceCard(
                    title = "Same every day",
                    lines = listOf(
                        "Every day has exactly $n topics." to null,
                        "Hard or easy, the number never changes." to null,
                        "Choose this if you want a fixed routine." to null,
                    ),
                    onClick = onBuildEven,
                    modifier = Modifier.weight(1f),
                ) {
                    // Every row identical — that IS the promise.
                    repeat(3) { day ->
                        DayBar("Day ${day + 1}", n, PlannerAccent.Teal)
                    }
                }
                PlanChoiceCard(
                    title = "Go by how hard it is",
                    lines = listOf(
                        "Hard topics" to ChapterDifficulty.TOUGH,
                        "  fewer in a day, so you get time for them." to null,
                        "Easy topics" to ChapterDifficulty.EASY,
                        "  more in a day, because they go fast." to null,
                        "Every day feels the same amount of work." to null,
                    ),
                    onClick = { goByDifficulty = true },
                    modifier = Modifier.weight(1f),
                ) {
                    // Same total width on every row: the bar is always "one day of
                    // work". Only the number of pieces changes with difficulty, so
                    // "same effort, different counts" is visible without a word
                    // about points.
                    DayBar("Day 1", (n / 2).coerceAtLeast(1), difficultyColor(ChapterDifficulty.TOUGH))
                    DayBar("Day 2", n, difficultyColor(ChapterDifficulty.NORMAL))
                    DayBar("Day 3", n * 2, difficultyColor(ChapterDifficulty.EASY))
                }
            }
            return@Column
        }

        val requiredGoal = remember(ratings, outline) {
            val totalPoints = outline.sumOf { subject ->
                subject.chapters.sumOf { chapter ->
                    val diffStr = ratings[subject.name to chapter.name] ?: ChapterDifficulty.NORMAL.wireValue
                    val points = when (diffStr) {
                        ChapterDifficulty.EASY.wireValue -> 1
                        ChapterDifficulty.TOUGH.wireValue -> 4
                        else -> 2
                    }
                    chapter.topicNames.size * points
                }
            }
            val estDays = 60
            val reqPoints = Math.ceil(totalPoints.toDouble() / estDays.toDouble()).toInt()
            Math.ceil(reqPoints.toDouble() / 2.0).toInt().coerceAtLeast(1)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val chapterCount = outline.sumOf { it.chapters.size }
                val ratedCount = ratings.size
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (requiredGoal > dailyGoal && dailyGoal > 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PlannerAccent.Amber.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, PlannerAccent.Amber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "⚡ SMART EFFORT ADJUSTMENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PlannerAccent.Amber,
                                )
                                Text(
                                    text = "Daily goal set to $requiredGoal topics/day",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface,
                                )
                                Text(
                                    text = "Because of your Tough chapters, we automatically adjusted your daily effort from $dailyGoal to $requiredGoal topics/day so all topics finish before your exam.",
                                    fontSize = 12.5.sp,
                                    color = scheme.onSurfaceVariant,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }

                    Text(
                        text = if (ratedCount > 0) {
                            "We rated these for you. Change any you disagree with. " +
                                "${(chapterCount - ratedCount).coerceAtLeast(0)} are Normal."
                        } else {
                            "Only rate the hard ones. " +
                                "${(chapterCount - ratedCount).coerceAtLeast(0)} stay Normal."
                        },
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
        val sourceAccent = PlannerFlatColors.PrimaryAccent

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val requiredGoal = remember(ratings, outline) {
                val totalPoints = outline.sumOf { subject ->
                    subject.chapters.sumOf { chapter ->
                        val diffStr = ratings[subject.name to chapter.name] ?: ChapterDifficulty.NORMAL.wireValue
                        val points = when (diffStr) {
                            ChapterDifficulty.EASY.wireValue -> 1
                            ChapterDifficulty.TOUGH.wireValue -> 4
                            else -> 2
                        }
                        chapter.topicNames.size * points
                    }
                }
                val estDays = 60
                val reqPoints = Math.ceil(totalPoints.toDouble() / estDays.toDouble()).toInt()
                Math.ceil(reqPoints.toDouble() / 2.0).toInt().coerceAtLeast(1)
            }

            if (goByDifficulty == true && requiredGoal > dailyGoal && dailyGoal > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PlannerAccent.Amber.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "💡 Adjusted for your Tough chapters",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                        )
                        Text(
                            text = "To finish all topics before your exam, your daily goal is set to $requiredGoal topics/day.",
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            MacOSPrimaryActionButton(
                text = "Build my plan",
                onClick = onContinue,
                isLight = isLight,
                customAccent = sourceAccent,
            )
        }
    }
}

/** Difficulty colours, shared with the rating chips so the same idea always
 *  wears the same colour: tough = coral, easy = teal, normal = amber. */
@Composable
private fun difficultyColor(difficulty: ChapterDifficulty): Color = when (difficulty) {
    ChapterDifficulty.TOUGH -> PlannerAccent.Coral
    ChapterDifficulty.NORMAL -> PlannerAccent.Amber
    ChapterDifficulty.EASY -> PlannerAccent.Teal
}

/**
 * One of the two ways to build the days. Deliberately wordless about effort
 * points — the student sees a picture of their days plus short plain sentences,
 * with the words "Hard" and "Easy" carrying the same colour as the rating chips.
 */
@Composable
private fun PlanChoiceCard(
    title: String,
    lines: List<Pair<String, ChapterDifficulty?>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    demo: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp),
        // Centred as a group: the card is now half the screen, so top-aligned
        // content would leave a large dead gap underneath.
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
        )
        lines.forEach { (text, difficulty) ->
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (difficulty != null) FontWeight.Bold else FontWeight.Normal,
                color = difficulty?.let { difficultyColor(it) } ?: scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { demo() }
    }
}

/**
 * One day drawn as a fixed-width bar cut into [chips] pieces. The bar is always
 * the same length — it represents one day of work — so more pieces simply means
 * smaller topics. That is the whole weighting idea, shown rather than explained.
 */
@Composable
private fun DayBar(dayLabel: String, chips: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dayLabel,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp),
        )
        Row(
            // Fixed width + weighted children: the row always spans the same
            // distance, and the pieces divide it evenly however many there are.
            modifier = Modifier.width(168.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Past ~10 pieces the chips stop being legible and the point is made.
            repeat(chips.coerceIn(1, 10)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
                )
            }
        }
    }
}
