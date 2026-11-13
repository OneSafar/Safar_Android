package com.safarparmar.app.ui.calmcentre

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Button as UiButton
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text as UiText
import com.composables.ui.components.Icon as UiIcon

@Composable
internal fun CalmImmersiveSession(practice: CalmPractice, technique: CalmBreathTechnique, minutes: Int,
    colors: CalmColors, elapsed: MutableLongState, playing: Boolean, cue: CalmCue, remaining: String,
    duration: Long, animations: Boolean, onPlayingChange: (Boolean) -> Unit, onChooseAnother: () -> Unit) {
    val title = if (practice == CalmPractice.BREATHE && technique != CalmBreathTechnique.GENTLE) technique.title else practice.title
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artHeight = (maxHeight * 0.30f).coerceIn(180.dp, 300.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colors.stage(practice),
                lerp(colors.stage(practice), colors.background, 0.35f)))), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = colors.ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).semantics { heading() })
                Text("${if (practice == CalmPractice.BREATHE) technique.title else practice.category} · $minutes min",
                    style = MaterialTheme.typography.labelLarge, color = colors.ink,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 18.dp, vertical = 9.dp))
                CalmArtwork(practice, Modifier.fillMaxWidth().height(artHeight), elapsedMillis = { elapsed.longValue },
                    durationMillis = duration, technique = technique, motionEnabled = animations)
                Canvas(Modifier.fillMaxWidth().height(30.dp)) {
                    val wave = Path().apply {
                        moveTo(0f, size.height)
                        cubicTo(size.width * 0.16f, -size.height * 0.2f, size.width * 0.72f, -size.height * 0.2f, size.width, size.height)
                        close()
                    }
                    drawPath(wave, colors.background)
                }
            }
            Column(Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 24.dp).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val progress = {
                    if (!playing || !animations) 1f
                    else if (practice == CalmPractice.BREATHE) calmBreathFrame(elapsed.longValue, technique).progress
                    else calmCueProgress(practice, elapsed.longValue, duration)
                }
                CalmTimedText(if (playing) cue.title else "A moment to pause",
                    MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold), colors.muted, colors.plum,
                    progress, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                CalmTimedText(if (playing) cue.detail else "Pick up where you left off whenever you are ready.",
                    MaterialTheme.typography.bodyLarge, colors.muted, colors.ink, progress, Modifier.heightIn(min = 48.dp))
                Text(remaining, color = colors.ink, style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { contentDescription = "$remaining remaining" })
                Canvas(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp)) {
                    drawRoundRect(colors.ink.copy(alpha = 0.12f), cornerRadius = CornerRadius(size.height))
                    drawRoundRect(colors.plum, size = Size(size.width * elapsed.longValue / duration, size.height), cornerRadius = CornerRadius(size.height))
                }
                UiButton(onClick = { onPlayingChange(!playing) }, style = ButtonStyle.Primary, shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    UiText(if (playing) "Pause practice" else "Resume practice", color = colors.onPlum,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    UiIcon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null,
                        tint = colors.onPlum, modifier = Modifier.size(22.dp))
                }
                Text(if (practice == CalmPractice.BREATHE) technique.description else practice.description,
                    color = colors.muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                UiButton(onClick = onChooseAnother, style = ButtonStyle.Ghost, modifier = Modifier.heightIn(min = 48.dp)) {
                    UiText("Choose another practice", color = colors.plum)
                }
                if (practice == CalmPractice.BREATHE && technique.hold > 0) Text("Prefer no holds? Choose gentle or belly breathing from the catalogue.",
                    color = colors.muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}
