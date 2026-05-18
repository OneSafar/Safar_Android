package com.safar.app.ui.ekagra.focusshield

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FocusShieldEntryPoint {
    fun focusShieldRepository(): FocusShieldRepository
}
