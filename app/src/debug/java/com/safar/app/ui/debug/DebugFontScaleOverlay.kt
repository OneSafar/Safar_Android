package com.safar.app.ui.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.BuildConfig

@Composable
fun DebugFontScaleOverlay(content: @Composable () -> Unit) {
    if (!BuildConfig.DEBUG) {
        content()
        return
    }
    val scales = listOf(1f, 1.3f, 1.5f, 2f)
    var scaleIndex by remember { mutableIntStateOf(0) }
    val currentScale = scales[scaleIndex]
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = currentScale
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            FloatingActionButton(
                onClick = {
                    scaleIndex = (scaleIndex + 1) % scales.size
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(48.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface
            ) {
                Text(
                    text = "${(currentScale * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}
