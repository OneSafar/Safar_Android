package com.safarparmar.app.ui.ekagra.focusshield

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FocusShieldEntryPoint {
    fun focusShieldRepository(): FocusShieldRepository
    fun youtubeInsightsRepository(): com.safarparmar.app.feature.youtubeinsights.YoutubeInsightsRepository
}
