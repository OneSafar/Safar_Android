package com.safarparmar.app.ui.calmcentre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Universal Lottie animation player for Headspace Calm Centre assets.
 * Seamlessly plays JSON animations packaged inside assets/animations/.
 */
@Composable
fun CalmAssetAnimation(
    assetPath: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1.0f,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: (@Composable () -> Unit)? = null,
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val composition = compositionResult.value

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                isPlaying = isPlaying,
                iterations = iterations,
                speed = speed,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            fallback?.invoke()
        }
    }
}

