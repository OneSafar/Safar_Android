package com.safar.app.ui.butterfly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import com.safar.app.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * A single-shot nudge banner with a small animated butterfly icon.
 * Good for contextual hints AFTER onboarding (e.g. "✨ Try long-pressing a card!").
 *
 * Place it near the top of a screen inside a Box/Column.
 * It auto-dismisses after [autoDismissMs] ms.
 *
 * Example:
 * ```kotlin
 * ButterflyNudge(
 *     message = "Long-press any card to pin it!",
 *     visible = showNudge,
 *     onDismiss = { showNudge = false },
 * )
 * ```
 */
@Composable
fun ButterflyNudge(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    wingColor: Color = ButterflyWing,
    bodyColor: Color = ButterflyBody,
    autoDismissMs: Long = 4000L,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(visible) {
        if (visible && autoDismissMs > 0L) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(ButterflyCardBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mini butterfly
            ButterflyDrawing(
                wingColor = wingColor,
                bodyColor = bodyColor,
                modifier = Modifier.size(36.dp),
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = message,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = ButterflyTextDark,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(6.dp))

            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Text("✕", fontSize = 12.sp, color = ButterflyGold)
            }
        }
    }
}
