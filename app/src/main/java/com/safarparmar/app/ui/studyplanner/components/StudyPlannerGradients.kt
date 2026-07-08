package com.safarparmar.app.ui.studyplanner.components



import android.graphics.Color as AndroidColor

import androidx.compose.foundation.background

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.widthIn

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.studyplanner.logic.examBadgeLabel

import kotlin.math.max



private val NavyInk = Color(0xFF0F172A)



private fun colorFromHsv(hue: Float, sat: Float, value: Float, alpha: Float = 1f): Color {

    val hsv = floatArrayOf(hue, sat, value)

    val a = (alpha * 255f).toInt().coerceIn(0, 255)

    val argb = AndroidColor.HSVToColor(a, hsv)

    return Color(argb)

}



/**

 * Soft translucent gradient for syllabus subject header strips.

 * Hues are spaced evenly by index so all subjects in a plan look distinct.

 */

fun subjectHeaderBrush(subjectIndex: Int, totalSubjects: Int): Brush {

    val n = max(1, totalSubjects)

    val hue = (subjectIndex * (360f / n)) % 360f

    val c1 = colorFromHsv(hue, 0.38f, 0.96f, 0.20f)

    val c2 = colorFromHsv((hue + 18f) % 360f, 0.48f, 0.90f, 0.26f)

    val c3 = colorFromHsv((hue + 36f) % 360f, 0.42f, 0.94f, 0.16f)

    return Brush.linearGradient(colors = listOf(c1, c2, c3))

}



/**

 * The one canonical color for a subject, parsed from its stored `color` hex field — the
 * same field the PDF export already reads via its own hex parser. Every place that shows
 * a subject (dots, chart bars) should call this instead of deriving a color from the
 * subject's position in a list, so a subject looks identical everywhere, including in
 * the exported PDF.

 */

fun subjectDotColor(colorHex: String): Color = runCatching {

    Color(AndroidColor.parseColor(colorHex))

}.getOrDefault(Color(0xFF0EA5E9))



/**

 * Same hue family as [subjectHeaderBrush], stronger saturation for liquid meter fills.

 */

fun subjectMeterBrush(subjectIndex: Int, totalSubjects: Int): Brush {

    val n = max(1, totalSubjects)

    val hue = (subjectIndex * (360f / n)) % 360f

    val c1 = colorFromHsv(hue, 0.72f, 0.94f, 1f)

    val c2 = colorFromHsv((hue + 28f) % 360f, 0.82f, 0.82f, 1f)

    return Brush.horizontalGradient(colors = listOf(c1, c2))

}



/** Chapter block: teal → emerald (distinct from subject). */

fun chapterHierarchyBrush(): Brush = Brush.linearGradient(

    colors = listOf(

        Color(0x280D9488),

        Color(0x3814B8A6),

        Color(0x22059669),

    ),

)



/** Topic row: amber → rose (distinct from chapter). */

fun topicHierarchyBrush(): Brush = Brush.linearGradient(

    colors = listOf(

        Color(0x26F59E0B),

        Color(0x32EA580C),

        Color(0x28F43F5E),

    ),

)



/**

 * Urgency gradient for exam countdown (days only). More intense warm tones as the exam nears.

 * [days] null = no date; negative = exam passed.

 */

fun examUrgencyBrush(days: Long?): Brush = when {

    days == null -> Brush.linearGradient(

        colors = listOf(

            Color(0xFFE2E8F0),

            Color(0xFFF1F5F9),

            Color(0xFFE8EDF3),

        ),

    )

    days < 0 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFF94A3B8),

            Color(0xFFCBD5E1),

            Color(0xFF64748B),

        ),

    )

    days >= 90 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFF22C55E),

            Color(0xFF14B8A6),

            Color(0xFF3B82F6),

        ),

    )

    days >= 60 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFF34D399),

            Color(0xFF2DD4BF),

            Color(0xFF2563EB),

        ),

    )

    days >= 30 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFF4ADE80),

            Color(0xFF38BDF8),

            Color(0xFF0EA5E9),

        ),

    )

    days >= 14 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFFFBBF24),

            Color(0xFFF97316),

            Color(0xFFEA580C),

        ),

    )

    days >= 7 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFFF97316),

            Color(0xFFEF4444),

            Color(0xFFDC2626),

        ),

    )

    days >= 1 -> Brush.linearGradient(

        colors = listOf(

            Color(0xFFFB923C),

            Color(0xFFF87171),

            Color(0xFFB91C1C),

            Color(0xFF991B1B),

        ),

    )

    else -> Brush.linearGradient(

        colors = listOf(

            Color(0xFFFF6467),

            Color(0xFFDC2626),

            Color(0xFF7F1D1D),

        ),

    )

}



fun examBadgeContentColor(days: Long?): Color = when {
    days == null -> Color(0xFF64748B) // Slate 500
    days < 0 -> Color(0xFF94A3B8) // Slate 400
    days < 10 -> Color(0xFFDC2626) // Red 600
    days < 30 -> Color(0xFFD97706) // Amber 600
    else -> Color(0xFF0284C7) // Sky 600
}

@Composable
fun examBadgeBackgroundColor(days: Long?): Color = when {
    days == null -> Color(0xFFF1F5F9) // Slate 100
    days < 0 -> Color(0xFFF8FAFC) // Slate 50
    days < 10 -> Color(0xFFFEF2F2) // Red 50
    days < 30 -> Color(0xFFFFFBEB) // Amber 50
    else -> Color(0xFFF0F9FF) // Sky 50
}



@Composable

fun ExamDaysCountdownBadge(

    days: Long?,

    modifier: Modifier = Modifier,

) {

    val shape = MaterialTheme.shapes.small

    val label = examBadgeLabel(days)

    Box(

        modifier = modifier
            .background(examBadgeBackgroundColor(days), shape)
            .border(1.dp, examBadgeContentColor(days).copy(alpha = 0.2f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),

        contentAlignment = Alignment.Center,

    ) {

        Text(

            text = label,

            color = examBadgeContentColor(days),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,

            maxLines = 2,

            textAlign = TextAlign.Center,

            lineHeight = 13.sp,

        )

    }

}

