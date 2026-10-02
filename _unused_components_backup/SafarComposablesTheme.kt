package com.safarparmar.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalSafarComposablesAccent = staticCompositionLocalOf<Color?> { null }
val LocalSafarComposablesScrim = staticCompositionLocalOf<Color?> { null }

@Composable
fun SafarComposablesTheme(content: @Composable () -> Unit) {
    content()
}
