package com.safarparmar.app.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.safarparmar.app.ui.components.popups.SafarDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.safarparmar.app.R

/** Shared practice chrome; feature hosts supply their real canvas and isolated demo. */
@Composable
internal fun FeatureTutorialPracticeWindow(
    @StringRes titleRes: Int,
    @StringRes practiceLabelRes: Int,
    isDarkTheme: Boolean,
    onExit: () -> Unit,
    practiceTag: String,
    labelTag: String,
    closeTag: String,
    onHelp: (() -> Unit)? = null,
    showHeader: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val featureDensity = LocalDensity.current
    CompositionLocalProvider(LocalTitliController provides null) {
    Dialog(
        fullScreen = true,
        onDismissRequest = onExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        CompositionLocalProvider(LocalDensity provides featureDensity) {
        val view = LocalView.current
        DisposableEffect(view, isDarkTheme) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val previousStatusAppearance = controller?.isAppearanceLightStatusBars
            val previousNavigationAppearance = controller?.isAppearanceLightNavigationBars
            controller?.isAppearanceLightStatusBars = !isDarkTheme
            controller?.isAppearanceLightNavigationBars = !isDarkTheme
            onDispose {
                previousStatusAppearance?.let { controller?.isAppearanceLightStatusBars = it }
                previousNavigationAppearance?.let { controller?.isAppearanceLightNavigationBars = it }
            }
        }
        BackHandler(onBack = onExit)
        val windowContent: @Composable () -> Unit = {
            val canvas = if (showHeader) MaterialTheme.colorScheme.background else backgroundColor
            val ink = if (showHeader) MaterialTheme.colorScheme.onSurface else contentColor
            Surface(Modifier.fillMaxSize().testTag(practiceTag), color = canvas, contentColor = ink) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    if (showHeader) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(titleRes), modifier = Modifier.weight(1f).padding(end = 12.dp),
                                fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                            TitliIcon(Modifier.size(32.dp).padding(end = 4.dp))
                            if (onHelp != null) IconButton(onClick = onHelp, modifier = Modifier.testTag("tutorial.help"), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.feature_help_title))
                            }
                            IconButton(onClick = onExit, modifier = Modifier.testTag(closeTag), colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.ekagra_tutorial_done))
                            }
                        }
                        Text(stringResource(practiceLabelRes), modifier = Modifier.testTag(labelTag).padding(top = 4.dp, bottom = 8.dp),
                            fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    content()
                }
            }
        }
        if (showHeader) TutorialVisualStyle { windowContent() } else windowContent()
        }
    }
    }
}
