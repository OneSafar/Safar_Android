package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.composables.ui.components.Text
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import kotlinx.coroutines.delay

data class BatchCompletionFeedback(val lectureId: String, val topic: String, val message: BatchNotice, val event: Long)

internal fun batchCompletionMessage(lecture: BatchLecture, overview: BatchOverview?, backlog: Boolean, today: String): BatchNotice {
    val activeIds = overview?.subjects.orEmpty().filter { it.enabled }.map { it.id }.toSet()
    val todayRows = overview?.lectures.orEmpty().filter { it.subjectId in activeIds && it.scheduledFor?.take(10) == today }
    return when {
        lecture.scheduledFor?.take(10) == today && todayRows.isNotEmpty() && todayRows.all { it.completedAt != null } ->
            BatchNotice(R.string.toppers_batch_all_today_s_lectures_completed_proud_of_you)
        backlog -> BatchNotice(R.string.toppers_batch_keep_it_up_you_re_1_lecture_closer)
        else -> BatchNotice(R.string.toppers_batch_congrats_you_re_consistent_and_on_track)
    }
}

@Composable
internal fun BatchCompletionBanner(feedback: BatchCompletionFeedback, enabled: Boolean, onUndo: () -> Unit) {
    val strings = rememberBatchStrings()
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Icon(Icons.Default.CheckCircle, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(strings.text(R.string.toppers_batch_count_completed, feedback.topic), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(strings.notice(feedback.message), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = batchFeatureAction(onUndo), enabled = enabled, style = ButtonStyle.Ghost,
                modifier = Modifier.heightIn(min = 48.dp)) { Text(strings.text(R.string.toppers_batch_undo)) }
        }
    }
}

@Composable
internal fun BatchCompletionCelebrationOverlay(
    feedback: BatchCompletionFeedback,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = rememberBatchStrings()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let { win ->
                win.setDimAmount(0.20f)
                win.setBackgroundDrawableResource(android.R.color.transparent)
            }
            onDispose {}
        }

        LaunchedEffect(feedback.event) {
            delay(4200)
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.TopCenter,
        ) {
            BatchCompletionConfetti(feedback.event, Modifier.fillMaxSize())

            Card(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* prevent dismiss when tapping the card itself */ },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = strings.text(R.string.toppers_batch_lecture_completed_2),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = feedback.topic,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = batchFeatureAction(onDismiss),
                            modifier = Modifier.size(36.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.text(R.string.toppers_batch_close),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = strings.notice(feedback.message),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (canUndo) {
                            Button(
                                onClick = batchFeatureAction(onUndo),
                                style = ButtonStyle.Ghost,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            ) {
                                Text(strings.text(R.string.toppers_batch_undo))
                            }
                        }
                        Button(
                            onClick = batchFeatureAction(onDismiss),
                            style = ButtonStyle.Primary,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        ) {
                            Text(strings.text(R.string.toppers_batch_awesome))
                        }
                    }
                }
            }
        }
    }
}

private data class CompletionParticle(
    val right: Boolean, val speedX: Float, val speedY: Float,
    val delay: Float, val spin: Float, val width: Float, val color: Int,
    val isDrop: Boolean = false, val startXRatio: Float = 0.5f,
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
        List(180) { index ->
            val isDrop = index % 3 == 0
            CompletionParticle(
                right = index % 2 == 0,
                speedX = 0.12f + random.nextFloat() * 0.42f,
                speedY = if (isDrop) (0.35f + random.nextFloat() * 0.45f) else (1.25f + random.nextFloat() * 0.65f),
                delay = random.nextFloat() * 0.30f,
                spin = (random.nextFloat() - 0.5f) * 720f,
                width = 4f + random.nextFloat() * 5f,
                color = index % 6,
                isDrop = isDrop,
                startXRatio = random.nextFloat(),
            )
        }
    }
    val colors = remember { listOf(Color(0xFF16A34A), Color(0xFFEC4899), Color(0xFFFBBF24),
        Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFF97316)) }
    Canvas(modifier) {
        val elapsed = progress.value * 2.6f
        if (progress.value > 0f && progress.value < 1f) particles.forEach { particle ->
            val time = elapsed - particle.delay
            if (time >= 0f) {
                val x: Float
                val y: Float
                if (particle.isDrop) {
                    x = size.width * particle.startXRatio + kotlin.math.sin(time * 6f + particle.spin) * 12.dp.toPx()
                    y = -0.04f * size.height + particle.speedY * size.height * time
                } else {
                    val direction = if (particle.right) -1f else 1f
                    x = size.width * (if (particle.right) 0.92f else 0.08f) +
                        direction * particle.speedX * size.width * time +
                        kotlin.math.sin(time * 9f + particle.spin) * 8.dp.toPx()
                    y = size.height * (1.02f - particle.speedY * time + 0.85f * time * time)
                }
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
