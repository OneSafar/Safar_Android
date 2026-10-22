package com.safarparmar.app.ui.studyplanner.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.safarparmar.app.performance.rememberDecorationPhase
import com.safarparmar.app.ui.components.SafarButton
import kotlin.math.PI
import kotlin.math.sin

/* Hallmark · component: navigation button · genre: playful · theme: SAFAR / reference pink
 * Pre-emit critique: P4 H5 E4 S5 R4 V4. Existing button owns focus, press and disabled semantics.
 * Loading/error/success remain owned by the destination; navigation has no pending state.
 */
internal object Gk5ButtonColors {
    val blossom = Color(0xFFFFB9E4)
    val rose = Color(0xFFF58ACC)
    val orchid = Color(0xFFE16CB8)
    val berry = Color(0xFFB33489)
    val ink = Color(0xFF35102C)
    val white = Color.White
}

/** The reference's moving light and drifting dots, with stationary, high-contrast labels. */
@Composable
internal fun PlannerGk5Button(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Lets Compose previews inspect real drawing at a deterministic point in its cycle.
    decorationPhase: (() -> Float)? = null,
) {
    val phase = decorationPhase ?: if (enabled) rememberDecorationPhase(9_000) else ({ 0f })
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val shape = RoundedCornerShape(28.dp)
    SafarButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Gk5ButtonColors.ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Gk5ButtonColors.ink,
        ),
        border = BorderStroke(if (focused) 2.dp else 1.dp,
            if (focused) Gk5ButtonColors.ink else Gk5ButtonColors.white.copy(alpha = 0.24f)),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactions,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                .gk5SparkleBackground(phase, hovered && enabled)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(Gk5ButtonColors.white.copy(alpha = 0.26f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.School, contentDescription = null,
                    tint = Gk5ButtonColors.ink, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Gk5ButtonColors.ink,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Gk5ButtonColors.ink,
                    style = MaterialTheme.typography.bodySmall)
            }
            Box(
                Modifier.size(42.dp).background(Gk5ButtonColors.white, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = Gk5ButtonColors.ink, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun Modifier.gk5SparkleBackground(phase: () -> Float, hovered: Boolean) = drawWithCache {
    val fill = Brush.horizontalGradient(
        0f to Gk5ButtonColors.blossom,
        0.48f to Gk5ButtonColors.rose,
        0.86f to Gk5ButtonColors.orchid,
        1f to Gk5ButtonColors.berry,
        startX = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f,
        endX = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width,
    )
    // Allocate brushes on size changes; animation state is read only during drawing.
    val lightRadius = size.width * 0.55f
    val light = Brush.radialGradient(
        listOf(Gk5ButtonColors.white.copy(alpha = 0.24f), Color.Transparent),
        center = Offset.Zero, radius = lightRadius.coerceAtLeast(1f),
    )
    val dotRadius = 1.4.dp.toPx()
    onDrawBehind {
        val progress = phase()
        val angle = progress * (2 * PI).toFloat()
        drawRect(fill)
        translate(left = size.width * (0.5f + 0.34f * sin(angle)), top = size.height * 0.2f) {
            drawCircle(light, radius = lightRadius, center = Offset.Zero)
        }
        repeat(16) { index ->
            val fraction = (index * 0.618034f + progress) % 1f
            val baseY = 0.14f + ((index * 37 % 83) / 83f) * 0.72f
            val drift = sin(angle + index * 1.9f) * size.height * 0.045f
            val edgeFade = (fraction * (1f - fraction) * 6f).coerceIn(0f, 1f)
            drawCircle(
                color = Gk5ButtonColors.white,
                radius = dotRadius * (0.65f + (index % 3) * 0.3f),
                center = Offset(fraction * size.width, baseY * size.height + drift),
                alpha = edgeFade * (0.22f + 0.22f * sin(angle + index).let { it * it }),
            )
        }
        if (hovered) drawRect(Gk5ButtonColors.white.copy(alpha = 0.08f))
    }
}
