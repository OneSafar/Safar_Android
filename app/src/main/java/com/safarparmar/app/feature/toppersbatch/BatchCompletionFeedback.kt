package com.safarparmar.app.feature.toppersbatch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Text

data class BatchCompletionFeedback(val lectureId: String, val topic: String, val message: String, val event: Long)

internal fun batchCompletionMessage(lecture: BatchLecture, overview: BatchOverview?, backlog: Boolean, today: String): String {
    val activeIds = overview?.subjects.orEmpty().filter { it.enabled }.map { it.id }.toSet()
    val todayRows = overview?.lectures.orEmpty().filter { it.subjectId in activeIds && it.scheduledFor?.take(10) == today }
    return when {
        lecture.scheduledFor?.take(10) == today && todayRows.isNotEmpty() && todayRows.all { it.completedAt != null } ->
            "All today's lectures completed. Proud of you!"
        backlog -> "Keep it up! You're 1 lecture closer."
        else -> "Congrats! You're consistent and on track."
    }
}

@Composable
internal fun BatchCompletionBanner(feedback: BatchCompletionFeedback) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF15803D)),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Icon(Icons.Default.CheckCircle, null,
                tint = Color(0xFF15803D), modifier = Modifier.size(30.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${feedback.topic} · Done", style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF15803D))
                Text(feedback.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private data class CompletionParticle(
    val right: Boolean, val speedX: Float, val speedY: Float,
    val delay: Float, val spin: Float, val width: Float, val color: Int,
)

@Composable
internal fun BatchCompletionConfetti(event: Long, modifier: Modifier = Modifier) {
    val progress = remember(event) { Animatable(0f) }
    // Linear time drives ballistic arcs; gravity supplies the deceleration and fall.
    LaunchedEffect(event) {
        progress.animateTo(1f, tween(2600, easing = androidx.compose.animation.core.LinearEasing))
    }
    val particles = remember(event) {
        val random = kotlin.random.Random(event)
        List(160) { index -> CompletionParticle(
            right = index % 2 == 0,
            speedX = 0.12f + random.nextFloat() * 0.42f,
            speedY = 1.25f + random.nextFloat() * 0.65f,
            delay = random.nextFloat() * 0.24f,
            spin = (random.nextFloat() - 0.5f) * 720f,
            width = 4f + random.nextFloat() * 4f, color = index % 6,
        ) }
    }
    val colors = remember { listOf(Color(0xFF16A34A), Color(0xFFEC4899), Color(0xFFFBBF24),
        Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFF97316)) }
    Canvas(modifier) {
        val elapsed = progress.value * 2.6f
        if (progress.value > 0f && progress.value < 1f) particles.forEach { particle ->
            val time = elapsed - particle.delay
            if (time >= 0f) {
                val direction = if (particle.right) -1f else 1f
                val x = size.width * (if (particle.right) 0.92f else 0.08f) +
                    direction * particle.speedX * size.width * time +
                    kotlin.math.sin(time * 9f + particle.spin) * 8.dp.toPx()
                val y = size.height * (1.02f - particle.speedY * time + 0.85f * time * time)
                val alpha = ((2.6f - elapsed) / 0.6f).coerceIn(0f, 1f)
                val center = Offset(x, y)
                withTransform({ rotate(particle.spin * time, center) }) {
                    val width = particle.width.dp.toPx()
                    val flutter = (0.3f + 0.7f * kotlin.math.abs(kotlin.math.cos(time * 12f + particle.spin)))
                    drawRect(colors[particle.color].copy(alpha = alpha),
                        Offset(x - width / 2, y), Size(width, width * 1.6f * flutter))
                }
            }
        }
    }
}
