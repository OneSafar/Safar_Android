package com.safarparmar.app.ui.nishtha.goals

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.isLightBackground

/** Shared plum/peach language; status colors remain semantic in both themes. */
object GoalsFlatColors {
    private val dark @Composable get() = !MaterialTheme.colorScheme.background.isLightBackground()
    val Bg @Composable get() = if (dark) BgDark else Color(0xFFFFF5F0)
    // Keep dialogs and sheets at their existing elevation color.
    val ComponentBackground @Composable get() = if (dark) Color(0xFF211921) else Color(0xFFFFF5F0)
    val Paper @Composable get() = if (dark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val Text @Composable get() = if (dark) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val Muted @Composable get() = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Hairline @Composable get() = if (dark) Color(0xFF654D66) else Color(0xFFE8DEE5)
    val Primary @Composable get() = if (dark) Color(0xFFF0BEDC) else Color(0xFF542347)
    val OnPrimary @Composable get() = if (dark) Color(0xFF39132E) else Color.White
    val PrimarySoft @Composable get() = if (dark) Color(0xFF513551) else Color(0xFFEDE0F4)
    val Peach @Composable get() = if (dark) Color(0xFF684047) else Color(0xFFFFCBBE)
    val Done @Composable get() = if (dark) Color(0xFF9FD9C2) else Color(0xFF27634F)
    val Today @Composable get() = Primary
    val Repeat @Composable get() = Primary
    val Scheduled @Composable get() = if (dark) Color(0xFFD2BCE8) else Color(0xFF765185)
    val Progress @Composable get() = Primary
    val Ekagra @Composable get() = if (dark) Color(0xFFFFB89E) else Color(0xFF98523D)
    val Amber @Composable get() = if (dark) Color(0xFFF4CF98) else Color(0xFF94632A)
    val Danger @Composable get() = if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
}
