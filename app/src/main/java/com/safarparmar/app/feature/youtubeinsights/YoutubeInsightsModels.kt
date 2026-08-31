package com.safarparmar.app.feature.youtubeinsights

data class YoutubeTotals(
    val productiveSeconds: Int = 0,
    val distractingSeconds: Int = 0,
    val shortsSeconds: Int = 0,
    val unidentifiedSeconds: Int = 0,
    val protectedProductiveSeconds: Int = 0,
    val protectedDistractingSeconds: Int = 0,
    val protectedShortsSeconds: Int = 0,
    val protectedUnidentifiedSeconds: Int = 0,
)
