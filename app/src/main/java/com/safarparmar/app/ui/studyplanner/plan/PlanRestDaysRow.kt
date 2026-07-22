package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.theme.isLightBackground

private val dayIndices = 0..6
private val dayLabelsFull = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val dayLabelsShort = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun PlanRestDaysRow(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.isLightBackground()

    val purpleLight = Color(0xFF7845E5)
    val purpleDark = Color(0xFFA78BFA)
    val activeAccent = if (isLight) purpleLight else purpleDark

    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6))
        )
    }

    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "MY REST DAYS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useShort = maxWidth < 320.dp
            val labels = if (useShort) dayLabelsShort else dayLabelsFull
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dayIndices.forEach { index ->
                    val isSelected = index in selected
                    val shape = CircleShape

                    val bodyColor = if (isSelected) {
                        activeAccent
                    } else {
                        if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E).copy(alpha = 0.65f)
                    }

                    val textColor = if (isSelected) {
                        Color.White
                    } else {
                        if (isLight) Color.Black else Color.White.copy(alpha = 0.7f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .shadow(
                                elevation = if (isSelected) 6.dp else (if (isLight) 2.dp else 4.dp),
                                shape = shape,
                                spotColor = shadowColor,
                                ambientColor = shadowColor,
                            )
                            .clip(shape)
                            .background(bodyColor)
                            .border(
                                width = 0.5.dp,
                                brush = if (isSelected) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))) else borderBrush,
                                shape = shape,
                            )
                            .clickable { onToggle(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = labels[index],
                            fontSize = if (useShort) 11.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
