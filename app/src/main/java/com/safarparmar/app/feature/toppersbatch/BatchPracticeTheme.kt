package com.safarparmar.app.feature.toppersbatch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.composables.ui.theme.ColorScheme
import com.composables.ui.theme.LocalColorScheme
import com.safarparmar.app.ui.theme.LocalSafarComposablesAccent
import com.safarparmar.app.ui.theme.SafarComposablesTheme

/** Same palette as the tracker; bridge Material and Unstyled ink for its native text. */
@Composable
internal fun BatchPracticeTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme.batchTrackerColors(dark)
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            LocalColorScheme provides if (dark) ColorScheme.Dark else ColorScheme.Light,
            LocalSafarComposablesAccent provides colors.primary,
        ) {
            // buildTheme installs its own content-color local; bridge ink inside it.
            SafarComposablesTheme {
                CompositionLocalProvider(
                    com.composeunstyled.LocalContentColor provides colors.onSurface,
                    androidx.compose.material3.LocalContentColor provides colors.onSurface,
                ) { content() }
            }
        }
    }
}
