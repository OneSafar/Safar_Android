package com.safarparmar.app.feature.youtubestudyv2

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Isolates the tutorial from the main task, so only the guide enters PiP. */
class YoutubeFocusTutorialActivity : ComponentActivity() {
    private var startedGuide = false
    private var enteredPip = false
    private var expandedGuide by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val light = intent.getBooleanExtra("light", true)
        setContent {
            MaterialTheme(colorScheme = if (light) lightColorScheme() else darkColorScheme()) {
                if (expandedGuide) {
                    YoutubeFocusAccessibilityTutorialSheet(
                        onDismiss = { finish() },
                        onOpenAccessibilitySettings = { openGuideInSettings() },
                        isLight = light,
                        onContinueGuide = {
                            expandedGuide = false
                            window.decorView.post { openGuideInSettings() }
                        },
                    )
                } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    // Render the same 320dp illustration at every PiP size instead
                    // of reflowing its labels into the tiny window.
                    val pixels = constraints.maxWidth.toFloat()
                    CompositionLocalProvider(LocalDensity provides Density(pixels / 320f, 1f)) {
                        PhoneMockup(light, Modifier.fillMaxSize())
                    }
                }
                }
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (startedGuide) return
        startedGuide = true
        window.decorView.post {
            if (isFinishing || isDestroyed) return@post
            openGuideInSettings()
        }
    }

    private fun openGuideInSettings() {
            enteredPip = runCatching {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(320, 430)).build()
                )
            }.getOrDefault(false)
            runCatching {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { finish() }
            if (!enteredPip) finish()
    }

    override fun onPictureInPictureModeChanged(inPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(inPictureInPictureMode, newConfig)
        // The system expand control restores the instructions and large preview.
        if (enteredPip) expandedGuide = !inPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        // System Close removes the guide completely; never restart it in background.
        if (startedGuide && !isChangingConfigurations) finish()
    }

    companion object {
        fun launch(context: Context, isLight: Boolean): Boolean {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
            return runCatching {
                context.startActivity(Intent(context, YoutubeFocusTutorialActivity::class.java)
                    .putExtra("light", isLight)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
        }
    }
}
