package com.safarparmar.app.performance

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/** Decoration is optional; navigation, touch feedback and functional timers are not. */
@Immutable
data class MotionPolicy(val constrained: Boolean = false, val animationsEnabled: Boolean = true) {
    val decorationsEnabled: Boolean get() = !constrained && animationsEnabled
    val navigationMillis: Int get() = if (!animationsEnabled) 0 else if (constrained) 150 else 200
}

val LocalMotionPolicy = staticCompositionLocalOf { MotionPolicy() }

fun isConstrainedDevice(context: Context): Boolean {
    val manager = context.getSystemService(ActivityManager::class.java) ?: return true
    val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
    return manager.isLowRamDevice || info.totalMem <= 4L * 1024 * 1024 * 1024
}

@Composable
fun ProvideMotionPolicy(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val constrained = remember(context) {
        when (com.safarparmar.app.BuildConfig.PERFORMANCE_EFFECTS) {
            "constrained" -> true
            "full" -> false
            else -> isConstrainedDevice(context)
        }
    }
    var animationsEnabled by remember { mutableStateOf(true) }
    DisposableEffect(context) {
        val resolver = context.contentResolver
        fun update() {
            animationsEnabled = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = update()
        }
        update()
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalMotionPolicy provides MotionPolicy(constrained, animationsEnabled)) {
        ProvideDecorationClock(content)
    }
}

/** Also reads the destination lifecycle, so outgoing/background screens stop decorating. */
@Composable
fun decorativeMotionEnabled(): Boolean {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return LocalMotionPolicy.current.decorationsEnabled && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
}
