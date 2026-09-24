package com.safarparmar.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process-wide activity tracker to safely detect whether the application
 * has an active foreground window/Activity.
 *
 * Used to enforce Android 12+ (API 31+) foreground service restrictions:
 * calling Context.startForegroundService() when no Activity is in foreground
 * throws ForegroundServiceStartNotAllowedException and results in fatal
 * ForegroundServiceDidNotStartInTimeException crashes.
 */
object AppForegroundTracker : Application.ActivityLifecycleCallbacks {

    private val startedActivities = AtomicInteger(0)

    val isAppInForeground: Boolean
        get() = startedActivities.get() > 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities.incrementAndGet()
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
