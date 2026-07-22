package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.studyplanner.TopicSize

/**
 * A topic's effort, drawn as three rising bars instead of spelled out.
 *
 * The whole planner now weights topics — but "Big" written on a card is a word
 * the student has to read, translate and remember. Three bars where a heavier
 * topic is visibly taller needs no reading at all, works the same for a student
 * whose English is shaky, and stays legible at 10sp inside a dense list.
 *
 * Small = 1 bar, Medium = 2, Big = 3. (Effort points are 1/2/4; the bars are a
 * rank, not a scale — three ascending marks read faster than four.)
 */
@Composable
fun TopicEffortBars(
    size: TopicSize,
    modifier: Modifier = Modifier,
    activeColor: Color = PlannerFlatColors.PrimaryAccent,
    inactiveColor: Color = PlannerFlatColors.BorderSoft,
    /** Larger variant for rows where the bars are the only trailing element. */
    large: Boolean = false,
) {
    val filled = when (size) {
        TopicSize.SMALL -> 1
        TopicSize.MEDIUM -> 2
        TopicSize.BIG -> 3
    }
    Row(
        modifier = modifier.semantics { contentDescription = "${size.label} topic" },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(if (large) 2.dp else 1.5.dp),
    ) {
        // Ascending heights carry the meaning even in greyscale, so the bars
        // survive colour-blindness and the app's light/dark themes alike.
        val heights = if (large) listOf(6.dp, 10.dp, 14.dp) else listOf(4.dp, 7.dp, 10.dp)
        heights.forEachIndexed { index, barHeight ->
            Box(
                modifier = Modifier
                    .width(if (large) 3.5.dp else 2.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (index < filled) activeColor else inactiveColor),
            )
        }
    }
}
